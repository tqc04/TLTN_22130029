// Product interface
export interface Product {
  id: string;
  name: string;
  price: number;
  salePrice?: number;
  averageRating: number;
  reviewCount: number;
  images?: Array<{
    id: number;
    imageUrl: string;
    alt?: string;
    isPrimary?: boolean;
  }>;
  isOnSale?: boolean;
  isFeatured?: boolean;
  stockQuantity: number;
  brand?: {
    id: number;
    name: string;
  };
  category?: {
    id: number;
    name: string;
  };
  description?: string;
  createdAt: string;
  updatedAt: string;
}

// Cart interfaces
export interface CartItem {
  id: number;
  product: Product;
  quantity: number;
  price: number;
}

export interface Cart {
  items: CartItem[];
  totalItems: number;
  subtotal: number;
}

// Order interfaces
export interface Order {
  id: number;
  orderNumber: string;
  status: string;
  totalAmount: number;
  items: OrderItem[];
  shippingAddress: string;
  billingAddress: string;
  paymentMethod: string;
  createdAt: string;
  updatedAt: string;
}

export interface OrderItem {
  id: number;
  product: Product;
  quantity: number;
  price: number;
}

// User interfaces
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  address?: string;
  createdAt: string;
  updatedAt: string;
}

// API Response interfaces
export interface ApiResponse<T> {
  data: T;
  message: string;
  success: boolean;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Form interfaces
export interface LoginForm {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterForm {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
  phone?: string;
  acceptTerms: boolean;
}

export interface CheckoutForm {
  shippingAddress: string;
  billingAddress: string;
  paymentMethod: string;
  cardNumber?: string;
  expiryDate?: string;
  cvv?: string;
  cardholderName?: string;
}

// Filter interfaces
export interface ProductFilters {
  search?: string;
  category?: string;
  brand?: string;
  minPrice?: number;
  maxPrice?: number;
  sortBy?: string;
  page?: number;
  size?: number;
}

// Notification interfaces
export interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number;
  action?: {
    label: string;
    onClick: () => void;
  };
}

// Bank Transfer interfaces
export interface BankInfo {
  bankName: string;
  accountNumber: string;
  accountName: string;
  amount: number;
  transferCode: string;
  qrCode?: string;
}

// Chat interfaces
export interface ChatSession {
  id: number;
  userId: string;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessage {
  id: number;
  sessionId: string;
  message: string;
  isFromUser: boolean;
  createdAt: string;
}
