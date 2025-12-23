# 🛒 E-Commerce Platform with AI Chatbot

> **Nền tảng thương mại điện tử thiết bị điện tử tích hợp trí tuệ nhân tạo và chatbot**

---

## 📋 Mục Lục

- [Giới Thiệu](#giới-thiệu)
- [Công Nghệ](#công-nghệ)
- [Kiến Trúc](#kiến-trúc)
- [Chức Năng](#chức-năng)
- [Cài Đặt](#cài-đặt)
- [Chạy Dự Án](#chạy-dự-án)
- [Testing](#testing)
- [Documentation](#documentation)

---

## 🎯 Giới Thiệu

Nền tảng thương mại điện tử hiện đại với kiến trúc microservices, tích hợp:
- ✅ AI Chatbot (Gemini API)
- ✅ Hệ thống gợi ý sản phẩm thông minh
- ✅ Phân tích cảm xúc đánh giá
- ✅ OAuth2 (Google, Facebook)
- ✅ Payment Gateway (VNPay, Stripe)
- ✅ Shipping Integration (GHN)

---

## 🛠️ Công Nghệ

### Backend
- **Spring Boot 3.3.4** - Microservices framework
- **Spring Cloud** - Service discovery, API Gateway
- **MySQL 8.0** - Database
- **Redis** - Caching
- **Apache Kafka** - Message queue
- **Eureka** - Service registry

### Frontend
- **ReactJS 18** - UI framework
- **Material-UI (MUI)** - Component library
- **TypeScript** - Type safety
- **Vite** - Build tool

### AI & ML
- **Gemini API** - AI chatbot & content generation
- **Python FastAPI** - ML recommendation service
- **Scikit-learn** - Collaborative & Content-Based Filtering
- **Simple CF Model** - User/Item bias recommendation

---

## 🏗️ Kiến Trúc

```
┌─────────────┐
│   Frontend  │ (React + MUI)
│  Port: 5173 │
└──────┬──────┘
       │
┌──────▼──────────────────────────────────────┐
│        API Gateway (Port: 8080)             │
└──────┬──────────────────────────────────────┘
       │
       ├─► Auth Service (8081)      - Authentication & OAuth2
       ├─► User Service (8082)      - User management
       ├─► Product Service (8083)   - Products CRUD
       ├─► Cart Service (8084)      - Shopping cart
       ├─► Order Service (8085)     - Order processing
       ├─► Payment Service (8086)   - VNPay, Stripe
       ├─► Review Service (8087)    - Product reviews
       ├─► AI Service (8088)        - 🤖 Chatbot & AI
       ├─► Category Service (8089)  - Categories
       ├─► Brand Service (8090)     - Brands
       ├─► Inventory Service (8091) - Stock management
       ├─► Voucher Service (8092)   - Coupons & vouchers
       ├─► Warranty Service (8093)  - Warranty tracking
       ├─► Notification (8094)      - Email/SMS
       ├─► Favorites Service (8095) - Wishlist
       └─► Admin Service (8096)     - Admin dashboard

Support Services:
├─► Config Server (8888)    - Centralized config
├─► Discovery Server (8761) - Eureka service registry
└─► Reco Service (8000)     - Python ML recommendations
```

---

## ✨ Chức Năng

### 🤖 AI Chatbot (Core Feature)
- **Trợ lý bán hàng tự động** 24/7
- **Gợi ý sản phẩm thông minh** dựa trên database
- **Context-aware responses** với session management
- **NLP processing** bằng Gemini API
- **Product search** tích hợp trực tiếp với database
- **Public API** - không cần đăng nhập

**Endpoints:**
```bash
POST /api/ai/chat/public           # Public chat
POST /api/ai/chat                   # Authenticated chat
GET  /api/ai/chat/history/{userId} # Chat history
POST /api/ai/chat/product-recommendations # Product suggestions
```

### 👤 User Management
- Đăng ký/Đăng nhập
- OAuth2 (Google, Facebook)
- Quản lý profile
- Reset password
- Role-based access (User, Admin, Employee)

### 🛍️ Shopping
- Browse products với filter/search
- Product details với reviews
- Shopping cart với Redis caching
- Checkout process
- Order tracking
- Review & rating

### 💳 Payment
- VNPay integration
- Stripe integration
- Multiple payment methods

### 📦 Shipping
- GHN API integration
- Real-time shipping calculation
- Order tracking

### 🎁 Marketing & AI Recommendations
- **AI Product Recommendations** (Collaborative + Content-Based Filtering)
  - Personalized suggestions based on user behavior
  - Trained from real user data (reviews, favorites, purchases)
  - Hybrid approach (CF 60% + CBF 40%)
- Voucher system
- Flash sales
- Favorites/Wishlist

### 📊 Admin Dashboard
- User management
- Product management
- Order management
- Sales analytics
- Inventory control

---

## 🚀 Cài Đặt

### Requirements
- Java 17+
- Node.js 18+
- MySQL 8.0
- Redis
- Maven
- Python 3.8+ (for ML service)

### 1. Clone Repository
```bash
git clone <repository-url>
cd Buildd30_7/Buildd43
```

### 2. Setup Database
```sql
-- Create database
CREATE DATABASE ecommerce_db;

-- Import schema (tự động với Hibernate)
```

### 3. Configure Environment
Copy `env.example` và điền thông tin:
```bash
# Database
DB_USERNAME=root
DB_PASSWORD=your_password

# Gemini API (Required for chatbot)
GEMINI_API_KEY=your_gemini_api_key

# OAuth2 (Optional)
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_secret
FACEBOOK_APP_ID=your_facebook_app_id
FACEBOOK_APP_SECRET=your_facebook_secret

# Payment (Optional)
VNPAY_MERCHANT_ID=your_vnpay_id
VNPAY_SECRET_KEY=your_vnpay_key

# Shipping (Optional)
GHN_API_TOKEN=your_ghn_token

# Email (Optional)
SMTP_HOST=smtp.gmail.com
SMTP_USER=your_email@gmail.com
SMTP_PASSWORD=your_app_password
```

### 4. Install Dependencies

**Backend:**
```bash
cd services
mvn clean install
```

**Frontend:**
```bash
cd frontend
npm install
```

---

## 🎮 Chạy Dự Án

### Option 1: Start All Services (Recommended)

```bash
# From root directory
cd services
.\START.ps1
```

### Option 2: Start Individual Services

**Backend Services:**
```bash
# 1. Config Server (start first)
cd services/config-server
mvn spring-boot:run

# 2. Discovery Server
cd services/discovery-server
mvn spring-boot:run

# 3. Gateway
cd services/gateway
mvn spring-boot:run

# 4. AI Service (Chatbot) - Priority
cd services/ai-service
mvn spring-boot:run

# 5. Other services...
cd services/[service-name]
mvn spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm run dev
# Access: http://localhost:5173
```

**ML Recommendation Service (Python):**
```bash
cd reco_service
pip install -r requirements.txt
python app.py
# Access: http://localhost:8000

# Train new model (optional)
cd services/ml-training
python extract_training_data.py  # Extract from DB
python train_simple_model.py     # Train model
# Copy model files to reco_service/ and restart
```

---

## 🧪 Testing

### Quick Test - Chatbot
```bash
.\check-service.ps1   # Check if service is running
.\test-simple.ps1     # Quick chatbot test
.\test-chatbot-fixed.ps1  # Full test suite
```

### Manual API Testing

**Health Check:**
```bash
curl http://localhost:8088/api/ai/health
```

**Chat Test:**
```bash
curl -X POST http://localhost:8088/api/ai/chat/public \
  -H "Content-Type: application/json" \
  -d '{"message": "Tôi muốn mua laptop gaming"}'
```

**Expected Response:**
```json
{
  "success": true,
  "sessionId": "abc-123",
  "response": "Dựa trên yêu cầu của bạn, tôi gợi ý..."
}
```

---

## 📚 Documentation

### Important Files
- `DE_CUONG_ANALYSIS.md` - Phân tích đề cương tiểu luận
- `FEATURE_CHECKLIST.md` - Checklist tất cả chức năng
- `CHATBOT_BUGFIXES.md` - Bugs đã fix trong chatbot
- `CHATBOT_QUICKSTART.md` - Quick guide cho chatbot
- `FACEBOOK_LOGIN_SETUP.md` - Setup Facebook OAuth
- `GOOGLE_OAUTH_SETUP.md` - Setup Google OAuth
- `FINAL_PROJECT_STATUS.md` - Tổng kết dự án

### Service Documentation
- Each service có README riêng trong `services/[service-name]/`
- ML Recommendation: `reco_service/README.md`
- ML Training: `services/ml-training/README.md`
- Recommendation Logic: `services/ml-training/RECOMMENDATION_LOGIC.md`

---

## 🐛 Troubleshooting

### Port Conflicts
```bash
# Check port usage
netstat -ano | findstr :8088

# Kill process
taskkill /PID <PID> /F
```

### Database Connection
```bash
# Check MySQL running
Get-Service MySQL*

# Start MySQL
net start MySQL80
```

### Chatbot Not Responding
1. Check if AI service started: `.\check-service.ps1`
2. Verify Gemini API key in `application.yml`
3. Check logs in service window
4. Wait 30 seconds after "Started AiServiceApplication"

---

## 📊 Project Status

**Completion:** 99% ✅

### Implemented Features:
- ✅ All core microservices (16 services)
- ✅ AI Chatbot với database integration
- ✅ **AI Product Recommendations** (Collaborative + Content-Based Filtering)
- ✅ Product management (CRUD, search, filter)
- ✅ Shopping cart với Redis
- ✅ Order processing
- ✅ Payment integration (VNPay)
- ✅ OAuth2 (Google, Facebook)
- ✅ Review system với sentiment analysis
- ✅ Admin dashboard
- ✅ Responsive frontend (React + MUI)

### Ready For:
- ✅ Testing
- ✅ Demo
- ✅ Bảo vệ tiểu luận
- ✅ Production deployment

---

## 👨‍💻 Author

**Từ Quang Chương**
- MSSV: 22130029
- Lớp: DH22DTB
- Khoa: Công nghệ thông tin

**Giảng viên hướng dẫn:** TS. Nguyễn Thị Phương Trâm

---

## 📝 License

This project is for educational purposes (Graduation Thesis).

---

## 🎉 Quick Start Commands

```bash
# Start ALL services (Infrastructure + AI + Business)
cd services
.\RUN.bat

# Check health
.\check-health.ps1

# Test AI Recommendations
.\test-ai-recommendation.bat

# Start frontend
cd ..\frontend
npm run dev

# Stop all services
cd ..\services
.\STOP.bat
```

---

## 📚 Documentation

**Quick References:**
- 🚀 **[QUICK_START.md](services/QUICK_START.md)** - Daily usage guide
- 🤖 **[AI_RECOMMENDATION_QUICKSTART.md](services/AI_RECOMMENDATION_QUICKSTART.md)** - AI system guide
- 📊 **[GENERATE_DATA_GUIDE.md](services/ml-training/GENERATE_DATA_GUIDE.md)** - Data generation

**Detailed Guides:**
- 📖 [Services README](services/README.md) - Services overview
- 🔧 [ML Training README](services/ml-training/README.md) - Model training

---

**🚀 Project sẵn sàng để test và demo!**

*Last updated: 2025-12-10*

