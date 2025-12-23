"""Extract training data from MySQL for recommendation model"""
import pymysql
import pandas as pd
from datetime import datetime
import os


def connect_to_db(database=None):
    """Connect to MySQL"""
    db_name = database or os.getenv('DB_NAME', 'ecommerce_db')
    return pymysql.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        port=int(os.getenv('DB_PORT', 3306)),
        user=os.getenv('DB_USER', 'root'),
        password=os.getenv('DB_PASSWORD', ''),
        database=db_name,
        charset='utf8mb4'
        # Don't use DictCursor for pd.read_sql - it causes issues
    )


def extract_explicit_ratings():
    """Extract explicit ratings from product_reviews table (from review_service_db)"""
    print("📊 Extracting explicit ratings from product_reviews...")
    
    # Try review_service_db first (microservice database)
    try:
        conn = connect_to_db('review_service_db')
        query = """
        SELECT 
            pr.user_id as userId,
            pr.product_id as productId,
            pr.rating as rating,
            UNIX_TIMESTAMP(pr.created_at) as timestamp
        FROM product_reviews pr
        WHERE pr.rating IS NOT NULL
        AND pr.rating BETWEEN 1 AND 5
        ORDER BY pr.created_at
        """
        df = pd.read_sql(query, conn)
        conn.close()
        if len(df) > 0:
            print(f"   ✅ Found {len(df):,} explicit ratings from review_service_db")
            return df
    except Exception as e:
        print(f"   ⚠️  review_service_db: {e}")
    
    # Fallback to product_db
    try:
        conn = connect_to_db('product_db')
        query = """
        SELECT 
            pr.user_id as userId,
            pr.product_id as productId,
            pr.rating as rating,
            UNIX_TIMESTAMP(pr.created_at) as timestamp
        FROM product_reviews pr
        WHERE pr.rating IS NOT NULL
        AND pr.rating BETWEEN 1 AND 5
        ORDER BY pr.created_at
        """
        df = pd.read_sql(query, conn)
        conn.close()
        if len(df) > 0:
            print(f"   ✅ Found {len(df):,} explicit ratings from product_db")
            return df
    except Exception as e:
        print(f"   ⚠️  product_db: {e}")
    
    print(f"   ✅ Found 0 explicit ratings")
    return pd.DataFrame()


def extract_purchase_implicit_ratings():
    """Extract purchase data as implicit 5-star ratings (from order_db)"""
    print("📊 Extracting purchase data (implicit ratings)...")
    
    # Try order_db first (microservice database)
    try:
        conn = connect_to_db('order_db')
        query = """
        SELECT 
            o.user_id as userId,
            oi.product_id as productId,
            5 as rating,
            UNIX_TIMESTAMP(o.created_at) as timestamp
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        WHERE o.order_status = 'COMPLETED' OR o.order_status = 'DELIVERED'
        ORDER BY o.created_at
        """
        df = pd.read_sql(query, conn)
        conn.close()
        if len(df) > 0:
            print(f"   ✅ Found {len(df):,} purchases (implicit 5-star) from order_db")
            return df
    except Exception as e:
        print(f"   ⚠️  order_db: {e}")
    
    # Fallback to ecommerce_db
    try:
        conn = connect_to_db('ecommerce_db')
        query = """
        SELECT 
            o.user_id as userId,
            oi.product_id as productId,
            5 as rating,
            UNIX_TIMESTAMP(o.created_at) as timestamp
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        WHERE o.order_status = 'COMPLETED' OR o.order_status = 'DELIVERED'
        ORDER BY o.created_at
        """
        df = pd.read_sql(query, conn)
        conn.close()
        if len(df) > 0:
            print(f"   ✅ Found {len(df):,} purchases (implicit 5-star) from ecommerce_db")
            return df
    except Exception as e:
        print(f"   ⚠️  ecommerce_db: {e}")
    
    print(f"   ✅ Found 0 purchases")
    return pd.DataFrame()


def extract_favorites_implicit_ratings():
    """Extract user favorites as implicit 5-star ratings (from favorites_db)"""
    print("📊 Extracting user favorites (implicit 5-star ratings)...")
    
    try:
        conn = connect_to_db('favorites_db')
        query = """
        SELECT 
            f.user_id as userId,
            f.product_id as productId,
            5 as rating,
            UNIX_TIMESTAMP(f.created_at) as timestamp
        FROM favorites f
        WHERE f.product_id IS NOT NULL
        ORDER BY f.created_at
        """
        df = pd.read_sql(query, conn)
        conn.close()
        if len(df) > 0:
            print(f"   ✅ Found {len(df):,} favorites from favorites_db")
            return df
    except Exception as e:
        print(f"   ⚠️  favorites_db: {e}")
    
    print(f"   ✅ Found 0 favorites")
    return pd.DataFrame()


def combine_and_clean_data(dfs):
    """Combine all data sources and clean"""
    print("\n🔄 Combining and cleaning data...")
    
    if not dfs:
        return pd.DataFrame(columns=['userId', 'productId', 'rating', 'timestamp'])
    
    # Combine all dataframes
    print(f"   DEBUG: Number of dataframes to combine: {len(dfs)}")
    for i, df in enumerate(dfs):
        print(f"   DEBUG: DF {i} shape: {df.shape}, columns: {list(df.columns)}")
        if len(df) > 0:
            print(f"   DEBUG: DF {i} first 3 rows:")
            print(df.head(3))
            # Save to debug file
            df.to_csv(f"debug_df_{i}.csv", index=False)
            print(f"   DEBUG: Saved to debug_df_{i}.csv")
    
    df_combined = pd.concat(dfs, ignore_index=True)
    print(f"   Total records: {len(df_combined):,}")
    
    if len(df_combined) == 0:
        return df_combined
    
    # Drop rows with null userId or productId BEFORE conversion
    before_null_drop = len(df_combined)
    df_combined = df_combined.dropna(subset=['userId', 'productId'])
    after_null_drop = len(df_combined)
    if before_null_drop != after_null_drop:
        print(f"   ⚠️  Dropped {before_null_drop - after_null_drop} records with null userId/productId")
    
    if len(df_combined) == 0:
        print("   ❌ No records left after removing nulls!")
        return df_combined
    
    # Convert userId and productId to string for consistency BEFORE deduplication
    df_combined['userId'] = df_combined['userId'].astype(str)
    df_combined['productId'] = df_combined['productId'].astype(str)
    
    # Debug: Check unique values before dedup
    print(f"   DEBUG: Unique users before dedup: {df_combined['userId'].nunique()}")
    print(f"   DEBUG: Unique products before dedup: {df_combined['productId'].nunique()}")
    print(f"   DEBUG: Sample userId values: {df_combined['userId'].head(10).tolist()}")
    print(f"   DEBUG: Sample productId values: {df_combined['productId'].head(10).tolist()}")
    
    # Remove duplicates (keep latest rating for same user-product pair)
    # Handle missing timestamps
    if 'timestamp' in df_combined.columns:
        df_combined['timestamp'] = pd.to_numeric(df_combined['timestamp'], errors='coerce')
        df_combined = df_combined.sort_values('timestamp', na_position='last')
    df_combined = df_combined.drop_duplicates(
        subset=['userId', 'productId'], 
        keep='last'
    )
    print(f"   After deduplication: {len(df_combined):,}")
    
    if len(df_combined) == 0:
        return df_combined
    
    # Convert rating to numeric and validate
    print(f"   Before rating validation: {len(df_combined):,} records")
    df_combined['rating'] = pd.to_numeric(df_combined['rating'], errors='coerce')
    before_dropna = len(df_combined)
    df_combined = df_combined.dropna(subset=['rating'])
    after_dropna = len(df_combined)
    if before_dropna != after_dropna:
        print(f"   ⚠️  Dropped {before_dropna - after_dropna} records with invalid ratings")
    
    if len(df_combined) == 0:
        return df_combined
    
    before_filter = len(df_combined)
    df_combined = df_combined[
        (df_combined['rating'] >= 1) & 
        (df_combined['rating'] <= 5)
    ]
    after_filter = len(df_combined)
    if before_filter != after_filter:
        print(f"   ⚠️  Dropped {before_filter - after_filter} records with ratings outside 1-5 range")
    
    if len(df_combined) > 0:
        df_combined['rating'] = df_combined['rating'].astype(int)
    print(f"   After rating validation: {len(df_combined):,} records")
    
    # For small datasets, skip filtering or use very low threshold
    if len(df_combined) < 100:
        print(f"   ⚠️  Small dataset ({len(df_combined)} records), skipping user/product filtering")
        df_filtered = df_combined
    else:
        # Filter users and products with minimum interactions
        min_interactions = 1  # Lowered from 5 to handle small datasets
        
        user_counts = df_combined['userId'].value_counts()
        valid_users = user_counts[user_counts >= min_interactions].index
        df_filtered = df_combined[df_combined['userId'].isin(valid_users)]
        
        product_counts = df_filtered['productId'].value_counts()
        valid_products = product_counts[product_counts >= min_interactions].index
        df_filtered = df_filtered[df_filtered['productId'].isin(valid_products)]
        
        print(f"   After filtering (min {min_interactions} interactions):")
    
    print(f"      Records: {len(df_filtered):,}")
    print(f"      Users: {df_filtered['userId'].nunique():,}")
    print(f"      Products: {df_filtered['productId'].nunique():,}")
    
    return df_filtered


def calculate_statistics(df):
    """Calculate and display dataset statistics"""
    print("\n📊 Dataset Statistics:")
    print(f"   Total ratings: {len(df):,}")
    print(f"   Unique users: {df['userId'].nunique():,}")
    print(f"   Unique products: {df['productId'].nunique():,}")
    print(f"   Rating range: {df['rating'].min()} - {df['rating'].max()}")
    print(f"   Average rating: {df['rating'].mean():.2f}")
    
    print("\n⭐ Rating Distribution:")
    for rating in sorted(df['rating'].unique()):
        count = (df['rating'] == rating).sum()
        pct = count / len(df) * 100
        print(f"      {int(rating)} stars: {count:,} ({pct:.1f}%)")
    
    # Sparsity
    n_users = df['userId'].nunique()
    n_products = df['productId'].nunique()
    sparsity = 1 - (len(df) / (n_users * n_products)) if (n_users * n_products) > 0 else 0.0
    print(f"\n   Sparsity: {sparsity:.4f} ({sparsity*100:.2f}%)")


def save_training_data(df, output_path):
    """Save training data to CSV"""
    print(f"\n💾 Saving training data to {output_path}...")
    
    # Ensure output directory exists
    os.makedirs(os.path.dirname(output_path) if os.path.dirname(output_path) else '.', exist_ok=True)
    
    # Save as CSV for Surprise library
    df[['userId', 'productId', 'rating', 'timestamp']].to_csv(
        output_path, 
        index=False
    )
    
    print(f"   ✅ Saved {len(df):,} records")
    print(f"   📁 File: {output_path}")
    
    # Save metadata
    metadata = {
        'created_at': datetime.now().isoformat(),
        'total_ratings': len(df),
        'n_users': int(df['userId'].nunique()),
        'n_products': int(df['productId'].nunique()),
        'avg_rating': float(df['rating'].mean()),
        'rating_distribution': df['rating'].value_counts().to_dict()
    }
    
    metadata_path = output_path.replace('.csv', '_metadata.json')
    import json
    with open(metadata_path, 'w') as f:
        json.dump(metadata, f, indent=2)
    print(f"   ✅ Metadata saved: {metadata_path}")


def main():
    """Main execution"""
    print("=" * 60)
    print("🚀 EXTRACTING TRAINING DATA FROM DATABASE")
    print("=" * 60)
    
    try:
        # Extract data from different sources (from multiple databases)
        dfs = []
        
        # 1. Explicit ratings from reviews (review_service_db)
        print("\n📊 Extracting explicit ratings from reviews...")
        df_reviews = extract_explicit_ratings()
        if len(df_reviews) > 0:
            dfs.append(df_reviews)
        
        # 2. Purchases (implicit 5-star) (order_db)
        print("\n📊 Extracting purchase data...")
        df_purchases = extract_purchase_implicit_ratings()
        if len(df_purchases) > 0:
            dfs.append(df_purchases)
        
        # 3. Favorites (implicit 5-star) (favorites_db)
        print("\n📊 Extracting favorites data...")
        df_favorites = extract_favorites_implicit_ratings()
        if len(df_favorites) > 0:
            dfs.append(df_favorites)
        
        # 3. User behaviors (implicit 3-5 stars) (ecommerce_db)
        print("\n📊 Extracting user behavior data...")
        
        if not dfs:
            print("\n❌ No data found in database!")
            print("   Make sure you have data in:")
            print("   - product_reviews (review_service_db)")
            print("   - order_items + orders (order_db)")
            print("   - user_behaviors (ecommerce_db)")
            return
        
        # Combine and clean
        df_final = combine_and_clean_data(dfs)
        
        if len(df_final) == 0:
            print("\n❌ No valid data after cleaning!")
            return
        
        if len(df_final) < 100:
            print("\n⚠️  WARNING: Very little data available!")
            print(f"   Only {len(df_final)} ratings found.")
            print("   Recommendation quality will be poor.")
            print("   Need at least 1000+ ratings for good results.")
        
        # Calculate statistics
        calculate_statistics(df_final)
        
        # Save training data
        output_path = os.path.join(
            os.path.dirname(__file__),
            'training_data.csv'
        )
        save_training_data(df_final, output_path)
        
        print("\n" + "=" * 60)
        print("✅ DATA EXTRACTION COMPLETE!")
        print("=" * 60)
        print("\n📝 Next steps:")
        print("   1. Review the training_data.csv file")
        print("   2. Run: python train_model.py")
        print("   3. Model files will be saved to Buildd30_7/")
        print("   4. Restart Python ML Service (reco_service/app.py)")
        
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()

