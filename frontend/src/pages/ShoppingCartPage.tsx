import React, { useState } from 'react'
import {
  Box,
  Container,
  Typography,
  Button,
  IconButton,
  Grid,
  Divider,
  Chip,
  TextField,
  CircularProgress,
} from '@mui/material'
import {
  Add,
  Remove,
  Delete,
  FavoriteOutlined,
  ShoppingBag,
  PaymentOutlined,
  Receipt,
} from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { useCart } from '../contexts/CartContext'
import notificationService from '../services/notificationService'

const ShoppingCartPage: React.FC = () => {
  const navigate = useNavigate()
  const {
    cart,
    cartLoading,
    updateCartItem,
    removeFromCart
  } = useCart()
  
  // Track which items are being updated
  const [updatingItems, setUpdatingItems] = useState<Set<string>>(new Set())

  const updateQuantity = async (productId: string, newQuantity: number) => {
    // Prevent negative quantities
    if (newQuantity < 0) {
      notificationService.error('Quantity cannot be negative')
      return
    }
    
    if (newQuantity === 0) {
      await handleRemoveItem(productId)
      return
    }
    
    // Add to updating items set
    setUpdatingItems(prev => new Set(prev).add(productId))
    
    try {
      const success = await updateCartItem(productId, newQuantity)
      if (success) {
        notificationService.success('Cart updated successfully')
      } else {
        notificationService.error('Failed to update cart item')
      }
    } catch (error) {
      console.error('Error updating quantity:', error)
      notificationService.error('Failed to update cart item')
    } finally {
      // Remove from updating items set
      setUpdatingItems(prev => {
        const newSet = new Set(prev)
        newSet.delete(productId)
        return newSet
      })
    }
  }

  const handleRemoveItem = async (productId: string) => {
    try {
      const success = await removeFromCart(productId)
      if (success) {
        notificationService.success('Product removed from cart')
      } else {
        notificationService.error('Failed to remove product from cart')
      }
    } catch (error) {
      notificationService.error('Failed to remove product from cart')
    }
  }

  // Clear cart functionality removed - not used

  const formatPrice = (price: number | undefined | null) => {
    // Handle NaN, null, undefined cases
    const safePrice = Number(price) || 0;
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(safePrice)
  }

  const handleCheckout = () => {
    if (!cart || cart.items.length === 0) {
      notificationService.error('Your cart is empty')
      return
    }
    
    // Navigate to checkout or implement checkout logic
    navigate('/checkout')
  }

  // Show loading state
  if (cartLoading) {
    return (
      <Container maxWidth="lg" sx={{ py: 4, pt: '40px', textAlign: 'center' }}>
        <CircularProgress size={60} />
        <Typography variant="h6" sx={{ mt: 2 }}>
          Loading cart...
        </Typography>
      </Container>
    )
  }

  // Show empty cart
  if (!cart || cart.items.length === 0) {
    return (
      <Container maxWidth="lg" sx={{ py: 4, pt: '40px' }}>
        <Box sx={{ textAlign: 'center', borderRadius: 3, p: 8 }}>
          <ShoppingBag sx={{ fontSize: 80, color: 'text.secondary', mb: 2 }} />
          <Typography variant="h4" gutterBottom fontWeight="bold">
            Your cart is empty
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
            Looks like you haven't added any items to your cart yet.
          </Typography>
          <Button
            variant="contained"
            size="large"
            startIcon={<ShoppingBag />}
            onClick={() => navigate('/products')}
            sx={{ borderRadius: 3, px: 4 }}
          >
            Continue Shopping
          </Button>
        </Box>
      </Container>
    )
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4, pt: '40px' }}>
      {/* Header */}
      <Box sx={{ 
        mb: 4, 
        p: 3, 
        borderRadius: 3, 
        bgcolor: 'primary.light', 
        color: 'white',
        background: 'linear-gradient(135deg, #1976d2 0%, #42a5f5 100%)'
      }}>
        <Typography variant="h3" component="h1" gutterBottom fontWeight="bold" sx={{ mb: 2 }}>
          🛒 Shopping Cart
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Chip 
            label={cart.totalItems} 
            color="secondary" 
            size="medium" 
            sx={{ 
              fontWeight: 'bold',
              fontSize: '1rem',
              height: 32
            }} 
          />
          <Typography variant="h6" sx={{ fontWeight: 'medium' }}>
            {cart.totalItems} {cart.totalItems === 1 ? 'item' : 'items'} in your cart
          </Typography>
        </Box>
      </Box>

      <Grid container spacing={4}>
        {/* Cart Items */}
        <Grid item xs={12} lg={8}>
          <Box sx={{ 
            borderRadius: 3, 
            overflow: 'hidden', 
            bgcolor: 'background.paper', 
            boxShadow: 2,
            border: '1px solid',
            borderColor: 'divider'
          }}>
            {cart.items.map((item, index) => (
              <Box key={item.productId}>
                <Box sx={{ p: 4 }}>
                  <Grid container spacing={3} alignItems="stretch">
                    {/* Product Image */}
                    <Grid item xs={12} sm={3}>
                      <Box
                        sx={{
                          position: 'relative',
                          borderRadius: 3,
                          overflow: 'hidden',
                          boxShadow: 2,
                          transition: 'all 0.3s ease',
                          height: '100%',
                          minHeight: 200,
                          '&:hover': {
                            transform: 'scale(1.02)',
                            boxShadow: 4
                          }
                        }}
                      >
                      <Box
                        component="img"
                          src={item.productImage || `https://via.placeholder.com/250x200/f5f5f5/666666?text=${encodeURIComponent(item.productName)}`}
                        alt={item.productName}
                        sx={{ 
                          width: '100%',
                            height: '100%',
                            minHeight: 200,
                          objectFit: 'cover',
                            cursor: 'pointer',
                            display: 'block'
                        }}
                        onClick={() => navigate(`/products/${item.productId}`)}
                          onError={(e) => {
                            const target = e.target as HTMLImageElement;
                            target.src = `https://via.placeholder.com/250x200/f5f5f5/666666?text=${encodeURIComponent(item.productName)}`;
                          }}
                        />
                        {/* Image overlay on hover */}
                        <Box
                          sx={{
                            position: 'absolute',
                            top: 0,
                            left: 0,
                            right: 0,
                            bottom: 0,
                            bgcolor: 'rgba(0,0,0,0.1)',
                            opacity: 0,
                            transition: 'opacity 0.3s ease',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            cursor: 'pointer',
                            '&:hover': {
                              opacity: 1
                            }
                          }}
                          onClick={() => navigate(`/products/${item.productId}`)}
                        >
                          <Typography variant="caption" color="white" sx={{ fontWeight: 'bold' }}>
                            View Details
                          </Typography>
                        </Box>
                      </Box>
                    </Grid>

                    {/* Product Details */}
                    <Grid item xs={12} sm={6}>
                      <Box sx={{ 
                        height: '100%', 
                        display: 'flex', 
                        flexDirection: 'column', 
                        justifyContent: 'space-between',
                        p: 2
                      }}>
                        <Box>
                      <Typography 
                            variant="h5" 
                        gutterBottom 
                        sx={{ 
                          cursor: 'pointer',
                              fontWeight: 'bold',
                              lineHeight: 1.3,
                              '&:hover': { color: 'primary.main' },
                              transition: 'color 0.3s ease',
                              mb: 2
                        }}
                        onClick={() => navigate(`/products/${item.productId}`)}
                      >
                        {item.productName}
                      </Typography>
                      
                          <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
                            <Chip 
                              label={item.categoryName || 'General'} 
                              size="small" 
                              color="primary" 
                              variant="outlined"
                              sx={{ fontWeight: 'bold' }}
                            />
                            <Chip 
                              label={item.brandName || 'Generic'} 
                              size="small" 
                              color="secondary" 
                              variant="outlined"
                              sx={{ fontWeight: 'bold' }}
                            />
                      </Box>

                          <Typography variant="h4" color="primary" fontWeight="bold" gutterBottom sx={{ mb: 2 }}>
                        {formatPrice(item.productPrice)}
                      </Typography>

                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                        {item.stockQuantity && item.stockQuantity < 10 && (
                          <Chip 
                            label={`Only ${item.stockQuantity} left!`} 
                            size="small" 
                            color="warning" 
                            variant="filled"
                                sx={{ fontWeight: 'bold' }}
                          />
                        )}
                            <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 'medium' }}>
                          In Stock: {item.stockQuantity || 'N/A'}
                        </Typography>
                          </Box>
                        </Box>
                      </Box>
                    </Grid>

                    {/* Quantity & Actions */}
                    <Grid item xs={12} sm={3}>
                      <Box sx={{ 
                        display: 'flex', 
                        flexDirection: 'column', 
                        alignItems: 'center', 
                        justifyContent: 'center',
                        gap: 3,
                        p: 3,
                        borderRadius: 3,
                        bgcolor: 'grey.50',
                        height: '100%',
                        minHeight: 200
                      }}>
                        {/* Quantity Controls */}
                        <Box sx={{ 
                          display: 'flex', 
                          alignItems: 'center', 
                          border: '2px solid',
                          borderColor: 'primary.main',
                          borderRadius: 3,
                          bgcolor: 'white',
                          overflow: 'hidden'
                        }}>
                          <IconButton 
                            size="small" 
                            onClick={() => updateQuantity(item.productId, item.quantity - 1)}
                            disabled={item.quantity <= 1 || cartLoading || updatingItems.has(item.productId)}
                            sx={{ 
                              color: 'primary.main',
                              '&:hover': { bgcolor: 'primary.light', color: 'white' }
                            }}
                          >
                            {updatingItems.has(item.productId) ? (
                              <CircularProgress size={16} />
                            ) : (
                              <Remove fontSize="small" />
                            )}
                          </IconButton>
                          
                          <TextField
                            size="small"
                            value={item.quantity}
                            onChange={(e) => {
                              const value = parseInt(e.target.value) || 1
                              updateQuantity(item.productId, value)
                            }}
                            inputProps={{
                              min: 1,
                              max: item.stockQuantity,
                              style: { textAlign: 'center', width: '50px', fontWeight: 'bold' }
                            }}
                            variant="outlined"
                            disabled={cartLoading || updatingItems.has(item.productId)}
                            sx={{
                              '& .MuiOutlinedInput-root': {
                                '& fieldset': { border: 'none' },
                              },
                            }}
                          />
                          
                          <IconButton 
                            size="small" 
                            onClick={() => updateQuantity(item.productId, item.quantity + 1)}
                            disabled={item.stockQuantity && item.quantity >= item.stockQuantity || cartLoading || updatingItems.has(item.productId)}
                            sx={{ 
                              color: 'primary.main',
                              '&:hover': { bgcolor: 'primary.light', color: 'white' }
                            }}
                          >
                            {updatingItems.has(item.productId) ? (
                              <CircularProgress size={16} />
                            ) : (
                              <Add fontSize="small" />
                            )}
                          </IconButton>
                        </Box>

                        {/* Item Total */}
                        <Typography variant="h5" fontWeight="bold" color="primary" sx={{ textAlign: 'center' }}>
                          {formatPrice(item.subtotal ?? (item.productPrice * item.quantity))}
                        </Typography>

                        {/* Actions */}
                        <Box sx={{ display: 'flex', gap: 1 }}>
                          <IconButton 
                            size="small" 
                            color="primary"
                            sx={{ 
                              bgcolor: 'primary.light',
                              color: 'white',
                              '&:hover': { bgcolor: 'primary.dark' }
                            }}
                          >
                            <FavoriteOutlined fontSize="small" />
                          </IconButton>
                          
                          <IconButton 
                            size="small" 
                            color="error"
                            onClick={() => handleRemoveItem(item.productId)}
                            disabled={cartLoading}
                            sx={{ 
                              bgcolor: 'error.light',
                              color: 'white',
                              '&:hover': { bgcolor: 'error.dark' }
                            }}
                          >
                            <Delete fontSize="small" />
                          </IconButton>
                        </Box>
                      </Box>
                    </Grid>
                  </Grid>
                </Box>
                
                {index < cart.items.length - 1 && (
                  <Divider sx={{ mx: 4, borderColor: 'divider' }} />
                )}
              </Box>
            ))}
          </Box>

          {/* Continue Shopping */}
          <Box sx={{ mt: 4, textAlign: 'center' }}>
            <Button
              variant="contained"
              size="large"
              startIcon={<ShoppingBag />}
              onClick={() => navigate('/products')}
              sx={{ 
                borderRadius: 3,
                px: 4,
                py: 1.5,
                fontSize: '1.1rem',
                fontWeight: 'bold',
                background: 'linear-gradient(45deg, #2196F3 30%, #21CBF3 90%)',
                boxShadow: '0 3px 5px 2px rgba(33, 203, 243, .3)',
                '&:hover': {
                  background: 'linear-gradient(45deg, #1976D2 30%, #1CB5E0 90%)',
                  boxShadow: '0 6px 10px 4px rgba(33, 203, 243, .3)',
                  transform: 'translateY(-2px)'
                },
                transition: 'all 0.3s ease'
              }}
            >
              🛍️ Continue Shopping
            </Button>
          </Box>
        </Grid>

        {/* Right Sidebar */}
        <Grid item xs={12} lg={4}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            {/* Cart Summary */}
            <Box sx={{ 
              borderRadius: 3, 
              bgcolor: 'background.paper', 
              boxShadow: 2,
              border: '1px solid',
              borderColor: 'divider',
              p: 3
            }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
                <Receipt color="primary" />
                <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                  Tóm tắt giỏ hàng
                </Typography>
              </Box>

              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="body1">Tổng tiền hàng:</Typography>
                <Typography variant="h6" sx={{ fontWeight: 'bold', color: 'primary.main' }}>
                  {formatPrice(cart?.subtotal)}
                </Typography>
              </Box>

              <Divider sx={{ my: 2 }} />

              <Box sx={{ 
                p: 2, 
                bgcolor: 'info.light', 
                borderRadius: 2, 
                border: '1px solid', 
                borderColor: 'info.main' 
              }}>
                <Typography variant="caption" sx={{ fontWeight: 'bold', color: 'info.dark' }}>
                  💡 Lưu ý:
                </Typography>
                <Typography variant="caption" color="info.dark" sx={{ display: 'block', mt: 0.5 }}>
                  • Phí vận chuyển sẽ được tính sau khi nhập địa chỉ
                  • Mã giảm giá có thể áp dụng ở bước thanh toán
                  • Giá chưa bao gồm thuế và phí vận chuyển
                </Typography>
              </Box>
            </Box>

          {/* Checkout Button */}
          <Button
            fullWidth
            variant="contained"
            size="large"
            startIcon={<PaymentOutlined />}
            onClick={handleCheckout}
            sx={{
              borderRadius: 3,
                py: 2,
                fontSize: '1.2rem',
              fontWeight: 'bold',
                mt: 2,
                background: 'linear-gradient(45deg, #FF6B35 30%, #F7931E 90%)',
                boxShadow: '0 4px 8px 3px rgba(255, 107, 53, .3)',
                '&:hover': {
                  background: 'linear-gradient(45deg, #E55A2B 30%, #E8821A 90%)',
                  boxShadow: '0 8px 16px 6px rgba(255, 107, 53, .4)',
                  transform: 'translateY(-2px)'
                },
                transition: 'all 0.3s ease'
              }}
            >
              💳 Tiến hành thanh toán
          </Button>

          {/* Security Info removed as requested */}
          </Box>
        </Grid>
      </Grid>
    </Container>
  )
}

export default ShoppingCartPage 