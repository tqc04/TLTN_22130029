import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react'
import { apiService } from '../services/api'
import { useAuth } from './AuthContext'
import { useNotification } from './NotificationContext'
import { useTranslation } from 'react-i18next'
import NProgress from 'nprogress'

// Cart Item interface
export interface CartItem {
  productId: string
  productName: string
  productSku: string
  productPrice: number
  quantity: number
  subtotal: number
  productImage?: string
  categoryName?: string
  brandName?: string
  stockQuantity?: number
  isActive?: boolean
}

// Cart interface
export interface Cart {
  userId?: string
  sessionId?: string
  items: CartItem[]
  totalItems: number
  subtotal: number
  taxAmount: number
  shippingAmount: number
  discountAmount: number
  totalAmount: number
  promoCode?: string
  voucherCode?: string
  voucherId?: number
  voucherMessage?: string
  createdAt: string
  updatedAt: string
}

// Cart Context Type
interface CartContextType {
  cart: Cart | null
  cartLoading: boolean
  addToCart: (productId: string, quantity?: number, variantId?: number) => Promise<boolean>
  removeFromCart: (productId: string) => Promise<boolean>
  updateCartItem: (productId: string, quantity: number) => Promise<boolean>
  clearCart: () => Promise<boolean>
  applyVoucher: (voucherCode: string) => Promise<boolean>
  removeVoucher: () => Promise<boolean>
  getCartCount: () => number
  refreshCart: () => Promise<void>
  mergeGuestCart: (userId: string) => Promise<boolean>
}

// Create context
const CartContext = createContext<CartContextType | undefined>(undefined)

// Hook to use cart context
export const useCart = () => {
  const context = useContext(CartContext)
  if (!context) {
    throw new Error('useCart must be used within a CartProvider')
  }
  return context
}

// Cart Provider Props
interface CartProviderProps {
  children: ReactNode
  userId?: string // Optional for authenticated users
}

// Cart Provider Component
export const CartProvider: React.FC<CartProviderProps> = ({ children, userId }) => {
  const [cart, setCart] = useState<Cart | null>(null)
  const [cartLoading, setCartLoading] = useState(false)
  const { isAuthenticated } = useAuth()
  const { notify } = useNotification()
  const { t } = useTranslation()

  // Load cart from backend
  const loadCart = useCallback(async () => {
    if (!userId) return
    
    try {
      setCartLoading(true)
      const response = await apiService.getCart(userId)
      if (response.success) {
        // Map backend data to frontend format
        const backendCart = response.data as any
        const mappedCart: Cart = {
          userId: backendCart.userId ? String(backendCart.userId) : undefined,
          items: backendCart.items?.map((item: any) => ({
            productId: item.productId,
            productName: item.productName,
            productSku: item.productSku || '',
            productPrice: Number(item.price) || 0, // Map 'price' to 'productPrice'
            quantity: item.quantity,
            subtotal: Number(item.total) || 0,
            productImage: item.productImage,
            categoryName: item.categoryName,
            brandName: item.brandName,
            stockQuantity: item.stockQuantity,
            isActive: item.isActive
          })) || [],
          totalItems: backendCart.items?.length || 0,
          subtotal: Number(backendCart.subtotal) || 0,
          taxAmount: Number(backendCart.tax) || 0,
          shippingAmount: Number(backendCart.shipping) || 0,
          discountAmount: Number(backendCart.discount) || 0,
          totalAmount: Number(backendCart.total) || 0,
          promoCode: backendCart.promoCode,
          voucherCode: backendCart.voucherCode,
          voucherId: backendCart.voucherId,
          voucherMessage: backendCart.voucherMessage,
          createdAt: backendCart.createdAt || new Date().toISOString(),
          updatedAt: backendCart.updatedAt || new Date().toISOString()
        }
        setCart(mappedCart)
      }
    } catch (error) {
      console.error('Failed to load cart:', error)
      // Initialize empty cart on error
      setCart({
        items: [],
        totalItems: 0,
        subtotal: 0,
        taxAmount: 0,
        shippingAmount: 0,
        discountAmount: 0,
        totalAmount: 0,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      })
    } finally {
      setCartLoading(false)
    }
  }, [userId])

  // Load cart on component mount and when userId changes
  useEffect(() => {
    if (isAuthenticated && userId) {
      loadCart()
    } else {
      // Clear cart when user is not authenticated
      setCart(null)
    }
  }, [userId, isAuthenticated, loadCart])

  // Add product to cart
  const addToCart = async (productId: string, quantity: number = 1, variantId?: number): Promise<boolean> => {
    if (!isAuthenticated || !userId) {
      return false
    }

    try {
      NProgress.start()
      setCartLoading(true)
      const response = await apiService.addToCart(productId, quantity, userId, variantId)
      if (response.success) {
        const backendCart = response.data as any
        const mappedCart: Cart = {
          userId: backendCart.userId ? String(backendCart.userId) : undefined,
          items: backendCart.items?.map((item: any) => ({
            productId: item.productId ? String(item.productId) : '',
            productName: item.productName,
            productSku: item.productSku || '',
            productPrice: Number(item.price) || 0,
            quantity: item.quantity,
            subtotal: Number(item.total) || 0,
            productImage: item.productImage,
            categoryName: item.categoryName,
            brandName: item.brandName,
            stockQuantity: item.stockQuantity,
            isActive: item.isActive
          })) || [],
          totalItems: backendCart.items?.length || 0,
          subtotal: Number(backendCart.subtotal) || 0,
          taxAmount: Number(backendCart.tax) || 0,
          shippingAmount: Number(backendCart.shipping) || 0,
          discountAmount: Number(backendCart.discount) || 0,
          totalAmount: Number(backendCart.total) || 0,
          promoCode: backendCart.promoCode,
          voucherCode: backendCart.voucherCode,
          voucherId: backendCart.voucherId,
          voucherMessage: backendCart.voucherMessage,
          createdAt: backendCart.createdAt || new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
        setCart(mappedCart)
        notify(t('cart.success.addedToCart'), 'success')
        return true
      }
      return false
    } catch (error: unknown) {
      console.error('Failed to add to cart:', error)

      // Check for new CartDTO error format first
      if (error && typeof error === 'object' && 'response' in error) {
        const axiosError = error as { response?: { data?: { hasError?: boolean; errorMessage?: string; error?: string; message?: string } } }
        if (axiosError?.response?.data?.hasError && axiosError?.response?.data?.errorMessage) {
          notify(axiosError.response.data.errorMessage, 'warning', 4000)
          return false
        }

        // Fallback to legacy error handling
        const errorObj = error as { message?: string }
        const msg: string = axiosError?.response?.data?.error || axiosError?.response?.data?.message || errorObj.message || t('cart.errors.addToCartFailed')
        const lower = msg.toLowerCase()
        if (lower.includes('out of stock') || lower.includes('insufficient stock')) {
          notify(t('cart.errors.outOfStock'), 'warning', 4000)
        } else if (lower.includes('exceeds available stock')) {
          notify(t('cart.errors.exceedsStock'), 'warning', 4000)
        } else if (lower.includes('not active')) {
          notify(t('cart.errors.productNotActive'), 'warning', 4000)
        } else if (lower.includes('product not found')) {
          notify(t('cart.errors.productNotFound'), 'error', 4000)
        } else {
          notify(msg, 'error', 4000)
        }
      } else {
        notify(t('cart.errors.addToCartFailed'), 'error', 4000)
      }
      return false
    } finally {
      NProgress.done()
      setCartLoading(false)
    }
  }

  // Remove product from cart
  const removeFromCart = async (productId: string): Promise<boolean> => {
    if (!isAuthenticated || !userId) {
      return false
    }

    try {
      setCartLoading(true)
      const response = await apiService.removeFromCart(productId, userId)
      if (response.success) {
        const backendCart = response.data as any
        const mappedCart: Cart = {
          userId: backendCart.userId ? String(backendCart.userId) : undefined,
          items: backendCart.items?.map((item: any) => ({
            productId: item.productId ? String(item.productId) : '',
            productName: item.productName,
            productSku: item.productSku || '',
            productPrice: Number(item.price) || 0,
            quantity: item.quantity,
            subtotal: Number(item.total) || 0,
            productImage: item.productImage,
            categoryName: item.categoryName,
            brandName: item.brandName,
            stockQuantity: item.stockQuantity,
            isActive: item.isActive
          })) || [],
          totalItems: backendCart.items?.length || 0,
          subtotal: Number(backendCart.subtotal) || 0,
          taxAmount: Number(backendCart.tax) || 0,
          shippingAmount: Number(backendCart.shipping) || 0,
          discountAmount: Number(backendCart.discount) || 0,
          totalAmount: Number(backendCart.total) || 0,
          promoCode: backendCart.promoCode,
          voucherCode: backendCart.voucherCode,
          voucherId: backendCart.voucherId,
          voucherMessage: backendCart.voucherMessage,
          createdAt: backendCart.createdAt || new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
        setCart(mappedCart)
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to remove from cart:', error)
      return false
    } finally {
      setCartLoading(false)
    }
  }

  // Update cart item quantity
  const updateCartItem = async (productId: string, quantity: number): Promise<boolean> => {
    if (!isAuthenticated || !userId) {
      return false
    }

    try {
      setCartLoading(true)
      const response = await apiService.updateCartItem(productId, quantity, userId)
      if (response.success) {
        // Reload cart from server via AJAX to ensure totals stay in sync
        await loadCart()
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to update cart item:', error)
      return false
    } finally {
      setCartLoading(false)
    }
  }

  // Clear entire cart
  const clearCart = async (): Promise<boolean> => {
    if (!isAuthenticated || !userId) {
      return false
    }

    try {
      setCartLoading(true)
      const response = await apiService.clearCart(userId)
      if (response.success) {
        // Set cart to empty state instead of response.data to ensure consistency
        setCart({
          userId: userId ? String(userId) : undefined,
          items: [],
          totalItems: 0,
          subtotal: 0,
          taxAmount: 0,
          shippingAmount: 0,
          discountAmount: 0,
          totalAmount: 0,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        })
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to clear cart:', error)
      return false
    } finally {
      setCartLoading(false)
    }
  }

  // Apply voucher code
  const applyVoucher = async (voucherCode: string): Promise<boolean> => {
    if (!isAuthenticated || !userId) {
      return false
    }

    try {
      setCartLoading(true)

      // Get current cart first to build validation request
      const currentCart = await loadCartFromServer()
      if (!currentCart) {
        return false
      }

      // Build voucher validation request
      const validationRequest = {
        voucherCode,
        userId: userId ? String(userId) : '',
        orderAmount: currentCart.subtotal,
        items: currentCart.items.map(item => ({
          productId: item.productId,
          productName: item.productName,
          categoryId: 1, // Default category ID
          brandId: 1,    // Default brand ID
          price: item.productPrice,
          quantity: item.quantity,
        }))
      }

      // Call voucher validation API
      const response = await apiService.validateVoucher(validationRequest)
      if (response.success && (response.data as any)?.valid) {
        const data = response.data as any
        // Update cart with voucher info
        const updatedCart = {
          ...currentCart,
          voucherCode: data.voucherCode,
          voucherId: data.voucherId,
          discountAmount: data.discountAmount,
          voucherMessage: data.message,
          totalAmount: data.finalAmount
        }

        setCart(updatedCart)
        notify('Áp dụng mã voucher thành công!', 'success')
        return true
      } else {
        const errorMessage = (response.data as any)?.message || response.message || 'Mã voucher không hợp lệ'
        notify(errorMessage, 'error')
        return false
      }
    } catch (error: any) {
      console.error('Failed to apply voucher:', error)
      const errorMessage = error.message || 'Không thể áp dụng mã voucher'
      notify(errorMessage, 'error')
      return false
    } finally {
      setCartLoading(false)
    }
  }

  // Remove voucher code
  const removeVoucher = async (): Promise<boolean> => {
    if (!isAuthenticated || !userId) {
      return false
    }

    try {
      setCartLoading(true)

      // Get current cart
      const currentCart = await loadCartFromServer()
      if (!currentCart) {
        return false
      }

      // Update cart to remove voucher
      const updatedCart = {
        ...currentCart,
        voucherCode: undefined,
        voucherId: undefined,
        discountAmount: 0,
        voucherMessage: undefined,
        totalAmount: currentCart.subtotal + currentCart.taxAmount + currentCart.shippingAmount
      }

      setCart(updatedCart)
      notify('Đã xóa mã voucher', 'info')
      return true
    } catch (error) {
      console.error('Failed to remove voucher:', error)
      notify('Không thể xóa mã voucher', 'error')
      return false
    } finally {
      setCartLoading(false)
    }
  }

  // Helper method to load cart from server
  const loadCartFromServer = async (): Promise<Cart | null> => {
    try {
      const response = await apiService.getCart(userId)
      return response.success ? response.data as Cart : null
    } catch (error) {
      console.error('Failed to load cart from server:', error)
      return null
    }
  }

  // Get cart count
  const getCartCount = (): number => {
    return cart?.totalItems || 0
  }

  // Refresh cart
  const refreshCart = async (): Promise<void> => {
    await loadCart()
  }

  // Merge guest cart with user cart
  const mergeGuestCart = async (userId: string): Promise<boolean> => {
    try {
      setCartLoading(true)
      const response = await apiService.mergeGuestCart(userId)
      if (response.success) {
        setCart(response.data as Cart)
        return true
      }
      return false
    } catch (error) {
      console.error('Failed to merge guest cart:', error)
      return false
    } finally {
      setCartLoading(false)
    }
  }

  const value: CartContextType = {
    cart,
    cartLoading,
    addToCart,
    removeFromCart,
    updateCartItem,
    clearCart,
    applyVoucher,
    removeVoucher,
    getCartCount,
    refreshCart,
    mergeGuestCart
  }

  return (
    <CartContext.Provider value={value}>
      {children}
    </CartContext.Provider>
  )
}

export default CartContext 