import axios, { AxiosInstance, AxiosResponse } from 'axios'

// Debounce utility for search requests
const debounce = <T extends (...args: any[]) => any>(func: T, wait: number): T => {
  let timeout: NodeJS.Timeout;
  return ((...args: any[]) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  }) as T;
};

// Base API configuration: use relative paths for Vite proxy
// Increased timeout for order creation which may involve multiple service calls
const REQUEST_TIMEOUT = 30000 // 30 seconds for complex operations like order creation

// Create axios instance with default configuration
const apiClient: AxiosInstance = axios.create({
  baseURL: '', // Empty baseURL to use relative paths with Vite proxy
  timeout: REQUEST_TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
})

// Request interceptor for auth tokens and performance optimization
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token') || localStorage.getItem('jwt')

    // Add performance headers
    if (config.headers) {
      config.headers['X-Requested-With'] = 'XMLHttpRequest';
      config.headers['Cache-Control'] = 'no-cache';
    }

    // Only log in development mode
    if (import.meta.env.DEV) {
      console.log('=== REQUEST INTERCEPTOR ===');
      console.log('Request interceptor - Token found:', token ? 'YES' : 'NO');
      console.log('Request interceptor - URL:', config.url);
      console.log('Request interceptor - Method:', config.method);
    }

    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`
      if (import.meta.env.DEV) {
        console.log('Request interceptor - Authorization header set');
      }
    } else if (import.meta.env.DEV) {
      console.warn('Request interceptor - No token found');
    }

    // Propagate user identity and role for gateway/services that rely on headers
    try {
      const rawUser = localStorage.getItem('auth_user')
      if (rawUser && config.headers) {
        const user = JSON.parse(rawUser)
        if (user?.id || user?.userId) config.headers['X-User-Id'] = String(user.id || user.userId)
        if (user?.role || user?.roles?.[0]) config.headers['X-User-Role'] = String(user.role || user.roles?.[0])
      }
    } catch {}

    if (import.meta.env.DEV) {
      console.log('=== END REQUEST INTERCEPTOR ===');
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for error handling and caching
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    // Add cache headers for successful responses
    if (response.status >= 200 && response.status < 300) {
      // Cache successful GET requests for 5 minutes
      if (response.config.method?.toUpperCase() === 'GET') {
        const url = response.config.url;
        if (url && (url.includes('/orders/') || url.includes('/products/') || url.includes('/categories/'))) {
          // These are relatively static data, cache them
          response.headers['cache-control'] = 'public, max-age=300';
        }
      }
    }

    // Only log in development mode
    if (import.meta.env.DEV) {
      console.log('=== RESPONSE INTERCEPTOR - SUCCESS ===');
      console.log('Response interceptor - Success:', {
        url: response.config.url,
        status: response.status,
        method: response.config.method,
        cached: response.headers['x-cached'] === 'true'
      });
      console.log('=== END RESPONSE INTERCEPTOR ===');
    }
    return response;
  },
  (error) => {
    // Always log errors, but with different levels
    if (import.meta.env.DEV) {
      console.log('=== RESPONSE INTERCEPTOR - ERROR ===');
      console.log('Response interceptor - Error:', {
        url: error.config?.url,
        status: error.response?.status,
        method: error.config?.method,
        responseURL: error.response?.request?.responseURL
      });
      console.log('Response interceptor - Error response data:', error.response?.data);
    } else {
      // Production: only log critical errors
      console.error('API Error:', {
        url: error.config?.url,
        status: error.response?.status,
        method: error.config?.method
      });
    }
    
    if (error.response?.status === 401) {
      if (import.meta.env.DEV) {
        console.warn('=== 401 UNAUTHORIZED DETECTED ===');
        console.warn('Unauthorized request detected');
        console.warn('Error response data:', error.response?.data);
        console.warn('Error response status:', error.response?.status);
      }
      
      // Check if this is a public endpoint that shouldn't require auth
      const publicEndpoints = [
        '/api/products',
        '/api/brands', 
        '/api/categories',
        '/api/reviews',
        '/api/vouchers',
        '/api/shipping',
        '/api/payments/vnpay',
        '/api/orders/health',
        '/api/orders/number',
        '/api/chatbot',
        '/api/ai',
        '/api/recommendations'
      ];
      
      const isPublicEndpoint = publicEndpoints.some(endpoint => 
        error.config?.url?.includes(endpoint)
      );
      
      // Only logout if token is completely invalid or if it's a protected endpoint
      if (!isPublicEndpoint && (
        error.response?.data?.message?.includes('token expired') || 
        error.response?.data?.message?.includes('invalid token') ||
        error.response?.data?.message?.includes('Unauthorized') ||
        error.response?.data?.error?.includes('Unauthorized')
      )) {
        if (import.meta.env.DEV) {
          console.log('Token is invalid, logging out');
        }
        localStorage.removeItem('auth_token')
        localStorage.removeItem('jwt')
        localStorage.removeItem('auth_user')
        
        if (window.location.pathname !== '/login' && window.location.pathname !== '/signup') {
          window.location.href = '/login'
        }
      } else if (import.meta.env.DEV) {
        console.warn('401 error but token might still be valid or endpoint is public - not logging out');
      }
    }
    
    if (import.meta.env.DEV) {
      console.log('=== END RESPONSE INTERCEPTOR ===');
    }
    return Promise.reject(error)
  }
)

// Types for API responses
export interface ApiResponse<T = unknown> {
  data: T
  message?: string
  success: boolean
}

export interface Product {
  id: string
  name: string
  description: string
  price: number
  imageUrl?: string
  images?: Array<{
    id: number
    imageUrl: string
    alt?: string
    isPrimary?: boolean
  }>
  category: string
  brand: string
  categoryId?: number
  brandId?: number
  stockQuantity: number
  rating?: number
  averageRating?: number
  reviewCount?: number
  sku?: string
  isActive?: boolean
  createdAt: string
  updatedAt: string
  isOnSale?: boolean
  salePrice?: number
  saleStartAt?: string
  saleEndAt?: string
  variants?: ProductVariant[]
}

export interface ProductVariant {
  id: number
  productId: string
  variantName: string
  sku: string
  color?: string
  size?: string
  material?: string
  style?: string
  price: number
  costPrice?: number
  stockQuantity: number
  weightKg?: number
  dimensions?: string
  isActive: boolean
  isDefault: boolean
  imageUrl?: string
  barcode?: string
  additionalAttributes?: string
  createdAt: string
  updatedAt: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface User {
  id: string
  username: string
  email: string
  firstName: string
  lastName: string
  phoneNumber?: string
  address?: string
  dateOfBirth?: string
  avatarUrl?: string
  profileImageUrl?: string
  role: string
  isEmailVerified?: boolean
  personalizationEnabled?: boolean
  chatbotEnabled?: boolean
  recommendationEnabled?: boolean
  notificationSettings?: string
  createdAt: string
}

export interface Order {
  id: number
  orderNumber: string
  userId: string
  status: string
  totalAmount: number
  subtotal: number
  taxAmount: number
  shippingFee: number
  discountAmount: number
  shippingAddress: string
  billingAddress: string
  notes: string
  createdAt: string
  updatedAt: string
  deliveredDate: string
  userFullName: string
  orderItems: OrderItem[]
  paymentMethod?: string
  paymentStatus?: string
}

export interface OrderItem {
  id: number
  productId: string
  quantity: number
  unitPrice: number
  price: number
  totalPrice: number
  productName: string
  productSku: string
  productImage: string
}

export interface ChatMessage {
  id?: number
  sessionId: string
  message: string
  response?: string
  messageType: 'USER' | 'BOT'
  timestamp: string
}

export interface DashboardStats {
  totalUsers: number
  totalProducts: number
  totalOrders: number
  totalRevenue: number
  recentOrders: Order[]
  topProducts: Product[]
}

export interface Brand {
  id: number
  name: string
}

export interface Category {
  id: number
  name: string
}

// API Service class
class ApiService {
  private normalizeProductVariant(raw: any, fallbackProductId: string = ''): ProductVariant {
    const toNumber = (v: any, fallback = 0): number => {
      if (v === undefined || v === null) return fallback
      if (typeof v === 'number') return v
      if (typeof v === 'string') {
        const n = Number(v)
        return Number.isNaN(n) ? fallback : n
      }
      // BigDecimal-like object or nested shape
      if (typeof v === 'object') {
        const nested = v.value ?? v.amount ?? v.number ?? v.low ?? v.high
        if (nested !== undefined) return toNumber(nested, fallback)
      }
      return fallback
    }

    const toBoolean = (v: any, fallback = false): boolean => {
      if (v === undefined || v === null) return fallback
      if (typeof v === 'boolean') return v
      if (typeof v === 'string') return v.toLowerCase() === 'true'
      if (typeof v === 'number') return v !== 0
      return fallback
    }

    const now = new Date().toISOString()

    return {
      id: toNumber(raw?.id, 0),
      productId: String(raw?.productId ?? raw?.product?.id ?? fallbackProductId ?? ''),
      variantName: String(raw?.variantName ?? raw?.name ?? ''),
      sku: String(raw?.sku ?? ''),
      color: raw?.color ?? undefined,
      size: raw?.size ?? undefined,
      material: raw?.material ?? undefined,
      style: raw?.style ?? undefined,
      price: toNumber(raw?.price, 0),
      costPrice: raw?.costPrice != null ? toNumber(raw?.costPrice, 0) : undefined,
      stockQuantity: toNumber(raw?.stockQuantity, 0),
      weightKg: raw?.weightKg != null ? toNumber(raw?.weightKg, 0) : undefined,
      dimensions: raw?.dimensions ?? undefined,
      // Backend serializes boolean getters as `active` and `default` (Jackson), while UI expects `isActive`/`isDefault`
      isActive: toBoolean(raw?.isActive ?? raw?.active, true),
      isDefault: toBoolean(raw?.isDefault ?? raw?.default, false),
      imageUrl: raw?.imageUrl ?? undefined,
      barcode: raw?.barcode ?? undefined,
      additionalAttributes: raw?.additionalAttributes ?? undefined,
      createdAt: raw?.createdAt ? String(raw.createdAt) : now,
      updatedAt: raw?.updatedAt ? String(raw.updatedAt) : now
    }
  }
  // Health check
  async healthCheck(): Promise<unknown> {
    try {
      // Use Vite proxy for actuator as well
      const response = await axios.get('/actuator/health')
      return response.data
    } catch (error) {
      throw error
    }
  }

  // Products API with debouncing for search
  async getProducts(page = 0, size = 12, search?: string, category?: string): Promise<ApiResponse<{ content: Product[]; totalElements: number }>> {
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
        sort: 'id,asc'
      })
      if (search) params.append('search', search)
      if (category) params.append('category', category)

      const response = await apiClient.get(`/api/products?${params}`, { headers: { 'X-Skip-Auth': 'true' } })
      return {
        data: response.data,
        success: true,
      }
    } catch (error) {
      throw error
    }
  }

  async getSaleProducts(limit = 12): Promise<ApiResponse<Product[]>> {
    try {
      const response = await apiClient.get(`/api/products/on-sale?size=${limit}&page=0`, { headers: { 'X-Skip-Auth': 'true' } })
      // Backend returns Page<ProductDTO>, extract content array
      const products = response.data?.content || response.data || []
      return { data: products, success: true }
    } catch (error) {
      throw error
    }
  }

  // Flash Sale slots (time windows) - Public
  async getFlashSaleSlots(): Promise<ApiResponse<string[]>> {
    try {
      const response = await apiClient.get(`/api/products/flash-sale/slots`, { headers: { 'X-Skip-Auth': 'true' } })
      return { data: Array.isArray(response.data) ? response.data : [], success: true }
    } catch (error) {
      return { data: [], success: false, message: 'Failed to load flash sale slots' }
    }
  }

  // Flash Sale products for a specific slot (2-hour window) - Public
  async getFlashSaleProducts(slotStart: string, limit = 24): Promise<ApiResponse<Product[]>> {
    try {
      const params = new URLSearchParams({
        onSale: 'true',
        saleTimeSlot: slotStart,
        page: '0',
        size: String(limit),
        sort: 'salePrice,asc'
      })
      const response = await apiClient.get(`/api/products?${params.toString()}`, { headers: { 'X-Skip-Auth': 'true' } })
      const products = response.data?.content || response.data || []
      return { data: Array.isArray(products) ? products : [], success: true }
    } catch (error) {
      return { data: [], success: false, message: 'Failed to load flash sale products' }
    }
  }

  async getNewProducts(limit = 12): Promise<ApiResponse<Product[]>> {
    try {
      // Lấy nhiều hơn để đảm bảo có đủ sản phẩm sau khi filter theo tháng
      const response = await apiClient.get(`/api/products?size=${limit * 3}&page=0&sort=createdAt,desc`, { headers: { 'X-Skip-Auth': 'true' } })
      // Backend returns Page<ProductDTO>, extract content array
      const allProducts = response.data?.content || response.data || []
      
      // Filter sản phẩm được tạo trong 1 tháng gần nhất
      const oneMonthAgo = new Date()
      oneMonthAgo.setMonth(oneMonthAgo.getMonth() - 1)
      
      const recentProducts = allProducts
        .filter((product: Product) => {
          if (!product.createdAt) return false
          const createdAt = new Date(product.createdAt)
          return createdAt >= oneMonthAgo
        })
        .slice(0, limit) // Chỉ lấy số lượng cần thiết
      
      return { data: recentProducts, success: true }
    } catch (error) {
      throw error
    }
  }

  async getBestSellingProducts(limit = 12): Promise<ApiResponse<Product[]>> {
    try {
      const response = await apiClient.get(`/api/products/best-selling?size=${limit}&page=0`, { headers: { 'X-Skip-Auth': 'true' } })
      // Backend returns Page<ProductDTO>, extract content array
      const products = response.data?.content || response.data || []
      return { data: products, success: true }
    } catch (error) {
      throw error
    }
  }

  async getProduct(id: string): Promise<ApiResponse<Product>> {
    try {
      const response = await apiClient.get(`/api/products/${id}`, { headers: { 'X-Skip-Auth': 'true' } })
      return {
        data: response.data,
        success: true,
      }
    } catch (error) {
      throw error
    }
  }

  async getProductImages(productId: string): Promise<ApiResponse<Array<{ id: number; imageUrl: string; isPrimary?: boolean; displayOrder?: number; altText?: string }>>> {
    try {
      const response = await apiClient.get(`/api/products/${productId}/images`, { headers: { 'X-Skip-Auth': 'true' } })
      const images = response.data?.images || response.data || []
      return {
        data: images,
        success: true
      }
    } catch (error: any) {
      console.error('Error fetching product images:', error)
      return { 
        data: [], 
        success: false, 
        message: error.message || 'Failed to fetch product images'
      }
    }
  }

  // Product Variants API
  async getProductVariants(productId: string): Promise<ApiResponse<ProductVariant[]>> {
    try {
      const response = await apiClient.get(`/api/products/${productId}/variants`, { headers: { 'X-Skip-Auth': 'true' } })
      return {
        data: Array.isArray(response.data)
          ? response.data.map((v: any) => this.normalizeProductVariant(v, productId))
          : [],
        success: true,
      }
    } catch (error) {
      throw error
    }
  }

  async getDefaultVariant(productId: string): Promise<ApiResponse<ProductVariant>> {
    try {
      const response = await apiClient.get(`/api/products/${productId}/variants/default`, { headers: { 'X-Skip-Auth': 'true' } })
      return {
        data: this.normalizeProductVariant(response.data, productId),
        success: true,
      }
    } catch (error) {
      throw error
    }
  }

  // Admin Variants API
  async createVariant(productId: string, variant: Partial<ProductVariant>): Promise<ApiResponse<ProductVariant>> {
    try {
      const response = await apiClient.post(`/api/products/${productId}/variants`, variant)
      return {
        data: response.data,
        success: true,
      }
    } catch (error) {
      throw error
    }
  }

  async updateVariant(variantId: number, variant: Partial<ProductVariant>): Promise<ApiResponse<ProductVariant>> {
    try {
      const response = await apiClient.put(`/api/products/variants/${variantId}`, variant)
      return {
        data: response.data,
        success: true,
      }
    } catch (error) {
      throw error
    }
  }

  async deleteVariant(variantId: number): Promise<ApiResponse<void>> {
    try {
      await apiClient.delete(`/api/products/variants/${variantId}`)
      return {
        data: undefined,
        success: true,
      }
    } catch (error) {
      throw error
    }
  }

  // Admin: get ALL variants (including inactive)
  async adminGetProductVariants(productId: string): Promise<ApiResponse<ProductVariant[]>> {
    try {
      const response = await apiClient.get(`/api/admin/products/${productId}/variants`)
      const raw = (response.data as any)?.variants ?? response.data
      const list = Array.isArray(raw) ? raw : []
      return {
        data: list.map((v: any) => this.normalizeProductVariant(v, productId)),
        success: true
      }
    } catch (error) {
      throw error
    }
  }

  // Orders API
  async getOrders(page = 0, size = 10, userId?: string): Promise<ApiResponse<{ content: Order[], totalElements: number }>> {
    try {
      // Use my-orders endpoint to get current user's orders
      const currentUser = JSON.parse(localStorage.getItem('auth_user') || '{}');
      const uid = userId || currentUser.id || currentUser.userId || 'anonymous';

      const response = await apiClient.get(`/api/orders/my-orders?userId=${uid}&page=${page}&size=${size}`)
      return {
        data: response.data,
        success: true,
      }
    } catch (error) {
      throw error
    }
  }

  async getOrderByNumber(orderNumber: string): Promise<ApiResponse<Order>> {
    try {
      const response = await apiClient.get(`/api/orders/number/${orderNumber}`)
      return {
        data: response.data,
        success: true,
      }
    } catch (error) {
      throw error
    }
  }

  async cancelOrderByNumber(orderNumber: string, reason: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post(`/api/orders/by-number/${orderNumber}/cancel`, { reason }, {
        headers: {
          'X-Skip-Auth': 'true'
        }
      })
      return {
        data: response.data,
        success: true,
      }
    } catch (error) {
      console.error('Failed to cancel order:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to cancel order'
      }
    }
  }

  // Debounced search for better performance
  private debouncedSearch: ((search: string, category?: string) => Promise<ApiResponse<{ content: Product[]; totalElements: number }>>) | null = null

  async searchProducts(search: string, category?: string): Promise<ApiResponse<{ content: Product[]; totalElements: number }>> {
    if (!this.debouncedSearch) {
      this.debouncedSearch = debounce(async (searchTerm: string, cat?: string) => {
        return this.getProducts(0, 12, searchTerm, cat);
      }, 300); // 300ms debounce
    }

    return this.debouncedSearch(search, category);
  }

  async createOrder(orderData: Record<string, unknown>): Promise<ApiResponse<Order>> {
    try {
      const response = await apiClient.post('/api/orders', orderData)
      return {
        data: response.data,
        success: true,
        message: 'Order created successfully',
      }
    } catch (error) {
      throw error
    }
  }

  async cancelOrder(orderId: number, reason: string): Promise<ApiResponse<void>> {
    try {
      // First get order details to get orderNumber
      const orderResponse = await apiClient.get(`/api/orders/${orderId}`)
      if (orderResponse.data && orderResponse.data.orderNumber) {
        const orderNumber = orderResponse.data.orderNumber
        const response = await apiClient.post(`/api/orders/by-number/${orderNumber}/cancel`, { reason })
        return {
          data: response.data,
          success: true,
          message: 'Order cancelled successfully',
        }
      } else {
        throw new Error('Order number not found')
      }
    } catch (error) {
      console.error('Failed to cancel order:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to cancel order'
      }
    }
  }

  // ===== ADMIN NAMESPACE =====
  admin = {
    // Products
    getProducts: async (page = 0, size = 20, search = ''): Promise<ApiResponse<{ content: any[]; totalElements: number }>> => {
      try {
        const params = new URLSearchParams({ page: String(page), size: String(size) })
        if (search) params.append('q', search)
        const res = await apiClient.get(`/api/admin/products?${params.toString()}`)
        return { data: res.data, success: true }
      } catch (e) {
        return { data: { content: [], totalElements: 0 }, success: false, message: 'Failed to load products' }
      }
    },
    createProduct: async (payload: Record<string, unknown>): Promise<ApiResponse<any>> => {
      try {
        const res = await apiClient.post('/api/admin/products', payload)
        return { data: res.data, success: true }
      } catch (e) {
        return { data: undefined, success: false, message: 'Failed to create product' }
      }
    },
    updateProduct: async (id: number, payload: Record<string, unknown>): Promise<ApiResponse<any>> => {
      try {
        const res = await apiClient.put(`/api/admin/products/${id}`, payload)
        return { data: res.data, success: true }
      } catch (e) {
        return { data: undefined, success: false, message: 'Failed to update product' }
      }
    },
    deleteProduct: async (id: number): Promise<ApiResponse<void>> => {
      try {
        await apiClient.delete(`/api/admin/products/${id}`)
        return { data: undefined, success: true }
      } catch (e) {
        return { data: undefined, success: false, message: 'Failed to delete product' }
      }
    },

    // Users (user-service)
    getUsers: async (page = 0, size = 20, search = ''): Promise<ApiResponse<{ content: any[]; totalElements: number }>> => {
      try {
        const params = new URLSearchParams({ page: String(page), size: String(size) })
        if (search) params.append('q', search)
        const res = await apiClient.get(`/api/users?${params.toString()}`)
        return { data: res.data, success: true }
      } catch (e) {
        return { data: { content: [], totalElements: 0 }, success: false, message: 'Failed to load users' }
      }
    },
    setUserRole: async (userId: string, role: string): Promise<ApiResponse<void>> => {
      try {
        await apiClient.post(`/api/users/${userId}/role`, { role })
        return { data: undefined, success: true }
      } catch (e) {
        return { data: undefined, success: false, message: 'Failed to set user role' }
      }
    },
    toggleUserLock: async (userId: string, locked: boolean): Promise<ApiResponse<void>> => {
      try {
        await apiClient.post(`/api/users/${userId}/${locked ? 'lock' : 'unlock'}`)
        return { data: undefined, success: true }
      } catch (e) {
        return { data: undefined, success: false, message: 'Failed to update user lock' }
      }
    },

    createUser: async (payload: Record<string, unknown>): Promise<ApiResponse<any>> => {
      // Use user registration endpoint for admin-created users
      try {
        const res = await apiClient.post('/api/users/register', payload)
        return { data: res.data, success: true }
      } catch (e) {
        return { data: undefined, success: false, message: 'Failed to create user' }
      }
    },
    updateUser: async (userId: number, payload: Record<string, unknown>): Promise<ApiResponse<any>> => {
      // Use profile update with admin override
      try {
        const params = new URLSearchParams({ userId: String(userId) })
        const res = await apiClient.put(`/api/users/profile?${params.toString()}`, payload)
        return { data: res.data, success: true }
      } catch (e) {
        return { data: undefined, success: false, message: 'Failed to update user' }
      }
    },
    deleteUser: async (userId: number): Promise<ApiResponse<void>> => {
      try {
        const res = await apiClient.delete(`/api/users/${userId}`)
        return { data: res.data, success: true }
      } catch (e) {
        return { data: undefined, success: false, message: 'Failed to delete user' }
      }
    },

    // Vouchers (voucher-service)
    getVouchers: async (page = 0, size = 20, search = ''): Promise<ApiResponse<{ content: any[]; totalElements: number }>> => {
      try {
        const params = new URLSearchParams({ page: String(page), size: String(size) })
        if (search) params.append('q', search)
        const res = await apiClient.get(`/api/vouchers?${params.toString()}`)
        return { data: res.data, success: true }
      } catch (e) {
        return { data: { content: [], totalElements: 0 }, success: false, message: 'Failed to load vouchers' }
      }
    },
    createVoucher: async (payload: Record<string, unknown>): Promise<ApiResponse<any>> => {
      try {
        const res = await apiClient.post('/api/vouchers', payload)
        return { data: res.data, success: true }
      } catch (e) {
        return { data: undefined, success: false, message: 'Failed to create voucher' }
      }
    },
    updateVoucher: async (id: number, payload: Record<string, unknown>): Promise<ApiResponse<any>> => {
      try {
        const res = await apiClient.put(`/api/vouchers/${id}`, payload)
        return { data: res.data, success: true }
      } catch (e) {
        return { data: undefined, success: false, message: 'Failed to update voucher' }
      }
    },
    deleteVoucher: async (id: number): Promise<ApiResponse<void>> => {
      try {
        await apiClient.delete(`/api/vouchers/${id}`)
        return { data: undefined, success: true }
      } catch (e) {
        return { data: undefined, success: false, message: 'Failed to delete voucher' }
      }
    },

    // Orders (order-service)
    getOrders: async (page = 0, size = 20, status?: string, q?: string): Promise<ApiResponse<{ content: any[]; totalElements: number }>> => {
      try {
        const params = new URLSearchParams({ page: String(page), size: String(size) })
        if (status) params.append('status', status)
        if (q) params.append('q', q)
        const res = await apiClient.get(`/api/orders?${params.toString()}`)
        return { data: res.data, success: true }
      } catch (e) {
        return { data: { content: [], totalElements: 0 }, success: false, message: 'Failed to load orders' }
      }
    },
    confirmOrder: async (orderId: number): Promise<ApiResponse<any>> => {
      try {
        const res = await apiClient.post(`/api/orders/${orderId}/confirm`)
        return { data: res.data, success: true }
      } catch (e) {
        return { data: undefined, success: false, message: 'Failed to confirm order' }
      }
    },
    updateOrderStatus: async (orderId: number, status: string, trackingNumber?: string): Promise<ApiResponse<any>> => {
      try {
        const res = await apiClient.post(`/api/orders/${orderId}/status`, { status, trackingNumber })
        return { data: res.data, success: true }
      } catch (e) {
        return { data: undefined, success: false, message: 'Failed to update order status' }
      }
    },
    refundOrder: async (orderId: number, reason = 'Admin refund'): Promise<ApiResponse<any>> => {
      try {
        const res = await apiClient.post(`/api/orders/${orderId}/refund`, { reason })
        return { data: res.data, success: true }
      } catch (e) {
        return { data: undefined, success: false, message: 'Failed to refund order' }
      }
    }
  }

  // Cart API
  async getCart(userId?: string): Promise<ApiResponse<unknown>> {
    try {
      const params = userId ? `?userId=${userId}` : ''
      const response = await apiClient.get(`/api/cart${params}`, {
        headers: {
          'X-Skip-Auth': 'true'
        }
      })
      return {
        data: response.data,
        success: true,
        message: 'Cart retrieved successfully',
      }
    } catch (error) {
      throw error
    }
  }

  async addToCart(productId: string, quantity: number, userId?: string, variantId?: number): Promise<ApiResponse<unknown>> {
    try {
      const params = userId ? `?userId=${userId}` : ''
      const payload: any = {
        productId,
        quantity
      }
      if (variantId !== undefined && variantId !== null) {
        payload.variantId = variantId
      }
      const response = await apiClient.post(`/api/cart/add${params}`, payload, {
        headers: {
          'X-Skip-Auth': 'true'
        }
      })
      return {
        data: response.data,
        success: true,
        message: 'Product added to cart successfully',
      }
    } catch (error) {
      throw error
    }
  }

  async updateCartItem(productId: string, quantity: number, userId?: string): Promise<ApiResponse<unknown>> {
    try {
      if (!userId) {
        throw new Error('Missing userId for updateCartItem')
      }
      const response = await apiClient.put(`/api/cart/${userId}/update`, {
        productId,
        quantity
      }, {
        headers: {
          'X-Skip-Auth': 'true'
        }
      })
      return {
        data: response.data,
        success: true,
        message: 'Cart item updated successfully',
      }
    } catch (error) {
      throw error
    }
  }

  async removeFromCart(productId: string, userId?: string): Promise<ApiResponse<unknown>> {
    try {
      if (!userId) {
        throw new Error('Missing userId for removeFromCart')
      }
      const response = await apiClient.delete(`/api/cart/${userId}/remove/${productId}`, {
        headers: {
          'X-Skip-Auth': 'true'
        }
      })
      return {
        data: response.data,
        success: true,
        message: 'Product removed from cart successfully',
      }
    } catch (error) {
      throw error
    }
  }

  async clearCart(userId?: string): Promise<ApiResponse<unknown>> {
    try {
      if (!userId) {
        throw new Error('Missing userId for clearCart')
      }
      const response = await apiClient.delete(`/api/cart/${userId}/clear`, {
        headers: {
          'X-Skip-Auth': 'true'
        }
      })
      return {
        data: response.data,
        success: true,
        message: 'Cart cleared successfully',
      }
    } catch (error) {
      throw error
    }
  }

  async getCartItemCount(userId?: number): Promise<ApiResponse<number>> {
    try {
      const params = userId ? `?userId=${userId}` : ''
      const response = await apiClient.get(`/api/cart/count${params}`, {
        headers: {
          'X-Skip-Auth': 'true'
        }
      })
      return {
        data: response.data,
        success: true,
        message: 'Cart count retrieved successfully',
      }
    } catch (error) {
      throw error
    }
  }

  async mergeGuestCart(userId: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post(`/api/cart/merge?userId=${userId}`, {}, {
        headers: {
          'X-Skip-Auth': 'true'
        }
      })
      return {
        data: response.data,
        success: true,
        message: 'Guest cart merged successfully',
      }
    } catch (error) {
      throw error
    }
  }

  // Users API - Now calls the real backend endpoint
  async getUsers(page = 0, size = 100): Promise<ApiResponse<Page<User>>> {
    try {
      if (import.meta.env.DEV) {
        console.log('Calling getUsers API:', `/api/users?page=${page}&size=${size}`);
      }
      const response = await apiClient.get(`/api/users?page=${page}&size=${size}`);
      if (import.meta.env.DEV) {
        console.log('Users API response:', response.data);
        console.log('Users content:', response.data.content);
        console.log('Total users:', response.data.totalElements);
      }
      return {
        data: response.data,
        success: true,
      }
    } catch (error) {
      console.error('Failed to fetch users:', error)
      throw error
    }
  }
  
  // Admin - Update user role
  async updateUserRole(userId: string, newRole: string, reason?: string): Promise<ApiResponse<unknown>> {
    try {
      if (import.meta.env.DEV) {
        console.log('Updating user role:', { userId, newRole, reason });
      }
      const response = await apiClient.put(`/api/admin/users/${userId}/role`, {
        role: newRole,
        reason: reason || 'Role updated by admin'
      });
      return { data: response.data, success: true };
    } catch (error) {
      console.error('Failed to update user role:', error);
      throw error;
    }
  }
  
  // Admin - Toggle user status
  async toggleUserStatus(userId: string, isActive: boolean): Promise<ApiResponse<unknown>> {
    try {
      if (import.meta.env.DEV) {
        console.log('Toggling user status:', { userId, isActive });
      }
      const response = await apiClient.put(`/api/admin/users/${userId}/status`, {
        isActive: isActive
      });
      return { data: response.data, success: true };
    } catch (error) {
      console.error('Failed to toggle user status:', error);
      throw error;
    }
  }
  
  // Admin - Create admin user (for testing)
  async createAdminUser(): Promise<ApiResponse<unknown>> {
    try {
      if (import.meta.env.DEV) {
        console.log('Creating admin user...');
      }
      const response = await apiClient.post('/api/admin/create-admin');
      return { data: response.data, success: true };
    } catch (error) {
      console.error('Failed to create admin user:', error);
      throw error;
    }
  }

  // Admin - Users
  async adminCreateUser(payload: Partial<User> & { password: string, username: string }): Promise<ApiResponse<User>> {
    const response = await apiClient.post(`/api/admin/users`, payload)
    return { data: response.data, success: true }
  }
  async adminUpdateUser(id: string, payload: Partial<User>): Promise<ApiResponse<User>> {
    try {
      if (import.meta.env.DEV) {
        console.log('=== ADMIN UPDATE USER ===');
        console.log('Admin updating user:', { id, payload });
        console.log('Current localStorage:', {
          auth_token: localStorage.getItem('auth_token') ? 'EXISTS' : 'NOT_FOUND',
          jwt: localStorage.getItem('jwt') ? 'EXISTS' : 'NOT_FOUND',
          auth_user: localStorage.getItem('auth_user') ? 'EXISTS' : 'NOT_FOUND'
        });
      }
      
      const response = await apiClient.put(`/api/admin/users/${id}`, payload);
      if (import.meta.env.DEV) {
        console.log('User update API response:', response.data);
        console.log('=== END ADMIN UPDATE USER ===');
      }
      return { data: response.data, success: true }
    } catch (error) {
      console.error('=== ADMIN UPDATE USER ERROR ===');
      console.error('Failed to update user:', error);
      if (import.meta.env.DEV) {
        // unknown error shape, log raw
        console.error('Error response (raw):', error);
      }
      console.error('=== END ADMIN UPDATE USER ERROR ===');
      throw error
    }
  }
  async adminDeleteUser(id: string): Promise<ApiResponse<unknown>> {
    const response = await apiClient.delete(`/api/admin/users/${id}`)
    return { data: response.data, success: true }
  }



  // Admin - Products
  async adminGetProducts(page = 0, size = 10): Promise<ApiResponse<{ content: Product[]; totalElements: number }>> {
    try {
      if (import.meta.env.DEV) {
        console.log('Calling adminGetProducts API:', `/api/admin/products?page=${page}&size=${size}`)
        console.log('Request headers:', apiClient.defaults.headers)
      }
      
      const response = await apiClient.get(`/api/admin/products?page=${page}&size=${size}`)
      if (import.meta.env.DEV) {
        console.log('Admin products API response:', response)
        console.log('Response status:', response.status)
        console.log('Response data:', response.data)
        console.log('Response URL:', response.config.url)
      }
      
      return { data: response.data, success: true }
    } catch (error) {
      console.error('Error fetching admin products:', error)
      throw error
    }
  }
  async adminCreateProduct(payload: Partial<Product>): Promise<ApiResponse<Product>> {
    const response = await apiClient.post('/api/admin/products', payload)
    return { data: response.data, success: true }
  }
  async adminUpdateProduct(id: string, payload: Partial<Product>): Promise<ApiResponse<Product>> {
    const response = await apiClient.put(`/api/admin/products/${id}`, payload)
    return { data: response.data, success: true }
  }
  async adminDeleteProduct(id: string): Promise<ApiResponse<unknown>> {
    const response = await apiClient.delete(`/api/admin/products/${id}`)
    return { data: response.data, success: true }
  }

  // Admin - Inventory/Restock
  async adminRestockProduct(productId: string, quantity: number, warehouseId?: number): Promise<ApiResponse<unknown>> {
    const response = await apiClient.post('/api/inventory/restock', { 
      productId, 
      quantity,
      warehouseId 
    })
    return { data: response.data, success: true }
  }

  // Admin - Fix all products stock (DEVELOPMENT)
  async adminFixAllProductStock(): Promise<ApiResponse<unknown>> {
    const response = await apiClient.post('/api/admin/products/fix-stock')
    return { data: response.data, success: true }
  }

  // Admin - Orders
  async adminGetOrders(page = 0, size = 10, status?: string): Promise<ApiResponse<{ content: Order[], totalElements: number }>> {
    try {
      const qs = new URLSearchParams({ page: String(page), size: String(size) })
      if (status) qs.set('status', status)
      const response = await apiClient.get(`/api/admin/orders?${qs.toString()}`)
      return { data: response.data, success: true }
    } catch (error) {
      console.error('Error fetching admin orders:', error)
      throw error
    }
  }
  async adminGetOrder(orderId: number): Promise<ApiResponse<Order>> {
    try {
      const response = await apiClient.get(`/api/admin/orders/${orderId}`)
      return { data: response.data, success: true }
    } catch (error) {
      console.error('Error fetching order details:', error)
      throw error
    }
  }
  async adminUpdateOrderStatus(orderId: number, status: string, reason?: string): Promise<ApiResponse<unknown>> {
    const response = await apiClient.put(`/api/admin/orders/${orderId}/status`, { status, reason })
    return { data: response.data, success: true }
  }

  // Admin - Uploads
  async adminUploadImage(file: File): Promise<ApiResponse<{ url: string; filename: string }>> {
    const form = new FormData()
    form.append('file', file)
    const response = await apiClient.post('/api/admin/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return { data: response.data, success: true }
  }

  // Admin - Dashboard stats
  async adminGetDashboardStats(): Promise<ApiResponse<unknown>> {
    const response = await apiClient.get('/api/admin/dashboard/stats')
    return { data: response.data, success: true }
  }

  // Admin - Product Images (multi)
  async adminListProductImages(productId: string): Promise<ApiResponse<unknown[]>> {
    const response = await apiClient.get(`/api/admin/products/${productId}/images`)
    return { data: response.data, success: true }
  }
  async adminUploadProductImages(productId: string, files: File[]): Promise<ApiResponse<unknown[]>> {
    const form = new FormData()
    files.forEach(f => form.append('files', f))
    const response = await apiClient.post(`/api/admin/products/${productId}/images`, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return { data: response.data, success: true }
  }
  async adminDeleteProductImage(productId: string, imageId: string): Promise<ApiResponse<unknown>> {
    const response = await apiClient.delete(`/api/admin/products/${productId}/images/${imageId}`)
    return { data: response.data, success: true }
  }

  /**
   * User login
   */
  async login(username: string, password: string): Promise<ApiResponse<{ token: string, user: User }>> {
    try {
      if (import.meta.env.DEV) {
        console.log('API Login - Calling /api/auth/login with username:', username);
      }
      const response = await apiClient.post('/api/auth/login', {
        username,
        password
      })
      if (import.meta.env.DEV) {
        console.log('API Login - Raw response:', response);
        console.log('API Login - Response data:', response.data);
      }
      
      const token: string = response.data.token;
      const userRaw: any = response.data.user || {};
      // Normalize user object
      try {
        // Map userId -> id if needed
        if (userRaw && userRaw.userId && !userRaw.id) {
          userRaw.id = userRaw.userId;
        }
        // If backend didn't include role, derive from JWT
        if (!userRaw?.role && token) {
          const parts = token.split('.');
          if (parts.length === 3) {
            const payload = JSON.parse(atob(parts[1]));
            if (payload && payload.role) {
              userRaw.role = String(payload.role);
            }
          }
        }
      } catch (_e) {
        // ignore normalization errors
      }

      // Normalize role from backend: prefer user.role; if missing, try jwt payload 'role'
      try {
        if (!userRaw?.role && token) {
          const parts = token.split('.');
          if (parts.length === 3) {
            const payload = JSON.parse(atob(parts[1]));
            if (payload && payload.role) {
              userRaw.role = String(payload.role).toUpperCase();
            }
          }
        } else if (userRaw?.role) {
          userRaw.role = String(userRaw.role).toUpperCase();
        }
      } catch (_) {}

      const result = {
        data: {
          token: token,
          user: userRaw
        },
        message: response.data.message || 'Login successful',
        success: response.data.success || true
      };
      
      if (import.meta.env.DEV) {
        console.log('API Login - Processed result:', result);
      }
      return result;
    } catch (error) {
      console.error('API Login - Error:', error);
      throw error
    }
  }

  /**
   * User registration
   */
  async register(userData: {
    username: string
    email: string
    password: string
    firstName: string
    lastName: string
    phoneNumber?: string
  }): Promise<ApiResponse<{ user: User }>> {
    try {
      const response = await apiClient.post('/api/auth/register', userData)
      
      return {
        data: {
          user: response.data.user
        },
        message: response.data.message || 'Registration successful',
        success: response.data.success || true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Send OTP to phone number
   */
  async sendPhoneOtp(phone: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post('/api/auth/phone/send-otp', { phone })
      return {
        data: response.data,
        message: response.data.message || 'OTP sent',
        success: response.data.success || true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Verify phone OTP and create/login user
   */
  async verifyPhoneOtp(payload: {
    phone: string
    otp: string
    username?: string
    password?: string
    firstName?: string
    lastName?: string
    email?: string
  }): Promise<ApiResponse<{ token: string, user: User }>> {
    try {
      const response = await apiClient.post('/api/auth/phone/verify-otp', payload)
      return {
        data: {
          token: response.data.token,
          user: response.data.user
        },
        message: response.data.message || 'Phone verified',
        success: response.data.success || true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Forgot password - Send reset email
   */
  async forgotPassword(email: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post('/api/auth/forgot-password', { email })
      
      return {
        data: response.data,
        message: response.data.message || 'Reset email sent',
        success: response.data.success || true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Reset password with token
   */
  async resetPassword(token: string, password: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post('/api/auth/reset-password', { token, password })
      
      return {
        data: response.data,
        message: response.data.message || 'Password reset successful',
        success: response.data.success || true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Verify email with token
   */
  async verifyEmail(token: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post('/api/auth/verify-email', { token })
      return {
        data: response.data,
        message: response.data.message || 'Email verified successfully',
        success: response.data.success || true
      }
    } catch (error: any) {
      const message = error?.response?.data?.message 
        || error?.response?.data?.error 
        || error?.response?.data?.body 
        || 'Verification failed.'
      return { data: undefined, success: false, message }
    }
  }

  /**
   * Change password (authenticated user)
   */
  async changePassword(currentPassword: string, newPassword: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post('/api/users/change-password', {
        currentPassword,
        newPassword
      })
      
      return {
        data: response.data,
        message: response.data.message || 'Password changed successfully',
        success: response.data.success || true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Update user preferences
   */
  async updatePreferences(preferences: {
    personalizationEnabled: boolean
    chatbotEnabled: boolean
    recommendationEnabled: boolean
  }): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.put('/api/users/preferences', preferences)
      return {
        data: response.data,
        message: response.data.message || 'Preferences updated successfully',
        success: response.data.success || true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Update notification settings
   */
  async updateNotificationSettings(settings: {
    emailNotifications: boolean
    pushNotifications: boolean
    orderUpdates?: boolean
    promotionalEmails?: boolean
    securityAlerts?: boolean
  }): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.put('/api/users/notifications', settings)
      return {
        data: response.data,
        message: response.data.message || 'Notification settings updated successfully',
        success: response.data.success || true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Get user profile
   */
  async getUserProfile(): Promise<ApiResponse<User>> {
    try {
      // Get user from localStorage to extract identifier
      const storedUser = localStorage.getItem('auth_user');
      let identifier = '';
      
      if (storedUser) {
        try {
          const userData = JSON.parse(storedUser);
          identifier = userData.email || userData.username || '';
        } catch (e) {
          console.error('Failed to parse stored user data');
        }
      }
      
      // If no identifier from localStorage, try to decode from JWT token
      if (!identifier) {
        const token = localStorage.getItem('auth_token') || localStorage.getItem('jwt');
        if (token) {
          try {
            // Decode JWT payload (format: header.payload.signature)
            const payload = JSON.parse(atob(token.split('.')[1]));
            identifier = payload.sub || payload.email || payload.username || '';
          } catch (e) {
            console.error('Failed to decode JWT token:', e);
          }
        }
      }
      
      if (!identifier) {
        throw new Error('No user identifier available');
      }
      
      if (import.meta.env.DEV) {
        console.log('API getUserProfile - Calling /api/users/profile with identifier:', identifier);
        console.log('Request headers:', apiClient.defaults.headers);
      }
      
      const response = await apiClient.get('/api/users/profile', {
        params: { identifier }
      })
      
      if (import.meta.env.DEV) {
        console.log('API getUserProfile - Response:', response);
      }
      
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('API getUserProfile - Error:', error);
      throw error
    }
  }

  /**
   * Update user profile
   */
  async updateUserProfile(userData: Partial<User>): Promise<ApiResponse<User>> {
    try {
      const response = await apiClient.put('/api/users/profile', userData)
      return {
        data: response.data.user || response.data,
        message: response.data.message || 'Profile updated successfully',
        success: response.data.success || true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Upload avatar image
   */
  async uploadAvatar(userId: string, file: File): Promise<ApiResponse<{ avatarUrl: string; user: User }>> {
    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('userId', userId)

      const response = await apiClient.post('/api/users/avatar/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })

      return {
        data: {
          avatarUrl: response.data.avatarUrl,
          user: response.data.user
        },
        message: response.data.message || 'Avatar uploaded successfully',
        success: response.data.success || true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Add product to favorites
   */
  async addToFavorites(productId: string, userId: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post(`/api/favorites/user/${userId}/add`, {
        productId
      })
      return {
        data: response.data,
        message: 'Added to favorites',
        success: true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Remove product from favorites
   */
  async removeFromFavorites(productId: string, userId: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.delete(`/api/favorites/user/${userId}/remove/${productId}`)
      return {
        data: response.data,
        message: 'Removed from favorites',
        success: true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Get user favorites
   */
  async getFavorites(userId: string): Promise<ApiResponse<Product[]>> {
    try {
      const response = await apiClient.get(`/api/favorites/user/${userId}`)

      const favorites = Array.isArray(response.data) ? response.data : []

      const toNumber = (value: unknown, fallback?: number): number | undefined => {
        if (value === undefined || value === null) return fallback
        if (typeof value === 'number') return value
        if (typeof value === 'string') {
          const parsed = Number(value)
          return Number.isNaN(parsed) ? fallback : parsed
        }
        if (typeof value === 'object') {
          const record = value as Record<string, unknown>
          const nested =
            record.value ??
            record.amount ??
            record.number ??
            record.low ??
            record.high

          if (nested !== undefined) {
            return toNumber(nested, fallback)
          }
        }
        return fallback
      }

      const toStringSafe = (value: unknown, fallback = ''): string => {
        if (value === undefined || value === null) return fallback
        if (typeof value === 'string') return value
        try {
          return String(value)
        } catch {
          return fallback
        }
      }

      const toIsoString = (value: unknown): string => {
        if (!value) return new Date().toISOString()
        if (typeof value === 'string') return value
        try {
          return new Date(value as string).toISOString()
        } catch {
          return new Date().toISOString()
        }
      }

      const products = favorites
        .map((favorite: any) => {
          const resolvedProductId =
            favorite?.productId ??
            favorite?.product?.id ??
            favorite?.id

          const productIdString = resolvedProductId ? String(resolvedProductId) : null

          if (!productIdString || productIdString.trim() === '') {
            if (import.meta.env.DEV) {
              console.warn('Favorites API: invalid product id', { favorite })
            }
            return null
          }

          const basePrice =
            toNumber(favorite?.productPrice ?? favorite?.price, 0) ?? 0
          const salePriceValue = toNumber(
            favorite?.salePrice ?? favorite?.productSalePrice
          )

          const product: Product = {
            id: productIdString,
            name: toStringSafe(
              favorite?.productName ?? favorite?.name,
              'Unknown product'
            ),
            description: toStringSafe(
              favorite?.productDescription ?? favorite?.description,
              ''
            ),
            price: basePrice,
            imageUrl:
              favorite?.productImage ??
              favorite?.imageUrl ??
              undefined,
            images: Array.isArray(favorite?.images)
              ? favorite.images
              : undefined,
            category: toStringSafe(
              favorite?.productCategory ?? favorite?.category,
              'Uncategorized'
            ),
            brand: toStringSafe(
              favorite?.productBrand ?? favorite?.brand,
              'Unknown'
            ),
            categoryId: toNumber(favorite?.categoryId),
            brandId: toNumber(favorite?.brandId),
            stockQuantity:
              toNumber(
                favorite?.stockQuantity ?? favorite?.productStock,
                0
              ) ?? 0,
            rating: toNumber(favorite?.rating),
            averageRating: toNumber(favorite?.averageRating),
            reviewCount: toNumber(favorite?.reviewCount),
            sku: favorite?.sku
              ? toStringSafe(favorite?.sku)
              : undefined,
            isActive:
              typeof favorite?.isActive === 'boolean'
                ? favorite.isActive
                : undefined,
            createdAt: toIsoString(favorite?.createdAt),
            updatedAt: toIsoString(
              favorite?.updatedAt ??
                favorite?.lastModifiedAt ??
                favorite?.createdAt
            ),
            isOnSale:
              typeof favorite?.isOnSale === 'boolean'
                ? favorite.isOnSale
                : salePriceValue !== undefined &&
                  salePriceValue < basePrice,
            salePrice: salePriceValue,
            saleStartAt: favorite?.saleStartAt
              ? toIsoString(favorite.saleStartAt)
              : undefined,
            saleEndAt: favorite?.saleEndAt
              ? toIsoString(favorite.saleEndAt)
              : undefined,
            variants: Array.isArray(favorite?.variants)
              ? favorite.variants
              : undefined,
          }

          return product
        })
        .filter((product): product is Product => Boolean(product))

      return {
        data: products,
        success: true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Send AI chat message (Public - No auth required)
   */
  async sendChatMessagePublic(message: string, userId?: string): Promise<ApiResponse<ChatMessage>> {
    try {
      // Get userId from localStorage if not provided
      if (!userId) {
        try {
          const rawUser = localStorage.getItem('auth_user')
          if (rawUser) {
            const user = JSON.parse(rawUser)
            userId = user?.id || user?.userId
          }
        } catch (e) {
          // Ignore parsing errors
        }
      }
      
      const response = await apiClient.post('/api/ai/chat/public', {
        message,
        userId: userId || undefined // Only send userId if it exists
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Send AI chat message (With session)
   */
  async sendChatMessage(message: string, sessionId?: string): Promise<ApiResponse<ChatMessage>> {
    try {
      const response = await apiClient.post('/api/ai/chat', {
        message,
        sessionId
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Get chat logs (Admin)
   */
  async getChatLogs(
    page: number = 0,
    size: number = 20,
    userId?: string,
    isProductRelated?: boolean
  ): Promise<ApiResponse<{
    data: any[]
    totalElements: number
    totalPages: number
    currentPage: number
    size: number
  }>> {
    try {
      const params = new URLSearchParams()
      params.append('page', page.toString())
      params.append('size', size.toString())
      if (userId) params.append('userId', userId)
      if (isProductRelated !== undefined) params.append('isProductRelated', isProductRelated.toString())

      const response = await apiClient.get(`/api/ai/admin/chat-logs?${params.toString()}`)
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Get chat log statistics (Admin)
   */
  async getChatLogStatistics(): Promise<ApiResponse<any>> {
    try {
      const response = await apiClient.get('/api/ai/admin/chat-logs/statistics')
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Submit feedback for chat log
   */
  async submitChatLogFeedback(
    logId: number,
    feedback?: string,
    rating?: number
  ): Promise<ApiResponse<any>> {
    try {
      const response = await apiClient.post(`/api/ai/chat-logs/${logId}/feedback`, {
        feedback,
        rating
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Get missing product queries (Admin)
   */
  async getMissingProductQueries(
    page: number = 0,
    size: number = 20
  ): Promise<ApiResponse<{
    data: any[]
    totalElements: number
    totalPages: number
    currentPage: number
  }>> {
    try {
      const params = new URLSearchParams()
      params.append('page', page.toString())
      params.append('size', size.toString())

      const response = await apiClient.get(`/api/ai/admin/chat-logs/missing-products?${params.toString()}`)
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Get chat history
   */
  async getChatHistory(sessionId: string): Promise<ApiResponse<ChatMessage[]>> {
    try {
      const response = await apiClient.get(`/api/ai/chat/history/${sessionId}`)
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Create a new AI chat session
   */
  async createChatSession(userId: string, initialMessage: string = ''): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post('/api/ai/chat/session', {
        userId,
        title: initialMessage || 'New Chat'
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      throw error
    }
  }

  /**
   * Logout
   */
  async logout(): Promise<void> {
    // Clear any stored tokens
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
  }

  // Inventory API
  async getInventory(page = 0, size = 10): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.get(`/api/inventory?page=${page}&size=${size}`)
      return {
        data: response.data,
        success: true,
      }
    } catch (error) {
      throw error
    }
  }

  // Categories API
  async getCategoriesWithCount(): Promise<unknown[]> {
    const response = await apiClient.get('/api/categories/with-count', { headers: { 'X-Skip-Auth': 'true' } });
    return response.data;
  }

  async getAllBrands(): Promise<ApiResponse<Brand[]>> {
    const response = await apiClient.get('/api/brands', { headers: { 'X-Skip-Auth': 'true' } })
    return { data: response.data || [], success: true }
  }

  async getAllCategories(): Promise<ApiResponse<Category[]>> {
    const response = await apiClient.get('/api/categories', { headers: { 'X-Skip-Auth': 'true' } })
    return { data: response.data || [], success: true }
  }

  // Product-service meta (SQL-backed) brands/categories used by products.brandId/categoryId
  async getProductMetaBrands(): Promise<ApiResponse<Brand[]>> {
    try {
      const response = await apiClient.get('/api/products/meta/brands', { headers: { 'X-Skip-Auth': 'true' } })
      return { data: response.data || [], success: true }
    } catch (e) {
      return { data: [], success: false, message: 'Failed to load product meta brands' }
    }
  }

  async getProductMetaCategories(): Promise<ApiResponse<Category[]>> {
    try {
      const response = await apiClient.get('/api/products/meta/categories', { headers: { 'X-Skip-Auth': 'true' } })
      return { data: response.data || [], success: true }
    } catch (e) {
      return { data: [], success: false, message: 'Failed to load product meta categories' }
    }
  }

  async getCategory(id: number): Promise<ApiResponse<{id: number; name: string; description?: string}>> {
    try {
      const response = await apiClient.get(`/api/categories/${id}`, { headers: { 'X-Skip-Auth': 'true' } })
      return {
        data: response.data,
        success: true
      }
    } catch (error: any) {
      console.error('Failed to get category:', error)
      return {
        data: { id, name: `Category ${id}` },
        success: false,
        message: error.response?.data?.message || 'Failed to get category'
      }
    }
  }

  async getBrand(id: number): Promise<ApiResponse<{id: number; name: string; description?: string}>> {
    try {
      const response = await apiClient.get(`/api/brands/${id}`, { headers: { 'X-Skip-Auth': 'true' } })
      return {
        data: response.data,
        success: true
      }
    } catch (error: any) {
      console.error('Failed to get brand:', error)
      return {
        data: { id, name: `Brand ${id}` },
        success: false,
        message: error.response?.data?.message || 'Failed to get brand'
      }
    }
  }

  // Shipping endpoints used by EnhancedCheckout
  async getProvinces(): Promise<ApiResponse<unknown[]>> {
    try {
      const response = await apiClient.get('/api/shipping/provinces', {
        headers: {
          'X-Skip-Auth': 'true'
        }
      });
      return { data: response.data, success: true };
    } catch (error: any) {
      console.error('GHN API Error - Provinces:', error);

      // Provide fallback data or better error handling
      if (error.response?.status === 401 || error.response?.status === 500) {
        const errorMessage = error.response?.data?.error || '';
        if (errorMessage.includes('GHN_API_TOKEN') || errorMessage.includes('missing')) {
          console.warn('GHN API token is not configured. Please set GHN_API_TOKEN environment variable.');
          return {
            data: [],
            success: false,
            message: 'Shipping service temporarily unavailable. Please contact support.'
          };
        }
      }

      return {
        data: [],
        success: false,
        message: error.response?.data?.message || 'Failed to load provinces'
      };
    }
  }

  async getCommunesByProvince(provinceCode: string): Promise<ApiResponse<unknown[]>> {
    try {
      const response = await apiClient.get(`/api/shipping/provinces/${provinceCode}/communes`, {
        headers: {
          'X-Skip-Auth': 'true'
        }
      });
      return { data: response.data, success: true };
    } catch (error: any) {
      console.error('GHN API Error - Communes:', error);

      if (error.response?.status === 401 || error.response?.status === 500) {
        const errorMessage = error.response?.data?.error || '';
        if (errorMessage.includes('GHN_API_TOKEN') || errorMessage.includes('missing') || errorMessage.includes('not configured')) {
          console.warn('GHN API token is not configured. Please set GHN_API_TOKEN environment variable.');
          return {
            data: [],
            success: false,
            message: 'Shipping service temporarily unavailable. Please contact support.'
          };
        }
      }

      return {
        data: [],
        success: false,
        message: error.response?.data?.message || 'Failed to load communes'
      };
    }
  }

  // Lấy danh sách huyện theo tỉnh
  async getDistrictsByProvince(provinceCode: string): Promise<unknown[]> {
    try {
      const response = await apiClient.get(`/api/shipping/districts/getByProvince?provinceCode=${provinceCode}`, { headers: { 'X-Skip-Auth': 'true' } })
      return response.data
    } catch (error: any) {
      console.error('GHN API Error - Districts:', error);
      if (error.response?.status === 401 || error.response?.status === 500) {
        const errorMessage = error.response?.data?.error || '';
        if (errorMessage.includes('GHN_API_TOKEN') || errorMessage.includes('missing') || errorMessage.includes('not configured')) {
          console.warn('GHN API token is not configured. Please set GHN_API_TOKEN environment variable.');
        }
      }
      return []
    }
  }
  // Lấy danh sách xã/phường theo huyện
  async getWardsByDistrict(districtCode: string): Promise<unknown[]> {
    try {
      const response = await apiClient.get(`/api/shipping/wards/getByDistrict?districtCode=${districtCode}`, { headers: { 'X-Skip-Auth': 'true' } })
      return response.data
    } catch (error: any) {
      console.error('GHN API Error - Wards:', error);
      if (error.response?.status === 401 || error.response?.status === 500) {
        const errorMessage = error.response?.data?.error || '';
        if (errorMessage.includes('GHN_API_TOKEN') || errorMessage.includes('missing') || errorMessage.includes('not configured')) {
          console.warn('GHN API token is not configured. Please set GHN_API_TOKEN environment variable.');
        }
      }
      return []
    }
  }

  // Payment API methods
  async createPayment(paymentData: Record<string, unknown>): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post('/api/payments/create', paymentData)
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to create payment:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to create payment'
      }
    }
  }

  // VNPay specific methods
  async createVNPayPayment(orderId: number): Promise<ApiResponse<unknown>> {
    try {
      // Get current user ID from auth context or localStorage
      const currentUser = JSON.parse(localStorage.getItem('auth_user') || '{}');
      const userId = currentUser.id || currentUser.userId;

      if (!userId) {
        return {
          data: undefined,
          success: false,
          message: 'Bạn cần đăng nhập trước khi thực hiện thanh toán VNPay',
        }
      }

      const response = await apiClient.post('/api/payments/vnpay/create', {
        orderId,
        userId
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to create VNPay payment:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to create VNPay payment'
      }
    }
  }

  // VNPay return handling
  async processVNPayReturn(vnpParams: Record<string, string>): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post('/api/payments/vnpay/return', vnpParams, {
        headers: {
          'X-Skip-Auth': 'true'
        }
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to process VNPay return:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to process VNPay return'
      }
    }
  }

  // MoMo specific methods
  async createMoMoPayment(orderId: number): Promise<ApiResponse<unknown>> {
    try {
      // Get current user ID from auth context or localStorage
      const currentUser = JSON.parse(localStorage.getItem('auth_user') || '{}');
      const userId = currentUser.id || currentUser.userId;

      if (!userId) {
        return {
          data: undefined,
          success: false,
          message: 'Bạn cần đăng nhập trước khi thực hiện thanh toán MoMo',
        }
      }

      const response = await apiClient.post('/api/payments/momo/create', {
        orderId,
        userId
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to create MoMo payment:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to create MoMo payment'
      }
    }
  }

  async confirmStripePayment(paymentIntentId: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post('/api/payments/stripe/confirm', { paymentIntentId })
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to confirm Stripe payment:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to confirm payment'
      }
    }
  }

  async validateCard(cardInfo: Record<string, unknown>): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post('/api/payments/validate-card', cardInfo, {
        headers: {
          'X-Skip-Auth': 'true'
        }
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to validate card:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to validate card'
      }
    }
  }

  async getBankTransferStatus(orderNumber: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.get(`/api/payments/bank-transfer/status/${orderNumber}`)
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to get bank transfer status:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to get transfer status'
      }
    }
  }

  async getSupportedBanks(): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.get('/api/payments/banks', {
        headers: {
          'X-Skip-Auth': 'true'
        }
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to get supported banks:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to get banks'
      }
    }
  }

  // User behavior logging for recommendations
  async logUserBehavior(userId: string, productId: string, action: 'VIEW'|'CLICK'|'ADD_TO_CART'|'PURCHASE'): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post('/api/recommendations/behavior', { userId, productId, action })
      return { data: response.data, success: true }
    } catch (error) {
      return { data: undefined, success: false, message: 'Failed to log user behavior' }
    }
  }

  // Generic GET method for API calls
  async get(url: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.get(url)
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error(`Failed to GET ${url}:`, error)
      return {
        data: undefined,
        success: false,
        message: 'Request failed'
      }
    }
  }

  // Generic POST method for API calls
  async post(url: string, data?: unknown): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post(url, data)
      return {
        data: response.data,
        success: true
      }
    } catch (error: any) {
      console.error(`Failed to POST ${url}:`, error)
      const errorMessage = error.response?.data?.message || 
                          error.response?.data?.error || 
                          error.message || 
                          'Request failed'
      return {
        data: undefined,
        success: false,
        message: errorMessage
      }
    }
  }

  // Generic PUT method for API calls
  async put(url: string, data?: unknown): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.put(url, data)
      return {
        data: response.data,
        success: true
      }
    } catch (error: any) {
      console.error(`Failed to PUT ${url}:`, error)
      const errorMessage = error.response?.data?.message || 
                          error.response?.data?.error || 
                          error.message || 
                          'Request failed'
      return {
        data: undefined,
        success: false,
        message: errorMessage
      }
    }
  }

  // Generic DELETE method for API calls
  async delete(url: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.delete(url)
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error(`Failed to DELETE ${url}:`, error)
      return {
        data: undefined,
        success: false,
        message: 'Request failed'
      }
    }
  }

  // Check if user is authenticated
  isAuthenticated(): boolean {
    const token = localStorage.getItem('auth_token') || localStorage.getItem('jwt')
    return !!token
  }

  // Get current auth token
  getAuthToken(): string | null {
    return localStorage.getItem('auth_token') || localStorage.getItem('jwt')
  }

  // Clear all auth data
  clearAuth(): void {
    localStorage.removeItem('auth_token')
    localStorage.removeItem('jwt')
    localStorage.removeItem('auth_user')
  }

  // ML Recommendation APIs
  async getMLRecommendations(userId: string, limit: number = 10): Promise<ApiResponse<Product[]>> {
    try {
      const response = await apiClient.get(`/api/recommendations/ml/user/${userId}?limit=${limit}`, {
        headers: { 'X-Skip-Auth': 'true' }
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error: any) {
      console.error('Failed to get ML recommendations:', error)

      // Handle 401 errors gracefully
      if (error.response?.status === 401) {
        console.warn('ML recommendations service not available or not configured');
        return {
          data: [],
          success: false,
          message: 'Recommendation service temporarily unavailable'
        }
      }

      return {
        data: [],
        success: false,
        message: error.response?.data?.message || 'Failed to get ML recommendations'
      }
    }
  }

  async getMLStatus(): Promise<ApiResponse<{modelLoaded: boolean, modelInfo: string}>> {
    try {
      const response = await apiClient.get('/api/recommendations/ml/status', {
        headers: { 'X-Skip-Auth': 'true' }
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error: any) {
      console.error('Failed to get ML status:', error)

      // Handle 401 errors gracefully
      if (error.response?.status === 401) {
        console.warn('ML status service not available or not configured');
        return {
          data: {modelLoaded: false, modelInfo: 'Service unavailable'},
          success: false,
          message: 'ML service temporarily unavailable'
        }
      }

      return {
        data: {modelLoaded: false, modelInfo: 'Unknown'},
        success: false,
        message: error.response?.data?.message || 'Failed to get ML status'
      }
    }
  }

  // Personalized Recommendations (Collaborative Filtering)
  async getPersonalizedRecommendations(userId: string, limit: number = 10): Promise<ApiResponse<Product[]>> {
    try {
      const response = await apiClient.get(`/api/recommendations/personalized/${userId}?limit=${limit}`, {
        headers: { 'X-Skip-Auth': 'true' }
      })
      return {
        data: response.data || [],
        success: true
      }
    } catch (error: any) {
      console.error('Failed to get personalized recommendations:', error)
      return {
        data: [],
        success: false,
        message: error.response?.data?.message || 'Failed to get personalized recommendations'
      }
    }
  }

  // Similar Products (Content-Based Filtering)
  async getSimilarProducts(productId: string, limit: number = 10): Promise<ApiResponse<Product[]>> {
    try {
      const response = await apiClient.get(`/api/recommendations/similar/${productId}?limit=${limit}`, {
        headers: { 'X-Skip-Auth': 'true' }
      })
      return {
        data: response.data || [],
        success: true
      }
    } catch (error: any) {
      console.error('Failed to get similar products:', error)
      return {
        data: [],
        success: false,
        message: error.response?.data?.message || 'Failed to get similar products'
      }
    }
  }

  // Review API methods
  async getProductReviews(productId: string, page: number = 0, size: number = 10, sortBy: string = 'createdAt', sortDir: string = 'desc'): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.get(`/api/reviews/product/${productId}?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`, { headers: { 'X-Skip-Auth': 'true' } })
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to get product reviews:', error)
      return {
        data: {content: [], totalElements: 0},
        success: false,
        message: 'Failed to get product reviews'
      }
    }
  }

  async getReviewSummary(productId: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.get(`/api/reviews/product/${productId}/summary`, { headers: { 'X-Skip-Auth': 'true' } })
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to get review summary:', error)
      return {
        data: {totalReviews: 0, averageRating: 0},
        success: false,
        message: 'Failed to get review summary'
      }
    }
  }

  async getRelatedProducts(productId: string, limit: number = 4): Promise<ApiResponse<Product[]>> {
    try {
      const response = await apiClient.get(`/api/products/${productId}/related?limit=${limit}`, { headers: { 'X-Skip-Auth': 'true' } })
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to get related products:', error)
      return {
        data: [],
        success: false,
        message: 'Failed to get related products'
      }
    }
  }

  async getMyReviews(userId: string, page: number = 0, size: number = 10): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.get(`/api/reviews/my-reviews?userId=${userId}&page=${page}&size=${size}`)
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to get my reviews:', error)
      return {
        data: {content: [], totalElements: 0},
        success: false,
        message: 'Failed to get my reviews'
      }
    }
  }

  async createReview(reviewData: {
    userId: string
    productId: string
    rating: number
    title: string
    content: string
    pros?: string
    cons?: string
  }): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post('/api/reviews', reviewData)
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to create review:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to create review'
      }
    }
  }

  // Admin Review API methods
  async adminGetReviews(filters: {
    page?: number
    size?: number
    productId?: string
    userId?: string
    isApproved?: boolean
    isSpam?: boolean
    rating?: number
  }): Promise<ApiResponse<unknown>> {
    try {
      const params = new URLSearchParams()
      if (filters.page !== undefined) params.append('page', filters.page.toString())
      if (filters.size !== undefined) params.append('size', filters.size.toString())
      if (filters.productId) params.append('productId', filters.productId)
      if (filters.userId) params.append('userId', filters.userId)
      if (filters.isApproved !== undefined) params.append('isApproved', filters.isApproved.toString())
      if (filters.isSpam !== undefined) params.append('isSpam', filters.isSpam.toString())
      if (filters.rating !== undefined) params.append('rating', filters.rating.toString())
      
      const response = await apiClient.get(`/api/reviews/admin/all?${params.toString()}`)
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to get admin reviews:', error)
      return {
        data: { content: [], totalElements: 0 },
        success: false,
        message: 'Failed to get admin reviews'
      }
    }
  }

  async adminGetReviewStats(): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.get('/api/reviews/admin/stats')
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to get review stats:', error)
      return {
        data: null,
        success: false,
        message: 'Failed to get review stats'
      }
    }
  }

  async approveReview(reviewId: number): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post(`/api/reviews/${reviewId}/approve`)
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to approve review:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to approve review'
      }
    }
  }

  async rejectReview(reviewId: number): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post(`/api/reviews/${reviewId}/reject`)
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to reject review:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to reject review'
      }
    }
  }

  async updateReview(reviewId: number, reviewData: {
    userId: string
    rating: number
    title: string
    content: string
  }): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.put(`/api/reviews/${reviewId}`, reviewData)
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to update review:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to update review'
      }
    }
  }

  async deleteReview(reviewId: number, userId: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.delete(`/api/reviews/${reviewId}?userId=${userId}`)
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to delete review:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to delete review'
      }
    }
  }

  async markReviewHelpful(reviewId: number): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post(`/api/reviews/${reviewId}/helpful`, {})
      return {
        data: response.data,
        success: true
      }
    } catch (error) {
      console.error('Failed to mark review helpful:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to mark review helpful'
      }
    }
  }

  // Voucher API methods
  async validateVoucher(voucherRequest: {
    voucherCode: string
    userId: string
    orderAmount: number
    items: Array<{
      productId: string
      productName: string
      categoryId: number
      brandId: number
      price: number
      quantity: number
    }>
  }): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post('/api/vouchers/validate', voucherRequest, {
        headers: { 'X-Skip-Auth': 'true' }
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error: any) {
      console.error('Failed to validate voucher:', error)
      const message = error.response?.data?.message || 'Failed to validate voucher'
      return {
        data: error.response?.data || { valid: false, message },
        success: false,
        message
      }
    }
  }

  async getPublicVouchers(): Promise<ApiResponse<unknown[]>> {
    try {
      const response = await apiClient.get('/api/vouchers/public', {
        headers: { 'X-Skip-Auth': 'true' }
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error: any) {
      console.error('Failed to get public vouchers:', error)
      return {
        data: [],
        success: false,
        message: error.response?.data?.message || 'Failed to get vouchers'
      }
    }
  }

  // Claim/Obtain a voucher for user
  async claimVoucher(userId: string, voucherId: number): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post(`/api/vouchers/user/${userId}/claim/${voucherId}`, {}, {
        headers: { 'X-Skip-Auth': 'true' }
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error: any) {
      console.error('Failed to claim voucher:', error)
      return {
        data: null,
        success: false,
        message: error.response?.data?.message || error.message || 'Failed to claim voucher'
      }
    }
  }

  // Get available vouchers for user (vouchers that user has claimed and not used)
  async getUserAvailableVouchers(userId: string): Promise<ApiResponse<unknown[]>> {
    try {
      const response = await apiClient.get(`/api/vouchers/user/${userId}/available`, {
        headers: { 'X-Skip-Auth': 'true' }
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error: any) {
      console.error('Failed to get user available vouchers:', error)
      return {
        data: [],
        success: false,
        message: error.response?.data?.message || 'Failed to get user vouchers'
      }
    }
  }

  // Get all vouchers for user (including used ones)
  async getUserVouchers(userId: string): Promise<ApiResponse<unknown[]>> {
    try {
      const response = await apiClient.get(`/api/vouchers/user/${userId}/all`, {
        headers: { 'X-Skip-Auth': 'true' }
      })
      return {
        data: response.data,
        success: true
      }
    } catch (error: any) {
      console.error('Failed to get user vouchers:', error)
      return {
        data: [],
        success: false,
        message: error.response?.data?.message || 'Failed to get user vouchers'
      }
    }
  }

  // Warranty API methods
  async createWarrantyRequest(warrantyData: {
    customerName: string
    customerEmail: string
    customerPhone?: string
    orderNumber: string
    productName: string
    productSku?: string
    issueDescription: string
    priority?: string
  }): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post('/api/warranty/requests', warrantyData)
      return {
        data: response.data,
        success: true,
        message: 'Warranty request created successfully'
      }
    } catch (error) {
      console.error('Failed to create warranty request:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to create warranty request'
      }
    }
  }

  async getMyWarrantyRequests(page: number = 0, size: number = 10, status?: string): Promise<ApiResponse<unknown>> {
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString()
      })
      if (status) params.append('status', status)

      const response = await apiClient.get(`/api/warranty/requests/my?${params}`)
      return {
        data: response.data,
        success: true,
        message: 'Warranty requests retrieved successfully'
      }
    } catch (error) {
      console.error('Failed to get warranty requests:', error)
      return {
        data: { content: [], totalElements: 0, totalPages: 0 },
        success: false,
        message: 'Failed to get warranty requests'
      }
    }
  }

  async getWarrantyRequestById(id: number): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.get(`/api/warranty/requests/${id}`)
      return {
        data: response.data,
        success: true,
        message: 'Warranty request retrieved successfully'
      }
    } catch (error) {
      console.error('Failed to get warranty request:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to get warranty request'
      }
    }
  }

  async getWarrantyRequestByNumber(requestNumber: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.get(`/api/warranty/requests/number/${requestNumber}`)
      return {
        data: response.data,
        success: true,
        message: 'Warranty request retrieved successfully'
      }
    } catch (error) {
      console.error('Failed to get warranty request by number:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to get warranty request'
      }
    }
  }

  // Support API methods
  async sendSupportMessage(supportData: {
    name: string
    email: string
    subject: string
    category: string
    message: string
  }): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post('/api/support/contact', supportData)
      return {
        data: response.data,
        success: true,
        message: 'Support message sent successfully'
      }
    } catch (error) {
      console.error('Failed to send support message:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to send support message'
      }
    }
  }

  // Notification API methods
  async getUserNotifications(userId: string): Promise<ApiResponse<Notification[]>> {
    try {
      const response = await apiClient.get(`/api/notifications/user/${userId}`)
      return {
        data: response.data,
        success: true,
        message: 'Notifications retrieved successfully'
      }
    } catch (error) {
      console.error('Failed to get notifications:', error)
      return {
        data: [],
        success: false,
        message: 'Failed to get notifications'
      }
    }
  }

  async getUnreadNotifications(userId: string): Promise<ApiResponse<Notification[]>> {
    try {
      const response = await apiClient.get(`/api/notifications/user/${userId}/unread`)
      return {
        data: response.data,
        success: true,
        message: 'Unread notifications retrieved successfully'
      }
    } catch (error) {
      console.error('Failed to get unread notifications:', error)
      return {
        data: [],
        success: false,
        message: 'Failed to get unread notifications'
      }
    }
  }

  async getUnreadCount(userId: string): Promise<ApiResponse<number>> {
    try {
      const response = await apiClient.get(`/api/notifications/user/${userId}/count`)
      return {
        data: response.data?.unreadCount || 0,
        success: true,
        message: 'Unread count retrieved successfully'
      }
    } catch (error) {
      console.error('Failed to get unread count:', error)
      return {
        data: 0,
        success: false,
        message: 'Failed to get unread count'
      }
    }
  }

  async markNotificationAsRead(notificationId: number): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post(`/api/notifications/${notificationId}/read`)
      return {
        data: response.data,
        success: true,
        message: 'Notification marked as read'
      }
    } catch (error) {
      console.error('Failed to mark notification as read:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to mark notification as read'
      }
    }
  }

  async markAllNotificationsAsRead(userId: string): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.post(`/api/notifications/user/${userId}/read-all`)
      return {
        data: response.data,
        success: true,
        message: 'All notifications marked as read'
      }
    } catch (error) {
      console.error('Failed to mark all notifications as read:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to mark all notifications as read'
      }
    }
  }

  async deleteNotification(notificationId: number): Promise<ApiResponse<unknown>> {
    try {
      const response = await apiClient.delete(`/api/notifications/${notificationId}`)
      return {
        data: response.data,
        success: true,
        message: 'Notification deleted successfully'
      }
    } catch (error) {
      console.error('Failed to delete notification:', error)
      return {
        data: undefined,
        success: false,
        message: 'Failed to delete notification'
      }
    }
  }

  // Analytics API methods
  async getAnalyticsOverview(): Promise<ApiResponse<{
    totalSales: number
    totalOrders: number
    activeUsers: number
    todaySales: number
    todayOrders: number
    date: string
  }>> {
    try {
      const response = await apiClient.get('/api/admin/analytics/overview')
      return {
        data: response.data,
        success: true,
        message: 'Analytics overview retrieved successfully'
      }
    } catch (error: any) {
      console.error('Failed to get analytics overview:', error)
      return {
        data: {
          totalSales: 0,
          totalOrders: 0,
          activeUsers: 0,
          todaySales: 0,
          todayOrders: 0,
          date: new Date().toISOString()
        },
        success: false,
        message: error.response?.data?.message || 'Failed to get analytics overview'
      }
    }
  }

  async getAnalyticsSales(timeRange: '7days' | '30days' | '90days' | 'year' = '30days'): Promise<ApiResponse<{
    labels: string[]
    values: number[]
  }>> {
    try {
      const response = await apiClient.get(`/api/admin/analytics/sales?timeRange=${timeRange}`)
      return {
        data: response.data,
        success: true,
        message: 'Sales analytics retrieved successfully'
      }
    } catch (error: any) {
      console.error('Failed to get sales analytics:', error)
      return {
        data: { labels: [], values: [] },
        success: false,
        message: error.response?.data?.message || 'Failed to get sales analytics'
      }
    }
  }

  async getAnalyticsSalesByPeriod(period: 'day' | 'week' | 'month' | 'year' = 'month'): Promise<ApiResponse<{
    period: string
    data: Array<{
      date: string
      label: string
      sales: number
      orders: number
    }>
  }>> {
    try {
      const response = await apiClient.get(`/api/admin/analytics/sales-by-period?period=${period}`)
      return {
        data: response.data,
        success: true,
        message: 'Sales by period retrieved successfully'
      }
    } catch (error: any) {
      console.error('Failed to get sales by period:', error)
      return {
        data: { period, data: [] },
        success: false,
        message: error.response?.data?.message || 'Failed to get sales by period'
      }
    }
  }

  async getAnalyticsUsers(): Promise<ApiResponse<{
    newUsers: number
    totalUsers: number
    retainedUsers: number
  }>> {
    try {
      const response = await apiClient.get('/api/admin/analytics/users')
      return {
        data: response.data,
        success: true,
        message: 'User analytics retrieved successfully'
      }
    } catch (error: any) {
      console.error('Failed to get user analytics:', error)
      return {
        data: {
          newUsers: 0,
          totalUsers: 0,
          retainedUsers: 0
        },
        success: false,
        message: error.response?.data?.message || 'Failed to get user analytics'
      }
    }
  }

  async getTopProducts(limit: number = 10): Promise<ApiResponse<Array<{
    productId: string
    score: number
  }>>> {
    try {
      const response = await apiClient.get(`/api/admin/analytics/top-products?limit=${limit}`)
      return {
        data: response.data?.products || [],
        success: true,
        message: 'Top products retrieved successfully'
      }
    } catch (error: any) {
      console.error('Failed to get top products:', error)
      return {
        data: [],
        success: false,
        message: error.response?.data?.message || 'Failed to get top products'
      }
    }
  }

  async getAnalyticsProducts(): Promise<ApiResponse<{
    total: number
    lowStock: number
  }>> {
    try {
      const response = await apiClient.get('/api/admin/analytics/products')
      return {
        data: response.data || { total: 0, lowStock: 0 },
        success: true,
        message: 'Product stats retrieved successfully'
      }
    } catch (error: any) {
      console.error('Failed to get product stats:', error)
      return {
        data: { total: 0, lowStock: 0 },
        success: false,
        message: error.response?.data?.message || 'Failed to get product stats'
      }
    }
  }

  async getCategoryDistribution(): Promise<ApiResponse<Array<{
    name: string
    value: number
  }>>> {
    try {
      const response = await apiClient.get('/api/admin/analytics/category-distribution')
      return {
        data: response.data?.categories || [],
        success: true,
        message: 'Category distribution retrieved successfully'
      }
    } catch (error: any) {
      console.error('Failed to get category distribution:', error)
      return {
        data: [],
        success: false,
        message: error.response?.data?.message || 'Failed to get category distribution'
      }
    }
  }
}

// Notification interface - matches backend NotificationType enum
export interface Notification {
  id: number
  userId: string
  type: 'ORDER_CREATED' | 'ORDER_CONFIRMED' | 'ORDER_SHIPPED' | 'ORDER_DELIVERED' | 'ORDER_CANCELLED' | 
        'PAYMENT_SUCCESS' | 'PAYMENT_FAILED' | 'PAYMENT_REFUNDED' |
        'PRODUCT_IN_STOCK' | 'PRODUCT_OUT_OF_STOCK' | 'PRICE_DROP' |
        'PROMOTION' | 'SYSTEM_MAINTENANCE' | 'WARRANTY_UPDATE' | 'SUPPORT' |
        'ACCOUNT_VERIFICATION' | 'PASSWORD_RESET' | 'SECURITY_ALERT' | 'WELCOME' | 
        'BIRTHDAY' | 'CART_ABANDONMENT' | 'REVIEW_REMINDER' | 'CUSTOM'
  title: string
  message: string
  data?: Record<string, any> | string // Can be JSON string or object
  isRead: boolean
  isSent: boolean
  sentAt?: string
  readAt?: string
  priority: number
  channel: string
  externalId?: string
  retryCount?: number
  maxRetries?: number
  createdAt?: string
  updatedAt?: string
}

// Export singleton instance
export const apiService = new ApiService()
export default apiService 