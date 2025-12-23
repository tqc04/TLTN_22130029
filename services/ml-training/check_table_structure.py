"""
Check actual table structure to fix column name issues
"""
import pymysql
import os

DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = int(os.getenv('DB_PORT', 3306))
DB_USER = os.getenv('DB_USER', 'root')
DB_PASSWORD = os.getenv('DB_PASSWORD', '123456')


def check_table(database, table):
    """Check table structure"""
    try:
        conn = pymysql.connect(
            host=DB_HOST,
            port=DB_PORT,
            user=DB_USER,
            password=DB_PASSWORD,
            database=database,
            charset='utf8mb4'
        )
        cursor = conn.cursor()
        cursor.execute(f"DESCRIBE {table}")
        columns = cursor.fetchall()
        conn.close()
        return columns
    except Exception as e:
        print(f"Error: {e}")
        return None


print("=" * 70)
print("📋 CHECKING TABLE STRUCTURES")
print("=" * 70)

print("\n1. product_reviews table (review_service_db):")
cols = check_table('review_service_db', 'product_reviews')
if cols:
    for col in cols:
        print(f"   - {col[0]} ({col[1]})")

print("\n2. favorites table (favorites_db):")
cols = check_table('favorites_db', 'favorites')
if cols:
    for col in cols:
        print(f"   - {col[0]} ({col[1]})")

print("\n3. orders table (order_db):")
cols = check_table('order_db', 'orders')
if cols:
    for col in cols:
        print(f"   - {col[0]} ({col[1]})")

print("\n4. order_items table (order_db):")
cols = check_table('order_db', 'order_items')
if cols:
    for col in cols:
        print(f"   - {col[0]} ({col[1]})")

print("\n5. products table (product_db):")
cols = check_table('product_db', 'products')
if cols:
    for col in cols:
        print(f"   - {col[0]} ({col[1]})")

print("\n" + "=" * 70)

