"""
Script để verify test users trước khi chạy performance test
"""
import requests
import json
import sys

def verify_user(base_url: str, username: str, password: str) -> tuple:
    """Test login cho một user và return (success, status_code, message)"""
    url = f"{base_url}/api/auth/login"
    try:
        response = requests.post(
            url,
            json={"username": username, "password": password},
            headers={'X-Skip-Auth': 'true'},
            timeout=10
        )
        
        if response.status_code in [200, 201]:
            data = response.json()
            token = data.get('token', 'N/A')
            user = data.get('user', {})
            return (True, response.status_code, f"Success - Token: {token[:20]}..., User: {user.get('username', 'N/A')}")
        else:
            try:
                error_data = response.json()
                error_msg = error_data.get('error') or error_data.get('message') or error_data.get('body', 'Unknown error')
            except:
                error_msg = response.text[:100] if response.text else 'Unknown error'
            return (False, response.status_code, f"Failed: {error_msg}")
    except requests.exceptions.Timeout:
        return (False, 0, "Timeout")
    except requests.exceptions.ConnectionError:
        return (False, 0, "Connection Error - Backend không chạy?")
    except Exception as e:
        return (False, 0, f"Exception: {str(e)}")

def main():
    base_url = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
    test_users_file = sys.argv[2] if len(sys.argv) > 2 else "test_users.json"
    
    print("=" * 60)
    print("VERIFY TEST USERS FOR LOGIN")
    print("=" * 60)
    print(f"Base URL: {base_url}")
    print(f"Test Users File: {test_users_file}")
    print()
    
    # Load test users
    try:
        with open(test_users_file, 'r', encoding='utf-8') as f:
            test_users = json.load(f)
    except FileNotFoundError:
        print(f"ERROR: File {test_users_file} không tồn tại!")
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"ERROR: Invalid JSON trong {test_users_file}: {e}")
        sys.exit(1)
    
    if not isinstance(test_users, list):
        print(f"ERROR: {test_users_file} phải là một array!")
        sys.exit(1)
    
    print(f"Found {len(test_users)} test users\n")
    
    # Verify từng user
    results = []
    for i, user in enumerate(test_users, 1):
        username = user.get('username', '')
        password = user.get('password', '')
        
        if not username or not password:
            print(f"User {i}: SKIPPED - Missing username or password")
            results.append((False, username, "Missing credentials"))
            continue
        
        print(f"Testing user {i}: {username}...", end=" ")
        success, status_code, message = verify_user(base_url, username, password)
        results.append((success, username, message))
        
        if success:
            print(f"✅ {message}")
        else:
            print(f"❌ {message}")
    
    # Summary
    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)
    successful = sum(1 for r in results if r[0])
    failed = len(results) - successful
    
    print(f"Total Users: {len(results)}")
    print(f"✅ Successful: {successful}")
    print(f"❌ Failed: {failed}")
    
    if failed > 0:
        print("\nFailed Users:")
        for success, username, message in results:
            if not success:
                print(f"  - {username}: {message}")
        print("\n⚠️  WARNING: Một số users không thể login!")
        print("   Hãy kiểm tra lại credentials trong test_users.json")
        sys.exit(1)
    else:
        print("\n✅ Tất cả users đều có thể login thành công!")
        print("   Có thể chạy performance test an toàn.")
        sys.exit(0)

if __name__ == "__main__":
    main()
