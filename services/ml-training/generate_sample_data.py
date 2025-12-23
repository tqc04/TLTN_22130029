"""
Generate sample data for AI Recommendation training
This script creates realistic sample data to improve model training
"""
import pymysql
import random
import os
from datetime import datetime, timedelta

# Database configuration
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = int(os.getenv('DB_PORT', 3306))
DB_USER = os.getenv('DB_USER', 'root')
DB_PASSWORD = os.getenv('DB_PASSWORD', '123456')


def connect_db(database):
    """Connect to MySQL database"""
    return pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASSWORD,
        database=database,
        charset='utf8mb4'
    )


def get_existing_users():
    """Get existing users from user_db"""
    try:
        conn = connect_db('user_db')
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM users LIMIT 100")
        users = [str(row[0]) for row in cursor.fetchall()]
        conn.close()
        return users
    except Exception as e:
        print(f"Error getting users: {e}")
        return []


def get_existing_products():
    """Get existing products from product_db"""
    try:
        conn = connect_db('product_db')
        cursor = conn.cursor()
        # Try with status column first, fallback to all products if column doesn't exist
        try:
            cursor.execute("SELECT id FROM products WHERE status = 'ACTIVE' LIMIT 200")
        except:
            cursor.execute("SELECT id FROM products LIMIT 200")
        products = [str(row[0]) for row in cursor.fetchall()]
        conn.close()
        return products
    except Exception as e:
        print(f"Error getting products: {e}")
        return []


def generate_reviews(users, products, count=500):
    """Generate sample reviews"""
    print(f"\n📝 Generating {count} sample reviews...")
    
    try:
        conn = connect_db('review_service_db')
        cursor = conn.cursor()
        
        generated = 0
        for _ in range(count):
            user_id = random.choice(users)
            product_id = random.choice(products)
            
            # Check if review already exists
            cursor.execute(
                "SELECT id FROM product_reviews WHERE user_id = %s AND product_id = %s",
                (user_id, product_id)
            )
            if cursor.fetchone():
                continue  # Skip if already exists
            
            # Generate realistic rating distribution
            # 70% positive (4-5 stars), 20% neutral (3 stars), 10% negative (1-2 stars)
            rand = random.random()
            if rand < 0.7:
                rating = random.choice([4, 5])
            elif rand < 0.9:
                rating = 3
            else:
                rating = random.choice([1, 2])
            
            # Generate review text based on rating
            if rating >= 4:
                comments = [
                    "Sản phẩm tốt, đúng như mô tả",
                    "Chất lượng tốt, giao hàng nhanh",
                    "Rất hài lòng với sản phẩm này",
                    "Đáng tiền, sẽ mua lại",
                    "Sản phẩm chất lượng, shop uy tín"
                ]
            elif rating == 3:
                comments = [
                    "Sản phẩm tạm ổn",
                    "Bình thường, không có gì đặc biệt",
                    "Giá hơi cao so với chất lượng",
                    "Tạm được, có thể cải thiện",
                    "Không tốt lắm nhưng cũng không tệ"
                ]
            else:
                comments = [
                    "Không như mong đợi",
                    "Chất lượng kém",
                    "Không đáng tiền",
                    "Giao hàng chậm, sản phẩm không tốt",
                    "Không hài lòng"
                ]
            
            comment = random.choice(comments)
            
            # Random date in last 90 days
            days_ago = random.randint(0, 90)
            created_at = datetime.now() - timedelta(days=days_ago)
            
            cursor.execute("""
                INSERT INTO product_reviews (user_id, product_id, rating, content, created_at, updated_at)
                VALUES (%s, %s, %s, %s, %s, %s)
            """, (user_id, product_id, rating, comment, created_at, created_at))
            
            generated += 1
            
            if generated % 100 == 0:
                conn.commit()
                print(f"   Generated {generated} reviews...")
        
        conn.commit()
        conn.close()
        print(f"   ✅ Generated {generated} reviews successfully!")
        return generated
        
    except Exception as e:
        print(f"   ❌ Error generating reviews: {e}")
        return 0


def generate_favorites(users, products, count=300):
    """Generate sample favorites"""
    print(f"\n⭐ Generating {count} sample favorites...")
    
    try:
        conn = connect_db('favorites_db')
        cursor = conn.cursor()
        
        generated = 0
        for _ in range(count):
            user_id = random.choice(users)
            product_id = random.choice(products)
            
            # Check if favorite already exists
            cursor.execute(
                "SELECT id FROM favorites WHERE user_id = %s AND product_id = %s",
                (user_id, product_id)
            )
            if cursor.fetchone():
                continue  # Skip if already exists
            
            # Random date in last 60 days
            days_ago = random.randint(0, 60)
            created_at = datetime.now() - timedelta(days=days_ago)
            
            cursor.execute("""
                INSERT INTO favorites (user_id, product_id, created_at)
                VALUES (%s, %s, %s)
            """, (user_id, product_id, created_at))
            
            generated += 1
            
            if generated % 100 == 0:
                conn.commit()
                print(f"   Generated {generated} favorites...")
        
        conn.commit()
        conn.close()
        print(f"   ✅ Generated {generated} favorites successfully!")
        return generated
        
    except Exception as e:
        print(f"   ❌ Error generating favorites: {e}")
        return 0


def generate_orders(users, products, count=200):
    """Generate sample completed orders"""
    print(f"\n🛒 Generating {count} sample orders...")
    
    try:
        conn = connect_db('order_db')
        cursor = conn.cursor()
        
        generated_orders = 0
        generated_items = 0
        
        for _ in range(count):
            user_id = random.choice(users)
            
            # Random date in last 120 days
            days_ago = random.randint(30, 120)  # Orders are older
            created_at = datetime.now() - timedelta(days=days_ago)
            
            # Create order
            cursor.execute("""
                INSERT INTO orders (
                    user_id, order_status, total_amount, 
                    shipping_address, payment_method, payment_status,
                    created_at, updated_at
                )
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            """, (
                user_id, 
                random.choice(['COMPLETED', 'DELIVERED']),
                random.randint(100000, 5000000),  # 100k - 5M VND
                'Sample Address',
                random.choice(['COD', 'VNPAY', 'BANK_TRANSFER']),
                'PAID',
                created_at,
                created_at
            ))
            
            order_id = cursor.lastrowid
            generated_orders += 1
            
            # Add 1-3 items per order
            num_items = random.randint(1, 3)
            for _ in range(num_items):
                product_id = random.choice(products)
                quantity = random.randint(1, 2)
                price = random.randint(100000, 2000000)
                
                cursor.execute("""
                    INSERT INTO order_items (
                        order_id, product_id, quantity, unit_price, created_at
                    )
                    VALUES (%s, %s, %s, %s, %s)
                """, (order_id, product_id, quantity, price, created_at))
                
                generated_items += 1
            
            if generated_orders % 50 == 0:
                conn.commit()
                print(f"   Generated {generated_orders} orders, {generated_items} items...")
        
        conn.commit()
        conn.close()
        print(f"   ✅ Generated {generated_orders} orders, {generated_items} items successfully!")
        return generated_items
        
    except Exception as e:
        print(f"   ❌ Error generating orders: {e}")
        return 0


def check_current_data():
    """Check current data statistics"""
    print("\n📊 Checking current data...")
    
    stats = {}
    
    # Check reviews
    try:
        conn = connect_db('review_service_db')
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM product_reviews WHERE rating IS NOT NULL")
        stats['reviews'] = cursor.fetchone()[0]
        conn.close()
    except:
        stats['reviews'] = 0
    
    # Check favorites
    try:
        conn = connect_db('favorites_db')
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM favorites")
        stats['favorites'] = cursor.fetchone()[0]
        conn.close()
    except:
        stats['favorites'] = 0
    
    # Check orders
    try:
        conn = connect_db('order_db')
        cursor = conn.cursor()
        cursor.execute("""
            SELECT COUNT(DISTINCT oi.id) 
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.order_status IN ('COMPLETED', 'DELIVERED')
        """)
        stats['purchases'] = cursor.fetchone()[0]
        conn.close()
    except:
        stats['purchases'] = 0
    
    total = stats['reviews'] + stats['favorites'] + stats['purchases']
    
    print(f"   Reviews: {stats['reviews']}")
    print(f"   Favorites: {stats['favorites']}")
    print(f"   Purchases: {stats['purchases']}")
    print(f"   Total: {total}")
    
    return stats


def main():
    """Main execution"""
    print("=" * 70)
    print("🚀 GENERATE SAMPLE DATA FOR AI RECOMMENDATION TRAINING")
    print("=" * 70)
    
    # Check current data
    print("\n[1/5] Current Data Statistics")
    current_stats = check_current_data()
    current_total = sum(current_stats.values())
    
    if current_total >= 1000:
        print(f"\n✅ You already have {current_total} interactions!")
        print("   This is enough for good model training.")
        response = input("\n   Generate more data anyway? (y/N): ")
        if response.lower() != 'y':
            print("   Cancelled.")
            return
    
    # Get existing users and products
    print("\n[2/5] Getting existing users and products...")
    users = get_existing_users()
    products = get_existing_products()
    
    if not users or not products:
        print("\n❌ Error: No users or products found in database!")
        print("   Please make sure you have users and products in your database first.")
        return
    
    print(f"   Found {len(users)} users and {len(products)} products")
    
    # Ask how many to generate
    print("\n[3/5] How many interactions to generate?")
    print("   Recommendation: 1000+ total for good model")
    print(f"   Current total: {current_total}")
    print(f"   Target: {max(1000, current_total + 500)}")
    print()
    
    try:
        reviews_count = int(input("   Reviews to generate (default 500): ") or "500")
        favorites_count = int(input("   Favorites to generate (default 300): ") or "300")
        orders_count = int(input("   Orders to generate (default 200): ") or "200")
    except ValueError:
        print("   Invalid input, using defaults...")
        reviews_count = 500
        favorites_count = 300
        orders_count = 200
    
    # Generate data
    print("\n[4/5] Generating sample data...")
    
    generated = {
        'reviews': generate_reviews(users, products, reviews_count),
        'favorites': generate_favorites(users, products, favorites_count),
        'purchases': generate_orders(users, products, orders_count)
    }
    
    # Check final statistics
    print("\n[5/5] Final Statistics")
    final_stats = check_current_data()
    final_total = sum(final_stats.values())
    
    # Summary
    print("\n" + "=" * 70)
    print("✅ DATA GENERATION COMPLETE!")
    print("=" * 70)
    
    print("\n📊 Summary:")
    print(f"   Before: {current_total} interactions")
    print(f"   Generated: {sum(generated.values())} interactions")
    print(f"   After: {final_total} interactions")
    print()
    
    if final_total >= 1000:
        print("✅ You now have enough data for good model training!")
    else:
        print(f"⚠️  You have {final_total} interactions.")
        print(f"   Recommend: {1000 - final_total} more for optimal results.")
    
    print("\n📝 Next Steps:")
    print("   1. Extract training data:")
    print("      python extract_training_data.py")
    print()
    print("   2. Train new model:")
    print("      python train_simple_model.py")
    print()
    print("   3. Restart Python ML Service")
    print("      cd ../../reco_service")
    print("      python app.py")
    print()
    print("   4. Test recommendations:")
    print("      cd ../services")
    print("      .\\test-ai-recommendation.bat")
    print()


if __name__ == "__main__":
    main()

