import os
import json
from pathlib import Path
from typing import List, Dict, Any, Union, Tuple
from contextlib import asynccontextmanager

import requests
from fastapi import FastAPI, Body, HTTPException
from pydantic import BaseModel

try:
    import joblib
except Exception:  # pragma: no cover - optional dependency
    joblib = None

BASE_DIR = Path(__file__).resolve().parent
REPO_ROOT = BASE_DIR.parent
# Buildd30_7 is the actual root where model files are stored
BUILDD30_7_ROOT = REPO_ROOT.parent if REPO_ROOT.name == "Buildd43" else REPO_ROOT

# Model file paths with fallbacks
DEFAULT_MODEL_FALLBACKS = [
    BASE_DIR / "recommendation_model.pkl",
    BUILDD30_7_ROOT / "recommendation_model.pkl",
    REPO_ROOT / "recommendation_model.pkl",
    REPO_ROOT / "Buildd43" / "project" / "project" / "src" / "main" / "resources" / "recommendation_model.pkl",
    REPO_ROOT / "Buildd43" / "project" / "src" / "main" / "resources" / "recommendation_model.pkl",
]
DEFAULT_METADATA_FALLBACKS = [
    BASE_DIR / "model_metadata.json",
    BUILDD30_7_ROOT / "model_metadata.json",
    REPO_ROOT / "model_metadata.json",
    REPO_ROOT / "Buildd43" / "project" / "project" / "src" / "main" / "resources" / "model_metadata.json",
    REPO_ROOT / "Buildd43" / "project" / "src" / "main" / "resources" / "model_metadata.json",
]


def _resolve_path(env_value: str | None, fallbacks: List[Path]) -> str:
    if env_value:
        path = Path(env_value).expanduser().resolve()
        if path.is_file():
            return str(path)
    for candidate in fallbacks:
        if candidate.is_file():
            return str(candidate.resolve())
    # Return last fallback even if missing so we can surface helpful info
    return str(fallbacks[-1].resolve())


MODEL_PATH = _resolve_path(os.getenv("MODEL_PATH"), DEFAULT_MODEL_FALLBACKS)
METADATA_PATH = _resolve_path(os.getenv("MODEL_METADATA_PATH"), DEFAULT_METADATA_FALLBACKS)

# Label encoder paths
USER_ENCODER_FALLBACKS = [
    BUILDD30_7_ROOT / "user_label_encoder.joblib",
    REPO_ROOT / "user_label_encoder.joblib",
    BASE_DIR / "user_label_encoder.joblib",
]
ITEM_ENCODER_FALLBACKS = [
    BUILDD30_7_ROOT / "item_label_encoder.joblib",
    REPO_ROOT / "item_label_encoder.joblib",
    BASE_DIR / "item_label_encoder.joblib",
]
USER_ENCODER_PATH = _resolve_path(os.getenv("USER_ENCODER_PATH"), USER_ENCODER_FALLBACKS)
ITEM_ENCODER_PATH = _resolve_path(os.getenv("ITEM_ENCODER_PATH"), ITEM_ENCODER_FALLBACKS)

# Service URLs
PRODUCT_SERVICE_URL = os.getenv("PRODUCT_SERVICE_BASE_URL", "http://localhost:8083").rstrip("/")
REVIEW_SERVICE_URL = os.getenv("REVIEW_SERVICE_BASE_URL", "http://localhost:8095").rstrip("/")
FAVORITES_SERVICE_URL = os.getenv("FAVORITES_SERVICE_BASE_URL", "http://localhost:8096").rstrip("/")
DEFAULT_CANDIDATE_POOL = int(os.getenv("RECO_CANDIDATE_POOL", "120"))
MAX_LIMIT = 100


class PredictRequest(BaseModel):
    userId: str
    limit: int = 10
    productIds: List[Union[str, int]] | None = None


model: Any | None = None
model_loaded: bool = False
user_encoder: Any | None = None
item_encoder: Any | None = None
encoders_loaded: bool = False


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifespan event handler for loading model on startup."""
    global model, model_loaded, user_encoder, item_encoder, encoders_loaded
    import logging
    logger = logging.getLogger(__name__)
    
    # Startup: Load model
    try:
        # Load model
        path = Path(MODEL_PATH)
        logger.info(f"Attempting to load model from: {path}")
        if joblib is not None and path.is_file() and path.stat().st_size > 0:
            logger.info(f"Model file exists, size: {path.stat().st_size / (1024*1024):.2f} MB")
            try:
                model = joblib.load(path)
                model_loaded = True
                logger.info("Model loaded successfully")
            except Exception as e:
                logger.error(f"Failed to load model: {e}")
                model = None
                model_loaded = False
        else:
            logger.warning(f"Model file not found or invalid: {path}")
            logger.warning("Service will run in fallback mode (deterministic scoring)")
            model = None
            model_loaded = False
        
        # Load label encoders
        user_path = Path(USER_ENCODER_PATH)
        item_path = Path(ITEM_ENCODER_PATH)
        logger.info(f"Attempting to load encoders from: {user_path}, {item_path}")
        if joblib is not None and user_path.is_file() and item_path.is_file():
            try:
                logger.info("Loading user encoder...")
                user_encoder = joblib.load(user_path)
                logger.info("Loading item encoder...")
                item_encoder = joblib.load(item_path)
                encoders_loaded = True
                logger.info("Encoders loaded successfully")
            except Exception as e:
                logger.error(f"Failed to load encoders: {e}")
                user_encoder = None
                item_encoder = None
                encoders_loaded = False
        else:
            logger.warning(f"Encoder files not found: user={user_path.exists()}, item={item_path.exists()}")
            user_encoder = None
            item_encoder = None
            encoders_loaded = False
    except Exception as e:
        logger.error(f"Error during model/encoder loading: {e}")
        model = None
        model_loaded = False
        user_encoder = None
        item_encoder = None
        encoders_loaded = False
    
    yield  # Application runs here
    
    # Shutdown: Cleanup (if needed)
    logger.info("Shutting down recommendation service")


app = FastAPI(title="Recommendation Inference Service", version="0.2.0", lifespan=lifespan)


def _safe_hash_score(user_id: str, product_id: str) -> float:
    """Deterministic pseudo-score 3.0 - 5.0 for fallback when model is missing."""
    uh = abs(hash(user_id))
    ph = abs(hash(product_id))
    base = 3.0 + (uh % 3) * 0.5
    var = (ph % 10) * 0.1
    return min(5.0, base + var)


def _predict_rating(user_id: str, product_id: str) -> Tuple[float, bool]:
    """
    COLLABORATIVE FILTERING - Predict rating using trained model (AI-based)
    
    Supports two model types:
    1. Surprise library SVD model (has .predict() method)
    2. Simple CF model (dict with biases - our new model)
    
    Returns: (predicted_rating, used_model)
    - predicted_rating: Rating từ 1.0 đến 5.0 (scale của model)
    - used_model: True nếu sử dụng AI model, False nếu dùng fallback
    """
    if model_loaded and model is not None:
        try:
            # Check if model is Simple CF (dictionary with biases)
            if isinstance(model, dict) and 'type' in model and model['type'] == 'simple_cf':
                try:
                    if encoders_loaded and user_encoder is not None and item_encoder is not None:
                        try:
                            # Convert string IDs to internal IDs using encoders
                            user_internal = user_encoder.transform([user_id])[0]
                            item_internal = item_encoder.transform([product_id])[0]
                            
                            # Simple CF Prediction: global_mean + user_bias + item_bias
                            score = model['global_mean']
                            
                            # Add user bias if exists
                            if user_internal in model['user_biases']:
                                score += model['user_biases'][user_internal]
                            
                            # Add item bias if exists
                            if item_internal in model['item_biases']:
                                score += model['item_biases'][item_internal]
                            
                            # Ensure score is in valid range (1-5)
                            score = max(1.0, min(5.0, score))
                            
                            return score, True  # Model prediction successful
                        except (ValueError, KeyError, IndexError):
                            # Cold start: User or product not in training data
                            pass
                except Exception as e:
                    import logging
                    logging.getLogger(__name__).debug(f"Simple CF prediction error: {e}")
            
            # Check if model is from surprise library (SVD)
            elif hasattr(model, "predict"):
                try:
                    if encoders_loaded and user_encoder is not None and item_encoder is not None:
                        try:
                            # COLLABORATIVE FILTERING: Convert string IDs to internal IDs using encoders
                            user_internal = user_encoder.transform([user_id])[0]
                            item_internal = item_encoder.transform([product_id])[0]
                            
                            # AI Prediction: SVD model phân tích patterns và predict rating
                            pred = model.predict(user_internal, item_internal)
                            score = float(getattr(pred, "est", 0.0))
                            
                            # Ensure score is in valid range (1-5)
                            score = max(1.0, min(5.0, score))
                            
                            if score > 0:
                                return score, True  # AI model prediction successful
                        except (ValueError, KeyError, IndexError):
                            # Cold start: User or product not in training data
                            pass
                        except Exception as e:
                            import logging
                            logging.getLogger(__name__).debug(f"Surprise prediction error: {e}")
                except Exception:
                    pass
        except Exception:
            pass
    
    # Fallback: deterministic hash score (for cold start cases)
    return _safe_hash_score(user_id, product_id), False


def _rank_products_collaborative_filtering(user_id: str, product_ids: List[str], limit: int) -> Tuple[List[Dict[str, Any]], bool]:
    """
    COLLABORATIVE FILTERING - Rank products using AI model (SVD)
    
    Logic AI:
    1. Predict rating cho mỗi product bằng SVD model (phân tích patterns từ hành vi users tương tự)
    2. Rank products theo predicted rating (cao → thấp)
    3. Return top N products có rating cao nhất
    
    Model đã được train từ reviews/ratings thực tế nên sẽ predict ratings chính xác
    dựa trên hành vi của users tương tự (Collaborative Filtering principle).
    """
    scored: List[Dict[str, Any]] = []
    model_used = False
    
    for product_id in product_ids:
        # AI Prediction: Sử dụng SVD model để predict rating
        score, used_model = _predict_rating(user_id, product_id)
        if used_model:
            model_used = True
        
        # Store score (1-5 scale from model)
        scored.append({
            "productId": product_id, 
            "score": score,
            "normalizedScore": score / 5.0  # Normalize to 0-1 for consistency
        })
    
    # Sort by predicted rating (highest first) - AI ranking
    scored.sort(key=lambda x: x["score"], reverse=True)
    
    # Return top N recommendations
    return scored[:limit], not model_used  # fallback_used = not model_used


def _fetch_candidate_product_ids(limit: int) -> List[str]:
    """Fetch candidate products from Product Service."""
    size = max(limit, DEFAULT_CANDIDATE_POOL)
    url = f"{PRODUCT_SERVICE_URL}/api/products?page=0&size={size}"
    try:
        resp = requests.get(url, timeout=5)
        resp.raise_for_status()
        data = resp.json()
        content = data.get("content") or data.get("data") or data
        product_ids: List[str] = []

        if isinstance(content, list):
            for item in content:
                pid = item.get("id") if isinstance(item, dict) else None
                if pid is not None:
                    product_ids.append(str(pid))
        elif isinstance(content, dict) and "items" in content:
            for item in content["items"]:
                pid = item.get("id")
                if pid is not None:
                    product_ids.append(str(pid))

        if product_ids:
            return product_ids
    except Exception:
        pass

    return []


def _get_user_interested_products(user_id: str) -> List[str]:
    """
    Get products user has interacted with (reviews, favorites, orders).
    
    Used for:
    1. Cold start detection (new users with no history)
    2. Filtering out products user already knows
    3. Finding similar products for Content-Based Filtering
    """
    interested_products: set = set()
    
    # From reviews (explicit feedback)
    try:
        url = f"{REVIEW_SERVICE_URL}/api/reviews/user/{user_id}"
        resp = requests.get(url, timeout=3)
        if resp.status_code == 200:
            reviews = resp.json()
            if isinstance(reviews, list):
                for review in reviews:
                    pid = review.get("productId") if isinstance(review, dict) else None
                    if pid:
                        interested_products.add(str(pid))
    except Exception:
        pass
    
    # From favorites (implicit feedback)
    try:
        url = f"{FAVORITES_SERVICE_URL}/api/favorites/user/{user_id}"
        resp = requests.get(url, timeout=3)
        if resp.status_code == 200:
            favorites = resp.json()
            if isinstance(favorites, list):
                for fav in favorites:
                    pid = fav.get("productId") if isinstance(fav, dict) else None
                    if pid:
                        interested_products.add(str(pid))
    except Exception:
        pass
    
    return list(interested_products)


@app.get("/health")
def health() -> Dict[str, Any]:
    """Health check endpoint."""
    metadata = {}
    try:
        if Path(METADATA_PATH).is_file():
            with open(METADATA_PATH, 'r', encoding='utf-8') as f:
                metadata = json.load(f)
    except Exception:
        pass
    
    return {
        "status": "healthy",
        "modelLoaded": model_loaded,
        "encodersLoaded": encoders_loaded,
        "metadata": metadata,
        "modelPath": MODEL_PATH,
        "userEncoderPath": USER_ENCODER_PATH,
        "itemEncoderPath": ITEM_ENCODER_PATH,
    }


@app.post("/recommend")
def recommend(body: PredictRequest = Body(...)) -> Dict[str, Any]:
    """
    COLLABORATIVE FILTERING - AI-based Product Recommendation
    
    Sử dụng AI (SVD - Singular Value Decomposition) để phân tích hành vi người dùng
    và gợi ý sản phẩm dựa trên Collaborative Filtering:
    
    - Phân tích patterns từ reviews/ratings của users tương tự
    - Predict rating mà user sẽ đánh giá cho mỗi product
    - Rank và recommend top N products có predicted rating cao nhất
    
    Logic:
    1. Lấy candidate products (từ user history hoặc popular products)
    2. Predict rating cho mỗi product bằng SVD model (AI prediction)
    3. Rank products theo predicted rating (cao → thấp)
    4. Trả về top N products có rating cao nhất
    """
    user_id = str(body.userId)
    limit = max(1, min(MAX_LIMIT, body.limit or 10))
    product_ids = [str(pid) for pid in (body.productIds or [])]

    # Nếu không có productIds, lấy candidate products
    if not product_ids:
        # Lấy products user đã quan tâm (để detect cold start)
        interested_products = _get_user_interested_products(user_id)
        
        if interested_products:
            # User có lịch sử -> lấy products tương tự (không phải cold start)
            all_products = _fetch_candidate_product_ids(limit * 10)
            # Loại bỏ products user đã quan tâm (tránh recommend lại)
            candidate_products = [pid for pid in all_products if pid not in interested_products]
            product_ids = candidate_products if candidate_products else all_products
        else:
            # COLD START: User mới chưa có hành vi
            # -> lấy popular products (fallback strategy)
            product_ids = _fetch_candidate_product_ids(limit * 5)
        
        if not product_ids:
            raise HTTPException(status_code=503, detail="Unable to fetch candidate products")

    # COLLABORATIVE FILTERING: Predict ratings và rank
    # Model đã được train từ reviews/ratings thực tế nên sẽ predict chính xác
    ranked, fallback_used = _rank_products_collaborative_filtering(user_id, product_ids, limit)
    
    return {
        "recommendations": ranked,
        "modelLoaded": model_loaded,
        "fallbackUsed": fallback_used,
        "method": "Collaborative Filtering" if model_loaded and not fallback_used else "Fallback",
    }


@app.get("/recommendations/user/{user_id}")
def recommend_for_user(user_id: str, limit: int = 10) -> Dict[str, Any]:
    """GET endpoint for recommendations."""
    limit = max(1, min(MAX_LIMIT, limit))
    
    # Lấy products user đã quan tâm
    interested_products = _get_user_interested_products(user_id)
    
    if interested_products:
        all_products = _fetch_candidate_product_ids(limit * 10)
        candidate_products = [pid for pid in all_products if pid not in interested_products]
        product_ids = candidate_products if candidate_products else all_products
    else:
        product_ids = _fetch_candidate_product_ids(limit * 5)
    
    ranked, fallback_used = _rank_products_collaborative_filtering(str(user_id), product_ids, limit)
    return {
        "recommendations": ranked,
        "modelLoaded": model_loaded,
        "fallbackUsed": fallback_used,
        "method": "Collaborative Filtering" if model_loaded and not fallback_used else "Fallback",
    }


if __name__ == "__main__":  # pragma: no cover
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("PORT", "8000")))
