import React, { useState, useEffect } from 'react'
import { Skeleton } from '@mui/material'
import {
  Container,
  Typography,
  Box,
  Paper,
  Chip,
  Button,
  CircularProgress,
  Grid,
  Card,
  CardContent,
  Divider,
  List,
  ListItem,
  ListItemText,
  Avatar,
  Pagination,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
} from '@mui/material'
import {
  ShoppingCart,
  LocalShipping,
  CheckCircle,
  Cancel,
  Schedule,
  Receipt,
  Visibility,
} from '@mui/icons-material'
import { useQuery } from '@tanstack/react-query'
import { apiService, Order } from '../services/api'
import notificationService from '../services/notificationService'
import websocketService from '../services/websocketService'
import { calculateTotalAmount, formatPrice } from '../utils/priceUtils'

const OrdersPage: React.FC = React.memo(() => {
  const [page, setPage] = useState(0)
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null)
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false)
  const [orderToCancel, setOrderToCancel] = useState<Order | null>(null)
  const [cancelReason, setCancelReason] = useState('')
  const [cancelling, setCancelling] = useState(false)

  // Fetch orders
  const {
    data: ordersResponse,
    isLoading,
    error,
    refetch
  } = useQuery({
    queryKey: ['orders', page],
    queryFn: () => {
      const currentUser = JSON.parse(localStorage.getItem('auth_user') || '{}');
      const userId = currentUser.id || currentUser.userId || 1;
      return apiService.getOrders(page, 10, userId);
    },
    retry: 2,
    staleTime: 5 * 60 * 1000, // 5 minutes
  })

  const orders = ordersResponse?.data.content || []
  const totalElements = ordersResponse?.data.totalElements || 0
  const totalPages = Math.ceil(totalElements / 10)

  useEffect(() => {
    const unsubscribe = websocketService.onNotificationEvent((event) => {
      if (!event) return
      const data = event.data || {}
      const eventType = String(event.type || data.type || '').toUpperCase()

      if (eventType.startsWith('ORDER')) {
        const orderNumber = data.orderNumber || data.orderId || ''
        const status = data.newStatus || data.status || ''
        const msg =
          event.message ||
          (orderNumber ? `Order ${orderNumber} updated to ${status}` : 'Order updated')

        notificationService.info(msg)
        refetch()
      }
    })

    return () => {
      if (typeof unsubscribe === 'function') {
        unsubscribe()
      }
    }
  }, [refetch])

  // Handle page change
  const handlePageChange = (_event: React.ChangeEvent<unknown>, newPage: number) => {
    setPage(newPage - 1) // Convert to 0-based index
  }

  // Handle cancel order
  const handleCancelOrder = (order: Order) => {
    setOrderToCancel(order)
    setCancelDialogOpen(true)
  }

  const handleCancelConfirm = async () => {
    if (!orderToCancel || !cancelReason.trim()) {
      notificationService.error('Vui lòng nhập lý do hủy đơn')
      return
    }

    try {
      setCancelling(true)
      await apiService.cancelOrder(orderToCancel.id, cancelReason.trim())
      
      // Refresh orders list
      refetch()
      
      // Close dialog and reset state
      setCancelDialogOpen(false)
      setOrderToCancel(null)
      setCancelReason('')
      
      notificationService.success('Hủy đơn hàng thành công')
    } catch (error) {
      notificationService.error('Hủy đơn hàng thất bại')
    } finally {
      setCancelling(false)
    }
  }

  const handleCancelDialogClose = () => {
    if (!cancelling) {
      setCancelDialogOpen(false)
      setOrderToCancel(null)
      setCancelReason('')
    }
  }

  const isCOD = (method?: string) => {
    const m = (method || '').toUpperCase()
    return m.includes('COD') || m.includes('CASH') || m.includes('CASH_ON_DELIVERY')
  }

  const getStatusInfo = (status: string, paymentMethod?: string) => {
    switch (status) {
      case 'PENDING':
        return {
          label: 'Pending approval',
          color: 'warning' as const,
          icon: <Schedule />,
          description: 'Order created, waiting for processing'
        }
      case 'PENDING_PAYMENT':
        // If COD somehow carries PENDING_PAYMENT, treat it as pending approval for display
        if (isCOD(paymentMethod)) {
          return {
            label: 'Pending approval',
            color: 'info' as const,
            icon: <Schedule />,
            description: 'Waiting for admin approval'
          }
        }
        return {
          label: 'Pending payment',
          color: 'default' as const,
          icon: <Schedule />,
          description: 'Waiting for online payment'
        }
      case 'PAID':
        return {
          label: 'Paid • Pending approval',
          color: 'success' as const,
          icon: <CheckCircle />,
          description: 'Payment received, awaiting admin confirmation'
        }
      case 'PENDING_APPROVAL':
        return {
          label: 'Pending Approval',
          color: 'info' as const,
          icon: <Schedule />,
          description: 'Waiting for admin approval'
        }
      case 'APPROVED':
        return {
          label: 'Approved',
          color: 'success' as const,
          icon: <CheckCircle />,
          description: 'Order approved by admin'
        }
      case 'CONFIRMED':
        return {
          label: 'Confirmed',
          color: 'primary' as const,
          icon: <CheckCircle />,
          description: 'Order confirmed by admin'
        }
      case 'PROCESSING':
        return {
          label: 'Processing',
          color: 'secondary' as const,
          icon: <LocalShipping />,
          description: 'Order being processed and packed'
        }
      case 'SHIPPED':
        return {
          label: 'Shipped',
          color: 'info' as const,
          icon: <LocalShipping />,
          description: 'Order shipped to customer'
        }
      case 'DELIVERED':
        return {
          label: 'Delivered',
          color: 'success' as const,
          icon: <CheckCircle />,
          description: 'Order delivered successfully'
        }
      case 'COMPLETED':
        return {
          label: 'Completed',
          color: 'success' as const,
          icon: <CheckCircle />,
          description: 'Order completed by customer'
        }
      case 'CANCELLED':
        return {
          label: 'Cancelled',
          color: 'error' as const,
          icon: <Cancel />,
          description: 'Order has been cancelled'
        }
      case 'REFUNDED':
        return {
          label: 'Refunded',
          color: 'default' as const,
          icon: <Receipt />,
          description: 'Order refunded to customer'
        }
      default:
        return {
          label: status,
          color: 'default' as const,
          icon: <Schedule />,
          description: 'Unknown status'
        }
    }
  }

  const getPaymentInfo = (paymentStatus?: string, paymentMethod?: string, orderStatus?: string) => {
    // Nếu không có paymentMethod, coi như COD để tránh hiển thị Paid sai
    const treatAsCOD = isCOD(paymentMethod) || !paymentMethod
    // COD: chỉ chuyển sang Paid khi đã giao (Delivered/Completed)
    if (treatAsCOD) {
      const codPaid = orderStatus === 'DELIVERED' || orderStatus === 'COMPLETED'
      return codPaid ? { label: 'Paid', color: 'success' as const } : { label: 'Unpaid (COD)', color: 'warning' as const }
    }
    const paidByField = paymentStatus === 'COMPLETED' || paymentStatus === 'PAID'
    // Non-COD: coi là Paid sau khi thanh toán thành công hoặc đã qua bước chờ thanh toán
    const paidByStatus = !!orderStatus && (
      ['PAID', 'PENDING_APPROVAL', 'APPROVED', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED'].includes(orderStatus)
    )
    const paid = paidByField || paidByStatus
    if (paid) {
      return { label: 'Paid', color: 'success' as const }
    }
    // Treat COD and any non-completed online as unpaid
    if (isCOD(paymentMethod)) {
      return { label: 'Unpaid (COD)', color: 'warning' as const }
    }
    return { label: 'Unpaid', color: 'default' as const }
  }

  const canCancelOrder = (status: string) => {
    // Cho phép hủy với mọi trạng thái chưa kết thúc
    // Chỉ không cho hủy khi đơn đã giao/hoàn tất/đã hủy/hoàn tiền/thất bại
    return !['SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED', 'REFUNDED', 'FAILED'].includes(status)
  }

  // Format date
  const formatDate = (dateString: string) => {
    if (!dateString || dateString === 'Invalid Date') {
      return 'N/A'
    }
    
    try {
      const date = new Date(dateString)
      if (isNaN(date.getTime())) {
        return 'N/A'
      }
      
      return date.toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    } catch (error) {
      return 'N/A'
    }
  }

  // Calculate price breakdown for selected order
  const getOrderPriceBreakdown = (order: Order) => {
    return calculateTotalAmount(
      (order as any).subtotal || 0,
      order.shippingFee || 0,
      (order as any).discountAmount || 0
    )
  }

  // Handle view order details
  const handleViewOrder = (order: Order) => {
    setSelectedOrder(order)
    notificationService.info(`Viewing order #${order.id}`)
  }

  const displayOrders = orders

  // Show error state
  if (error && !displayOrders.length) {
    return (
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h6" color="error" gutterBottom>
            Failed to load orders
          </Typography>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            Showing demo data instead
          </Typography>
          <Button variant="contained" onClick={() => refetch()} sx={{ mt: 2 }}>
            Try Again
          </Button>
        </Paper>
      </Container>
    )
  }

  return (
    <>
      <Container maxWidth="lg" sx={{ py: 4, pt: '40px' }}>
        {/* Header */}
        <Box sx={{ mb: 4, display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
          <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 700, color: 'text.primary' }}>
            Your Orders
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 2 }}>
            Track and manage your order history
          </Typography>
        </Box>

        <Grid container spacing={3}>
          {/* Orders List */}
          <Grid item xs={12} md={selectedOrder ? 8 : 12}>
            {/* Loading State */}
            {isLoading ? (
              <Box>
                {[...Array(5)].map((_, index) => (
                  <Card key={index} sx={{ mb: 2 }}>
                    <CardContent>
                      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                        <Skeleton variant="text" width={200} height={32} />
                        <Skeleton variant="rectangular" width={80} height={24} />
                      </Box>
                      <Skeleton variant="text" width={150} height={24} />
                      <Skeleton variant="text" width={100} height={24} />
                    </CardContent>
                  </Card>
                ))}
              </Box>
            ) : displayOrders.length === 0 ? (
              <Paper sx={{ p: 8, textAlign: 'center' }}>
                <ShoppingCart sx={{ fontSize: 80, color: 'text.secondary', mb: 2 }} />
                <Typography variant="h6" color="text.secondary" gutterBottom>
                  No orders found
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Start shopping to see your orders here
                </Typography>
              </Paper>
            ) : (
              <>
                {displayOrders.map((order) => {
                  const statusInfo = getStatusInfo(order.status, order.paymentMethod)
                  
                  return (
                    <Card 
                      key={order.id}
                      sx={{ 
                        mb: 2,
                        cursor: 'pointer',
                        transition: 'all 0.2s',
                        '&:hover': {
                          boxShadow: 4,
                          transform: 'translateY(-2px)',
                        },
                        border: selectedOrder?.id === order.id ? 1 : 1,
                        borderColor: selectedOrder?.id === order.id ? 'primary.main' : 'rgba(148,163,184,0.2)',
                        background: 'linear-gradient(180deg, rgba(255,255,255,0.02), rgba(255,255,255,0.01))',
                      }}
                      onClick={() => handleViewOrder(order)}
                    >
                      <CardContent>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                          <Box>
                            <Typography variant="h6" gutterBottom>
                              Order #{order.id}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              Placed on {formatDate(order.createdAt)}
                            </Typography>
                          </Box>
                          <Box sx={{ display: 'flex', gap: 1 }}>
                            <Chip
                              icon={statusInfo.icon}
                              label={statusInfo.label}
                              color={statusInfo.color}
                              variant="outlined"
                            />
                            <Chip
                              label={getPaymentInfo((order as unknown as { paymentStatus?: string }).paymentStatus, order.paymentMethod, order.status).label}
                              color={getPaymentInfo((order as unknown as { paymentStatus?: string }).paymentStatus, order.paymentMethod, order.status).color}
                              variant="outlined"
                            />
                          </Box>
                        </Box>

                        <Divider sx={{ my: 2 }} />

                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Box>
                            <Typography variant="body2" color="text.secondary">
                              {order.orderItems?.length || 0} item(s)
                            </Typography>
                            <Typography variant="h6" color="primary" fontWeight="bold">
                              {order.status === 'CANCELLED' ? '-' : formatPrice(order.totalAmount)}
                            </Typography>
                          </Box>
                          
                          <Box sx={{ display: 'flex', gap: 1 }}>
                            {order.status !== 'CANCELLED' && (
                              <Button
                                variant="outlined"
                                startIcon={<Visibility />}
                                size="small"
                              >
                                View Details
                              </Button>
                            )}
                            
                            {canCancelOrder(order.status) && (
                              <Button
                                variant="outlined"
                                color="error"
                                startIcon={<Cancel />}
                                size="small"
                                onClick={() => handleCancelOrder(order)}
                              >
                                Cancel
                              </Button>
                            )}
                          </Box>
                        </Box>
                      </CardContent>
                    </Card>
                  )
                })}

                {/* Pagination */}
                {totalPages > 1 && (
                  <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
                    <Pagination
                      count={totalPages}
                      page={page + 1}
                      onChange={handlePageChange}
                      color="primary"
                      size="large"
                    />
                  </Box>
                )}
              </>
            )}
          </Grid>

          {/* Order Details Sidebar */}
          {selectedOrder && (
            <Grid item xs={12} md={4}>
              <Paper sx={{ p: 3, position: 'sticky', top: 20, border: '1px solid rgba(148,163,184,0.2)', background: 'linear-gradient(180deg, rgba(255,255,255,0.02), rgba(255,255,255,0.01))' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                  <Receipt sx={{ mr: 1, color: 'primary.main' }} />
                  <Typography variant="h6">
                    Order Details
                  </Typography>
                </Box>

                <Divider sx={{ mb: 2 }} />

                <Box sx={{ mb: 3 }}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Order ID
                  </Typography>
                  <Typography variant="body1" fontWeight="bold">
                    #{selectedOrder.id}
                  </Typography>
                </Box>

                <Box sx={{ mb: 3 }}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Status
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
                    <Chip
                      icon={getStatusInfo(selectedOrder.status, selectedOrder.paymentMethod).icon}
                      label={getStatusInfo(selectedOrder.status, selectedOrder.paymentMethod).label}
                      color={getStatusInfo(selectedOrder.status, selectedOrder.paymentMethod).color}
                      size="small"
                    />
                    <Chip
                      label={getPaymentInfo((selectedOrder as unknown as { paymentStatus?: string }).paymentStatus, selectedOrder.paymentMethod, selectedOrder.status).label}
                      color={getPaymentInfo((selectedOrder as unknown as { paymentStatus?: string }).paymentStatus, selectedOrder.paymentMethod, selectedOrder.status).color}
                      size="small"
                    />
                  </Box>
                </Box>

                {/* Order Breakdown */}
                <Box sx={{ mb: 3 }}>
                  <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                    Order Breakdown
                  </Typography>
                  <Box sx={{ bgcolor: 'grey.50', p: 2, borderRadius: 1 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography variant="body2">Subtotal:</Typography>
                      <Typography variant="body2" fontWeight="bold">
                        {formatPrice(getOrderPriceBreakdown(selectedOrder).subtotal)}
                      </Typography>
                    </Box>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography variant="body2">Tax:</Typography>
                      <Typography variant="body2" fontWeight="bold">
                        {formatPrice(getOrderPriceBreakdown(selectedOrder).taxAmount)}
                      </Typography>
                    </Box>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography variant="body2">Shipping:</Typography>
                      <Typography variant="body2" fontWeight="bold">
                        {formatPrice(selectedOrder.shippingFee || 0)}
                      </Typography>
                    </Box>
                    {getOrderPriceBreakdown(selectedOrder).discountAmount > 0 && (
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                        <Typography variant="body2" color="success.main">Discount:</Typography>
                        <Typography variant="body2" color="success.main" fontWeight="bold">
                          -{formatPrice(getOrderPriceBreakdown(selectedOrder).discountAmount)}
                        </Typography>
                      </Box>
                    )}
                    <Divider sx={{ my: 1 }} />
                    <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="h6" fontWeight="bold">Total:</Typography>
                      <Typography variant="h6" color="primary" fontWeight="bold">
                        {formatPrice(getOrderPriceBreakdown(selectedOrder).totalAmount)}
                      </Typography>
                    </Box>
                  </Box>
                </Box>

                <Box sx={{ mb: 3 }}>
                  <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                    Items ({selectedOrder.orderItems?.length || 0})
                  </Typography>
                  <List dense>
                    {(!selectedOrder.orderItems || selectedOrder.orderItems.length === 0) ? (
                      <ListItem sx={{ px: 0 }}>
                        <ListItemText
                          primary={selectedOrder.status === 'CANCELLED' ? 'This order was cancelled. Items have been removed.' : 'No items.'}
                        />
                      </ListItem>
                    ) : (
                      selectedOrder.orderItems.map((item, index) => (
                        <ListItem key={index} sx={{ px: 0, py: 1 }}>
                          <Avatar 
                            src={item.productImage || '/images/home.png'} 
                            sx={{ 
                              mr: 2, 
                              width: 48, 
                              height: 48,
                              borderRadius: 1
                            }}
                          />
                          <ListItemText
                            primary={
                              <Box>
                                <Typography variant="body1" fontWeight="bold">
                                  {item.productName}
                                </Typography>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 0.5 }}>
                                  <Chip 
                                    label={`Qty: ${item.quantity}`}
                                    size="small"
                                    color="primary"
                                    variant="outlined"
                                  />
                                  <Typography variant="body2" color="text.secondary">
                                    {formatPrice(item.unitPrice || (item.price / item.quantity))} each
                                  </Typography>
                                </Box>
                              </Box>
                            }
                            secondary={
                              <Typography variant="body2" color="primary.main" fontWeight="bold">
                                Total: {formatPrice(item.totalPrice || item.price)}
                              </Typography>
                            }
                          />
                        </ListItem>
                      ))
                    )}
                  </List>
                </Box>

                <Divider sx={{ mb: 2 }} />

                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                  <Typography variant="body2" color="text.secondary">
                    <strong>Ordered:</strong> {formatDate(selectedOrder.createdAt)}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    <strong>Updated:</strong> {formatDate(selectedOrder.updatedAt)}
                  </Typography>
                </Box>

                {/* Cancel Order Button */}
                {canCancelOrder(selectedOrder.status) && (
                  <Box sx={{ mt: 3 }}>
                    <Button
                      variant="outlined"
                      color="error"
                      startIcon={<Cancel />}
                      onClick={() => {
                        setOrderToCancel(selectedOrder)
                        setCancelDialogOpen(true)
                      }}
                      fullWidth
                      sx={{
                        borderRadius: 2,
                        py: 1.5,
                        fontWeight: 'bold',
                        borderColor: 'error.main',
                        color: 'error.main',
                        '&:hover': {
                          borderColor: 'error.dark',
                          backgroundColor: 'error.light',
                          color: 'error.dark'
                        }
                      }}
                    >
                      Hủy đơn hàng
                    </Button>
                  </Box>
                )}
              </Paper>
            </Grid>
          )}
        </Grid>
      </Container>

      {/* Cancel Order Dialog */}
      <Dialog
        open={cancelDialogOpen}
        onClose={handleCancelDialogClose}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>
          Hủy đơn hàng
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Bạn có chắc chắn muốn hủy đơn{' '}
            <strong>{orderToCancel?.orderNumber}</strong>?
          </Typography>
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Lý do hủy đơn"
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
            placeholder="Vui lòng nhập lý do hủy đơn..."
            required
          />
        </DialogContent>
        <DialogActions>
          <Button 
            onClick={handleCancelDialogClose}
            disabled={cancelling}
          >
            Đóng
          </Button>
          <Button
            onClick={handleCancelConfirm}
            color="error"
            variant="contained"
            disabled={!cancelReason.trim() || cancelling}
          >
            {cancelling ? <CircularProgress size={20} sx={{ mr: 1 }} /> : null}
            Xác nhận hủy
          </Button>
        </DialogActions>
      </Dialog>
    </>
  )
})

export default OrdersPage 