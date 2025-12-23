"""
Train recommendation model from extracted training data
This script trains a new model from your actual database reviews/ratings
"""
import pandas as pd
import joblib
import json
from datetime import datetime
from pathlib import Path
from surprise import Dataset, Reader, SVD
from surprise.model_selection import train_test_split
from surprise import accuracy
from sklearn.preprocessing import LabelEncoder
import numpy as np

# Configuration
TRAINING_DATA_PATH = Path(__file__).parent / "training_data.csv"
OUTPUT_DIR = Path(__file__).parent.parent.parent  # Buildd30_7 directory
MODEL_PATH = OUTPUT_DIR / "recommendation_model.pkl"
METADATA_PATH = OUTPUT_DIR / "model_metadata.json"
USER_ENCODER_PATH = OUTPUT_DIR / "user_label_encoder.joblib"
ITEM_ENCODER_PATH = OUTPUT_DIR / "item_label_encoder.joblib"


def load_training_data(csv_path):
    """Load training data from CSV"""
    print(f"📂 Loading training data from {csv_path}...")
    
    if not csv_path.exists():
        raise FileNotFoundError(
            f"Training data not found: {csv_path}\n"
            f"Please run extract_training_data.py first!"
        )
    
    df = pd.read_csv(csv_path)
    print(f"   ✅ Loaded {len(df):,} ratings")
    print(f"   Users: {df['userId'].nunique():,}")
    print(f"   Products: {df['productId'].nunique():,}")
    
    return df


def prepare_data(df):
    """Prepare data for training"""
    print("\n🔄 Preparing data for training...")
    
    # Ensure userId and productId are strings
    df['userId'] = df['userId'].astype(str)
    df['productId'] = df['productId'].astype(str)
    
    # Create label encoders for user and product IDs
    print("   Creating label encoders...")
    user_encoder = LabelEncoder()
    item_encoder = LabelEncoder()
    
    # Fit encoders
    df['userId_encoded'] = user_encoder.fit_transform(df['userId'])
    df['productId_encoded'] = item_encoder.fit_transform(df['productId'])
    
    print(f"   ✅ Encoded {len(user_encoder.classes_):,} users")
    print(f"   ✅ Encoded {len(item_encoder.classes_):,} products")
    
    return df, user_encoder, item_encoder


def train_model(df, user_encoder, item_encoder):
    """Train SVD model"""
    print("\n🤖 Training SVD recommendation model...")
    
    # Prepare data for Surprise library
    reader = Reader(rating_scale=(1, 5))
    data = Dataset.load_from_df(
        df[['userId_encoded', 'productId_encoded', 'rating']], 
        reader
    )
    
    # Split data (80% train, 20% test)
    trainset, testset = train_test_split(data, test_size=0.2, random_state=42)
    print(f"   Training set: {trainset.n_users:,} users, {trainset.n_items:,} items")
    print(f"   Test set: {len(testset):,} ratings")
    
    # Train SVD model
    print("   Training SVD model (this may take a few minutes)...")
    model = SVD(
        n_factors=50,
        n_epochs=20,
        lr_all=0.005,
        reg_all=0.02,
        random_state=42
    )
    model.fit(trainset)
    
    # Test the model
    print("   Testing model...")
    predictions = model.test(testset)
    rmse = accuracy.rmse(predictions, verbose=False)
    mae = accuracy.mae(predictions, verbose=False)
    
    print(f"\n   ✅ Model training completed!")
    print(f"   📊 Performance Metrics:")
    print(f"      RMSE: {rmse:.4f}")
    print(f"      MAE: {mae:.4f}")
    
    return model, rmse, mae


def save_model_and_metadata(model, user_encoder, item_encoder, df, rmse, mae):
    """Save model, encoders, and metadata"""
    print("\n💾 Saving model and metadata...")
    
    # Ensure output directory exists
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    
    # Save model
    joblib.dump(model, MODEL_PATH)
    print(f"   ✅ Model saved: {MODEL_PATH}")
    
    # Save encoders
    joblib.dump(user_encoder, USER_ENCODER_PATH)
    joblib.dump(item_encoder, ITEM_ENCODER_PATH)
    print(f"   ✅ User encoder saved: {USER_ENCODER_PATH}")
    print(f"   ✅ Item encoder saved: {ITEM_ENCODER_PATH}")
    
    # Calculate statistics
    n_users = df['userId'].nunique()
    n_products = df['productId'].nunique()
    n_ratings = len(df)
    avg_rating = df['rating'].mean()
    
    # Calculate sparsity
    total_possible = n_users * n_products
    sparsity = (1 - n_ratings / total_possible) * 100 if total_possible > 0 else 0
    
    # Create metadata
    metadata = {
        "model_info": {
            "type": "SVD",
            "n_factors": 50,
            "n_epochs": 20,
            "learning_rate": 0.005,
            "regularization": 0.02,
            "trained_on": "Your Database Reviews/Ratings"
        },
        "performance": {
            "rmse": float(rmse),
            "mae": float(mae)
        },
        "data_info": {
            "source": "Your Database (product_reviews, orders, user_behaviors)",
            "n_users": int(n_users),
            "n_products": int(n_products),
            "n_ratings": int(n_ratings),
            "avg_rating": float(avg_rating),
            "sparsity": float(sparsity),
            "cleaned_shape": [int(n_ratings), 3]
        },
        "created_at": datetime.now().isoformat(),
        "notes": "Model trained from your actual database reviews/ratings. "
                 "Model will fall back to deterministic scoring when a prediction cannot be made."
    }
    
    # Save metadata
    with open(METADATA_PATH, 'w', encoding='utf-8') as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)
    print(f"   ✅ Metadata saved: {METADATA_PATH}")
    
    # Display summary
    print(f"\n📊 Model Summary:")
    print(f"   Model Type: SVD")
    print(f"   RMSE: {rmse:.4f}")
    print(f"   MAE: {mae:.4f}")
    print(f"   Users: {n_users:,}")
    print(f"   Products: {n_products:,}")
    print(f"   Ratings: {n_ratings:,}")
    print(f"   Avg Rating: {avg_rating:.2f}")
    print(f"   Sparsity: {sparsity:.2f}%")
    print(f"\n   ✅ Model export completed!")
    print(f"\n   📍 Model files saved to: {OUTPUT_DIR}")
    print(f"      - recommendation_model.pkl")
    print(f"      - user_label_encoder.joblib")
    print(f"      - item_label_encoder.joblib")
    print(f"      - model_metadata.json")


def main():
    """Main execution"""
    print("=" * 70)
    print("🚀 TRAINING RECOMMENDATION MODEL FROM YOUR DATABASE")
    print("=" * 70)
    
    try:
        # Load training data
        df = load_training_data(TRAINING_DATA_PATH)
        
        if len(df) < 100:
            print("\n⚠️  WARNING: Very little training data!")
            print(f"   Only {len(df)} ratings found.")
            print("   Model quality will be poor. Need at least 1000+ ratings.")
            response = input("\n   Continue anyway? (y/N): ")
            if response.lower() != 'y':
                print("   Training cancelled.")
                return
        
        # Prepare data
        df, user_encoder, item_encoder = prepare_data(df)
        
        # Train model
        model, rmse, mae = train_model(df, user_encoder, item_encoder)
        
        # Save everything
        save_model_and_metadata(model, user_encoder, item_encoder, df, rmse, mae)
        
        print("\n" + "=" * 70)
        print("✅ TRAINING COMPLETE!")
        print("=" * 70)
        print("\n💡 Next steps:")
        print("   1. Restart Python ML Service (reco_service) to load new model")
        print("   2. Test recommendations via API")
        print("   3. Monitor recommendation quality")
        
    except FileNotFoundError as e:
        print(f"\n❌ Error: {e}")
        print("\n💡 Solution:")
        print("   1. Run extract_training_data.py first to extract data from database")
        print("   2. Then run this script to train the model")
    except Exception as e:
        print(f"\n❌ Error during training: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()

