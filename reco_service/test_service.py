"""
Test script for Recommendation Service
Run this after starting the service to verify it works correctly
"""
import requests
import json


def test_health():
    """Test health endpoint"""
    print("=" * 60)
    print("Testing /health endpoint...")
    print("=" * 60)
    
    try:
        response = requests.get("http://localhost:8000/health", timeout=5)
        print(f"Status: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            print(f"✅ Health check passed!")
            print(f"   Model Loaded: {data.get('modelLoaded')}")
            print(f"   Model Path: {data.get('modelPath')}")
            
            if 'metadata' in data and data['metadata']:
                meta = data['metadata']
                print(f"\n📊 Model Info:")
                if 'model_info' in meta:
                    print(f"   Type: {meta['model_info'].get('type')}")
                    print(f"   Factors: {meta['model_info'].get('n_factors')}")
                    print(f"   Epochs: {meta['model_info'].get('n_epochs')}")
                
                if 'performance' in meta:
                    print(f"\n📈 Performance:")
                    print(f"   RMSE: {meta['performance'].get('rmse'):.4f}")
                    print(f"   MAE: {meta['performance'].get('mae'):.4f}")
                
                if 'data_info' in meta:
                    print(f"\n📦 Dataset:")
                    print(f"   Users: {meta['data_info'].get('n_users'):,}")
                    print(f"   Products: {meta['data_info'].get('n_products'):,}")
                    print(f"   Ratings: {meta['data_info'].get('cleaned_shape', [0,0])[0]:,}")
            return True
        else:
            print(f"❌ Health check failed with status {response.status_code}")
            return False
    except requests.exceptions.ConnectionError:
        print("❌ Cannot connect to service. Is it running?")
        print("   Start it with: python app.py")
        return False
    except Exception as e:
        print(f"❌ Error: {e}")
        return False


def test_predict():
    """Test prediction endpoint"""
    print("\n" + "=" * 60)
    print("Testing /predict-top-n endpoint...")
    print("=" * 60)
    
    # Test case 1: Small product list
    payload = {
        "userId": "123",
        "limit": 5,
        "productIds": ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"]
    }
    
    print(f"\n📤 Request:")
    print(f"   User ID: {payload['userId']}")
    print(f"   Limit: {payload['limit']}")
    print(f"   Products to score: {len(payload['productIds'])}")
    
    try:
        response = requests.post(
            "http://localhost:8000/predict-top-n",
            json=payload,
            timeout=10
        )
        
        print(f"\n📥 Response Status: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            print(f"✅ Prediction successful!")
            print(f"   Model Loaded: {data.get('modelLoaded')}")
            print(f"   Fallback Used: {data.get('fallbackUsed', 'N/A')}")
            print(f"   Items Returned: {len(data.get('items', []))}")
            
            print(f"\n🎯 Top Recommendations:")
            for i, item in enumerate(data.get('items', [])[:5], 1):
                print(f"   {i}. Product {item['productId']}: Score {item['score']:.3f}")
            
            return True
        else:
            print(f"❌ Prediction failed with status {response.status_code}")
            print(f"   Response: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Error: {e}")
        return False


def test_recommend_endpoint():
    """Test /recommend endpoint compatibility"""
    print("\n" + "=" * 60)
    print("Testing /recommend endpoint...")
    print("=" * 60)

    payload = {"userId": "user-test-1", "limit": 5}
    response = requests.post("http://localhost:8000/recommend", json=payload, timeout=10)
    print(f"Status: {response.status_code}")

    if response.status_code != 200:
        print("❌ /recommend failed", response.text)
        return False

    data = response.json()
    recs = data.get("recommendations", [])
    print(f"   Received {len(recs)} recommendations")
    assert isinstance(recs, list)
    assert len(recs) > 0
    print("   ✅ Passed")
    return True


def test_recommendations_user():
    """Test GET /recommendations/user/{userId}"""
    print("\n" + "=" * 60)
    print("Testing GET /recommendations/user/{userId} endpoint...")
    print("=" * 60)

    response = requests.get("http://localhost:8000/recommendations/user/demo-user?limit=3", timeout=10)
    print(f"Status: {response.status_code}")

    if response.status_code != 200:
        print("❌ GET recommendations failed", response.text)
        return False

    data = response.json()
    recs = data.get("recommendations", [])
    print(f"   Received {len(recs)} recommendations")
    assert isinstance(recs, list)
    print("   ✅ Passed")
    return True


def test_large_batch():
    """Test with larger batch"""
    print("\n" + "=" * 60)
    print("Testing with large product batch...")
    print("=" * 60)
    
    # Generate 50 product IDs
    product_ids = [str(i) for i in range(1, 51)]
    
    payload = {
        "userId": "456",
        "limit": 10,
        "productIds": product_ids
    }
    
    print(f"\n📤 Request:")
    print(f"   User ID: {payload['userId']}")
    print(f"   Products to score: {len(payload['productIds'])}")
    print(f"   Limit: {payload['limit']}")
    
    try:
        response = requests.post(
            "http://localhost:8000/predict-top-n",
            json=payload,
            timeout=15
        )
        
        if response.status_code == 200:
            data = response.json()
            print(f"✅ Large batch prediction successful!")
            print(f"   Items Returned: {len(data.get('items', []))}")
            print(f"   Score Range: {data['items'][-1]['score']:.3f} - {data['items'][0]['score']:.3f}")
            return True
        else:
            print(f"❌ Failed with status {response.status_code}")
            return False
            
    except Exception as e:
        print(f"❌ Error: {e}")
        return False


def test_edge_cases():
    """Test edge cases"""
    print("\n" + "=" * 60)
    print("Testing edge cases...")
    print("=" * 60)
    
    # Test 1: Empty product list
    print("\n1️⃣ Empty product list:")
    payload = {"userId": "789", "limit": 10, "productIds": []}
    response = requests.post("http://localhost:8000/predict-top-n", json=payload)
    data = response.json()
    print(f"   Items returned: {len(data.get('items', []))}")
    assert len(data.get('items', [])) == 0, "Should return empty list"
    print("   ✅ Passed")
    
    # Test 2: Single product
    print("\n2️⃣ Single product:")
    payload = {"userId": "789", "limit": 10, "productIds": ["42"]}
    response = requests.post("http://localhost:8000/predict-top-n", json=payload)
    data = response.json()
    print(f"   Items returned: {len(data.get('items', []))}")
    assert len(data.get('items', [])) == 1, "Should return 1 item"
    print("   ✅ Passed")
    
    # Test 3: Limit larger than products
    print("\n3️⃣ Limit > Products:")
    payload = {"userId": "789", "limit": 100, "productIds": ["1", "2", "3"]}
    response = requests.post("http://localhost:8000/predict-top-n", json=payload)
    data = response.json()
    print(f"   Items returned: {len(data.get('items', []))}")
    assert len(data.get('items', [])) == 3, "Should return all 3 items"
    print("   ✅ Passed")
    
    print("\n✅ All edge cases passed!")
    return True


def main():
    """Run all tests"""
    print("\n" + "🚀" * 30)
    print("RECOMMENDATION SERVICE TEST SUITE")
    print("🚀" * 30)
    
    results = []
    
    # Run tests
    results.append(("Health Check", test_health()))
    
    if results[0][1]:  # Only continue if health check passed
        results.append(("Basic Prediction", test_predict()))
        results.append(("POST /recommend", test_recommend_endpoint()))
        results.append(("GET /recommendations/user", test_recommendations_user()))
        results.append(("Large Batch", test_large_batch()))
        results.append(("Edge Cases", test_edge_cases()))
    
    # Summary
    print("\n" + "=" * 60)
    print("TEST SUMMARY")
    print("=" * 60)
    
    for test_name, passed in results:
        status = "✅ PASS" if passed else "❌ FAIL"
        print(f"{status} - {test_name}")
    
    total = len(results)
    passed = sum(1 for _, p in results if p)
    
    print(f"\n📊 Results: {passed}/{total} tests passed")
    
    if passed == total:
        print("\n🎉 All tests passed! Service is working correctly.")
    else:
        print("\n⚠️ Some tests failed. Please check the logs above.")


if __name__ == "__main__":
    main()

