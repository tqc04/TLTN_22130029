import axios from 'axios';

// Use relative base URL so requests go through Vite proxy
const API_BASE_URL = '/api';

export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  salePrice?: number;
  imageUrl?: string;
  images?: Array<{
    id: number;
    imageUrl: string;
    alt?: string;
    isPrimary?: boolean;
  }>;
  category: string;
  brand: string;
  stockQuantity: number;
  rating: number;
  reviewCount: number;
  createdAt: string;
  updatedAt: string;
  discount?: number;
  isNew?: boolean;
  isFeatured?: boolean;
  isOnSale?: boolean;
}

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

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

class ProductService {
  private api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
      'Content-Type': 'application/json',
    },
  });

  constructor() {
    // Attach Authorization header from localStorage tokens
    this.api.interceptors.request.use((config) => {
      const token = localStorage.getItem('auth_token') || localStorage.getItem('jwt');
      if (token) {
        config.headers = config.headers || {};
        (config.headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
      }
      return config;
    });
  }

  // Get all products with filters
  async getProducts(filters: ProductFilters = {}): Promise<PaginatedResponse<Product>> {
    try {
      const params = new URLSearchParams();
      
      if (filters.search) params.append('search', filters.search);
      if (filters.category && filters.category !== 'all') params.append('category', filters.category);
      if (filters.brand && filters.brand !== 'all') params.append('brand', filters.brand);
      if (filters.minPrice !== undefined) params.append('minPrice', filters.minPrice.toString());
      if (filters.maxPrice !== undefined) params.append('maxPrice', filters.maxPrice.toString());
      if (filters.sortBy) {
        // Map frontend sortBy to backend sort format
        // Frontend: "price-asc", "price-desc", "name-asc", etc.
        // Backend: "price,asc", "price,desc", "name,asc", etc.
        const sortValue = filters.sortBy.replace('-', ',');
        params.append('sort', sortValue);
      }
      if (filters.page !== undefined) params.append('page', filters.page.toString());
      if (filters.size !== undefined) params.append('size', filters.size.toString());

      const response = await this.api.get(`/products?${params.toString()}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching products:', error);
      throw error;
    }
  }

  // Get product by ID
  async getProductById(id: string): Promise<Product> {
    try {
      const response = await this.api.get(`/products/${id}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching product:', error);
      throw error;
    }
  }

  // Get featured products
  async getFeaturedProducts(limit: number = 10): Promise<Product[]> {
    try {
      const response = await this.api.get(`/products/featured?limit=${limit}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching featured products:', error);
      throw error;
    }
  }

  // Get products by category
  async getProductsByCategory(categoryId: number, page: number = 0, size: number = 12): Promise<PaginatedResponse<Product>> {
    try {
      const response = await this.api.get(`/products/category/${categoryId}?page=${page}&size=${size}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching products by category:', error);
      throw error;
    }
  }

  // Get products by brand
  async getProductsByBrand(brandId: number, page: number = 0, size: number = 12): Promise<PaginatedResponse<Product>> {
    try {
      const response = await this.api.get(`/products/brand/${brandId}?page=${page}&size=${size}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching products by brand:', error);
      throw error;
    }
  }

  // Search products
  async searchProducts(query: string, page: number = 0, size: number = 12): Promise<PaginatedResponse<Product>> {
    try {
      const response = await this.api.get(`/products/search?q=${encodeURIComponent(query)}&page=${page}&size=${size}`);
      return response.data;
    } catch (error) {
      console.error('Error searching products:', error);
      throw error;
    }
  }

  // Autocomplete search for suggestions
  async autocomplete(query: string, limit: number = 5): Promise<Product[]> {
    try {
      if (!query || query.trim().length < 2) {
        return [];
      }
      const response = await this.api.get(`/products/autocomplete?q=${encodeURIComponent(query.trim())}&limit=${limit}`);
      return response.data || [];
    } catch (error) {
      console.error('Error fetching autocomplete suggestions:', error);
      return [];
    }
  }

  // Get categories
  async getCategories(): Promise<Array<{ id: number; name: string; description?: string }>> {
    try {
      // Use product-service meta endpoint (backed by product_db) to keep filters consistent
      const response = await this.api.get('/products/meta/categories');
      return response.data;
    } catch (error) {
      console.error('Error fetching categories:', error);
      throw error;
    }
  }

  // Get brands
  async getBrands(): Promise<Array<{ id: number; name: string; description?: string }>> {
    try {
      // Use product-service meta endpoint (backed by product_db) to keep filters consistent
      const response = await this.api.get('/products/meta/brands');
      return response.data;
    } catch (error) {
      console.error('Error fetching brands:', error);
      throw error;
    }
  }

  // Get sale products (products with discount/salePrice)
  async getSaleProducts(filters: ProductFilters & { saleTimeSlot?: string } = {}): Promise<PaginatedResponse<Product>> {
    try {
      const params = new URLSearchParams();
      params.append('onSale', 'true');
      
      if (filters.search) params.append('search', filters.search);
      if (filters.category && filters.category !== 'all') params.append('category', filters.category);
      if (filters.brand && filters.brand !== 'all') params.append('brand', filters.brand);
      if (filters.saleTimeSlot) params.append('saleTimeSlot', filters.saleTimeSlot);
      if (filters.page !== undefined) params.append('page', filters.page.toString());
      if (filters.size !== undefined) params.append('size', filters.size.toString());

      const response = await this.api.get(`/products?${params.toString()}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching sale products:', error);
      // Fallback to on-sale endpoint
      try {
        const params = new URLSearchParams();
        if (filters.page !== undefined) params.append('page', filters.page.toString());
        if (filters.size !== undefined) params.append('size', filters.size.toString());
        const fallbackResponse = await this.api.get(`/products/on-sale?${params.toString()}`);
        return fallbackResponse.data;
      } catch (fallbackError) {
        throw error;
      }
    }
  }

  // Get newest products
  async getNewProducts(filters: ProductFilters = {}): Promise<PaginatedResponse<Product>> {
    try {
      const params = new URLSearchParams();
      
      if (filters.search) params.append('search', filters.search);
      if (filters.category && filters.category !== 'all') params.append('category', filters.category);
      if (filters.brand && filters.brand !== 'all') params.append('brand', filters.brand);
      if (filters.page !== undefined) params.append('page', filters.page.toString());
      if (filters.size !== undefined) params.append('size', filters.size.toString());
      params.append('sort', 'createdAt,desc'); // Sort by newest first

      const response = await this.api.get(`/products?${params.toString()}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching new products:', error);
      throw error;
    }
  }

  // Get best selling products
  async getBestSellingProducts(filters: ProductFilters = {}): Promise<PaginatedResponse<Product>> {
    try {
      const params = new URLSearchParams();
      
      if (filters.page !== undefined) params.append('page', filters.page.toString());
      if (filters.size !== undefined) params.append('size', filters.size.toString());

      // Use dedicated best-selling endpoint
      const response = await this.api.get(`/products/best-selling?${params.toString()}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching best selling products:', error);
      // Fallback to regular products sorted by purchaseCount
      try {
        const params = new URLSearchParams();
        if (filters.search) params.append('search', filters.search);
        if (filters.category && filters.category !== 'all') params.append('category', filters.category);
        if (filters.brand && filters.brand !== 'all') params.append('brand', filters.brand);
        if (filters.page !== undefined) params.append('page', filters.page.toString());
        if (filters.size !== undefined) params.append('size', filters.size.toString());
        params.append('sort', 'purchaseCount,desc');
        const fallbackResponse = await this.api.get(`/products?${params.toString()}`);
        return fallbackResponse.data;
      } catch (fallbackError) {
        throw error;
      }
    }
  }
}

export default new ProductService();
