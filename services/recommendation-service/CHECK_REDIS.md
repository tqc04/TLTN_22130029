# Hướng dẫn kiểm tra Redis Connection

## 1. Kiểm tra Redis Server đang chạy

### Windows:
```powershell
# Kiểm tra Redis có đang chạy không
Get-Process -Name redis-server -ErrorAction SilentlyContinue

# Hoặc kiểm tra port 6379
netstat -an | findstr 6379
```

### Linux/Mac:
```bash
# Kiểm tra Redis process
ps aux | grep redis

# Hoặc kiểm tra port
netstat -an | grep 6379
```

## 2. Test Redis Connection qua Endpoint

Sau khi start recommendation-service, gọi endpoint test:

```bash
# Test Redis connection
curl http://localhost:8094/api/recommendations/redis/test

# Hoặc dùng browser
http://localhost:8094/api/recommendations/redis/test
```

### Response khi Redis kết nối thành công:
```json
{
  "connected": true,
  "ping": "OK",
  "writeRead": "OK",
  "testValue": "test-value-...",
  "existingBehaviorKeys": 0,
  "existingPopularityKeys": 0,
  "message": "Redis connection is working properly",
  "timestamp": 1234567890
}
```

### Response khi Redis không kết nối được:
```json
{
  "connected": false,
  "ping": "FAILED",
  "pingError": "Connection refused",
  "message": "Redis connection has issues",
  "error": "..."
}
```

## 3. Kiểm tra Interaction Statistics

```bash
# Lấy thống kê interactions
curl http://localhost:8094/api/recommendations/interactions/stats
```

## 4. Test Track Behavior

```bash
# Track một behavior test
curl -X POST http://localhost:8094/api/recommendations/behavior \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-1",
    "productId": "1",
    "action": "VIEW"
  }'

# Sau đó kiểm tra lại stats
curl http://localhost:8094/api/recommendations/interactions/stats
```

## 5. Cấu hình Redis

File: `src/main/resources/application.yml`

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
```

## 6. Troubleshooting

### Nếu Redis không kết nối được:

1. **Kiểm tra Redis server đang chạy:**
   ```bash
   # Windows (nếu dùng Redis từ WSL hoặc Docker)
   docker ps | grep redis
   
   # Hoặc start Redis
   redis-server
   ```

2. **Kiểm tra firewall:**
   - Port 6379 phải được mở

3. **Kiểm tra logs:**
   - Xem logs của recommendation-service
   - Tìm các error messages về Redis

4. **Test Redis trực tiếp:**
   ```bash
   redis-cli ping
   # Nếu trả về "PONG" thì Redis đang chạy
   ```

## 7. Lưu ý

- Nếu Redis không available, service vẫn chạy được (vì `@Autowired(required = false)`)
- Interaction statistics sẽ trả về 0 nếu Redis không kết nối được
- Dữ liệu sẽ được lưu trong Redis với TTL 90 ngày

