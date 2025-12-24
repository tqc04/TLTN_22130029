# E-Commerce Platform with AI Chatbot

> **Nền tảng thương mại điện tử thiết bị điện tử tích hợp trí tuệ nhân tạo và chatbot**

---

Mục Lục

- [Giới Thiệu](#giới-thiệu)
- [Công Nghệ](#công-nghệ)
- [Kiến Trúc](#kiến-trúc)
- [Chức Năng](#chức-năng)
- [Cài Đặt](#cài-đặt)
- [Chạy Dự Án](#chạy-dự-án)
- [Testing](#testing)
- [Documentation](#documentation)

---

##  Giới Thiệu

Nền tảng thương mại điện tử hiện đại với kiến trúc microservices, tích hợp:
- ✅ AI Chatbot (Gemini API)
- ✅ Hệ thống gợi ý sản phẩm thông minh
- ✅ Phân tích cảm xúc đánh giá
- ✅ OAuth2 (Google, Facebook)
- ✅ Payment Gateway (VNPay, Stripe)
- ✅ Shipping Integration (GHN)

---

## Công Nghệ

### Backend
- **Spring Boot 3.3.4** - Microservices framework
- **Spring Cloud** - Service discovery, API Gateway
- **MySQL 8.0** - Database
- **Redis** - Caching
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

##  Kiến Trúc

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
       ├─► AI Service (8088)        - Chatbot & AI
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

##  Chức Năng

###  AI Chatbot (Core Feature)
- **Trợ lý bán hàng tự động** 24/7
- **Gợi ý sản phẩm thông minh** dựa trên database
- **Context-aware responses** với session management
- **NLP processing** bằng Gemini API
- **Product search** tích hợp trực tiếp với database
- **Public API** - không cần đăng nhập

```

###  User Management
- Đăng ký/Đăng nhập
- OAuth2 (Google, Facebook)
- Quản lý profile
- Reset password
- Role-based access (User, Admin, Employee)

###  Shopping
- Browse products với filter/search
- Product details với reviews
- Shopping cart với Redis caching
- Checkout process
- Order tracking
- Review & rating

###  Payment
- VNPay integration
- Multiple payment methods

### Shipping
- GHN API integration
- Real-time shipping calculation
- Order tracking

###  Marketing & AI Recommendations
- **AI Product Recommendations** (Collaborative + Content-Based Filtering)
  - Personalized suggestions based on user behavior
  - Trained from real user data (reviews, favorites, purchases)
  - Hybrid approach (CF 60% + CBF 40%)
- Voucher system
- Flash sales
- Favorites/Wishlist

###  Admin Dashboard
- User management
- Product management
- Order management
- Sales analytics
- Inventory control

---



