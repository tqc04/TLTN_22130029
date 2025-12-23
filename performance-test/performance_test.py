"""
Performance Testing Tool for Product Service
Supports Stress Test and Load Test for:
- Product Search functionality
- Product Detail View functionality
- User Login functionality
- Add to Cart functionality
"""
import requests
import time
import statistics
import json
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from typing import List, Dict, Optional, Tuple
import argparse
import sys
import random
import uuid


class PerformanceTestResult:
    """Class to store test results"""
    def __init__(self):
        self.total_requests = 0
        self.successful_requests = 0
        self.failed_requests = 0
        self.response_times = []
        self.errors = []
        self.start_time = None
        self.end_time = None
        
    def add_result(self, response_time: float, success: bool, error: Optional[str] = None):
        self.total_requests += 1
        if success:
            self.successful_requests += 1
            self.response_times.append(response_time)
        else:
            self.failed_requests += 1
            # Store response time even for failed requests to calculate stats
            if response_time > 0:
                self.response_times.append(response_time)
            if error:
                self.errors.append(error)
    
    def get_statistics(self) -> Dict:
        """Calculate statistics from results"""
        duration = (self.end_time - self.start_time).total_seconds() if self.end_time and self.start_time else 1
        if duration <= 0:
            duration = 1
        throughput = self.total_requests / duration if duration > 0 else 0
        
        if not self.response_times:
            return {
                'total_requests': self.total_requests,
                'successful_requests': self.successful_requests,
                'failed_requests': self.failed_requests,
                'error_rate': (self.failed_requests / self.total_requests * 100) if self.total_requests > 0 else 0,
                'avg_ms': 0,
                'min_ms': 0,
                'max_ms': 0,
                'throughput': round(throughput, 2)
            }
        
        return {
            'total_requests': self.total_requests,
            'successful_requests': self.successful_requests,
            'failed_requests': self.failed_requests,
            'error_rate': (self.failed_requests / self.total_requests * 100) if self.total_requests > 0 else 0,
            'avg_ms': statistics.mean(self.response_times),
            'min_ms': min(self.response_times),
            'max_ms': max(self.response_times),
            'throughput': round(throughput, 2)
        }


class PerformanceTester:
    """Main class for performance testing"""
    
    def __init__(self, base_url: str = "http://localhost:8080", timeout: int = 30):
        self.base_url = base_url.rstrip('/')
        self.timeout = timeout
        self.session = requests.Session()
        # Add headers to skip auth if needed
        self.session.headers.update({'X-Skip-Auth': 'true'})
    
    def _make_request(self, url: str, params: Optional[Dict] = None, method: str = 'GET', 
                     data: Optional[Dict] = None, headers: Optional[Dict] = None) -> Tuple[float, bool, Optional[str]]:
        """Make a single HTTP request and return (response_time, success, error)"""
        start = time.time()
        try:
            request_headers = self.session.headers.copy()
            if headers:
                request_headers.update(headers)
            
            if method.upper() == 'GET':
                response = self.session.get(url, params=params, timeout=self.timeout, headers=request_headers)
            elif method.upper() == 'POST':
                response = self.session.post(url, json=data, params=params, timeout=self.timeout, headers=request_headers)
            else:
                return (0, False, f"Unsupported method: {method}")
            
            elapsed = (time.time() - start) * 1000  # Convert to milliseconds
            
            if response.status_code in [200, 201]:
                return (elapsed, True, None)
            else:
                # Try to get error message from response
                error_msg = f"HTTP {response.status_code}"
                try:
                    if response.text:
                        import json
                        error_data = response.json()
                        if isinstance(error_data, dict):
                            error_detail = error_data.get('error') or error_data.get('message') or error_data.get('body', '')
                            if error_detail:
                                error_msg = f"HTTP {response.status_code}: {error_detail}"
                except:
                    pass
                return (elapsed, False, error_msg)
        except requests.exceptions.Timeout:
            elapsed = (time.time() - start) * 1000
            return (elapsed, False, "Timeout")
        except requests.exceptions.ConnectionError:
            elapsed = (time.time() - start) * 1000
            return (elapsed, False, "Connection Error")
        except Exception as e:
            elapsed = (time.time() - start) * 1000
            return (elapsed, False, str(e))
    
    def stress_test_search(
        self, 
        num_requests: int, 
        search_query: str = "laptop",
        num_threads: int = None
    ) -> PerformanceTestResult:
        """
        Stress test for product search endpoint
        Sends concurrent requests to test system under load
        """
        if num_threads is None:
            num_threads = min(num_requests, 100)  # Limit concurrent threads
        
        result = PerformanceTestResult()
        result.start_time = datetime.now()
        
        url = f"{self.base_url}/api/products/search"
        params = {"q": search_query, "page": 0, "size": 12}
        
        print(f"\n{'='*60}")
        print(f"STRESS TEST - Product Search")
        print(f"{'='*60}")
        print(f"Total Requests: {num_requests}")
        print(f"Concurrent Threads: {num_threads}")
        print(f"Search Query: {search_query}")
        print(f"URL: {url}")
        print(f"Starting test...\n")
        
        def make_search_request():
            return self._make_request(url, params)
        
        with ThreadPoolExecutor(max_workers=num_threads) as executor:
            futures = [executor.submit(make_search_request) for _ in range(num_requests)]
            
            completed = 0
            for future in as_completed(futures):
                response_time, success, error = future.result()
                result.add_result(response_time, success, error)
                completed += 1
                
                if completed % 50 == 0:
                    print(f"Progress: {completed}/{num_requests} requests completed...")
        
        result.end_time = datetime.now()
        return result
    
    def stress_test_product_detail(
        self,
        num_requests: int,
        product_ids: List[str],
        num_threads: int = None
    ) -> PerformanceTestResult:
        """
        Stress test for product detail endpoint
        Sends concurrent requests to test system under load
        """
        if num_threads is None:
            num_threads = min(num_requests, 100)
        
        if not product_ids:
            raise ValueError("product_ids cannot be empty")
        
        result = PerformanceTestResult()
        result.start_time = datetime.now()
        
        print(f"\n{'='*60}")
        print(f"STRESS TEST - Product Detail View")
        print(f"{'='*60}")
        print(f"Total Requests: {num_requests}")
        print(f"Concurrent Threads: {num_threads}")
        print(f"Available Product IDs: {len(product_ids)}")
        print(f"Starting test...\n")
        
        def make_detail_request():
            # Rotate through product IDs
            import random
            product_id = random.choice(product_ids)
            url = f"{self.base_url}/api/products/{product_id}"
            return self._make_request(url)
        
        with ThreadPoolExecutor(max_workers=num_threads) as executor:
            futures = [executor.submit(make_detail_request) for _ in range(num_requests)]
            
            completed = 0
            for future in as_completed(futures):
                response_time, success, error = future.result()
                result.add_result(response_time, success, error)
                completed += 1
                
                if completed % 50 == 0:
                    print(f"Progress: {completed}/{num_requests} requests completed...")
        
        result.end_time = datetime.now()
        return result
    
    def load_test_search(
        self,
        duration_minutes: int,
        requests_per_second: float = 50,
        search_query: str = "laptop",
        use_filters: bool = False
    ) -> List[PerformanceTestResult]:
        """
        Load test for product search endpoint
        Sends requests continuously over a period of time
        """
        results = []
        total_duration = duration_minutes * 60  # Convert to seconds
        interval = 1.0 / requests_per_second  # Time between requests
        start_time = time.time()
        request_count = 0
        
        print(f"\n{'='*60}")
        print(f"LOAD TEST - Product Search")
        print(f"{'='*60}")
        print(f"Duration: {duration_minutes} minutes")
        print(f"Target Rate: {requests_per_second} requests/second")
        print(f"Search Query: {search_query}")
        print(f"Use Filters: {use_filters}")
        print(f"Starting test...\n")
        
        url = f"{self.base_url}/api/products/search"
        
        # Create result buckets for each minute
        current_minute_result = PerformanceTestResult()
        current_minute_result.start_time = datetime.now()
        last_minute = 0
        
        while (time.time() - start_time) < total_duration:
            # Build params
            params = {"q": search_query, "page": 0, "size": 12}
            if use_filters:
                params["sort"] = "name,asc"
                # Add some random filters occasionally
                import random
                if random.random() < 0.3:  # 30% chance
                    params["sort"] = random.choice(["name,asc", "name,desc", "price,asc", "price,desc"])
            
            response_time, success, error = self._make_request(url, params)
            current_minute_result.add_result(response_time, success, error)
            request_count += 1
            
            # Check if we've moved to a new minute
            current_minute = int((time.time() - start_time) / 60)
            if current_minute > last_minute:
                current_minute_result.end_time = datetime.now()
                results.append(current_minute_result)
                print(f"Minute {current_minute}: {current_minute_result.total_requests} requests completed")
                
                # Start new minute result
                current_minute_result = PerformanceTestResult()
                current_minute_result.start_time = datetime.now()
                last_minute = current_minute
        
        # Add final result
        if current_minute_result.total_requests > 0:
            current_minute_result.end_time = datetime.now()
            results.append(current_minute_result)
        
        return results
    
    def load_test_product_detail(
        self,
        duration_minutes: int,
        requests_per_second: float = 60,
        product_ids: List[str] = None
    ) -> List[PerformanceTestResult]:
        """
        Load test for product detail endpoint
        Sends requests continuously over a period of time
        """
        if product_ids is None or len(product_ids) == 0:
            # Try to get some product IDs first
            print("No product IDs provided, attempting to fetch from API...")
            try:
                response = self.session.get(
                    f"{self.base_url}/api/products",
                    params={"page": 0, "size": 20},
                    timeout=self.timeout
                )
                if response.status_code == 200:
                    data = response.json()
                    product_ids = [item['id'] for item in data.get('content', [])]
                    print(f"Found {len(product_ids)} product IDs")
                else:
                    raise ValueError("Could not fetch product IDs from API")
            except Exception as e:
                raise ValueError(f"Could not fetch product IDs: {e}")
        
        results = []
        total_duration = duration_minutes * 60
        interval = 1.0 / requests_per_second
        start_time = time.time()
        request_count = 0
        
        print(f"\n{'='*60}")
        print(f"LOAD TEST - Product Detail View")
        print(f"{'='*60}")
        print(f"Duration: {duration_minutes} minutes")
        print(f"Target Rate: {requests_per_second} requests/second")
        print(f"Available Product IDs: {len(product_ids)}")
        print(f"Starting test...\n")
        
        # Create result buckets for each minute
        current_minute_result = PerformanceTestResult()
        current_minute_result.start_time = datetime.now()
        last_minute = 0
        
        import random
        
        while (time.time() - start_time) < total_duration:
            product_id = random.choice(product_ids)
            url = f"{self.base_url}/api/products/{product_id}"
            
            response_time, success, error = self._make_request(url)
            current_minute_result.add_result(response_time, success, error)
            request_count += 1
            
            # Check if we've moved to a new minute
            current_minute = int((time.time() - start_time) / 60)
            if current_minute > last_minute:
                current_minute_result.end_time = datetime.now()
                results.append(current_minute_result)
                print(f"Minute {current_minute}: {current_minute_result.total_requests} requests completed")
                
                # Start new minute result
                current_minute_result = PerformanceTestResult()
                current_minute_result.start_time = datetime.now()
                last_minute = current_minute
            
            # Sleep to maintain rate
            time.sleep(interval)
        
        # Add final result
        if current_minute_result.total_requests > 0:
            current_minute_result.end_time = datetime.now()
            results.append(current_minute_result)
        
        return results
    
    def stress_test_login(
        self,
        num_requests: int,
        test_users: List[Dict[str, str]] = None,
        num_threads: int = None
    ) -> PerformanceTestResult:
        """
        Stress test for user login endpoint
        Sends concurrent login requests to test authentication system under load
        """
        if num_threads is None:
            num_threads = min(num_requests, 50)  # Limit concurrent threads for auth
        
        # Default test users if not provided
        if test_users is None:
            test_users = [
                {"username": "product_manager", "password": "password"},
                {"username": "cvc", "password": "123456c"},
                {"username": "admin", "password": "password"},
            ]
        
        result = PerformanceTestResult()
        result.start_time = datetime.now()
        
        url = f"{self.base_url}/api/auth/login"
        
        print(f"\n{'='*60}")
        print(f"STRESS TEST - User Login")
        print(f"{'='*60}")
        print(f"Total Requests: {num_requests}")
        print(f"Concurrent Threads: {num_threads}")
        print(f"Test Users: {len(test_users)}")
        print(f"URL: {url}")
        print(f"Starting test...\n")
        
        def make_login_request():
            # Rotate through test users
            user = random.choice(test_users)
            data = {
                "username": user["username"],
                "password": user["password"]
            }
            return self._make_request(url, method='POST', data=data)
        
        with ThreadPoolExecutor(max_workers=num_threads) as executor:
            futures = [executor.submit(make_login_request) for _ in range(num_requests)]
            
            completed = 0
            for future in as_completed(futures):
                response_time, success, error = future.result()
                result.add_result(response_time, success, error)
                completed += 1
                
                if completed % 50 == 0:
                    print(f"Progress: {completed}/{num_requests} requests completed...")
        
        result.end_time = datetime.now()
        return result
    
    def load_test_login(
        self,
        duration_minutes: int,
        requests_per_second: float = 20,
        test_users: List[Dict[str, str]] = None
    ) -> List[PerformanceTestResult]:
        """
        Load test for user login endpoint
        Sends login requests continuously over a period of time
        """
        # Default test users if not provided
        if test_users is None:
            test_users = [
                {"username": "testuser1", "password": "password123"},
                {"username": "testuser2", "password": "password123"},
                {"username": "admin", "password": "admin123"},
            ]
        
        results = []
        total_duration = duration_minutes * 60
        interval = 1.0 / requests_per_second
        start_time = time.time()
        
        print(f"\n{'='*60}")
        print(f"LOAD TEST - User Login")
        print(f"{'='*60}")
        print(f"Duration: {duration_minutes} minutes")
        print(f"Target Rate: {requests_per_second} requests/second")
        print(f"Test Users: {len(test_users)}")
        print(f"Starting test...\n")
        
        url = f"{self.base_url}/api/auth/login"
        
        # Create result buckets for each minute
        current_minute_result = PerformanceTestResult()
        current_minute_result.start_time = datetime.now()
        last_minute = 0
        
        while (time.time() - start_time) < total_duration:
            # Rotate through test users
            user = random.choice(test_users)
            data = {
                "username": user["username"],
                "password": user["password"]
            }
            
            response_time, success, error = self._make_request(url, method='POST', data=data)
            current_minute_result.add_result(response_time, success, error)
            
            # Check if we've moved to a new minute
            current_minute = int((time.time() - start_time) / 60)
            if current_minute > last_minute:
                current_minute_result.end_time = datetime.now()
                results.append(current_minute_result)
                print(f"Minute {current_minute}: {current_minute_result.total_requests} requests completed")
                
                # Start new minute result
                current_minute_result = PerformanceTestResult()
                current_minute_result.start_time = datetime.now()
                last_minute = current_minute
            
            # Sleep to maintain rate
            time.sleep(interval)
        
        # Add final result
        if current_minute_result.total_requests > 0:
            current_minute_result.end_time = datetime.now()
            results.append(current_minute_result)
        
        return results
    
    def stress_test_add_cart(
        self,
        num_requests: int,
        user_ids: List[str] = None,
        product_ids: List[str] = None,
        num_threads: int = None
    ) -> PerformanceTestResult:
        """
        Stress test for add to cart endpoint
        Sends concurrent add-to-cart requests to test cart system under load
        """
        if num_threads is None:
            num_threads = min(num_requests, 50)
        
        if not user_ids:
            raise ValueError("user_ids cannot be empty")
        if not product_ids:
            raise ValueError("product_ids cannot be empty")
        
        result = PerformanceTestResult()
        result.start_time = datetime.now()
        
        print(f"\n{'='*60}")
        print(f"STRESS TEST - Add to Cart")
        print(f"{'='*60}")
        print(f"Total Requests: {num_requests}")
        print(f"Concurrent Threads: {num_threads}")
        print(f"Available User IDs: {len(user_ids)}")
        print(f"Available Product IDs: {len(product_ids)}")
        print(f"Starting test...\n")
        
        def make_add_cart_request():
            # Random user and product
            user_id = random.choice(user_ids)
            product_id = random.choice(product_ids)
            quantity = random.randint(1, 3)  # Random quantity 1-3
            
            url = f"{self.base_url}/api/cart/add"
            params = {"userId": user_id}
            data = {
                "productId": product_id,
                "quantity": quantity
            }
            return self._make_request(url, method='POST', data=data, params=params)
        
        with ThreadPoolExecutor(max_workers=num_threads) as executor:
            futures = [executor.submit(make_add_cart_request) for _ in range(num_requests)]
            
            completed = 0
            for future in as_completed(futures):
                response_time, success, error = future.result()
                result.add_result(response_time, success, error)
                completed += 1
                
                if completed % 50 == 0:
                    print(f"Progress: {completed}/{num_requests} requests completed...")
        
        result.end_time = datetime.now()
        return result
    
    def load_test_add_cart(
        self,
        duration_minutes: int,
        requests_per_second: float = 30,
        user_ids: List[str] = None,
        product_ids: List[str] = None
    ) -> List[PerformanceTestResult]:
        """
        Load test for add to cart endpoint
        Sends add-to-cart requests continuously over a period of time
        """
        if not user_ids:
            raise ValueError("user_ids cannot be empty")
        if not product_ids:
            raise ValueError("product_ids cannot be empty")
        
        results = []
        total_duration = duration_minutes * 60
        interval = 1.0 / requests_per_second
        start_time = time.time()
        
        print(f"\n{'='*60}")
        print(f"LOAD TEST - Add to Cart")
        print(f"{'='*60}")
        print(f"Duration: {duration_minutes} minutes")
        print(f"Target Rate: {requests_per_second} requests/second")
        print(f"Available User IDs: {len(user_ids)}")
        print(f"Available Product IDs: {len(product_ids)}")
        print(f"Starting test...\n")
        
        url = f"{self.base_url}/api/cart/add"
        
        # Create result buckets for each minute
        current_minute_result = PerformanceTestResult()
        current_minute_result.start_time = datetime.now()
        last_minute = 0
        
        while (time.time() - start_time) < total_duration:
            # Random user and product
            user_id = random.choice(user_ids)
            product_id = random.choice(product_ids)
            quantity = random.randint(1, 3)  # Random quantity 1-3
            
            params = {"userId": user_id}
            data = {
                "productId": product_id,
                "quantity": quantity
            }
            
            response_time, success, error = self._make_request(url, method='POST', data=data, params=params)
            current_minute_result.add_result(response_time, success, error)
            
            # Check if we've moved to a new minute
            current_minute = int((time.time() - start_time) / 60)
            if current_minute > last_minute:
                current_minute_result.end_time = datetime.now()
                results.append(current_minute_result)
                print(f"Minute {current_minute}: {current_minute_result.total_requests} requests completed")
                
                # Start new minute result
                current_minute_result = PerformanceTestResult()
                current_minute_result.start_time = datetime.now()
                last_minute = current_minute
            
            # Sleep to maintain rate
            time.sleep(interval)
        
        # Add final result
        if current_minute_result.total_requests > 0:
            current_minute_result.end_time = datetime.now()
            results.append(current_minute_result)
        
        return results
    
    def print_results(self, result: PerformanceTestResult, test_name: str = "Test"):
        """Print formatted test results"""
        stats = result.get_statistics()
        
        print(f"\n{'='*60}")
        print(f"RESULTS - {test_name}")
        print(f"{'='*60}")
        print(f"Total Requests:     {stats['total_requests']}")
        print(f"Successful:         {stats['successful_requests']}")
        print(f"Failed:             {stats['failed_requests']}")
        print(f"Error Rate:         {stats['error_rate']:.2f}%")
        print(f"Average (ms):       {stats['avg_ms']:.2f}")
        print(f"Min (ms):           {stats['min_ms']:.2f}")
        print(f"Max (ms):           {stats['max_ms']:.2f}")
        print(f"Throughput:         {stats['throughput']:.2f} requests/second")
        
        if result.start_time and result.end_time:
            duration = (result.end_time - result.start_time).total_seconds()
            print(f"Duration:           {duration:.2f} seconds")
        
        if stats['failed_requests'] > 0 and result.errors:
            error_counts = {}
            for error in result.errors:
                error_counts[error] = error_counts.get(error, 0) + 1
            print(f"\nError Breakdown:")
            for error, count in error_counts.items():
                print(f"  {error}: {count}")
    
    def print_load_test_results(self, results: List[PerformanceTestResult], test_name: str = "Load Test"):
        """Print formatted load test results (multiple time periods)"""
        print(f"\n{'='*60}")
        print(f"RESULTS - {test_name}")
        print(f"{'='*60}")
        print(f"{'Time':<10} {'Total':<10} {'Success':<10} {'Failed':<10} {'Requests/s':<12} {'Min (ms)':<10} {'Avg (ms)':<10} {'Max (ms)':<10} {'Error (%)':<10}")
        print("-" * 100)
        
        for i, result in enumerate(results, 1):
            stats = result.get_statistics()
            time_label = f"{i} min"
            print(f"{time_label:<10} {stats['total_requests']:<10} {stats['successful_requests']:<10} {stats['failed_requests']:<10} "
                  f"{stats['throughput']:<12.2f} {stats['min_ms']:<10.2f} {stats['avg_ms']:<10.2f} {stats['max_ms']:<10.2f} "
                  f"{stats['error_rate']:<10.2f}")
            
            # Show error breakdown if there are errors
            if result.errors:
                error_counts = {}
                for error in result.errors:
                    error_counts[error] = error_counts.get(error, 0) + 1
                if error_counts:
                    print(f"           Errors: {', '.join([f'{k}: {v}' for k, v in error_counts.items()])}")
        
        # Calculate averages
        if results:
            total_requests = sum(r.total_requests for r in results)
            total_successful = sum(r.successful_requests for r in results)
            total_failed = sum(r.failed_requests for r in results)
            all_response_times = []
            for r in results:
                all_response_times.extend(r.response_times)
            
            if all_response_times:
                avg_error_rate = (total_failed / total_requests * 100) if total_requests > 0 else 0
                avg_throughput = sum(r.get_statistics()['throughput'] for r in results) / len(results)
                
                print("-" * 100)
                print(f"{'Avg':<10} {total_requests:<10} {total_successful:<10} {total_failed:<10} {avg_throughput:<12.2f} "
                      f"{min(all_response_times):<10.2f} {statistics.mean(all_response_times):<10.2f} "
                      f"{max(all_response_times):<10.2f} {avg_error_rate:<10.2f}")
            else:
                # Show summary even if no successful requests
                avg_error_rate = (total_failed / total_requests * 100) if total_requests > 0 else 0
                avg_throughput = sum(r.get_statistics()['throughput'] for r in results) / len(results) if results else 0
                print("-" * 100)
                print(f"{'Avg':<10} {total_requests:<10} {total_successful:<10} {total_failed:<10} {avg_throughput:<12.2f} "
                      f"{'N/A':<10} {'N/A':<10} {'N/A':<10} {avg_error_rate:<10.2f}")


def main():
    parser = argparse.ArgumentParser(description='Performance Testing Tool for Product Service')
    parser.add_argument('--base-url', default='http://localhost:8080', help='Base URL of the API')
    parser.add_argument('--test-type', choices=['stress-search', 'stress-detail', 'stress-login', 'stress-cart',
                                                'load-search', 'load-detail', 'load-login', 'load-cart', 'all'], 
                       default='all', help='Type of test to run')
    parser.add_argument('--requests', type=int, default=300, help='Number of requests for stress test')
    parser.add_argument('--duration', type=int, default=5, help='Duration in minutes for load test')
    parser.add_argument('--rate', type=float, default=50, help='Requests per second for load test')
    parser.add_argument('--search-query', default='laptop', help='Search query for search tests')
    parser.add_argument('--product-ids', nargs='+', help='Product IDs for detail/cart tests (space-separated)')
    parser.add_argument('--user-ids', nargs='+', help='User IDs for cart tests (space-separated)')
    parser.add_argument('--test-users', help='JSON file with test users for login tests [{"username": "user1", "password": "pass1"}]')
    parser.add_argument('--threads', type=int, help='Number of concurrent threads (default: auto)')
    parser.add_argument('--output', help='Output file for JSON results')
    
    args = parser.parse_args()
    
    tester = PerformanceTester(base_url=args.base_url)
    
    all_results = {}
    
    try:
        # Stress Test - Search (300, 500, 1000 requests)
        if args.test_type in ['stress-search', 'all']:
            print("\n" + "="*80)
            print("RUNNING STRESS TESTS - PRODUCT SEARCH")
            print("="*80)
            
            stress_levels = [300, 500, 1000]
            search_results = []
            
            for level in stress_levels:
                if args.requests and args.test_type != 'all':
                    # Use custom request count if specified
                    level = args.requests
                    result = tester.stress_test_search(level, args.search_query, args.threads)
                    tester.print_results(result, f"Product Search - {level} requests")
                    search_results.append((level, result))
                    break
                else:
                    result = tester.stress_test_search(level, args.search_query, args.threads)
                    tester.print_results(result, f"Product Search - {level} requests")
                    search_results.append((level, result))
            
            all_results['stress_search'] = {
                level: {
                    'stats': r.get_statistics(),
                    'timestamp': r.start_time.isoformat() if r.start_time else None
                }
                for level, r in search_results
            }
        
        # Stress Test - Product Detail
        if args.test_type in ['stress-detail', 'all']:
            print("\n" + "="*80)
            print("RUNNING STRESS TESTS - PRODUCT DETAIL")
            print("="*80)
            
            product_ids = args.product_ids if args.product_ids else None
            if not product_ids:
                # Try to fetch product IDs
                try:
                    response = requests.get(f"{args.base_url}/api/products", 
                                          params={"page": 0, "size": 20},
                                          headers={'X-Skip-Auth': 'true'},
                                          timeout=10)
                    if response.status_code == 200:
                        data = response.json()
                        product_ids = [item['id'] for item in data.get('content', [])]
                        print(f"Auto-fetched {len(product_ids)} product IDs")
                except Exception as e:
                    print(f"Warning: Could not auto-fetch product IDs: {e}")
                    print("Please provide product IDs using --product-ids argument")
                    if args.test_type == 'stress-detail':
                        sys.exit(1)
            
            if product_ids:
                result = tester.stress_test_product_detail(
                    args.requests, product_ids, args.threads
                )
                tester.print_results(result, f"Product Detail - {args.requests} requests")
                all_results['stress_detail'] = {
                    'stats': result.get_statistics(),
                    'timestamp': result.start_time.isoformat() if result.start_time else None
                }
        
        # Load Test - Search
        if args.test_type in ['load-search', 'all']:
            print("\n" + "="*80)
            print("RUNNING LOAD TESTS - PRODUCT SEARCH")
            print("="*80)
            
            results = tester.load_test_search(
                args.duration, args.rate, args.search_query, use_filters=True
            )
            tester.print_load_test_results(results, "Product Search with Filters")
            all_results['load_search'] = [
                {
                    'minute': i+1,
                    'stats': r.get_statistics(),
                    'timestamp': r.start_time.isoformat() if r.start_time else None
                }
                for i, r in enumerate(results)
            ]
        
        # Load Test - Product Detail
        if args.test_type in ['load-detail', 'all']:
            print("\n" + "="*80)
            print("RUNNING LOAD TESTS - PRODUCT DETAIL")
            print("="*80)
            
            product_ids = args.product_ids if args.product_ids else None
            results = tester.load_test_product_detail(
                args.duration, args.rate, product_ids
            )
            tester.print_load_test_results(results, "Product Detail View")
            all_results['load_detail'] = [
                {
                    'minute': i+1,
                    'stats': r.get_statistics(),
                    'timestamp': r.start_time.isoformat() if r.start_time else None
                }
                for i, r in enumerate(results)
            ]
        
        # Stress Test - Login
        if args.test_type in ['stress-login', 'all']:
            print("\n" + "="*80)
            print("RUNNING STRESS TESTS - USER LOGIN")
            print("="*80)
            
            test_users = None
            if args.test_users:
                try:
                    with open(args.test_users, 'r', encoding='utf-8') as f:
                        test_users = json.load(f)
                except Exception as e:
                    print(f"Warning: Could not load test users from file: {e}")
                    print("Using default test users")
            
            result = tester.stress_test_login(args.requests, test_users, args.threads)
            tester.print_results(result, f"User Login - {args.requests} requests")
            all_results['stress_login'] = {
                'stats': result.get_statistics(),
                'timestamp': result.start_time.isoformat() if result.start_time else None
            }
        
        # Load Test - Login
        if args.test_type in ['load-login', 'all']:
            print("\n" + "="*80)
            print("RUNNING LOAD TESTS - USER LOGIN")
            print("="*80)
            
            test_users = None
            if args.test_users:
                try:
                    with open(args.test_users, 'r', encoding='utf-8') as f:
                        test_users = json.load(f)
                except Exception as e:
                    print(f"Warning: Could not load test users from file: {e}")
                    print("Using default test users")
            
            results = tester.load_test_login(args.duration, args.rate, test_users)
            tester.print_load_test_results(results, "User Login")
            all_results['load_login'] = [
                {
                    'minute': i+1,
                    'stats': r.get_statistics(),
                    'timestamp': r.start_time.isoformat() if r.start_time else None
                }
                for i, r in enumerate(results)
            ]
        
        # Stress Test - Add to Cart
        if args.test_type in ['stress-cart', 'all']:
            print("\n" + "="*80)
            print("RUNNING STRESS TESTS - ADD TO CART")
            print("="*80)
            
            user_ids = args.user_ids if args.user_ids else None
            product_ids = args.product_ids if args.product_ids else None
            
            if not user_ids:
                print("Warning: No user IDs provided. Attempting to fetch from API...")
                try:
                    # Try to get user IDs (this would need an API endpoint to list users)
                    # For now, use placeholder
                    print("Please provide user IDs using --user-ids argument")
                    if args.test_type == 'stress-cart':
                        sys.exit(1)
                except Exception as e:
                    print(f"Error: {e}")
                    if args.test_type == 'stress-cart':
                        sys.exit(1)
            
            if not product_ids:
                # Try to fetch product IDs
                try:
                    response = requests.get(f"{args.base_url}/api/products", 
                                          params={"page": 0, "size": 20},
                                          headers={'X-Skip-Auth': 'true'},
                                          timeout=10)
                    if response.status_code == 200:
                        data = response.json()
                        product_ids = [item['id'] for item in data.get('content', [])]
                        print(f"Auto-fetched {len(product_ids)} product IDs")
                except Exception as e:
                    print(f"Warning: Could not auto-fetch product IDs: {e}")
                    print("Please provide product IDs using --product-ids argument")
                    if args.test_type == 'stress-cart':
                        sys.exit(1)
            
            if user_ids and product_ids:
                result = tester.stress_test_add_cart(args.requests, user_ids, product_ids, args.threads)
                tester.print_results(result, f"Add to Cart - {args.requests} requests")
                all_results['stress_cart'] = {
                    'stats': result.get_statistics(),
                    'timestamp': result.start_time.isoformat() if result.start_time else None
                }
        
        # Load Test - Add to Cart
        if args.test_type in ['load-cart', 'all']:
            print("\n" + "="*80)
            print("RUNNING LOAD TESTS - ADD TO CART")
            print("="*80)
            
            user_ids = args.user_ids if args.user_ids else None
            product_ids = args.product_ids if args.product_ids else None
            
            if not user_ids:
                print("Warning: No user IDs provided")
                print("Please provide user IDs using --user-ids argument")
                if args.test_type == 'load-cart':
                    sys.exit(1)
            
            if not product_ids:
                # Try to fetch product IDs
                try:
                    response = requests.get(f"{args.base_url}/api/products", 
                                          params={"page": 0, "size": 20},
                                          headers={'X-Skip-Auth': 'true'},
                                          timeout=10)
                    if response.status_code == 200:
                        data = response.json()
                        product_ids = [item['id'] for item in data.get('content', [])]
                        print(f"Auto-fetched {len(product_ids)} product IDs")
                except Exception as e:
                    print(f"Warning: Could not auto-fetch product IDs: {e}")
                    print("Please provide product IDs using --product-ids argument")
                    if args.test_type == 'load-cart':
                        sys.exit(1)
            
            if user_ids and product_ids:
                results = tester.load_test_add_cart(args.duration, args.rate, user_ids, product_ids)
                tester.print_load_test_results(results, "Add to Cart")
                all_results['load_cart'] = [
                    {
                        'minute': i+1,
                        'stats': r.get_statistics(),
                        'timestamp': r.start_time.isoformat() if r.start_time else None
                    }
                    for i, r in enumerate(results)
                ]
        
        # Save results to file if requested
        if args.output:
            with open(args.output, 'w', encoding='utf-8') as f:
                json.dump(all_results, f, indent=2, ensure_ascii=False)
            print(f"\nResults saved to {args.output}")
        
        print("\n" + "="*80)
        print("ALL TESTS COMPLETED")
        print("="*80)
        
    except KeyboardInterrupt:
        print("\n\nTest interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\nError during testing: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()

