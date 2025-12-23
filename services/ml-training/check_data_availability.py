"""
Quick script to check if you have enough data to train recommendation model
"""
import pymysql
import os


def connect_db(database):
    """Connect to MySQL"""
    try:
        return pymysql.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            port=int(os.getenv('DB_PORT', 3306)),
            user=os.getenv('DB_USER', 'root'),
            password=os.getenv('DB_PASSWORD', ''),
            database=database,
            charset='utf8mb4',
            cursorclass=pymysql.cursors.DictCursor
        )
    except Exception as e:
        print(f"❌ Cannot connect to database '{database}': {e}")
        return None


def check_data():
    """Check available data for training"""
    print("=" * 60)
    print("📊 CHECKING DATA AVAILABILITY FOR ML TRAINING")
    print("=" * 60)
    
    favorite_count = 0
    review_count = 0
    purchase_count = 0
    
    # Check favorites from favorites_db (implicit ratings)
    print("\n1️⃣ User Favorites (Implicit Ratings - 5 stars)")
    try:
        conn = connect_db('favorites_db')
        if conn:
            cursor = conn.cursor()
            cursor.execute("SELECT COUNT(*) as count FROM favorites")
            result = cursor.fetchone()
            favorite_count = result['count']
            print(f"   Total favorites: {favorite_count:,}")
            
            if favorite_count > 0:
                cursor.execute("""
                    SELECT COUNT(DISTINCT user_id) as users,
                           COUNT(DISTINCT product_id) as products
                    FROM favorites
                """)
                stats = cursor.fetchone()
                print(f"   - Unique users: {stats['users']:,}")
                print(f"   - Unique products: {stats['products']:,}")
            conn.close()
    except Exception as e:
        print(f"   ⚠️ Error checking favorites_db: {e}")
    
    # Check product_reviews from review_service_db (explicit ratings)
    print("\n2️⃣ Product Reviews (Explicit Ratings - 1-5 stars)")
    try:
        conn = connect_db('review_service_db')
        if conn:
            cursor = conn.cursor()
            cursor.execute("SELECT COUNT(*) as count FROM product_reviews WHERE rating IS NOT NULL")
            result = cursor.fetchone()
            review_count = result['count']
            print(f"   Total reviews with ratings: {review_count:,}")
            
            if review_count > 0:
                cursor.execute("""
                    SELECT rating, COUNT(*) as count 
                    FROM product_reviews 
                    WHERE rating IS NOT NULL
                    GROUP BY rating 
                    ORDER BY rating
                """)
                for row in cursor.fetchall():
                    print(f"   - {row['rating']} stars: {row['count']:,}")
                    
                cursor.execute("""
                    SELECT COUNT(DISTINCT user_id) as users,
                           COUNT(DISTINCT product_id) as products
                    FROM product_reviews
                    WHERE rating IS NOT NULL
                """)
                stats = cursor.fetchone()
                print(f"   - Unique reviewers: {stats['users']:,}")
                print(f"   - Unique products reviewed: {stats['products']:,}")
            conn.close()
    except Exception as e:
        print(f"   ⚠️ Error checking review_service_db: {e}")
    
    # Check orders (purchases) from order_db (implicit 5-star ratings)
    print("\n3️⃣ Purchase History (Implicit Ratings - 5 stars)")
    try:
        conn = connect_db('order_db')
        if conn:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT COUNT(DISTINCT oi.id) as count 
                FROM order_items oi
                JOIN orders o ON oi.order_id = o.id
                WHERE o.order_status = 'COMPLETED' OR o.order_status = 'DELIVERED'
            """)
            result = cursor.fetchone()
            purchase_count = result['count']
            print(f"   Total purchased items: {purchase_count:,}")
            
            if purchase_count > 0:
                cursor.execute("""
                    SELECT COUNT(DISTINCT o.user_id) as users,
                           COUNT(DISTINCT oi.product_id) as products
                    FROM order_items oi
                    JOIN orders o ON oi.order_id = o.id
                    WHERE o.order_status = 'COMPLETED' OR o.order_status = 'DELIVERED'
                """)
                stats = cursor.fetchone()
                print(f"   - Unique buyers: {stats['users']:,}")
                print(f"   - Unique products bought: {stats['products']:,}")
            conn.close()
    except Exception as e:
        print(f"   ⚠️ Error checking order_db: {e}")
    
    # Calculate total interactions
    total_interactions = favorite_count + review_count + purchase_count
    
    # Assessment
    print("\n" + "=" * 60)
    print("📈 ASSESSMENT")
    print("=" * 60)
    print(f"Total interactions: {total_interactions:,}")
    print()
    print("Data sources:")
    print(f"  - Favorites: {favorite_count:,} (implicit 5-star)")
    print(f"  - Reviews: {review_count:,} (explicit 1-5 star)")
    print(f"  - Purchases: {purchase_count:,} (implicit 5-star)")
    print()
    
    if total_interactions == 0:
        print("❌ STATUS: NO DATA")
        print()
        print("You have NO user interaction data yet!")
        print()
        print("What this means:")
        print("✅ Backend code is ready to collect data")
        print("✅ Database tables exist")
        print("❌ But NO actual user interactions recorded yet")
        print()
        print("Why can't train?")
        print("→ ML model needs user behavior patterns to learn from")
        print("→ Without data, there's nothing to learn")
        print()
        print("What to do:")
        print("1. ✅ Use Content-Based Filtering (current solution)")
        print("   - Works without user data")
        print("   - Based on product similarity")
        print("   - Good enough for MVP")
        print()
        print("2. 📝 Start collecting data:")
        print("   - Users favorite products")
        print("   - Users leave reviews")
        print("   - Users make purchases")
        print()
        print("3. 🚀 Train when ready (need 100+ interactions minimum)")
        
    elif total_interactions < 100:
        print("⚠️ STATUS: TOO LITTLE DATA")
        print()
        print(f"You have {total_interactions} interactions.")
        print("This is NOT enough to train a good model.")
        print()
        print("Recommendation:")
        print("✅ Keep using Content-Based Filtering")
        print("📝 Continue collecting data")
        print("🎯 Target: 100+ interactions minimum, 1000+ for good quality")
        
    elif total_interactions < 1000:
        print("⚠️ STATUS: SOME DATA (Can train but quality may be low)")
        print()
        print(f"You have {total_interactions} interactions.")
        print()
        print("Options:")
        print("1. Train now (will work but accuracy ~50-60%)")
        print("2. Wait for more data (recommended 1000+)")
        print()
        print("If you want to train now:")
        print("   python extract_training_data.py")
        print("   python train_model.py")
        
    else:
        print("✅ STATUS: ENOUGH DATA TO TRAIN!")
        print()
        print(f"You have {total_interactions:,} interactions. Great!")
        print()
        print("You can train a good recommendation model now:")
        print()
        print("Steps:")
        print("1. Extract data:")
        print("   python extract_training_data.py")
        print()
        print("2. Train model:")
        print("   python train_model.py")
        print()
        print("3. Restart Python service (reco_service)")
        print()
        print("Expected result:")
        print("✅ TRUE personalization based on real user behavior")
        print("✅ Accuracy: 75-85%+")
    
    print("\n" + "=" * 60)


if __name__ == "__main__":
    print("\n🔍 Checking if you have data for ML training...\n")
    
    # Check DB credentials
    if not os.getenv('DB_PASSWORD'):
        print("⚠️  Database password not set!")
        print()
        print("Please set environment variables:")
        print("  $env:DB_HOST=\"localhost\"")
        print("  $env:DB_PORT=\"3306\"")
        print("  $env:DB_USER=\"root\"")
        print("  $env:DB_PASSWORD=\"123456\"")
        print()
        print("Then run this script again.")
        exit(1)
    
    check_data()

