"""
Train a simple recommendation model without requiring C++ compiler
Uses collaborative filtering with user/item averages
"""
import pandas as pd
import joblib
import json
from datetime import datetime
from pathlib import Path
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
    print(f"Loading training data from {csv_path}...")
    
    if not csv_path.exists():
        raise FileNotFoundError(
            f"Training data not found: {csv_path}\n"
            f"Please run extract_training_data.py first!"
        )
    
    df = pd.read_csv(csv_path)
    print(f"   Loaded {len(df):,} ratings")
    print(f"   Users: {df['userId'].nunique():,}")
    print(f"   Products: {df['productId'].nunique():,}")
    
    return df


def prepare_data(df):
    """Prepare data for training"""
    print("\nPreparing data for training...")
    
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
    
    print(f"   Encoded {len(user_encoder.classes_):,} users")
    print(f"   Encoded {len(item_encoder.classes_):,} products")
    
    return df, user_encoder, item_encoder


def train_simple_model(df):
    """Train a simple collaborative filtering model"""
    print("\nTraining simple collaborative filtering model...")
    
    # Calculate global average
    global_mean = df['rating'].mean()
    
    # Calculate user biases (how much each user deviates from global mean)
    user_biases = df.groupby('userId_encoded')['rating'].mean() - global_mean
    
    # Calculate item biases (how much each item deviates from global mean)
    item_biases = df.groupby('productId_encoded')['rating'].mean() - global_mean
    
    # Create ratings matrix for collaborative filtering
    ratings_matrix = df.pivot_table(
        index='userId_encoded',
        columns='productId_encoded',
        values='rating',
        fill_value=0
    )
    
    # Calculate test metrics (simple train/test split)
    test_size = int(len(df) * 0.2)
    test_df = df.sample(n=test_size, random_state=42)
    
    predictions = []
    actuals = []
    
    for _, row in test_df.iterrows():
        user_id = row['userId_encoded']
        item_id = row['productId_encoded']
        actual = row['rating']
        
        # Predict using global mean + user bias + item bias
        pred = global_mean
        if user_id in user_biases.index:
            pred += user_biases[user_id]
        if item_id in item_biases.index:
            pred += item_biases[item_id]
        
        # Clip to valid rating range
        pred = np.clip(pred, 1, 5)
        
        predictions.append(pred)
        actuals.append(actual)
    
    # Calculate RMSE and MAE
    predictions = np.array(predictions)
    actuals = np.array(actuals)
    rmse = np.sqrt(np.mean((predictions - actuals) ** 2))
    mae = np.mean(np.abs(predictions - actuals))
    
    print(f"\n   Model training completed!")
    print(f"   Performance Metrics:")
    print(f"      RMSE: {rmse:.4f}")
    print(f"      MAE: {mae:.4f}")
    
    # Package model
    model = {
        'global_mean': global_mean,
        'user_biases': user_biases.to_dict(),
        'item_biases': item_biases.to_dict(),
        'ratings_matrix': ratings_matrix,
        'type': 'simple_cf'
    }
    
    return model, rmse, mae


def save_model_and_metadata(model, user_encoder, item_encoder, df, rmse, mae):
    """Save model, encoders, and metadata"""
    print("\nSaving model and metadata...")
    
    # Ensure output directory exists
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    
    # Save model
    joblib.dump(model, MODEL_PATH)
    print(f"   Model saved: {MODEL_PATH}")
    
    # Save encoders
    joblib.dump(user_encoder, USER_ENCODER_PATH)
    joblib.dump(item_encoder, ITEM_ENCODER_PATH)
    print(f"   User encoder saved: {USER_ENCODER_PATH}")
    print(f"   Item encoder saved: {ITEM_ENCODER_PATH}")
    
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
            "type": "Simple Collaborative Filtering",
            "method": "User/Item Biases",
            "trained_on": "Your Database Reviews/Ratings/Favorites"
        },
        "performance": {
            "rmse": float(rmse),
            "mae": float(mae)
        },
        "data_info": {
            "source": "Your Database (product_reviews, favorites)",
            "n_users": int(n_users),
            "n_products": int(n_products),
            "n_ratings": int(n_ratings),
            "avg_rating": float(avg_rating),
            "sparsity": float(sparsity)
        },
        "created_at": datetime.now().isoformat(),
        "notes": "Simple CF model using user/item biases. Works well for small datasets."
    }
    
    # Save metadata
    with open(METADATA_PATH, 'w', encoding='utf-8') as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)
    print(f"   Metadata saved: {METADATA_PATH}")
    
    # Display summary
    print(f"\nModel Summary:")
    print(f"   Model Type: Simple Collaborative Filtering")
    print(f"   RMSE: {rmse:.4f}")
    print(f"   MAE: {mae:.4f}")
    print(f"   Users: {n_users:,}")
    print(f"   Products: {n_products:,}")
    print(f"   Ratings: {n_ratings:,}")
    print(f"   Avg Rating: {avg_rating:.2f}")
    print(f"   Sparsity: {sparsity:.2f}%")
    print(f"\n   Model export completed!")
    print(f"\n   Model files saved to: {OUTPUT_DIR}")
    print(f"      - recommendation_model.pkl")
    print(f"      - user_label_encoder.joblib")
    print(f"      - item_label_encoder.joblib")
    print(f"      - model_metadata.json")


def main():
    """Main execution"""
    print("=" * 70)
    print("TRAINING RECOMMENDATION MODEL FROM YOUR DATABASE")
    print("=" * 70)
    
    try:
        # Load training data
        df = load_training_data(TRAINING_DATA_PATH)
        
        if len(df) < 10:
            print("\nWARNING: Very little training data!")
            print(f"   Only {len(df)} ratings found.")
            print("   Model quality will be poor. Need at least 100+ ratings.")
            response = input("\n   Continue anyway? (y/N): ")
            if response.lower() != 'y':
                print("   Training cancelled.")
                return
        
        # Prepare data
        df, user_encoder, item_encoder = prepare_data(df)
        
        # Train model
        model, rmse, mae = train_simple_model(df)
        
        # Save everything
        save_model_and_metadata(model, user_encoder, item_encoder, df, rmse, mae)
        
        print("\n" + "=" * 70)
        print("TRAINING COMPLETE!")
        print("=" * 70)
        print("\nNext steps:")
        print("   1. Copy model files to reco_service/ directory")
        print("   2. Restart Python ML Service (reco_service)")
        print("   3. Test recommendations via API")
        
    except FileNotFoundError as e:
        print(f"\nError: {e}")
        print("\nSolution:")
        print("   1. Run extract_training_data.py first to extract data from database")
        print("   2. Then run this script to train the model")
    except Exception as e:
        print(f"\nError during training: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()

