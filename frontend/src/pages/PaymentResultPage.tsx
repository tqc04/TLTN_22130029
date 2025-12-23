import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useNotification } from '../contexts/NotificationContext';
import { useCart } from '../contexts/CartContext';
import { apiService } from '../services/api';
import { calculateTotalAmount, formatPrice } from '../utils/priceUtils'
import {
  Box,
  Container,
  Typography,
  Button,
  Paper,
  CircularProgress,
  Grid,
  Card,
  CardContent,
  Divider,
  Chip,
  Avatar,
  Stepper,
  Step,
  StepLabel,
  StepContent,
  FormControlLabel,
  Checkbox,
} from '@mui/material';
import {
  CheckCircle,
  Error,
  Home,
  Refresh,
  Receipt,
  LocalShipping,
  Assignment,
  ArrowForward,
  ShoppingBag,
  Print,
} from '@mui/icons-material';

interface OrderDetails {
  id: number;
  orderNumber: string;
  status: string;
  totalAmount: number;
  subtotal: number;
  taxAmount: number;
  shippingFee: number;
  discountAmount: number;
  items: OrderItem[];
  createdAt: string;
  paymentMethod: string;
  shippingAddress: string;
}

interface OrderItem {
  id: number;
  product: {
    id: string;
    name: string;
    imageUrl: string;
    price: number;
  };
  quantity: number;
  price: number;
}

interface ApiOrderItem {
  id: number;
  productId: string;
  quantity: number;
  price: number;
  totalPrice: number;
  productName: string;
  productSku: string;
  productImage?: string;
}

// Simple error boundary to avoid white screen if any runtime error happens in this page
class PaymentResultErrorBoundary extends React.Component<{ children: React.ReactNode }, { hasError: boolean; error?: unknown }>{
  constructor(props: any) {
    super(props);
    this.state = { hasError: false };
  }
  static getDerivedStateFromError(error: unknown) {
    return { hasError: true, error };
  }
  componentDidCatch(error: unknown, info: unknown) {
    // Log for debugging
    console.error('PaymentResultPage crashed:', error, info);
  }
  render() {
    if (this.state.hasError) {
      return (
        <Container maxWidth="sm" sx={{ py: 8, pt: '40px' }}>
          <Paper elevation={3} sx={{ p: 4, textAlign: 'center', borderRadius: 3 }}>
            <Error sx={{ fontSize: 80, color: 'error.main', mb: 3 }} />
            <Typography variant="h5" gutterBottom color="error.main" fontWeight="bold">
              Có lỗi xảy ra khi hiển thị đơn hàng
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Vui lòng tải lại trang hoặc quay về trang chủ.
            </Typography>
            <Button variant="contained" onClick={() => window.location.reload()}>Tải lại</Button>
          </Paper>
        </Container>
      );
    }
    return this.props.children as any;
  }
}

const PaymentResultPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { notify } = useNotification();
  const { clearCart } = useCart();

  // Cache for order details to avoid repeated API calls
  const orderDetailsCache = useRef<Map<string, OrderDetails>>(new Map());
  
  const [orderDetails, setOrderDetails] = useState<OrderDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [paymentSuccess, setPaymentSuccess] = useState(false);
  const [disableOrderNotification, setDisableOrderNotification] = useState(false);
  const redirectUrl = decodeURIComponent(searchParams.get('redirectUrl') || '');
  const PENDING_REDIRECT_KEY = 'pending_payment_result_redirect'
  // Banner lớn hiển thị cố định (không auto-hide)
  const [showSuccessBanner] = useState(true);
  const hasNotifiedRef = useRef(false);
  const hasClearedCartRef = useRef(false);

  useEffect(() => {
    // Load user preference for order notifications
    const savedPreference = localStorage.getItem('disableOrderNotification');
    setDisableOrderNotification(savedPreference === 'true');
    // Clear pending redirect mark if any
    try { localStorage.removeItem(PENDING_REDIRECT_KEY); } catch {}
  }, []);

  useEffect(() => {
    const orderNumber = searchParams.get('orderNumber');
    const success = searchParams.get('success') === 'true';
    // Status removed - not used
    const message = searchParams.get('message');
    
    setPaymentSuccess(success);

    if (success && orderNumber) {
      // Fetch order details
      fetchOrderDetails(orderNumber);

      // Clear cart after successful payment (only once)
      if (!hasClearedCartRef.current) {
        const clearCartOnSuccess = async () => {
          try {
            await clearCart();
            hasClearedCartRef.current = true;
            console.log('Cart cleared after successful payment');
          } catch (error) {
            console.error('Failed to clear cart after successful payment:', error);
          }
        };
        clearCartOnSuccess();
      }
    } else if (!success) {
      // Payment failed - rollback order and restore inventory
      if (orderNumber) {
        const rollbackOrder = async () => {
          try {
            console.log('Payment failed, cancelling order:', orderNumber);
            await apiService.post(`/api/orders/by-number/${orderNumber}/cancel`, {
              reason: 'Payment failed - cancelled by system'
            });
            console.log('Order cancelled successfully');
          } catch (error) {
            console.error('Failed to cancel order:', error);
          }
        };
        rollbackOrder();
      }
      setLoading(false);
    }

    // Show notification only once
    if (!hasNotifiedRef.current) {
      if (success && !disableOrderNotification) {
        notify('Đặt hàng thành công!', 'success', 2500);
        hasNotifiedRef.current = true;
      } else if (!success) {
        notify(message || 'Thanh toán thất bại!', 'error', 4000);
        hasNotifiedRef.current = true;
      }
    }
    
    //
  }, [searchParams, disableOrderNotification, clearCart]);

  // Banner lớn không tự ẩn

  const handleNotificationPreferenceChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const checked = event.target.checked;
    setDisableOrderNotification(checked);
    localStorage.setItem('disableOrderNotification', checked.toString());
  };

  const fetchOrderDetails = useCallback(async (orderNumber: string) => {
    // Check cache first
    if (orderDetailsCache.current.has(orderNumber)) {
      setOrderDetails(orderDetailsCache.current.get(orderNumber)!);
      setLoading(false);
      return;
    }

    try {
      const response = await apiService.getOrderByNumber(orderNumber);
      if (response.success && response.data) {
        const orderData = response.data;
        
        // Transform API response to match our interface
        const safeItems: ApiOrderItem[] = Array.isArray(orderData.orderItems) ? orderData.orderItems : [];
        const orderDetails: OrderDetails = {
          id: orderData.id,
          orderNumber: orderData.orderNumber,
          status: orderData.status,
          totalAmount: orderData.totalAmount,
          subtotal: (orderData as any).subtotal || 0, // Use subtotal from backend
          taxAmount: (orderData as any).taxAmount || 0, // Use taxAmount from backend
          shippingFee: (orderData as any).shippingFee ?? (orderData as any).shippingAmount ?? 0,
          discountAmount: orderData.discountAmount || 0,
          items: safeItems.map((item: ApiOrderItem) => ({
            id: item.id,
            product: {
              id: item.productId || String(item.id), // Use productId from backend, fallback to item.id as string
              name: item.productName,
              imageUrl: item.productImage || '/images/home.png', // Use productImage if available
              price: item.price
            },
            quantity: item.quantity,
            price: item.price
          })),
          createdAt: orderData.createdAt || new Date().toISOString(),
          paymentMethod: (searchParams.get('paymentMethod') || orderData.paymentMethod || 'VNPAY').toUpperCase(),
          shippingAddress: orderData.shippingAddress || 'Địa chỉ không có sẵn'
        };
        
        // Cache the order details
        orderDetailsCache.current.set(orderNumber, orderDetails);
        setOrderDetails(orderDetails);
      } else {
        // Soft-fail without throwing to avoid type issues during build
        setOrderDetails(null);
      }
    } catch (error: unknown) {
      const errorMessage = error && typeof error === 'object' && 'message' in error ? String(error.message) : 'Error fetching order details';
      console.error('Error fetching order details:', errorMessage);
      notify('Không thể tải thông tin đơn hàng', 'error');
    } finally {
      setLoading(false);
    }
  }, [notify, searchParams]);

  // Calculate price breakdown for order details
  const getOrderPriceBreakdown = (orderDetails: OrderDetails) => {
    return calculateTotalAmount(
      orderDetails.subtotal || 0,
      orderDetails.shippingFee || 0,
      orderDetails.discountAmount || 0
    )
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Print invoice function
  const handlePrintInvoice = () => {
    if (!orderDetails) return;
    
    const priceBreakdown = getOrderPriceBreakdown(orderDetails);
    
    const printContent = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8">
        <title>Hóa đơn - ${orderDetails.orderNumber}</title>
        <style>
          * { margin: 0; padding: 0; box-sizing: border-box; }
          body { 
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
            padding: 40px; 
            max-width: 800px; 
            margin: 0 auto;
            color: #333;
          }
          .header { 
            text-align: center; 
            margin-bottom: 30px; 
            padding-bottom: 20px;
            border-bottom: 2px solid #4CAF50;
          }
          .header h1 { 
            color: #4CAF50; 
            font-size: 28px; 
            margin-bottom: 5px;
          }
          .header p { color: #666; font-size: 14px; }
          .invoice-info { 
            display: flex; 
            justify-content: space-between; 
            margin-bottom: 30px;
            padding: 20px;
            background: #f8f9fa;
            border-radius: 8px;
          }
          .invoice-info div { flex: 1; }
          .invoice-info h3 { 
            color: #333; 
            font-size: 14px; 
            margin-bottom: 8px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
          }
          .invoice-info p { 
            color: #666; 
            font-size: 13px; 
            line-height: 1.6;
          }
          .items-table { 
            width: 100%; 
            border-collapse: collapse; 
            margin-bottom: 30px;
          }
          .items-table th { 
            background: #4CAF50; 
            color: white; 
            padding: 12px 15px; 
            text-align: left;
            font-size: 13px;
            text-transform: uppercase;
          }
          .items-table td { 
            padding: 15px; 
            border-bottom: 1px solid #eee;
            font-size: 14px;
          }
          .items-table tr:hover { background: #f9f9f9; }
          .summary { 
            margin-left: auto; 
            width: 300px;
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
          }
          .summary-row { 
            display: flex; 
            justify-content: space-between; 
            padding: 8px 0;
            font-size: 14px;
          }
          .summary-row.total { 
            border-top: 2px solid #4CAF50; 
            margin-top: 10px;
            padding-top: 15px;
            font-weight: bold;
            font-size: 18px;
            color: #4CAF50;
          }
          .footer { 
            margin-top: 40px; 
            text-align: center; 
            padding-top: 20px;
            border-top: 1px solid #eee;
            color: #888;
            font-size: 12px;
          }
          .discount { color: #4CAF50; }
          @media print {
            body { padding: 20px; }
            .no-print { display: none; }
          }
        </style>
      </head>
      <body>
        <div class="header">
          <h1>🛒 TechHub</h1>
          <p>HÓA ĐƠN BÁN HÀNG</p>
        </div>
        
        <div class="invoice-info">
          <div>
            <h3>Thông tin đơn hàng</h3>
            <p><strong>Mã đơn:</strong> ${orderDetails.orderNumber}</p>
            <p><strong>Ngày đặt:</strong> ${formatDate(orderDetails.createdAt)}</p>
            <p><strong>Thanh toán:</strong> ${orderDetails.paymentMethod === 'COD' ? 'Thanh toán khi nhận hàng' : 
              orderDetails.paymentMethod === 'BANK_TRANSFER' ? 'Chuyển khoản ngân hàng' : 
              orderDetails.paymentMethod === 'VNPAY' ? 'VNPay' : orderDetails.paymentMethod}</p>
          </div>
          <div>
            <h3>Địa chỉ giao hàng</h3>
            <p>${orderDetails.shippingAddress}</p>
          </div>
        </div>
        
        <table class="items-table">
          <thead>
            <tr>
              <th>Sản phẩm</th>
              <th style="text-align: center;">Số lượng</th>
              <th style="text-align: right;">Đơn giá</th>
              <th style="text-align: right;">Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            ${orderDetails.items.map(item => `
              <tr>
                <td>${item.product.name}</td>
                <td style="text-align: center;">${item.quantity}</td>
                <td style="text-align: right;">${formatPrice(item.price / item.quantity)}</td>
                <td style="text-align: right;">${formatPrice(item.price)}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
        
        <div class="summary">
          <div class="summary-row">
            <span>Tạm tính:</span>
            <span>${formatPrice(priceBreakdown.subtotal)}</span>
          </div>
          <div class="summary-row">
            <span>Thuế (10%):</span>
            <span>${formatPrice(priceBreakdown.taxAmount)}</span>
          </div>
          <div class="summary-row">
            <span>Phí vận chuyển:</span>
            <span>${formatPrice(priceBreakdown.shippingAmount)}</span>
          </div>
          ${priceBreakdown.discountAmount > 0 ? `
            <div class="summary-row discount">
              <span>Giảm giá:</span>
              <span>-${formatPrice(priceBreakdown.discountAmount)}</span>
            </div>
          ` : ''}
          <div class="summary-row total">
            <span>Tổng cộng:</span>
            <span>${formatPrice(priceBreakdown.totalAmount)}</span>
          </div>
        </div>
        
        <div class="footer">
          <p>Cảm ơn bạn đã mua sắm tại TechHub!</p>
          <p>Hotline: 1900-xxxx | Email: support@techhub.vn</p>
        </div>
      </body>
      </html>
    `;
    
    const printWindow = window.open('', '_blank');
    if (printWindow) {
      printWindow.document.write(printContent);
      printWindow.document.close();
      printWindow.focus();
      setTimeout(() => {
        printWindow.print();
      }, 250);
    }
  };

  if (loading) {
    return (
      <Container maxWidth="lg" sx={{ py: 8, pt: '40px' }}>
        <Box sx={{ textAlign: 'center' }}>
          <CircularProgress size={60} sx={{ color: 'primary.main' }} />
          <Typography variant="h6" sx={{ mt: 3, color: 'text.secondary' }}>
            Đang xử lý thông tin đơn hàng...
          </Typography>
        </Box>
      </Container>
    );
  }

  if (!paymentSuccess) {
    return (
      <Container maxWidth="sm" sx={{ py: 8, pt: '40px' }}>
        <Paper elevation={3} sx={{ p: 4, textAlign: 'center', borderRadius: 3 }}>
          <Error sx={{ fontSize: 80, color: 'error.main', mb: 3 }} />
          <Typography variant="h4" gutterBottom color="error.main" fontWeight="bold">
            Thanh toán thất bại
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
            Có lỗi xảy ra trong quá trình thanh toán. Vui lòng thử lại.
          </Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Button
              variant="contained"
              color="error"
              startIcon={<Refresh />}
              onClick={() => navigate('/checkout')}
              size="large"
              sx={{ borderRadius: 2 }}
            >
              Thử lại thanh toán
            </Button>
            <Button
              variant="outlined"
              startIcon={<Home />}
              onClick={() => navigate('/')}
              size="large"
              sx={{ borderRadius: 2 }}
            >
              Về trang chủ
            </Button>
          </Box>
        </Paper>
      </Container>
    );
  }

  if (!orderDetails) {
    return (
      <Container maxWidth="sm" sx={{ py: 8, pt: '40px' }}>
        <Paper elevation={3} sx={{ p: 4, textAlign: 'center', borderRadius: 3 }}>
          <Error sx={{ fontSize: 80, color: 'error.main', mb: 3 }} />
          <Typography variant="h4" gutterBottom color="error.main" fontWeight="bold">
            Không tìm thấy đơn hàng
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
            Không thể tải thông tin đơn hàng. Vui lòng thử lại.
          </Typography>
          <Button
            variant="contained"
            startIcon={<Home />}
            onClick={() => navigate('/')}
            size="large"
            sx={{ borderRadius: 2 }}
          >
            Về trang chủ
          </Button>
        </Paper>
      </Container>
    );
  }

  return (
    <PaymentResultErrorBoundary>
    <Container maxWidth="lg" sx={{ py: 4, pt: '40px' }}>
      {/* Success Header (auto-hide and closable) */}
      {showSuccessBanner && (
        <Paper 
          elevation={0} 
          sx={{ 
            p: 4, 
            mb: 4, 
            textAlign: 'center', 
            borderRadius: 3,
            background: 'linear-gradient(135deg, #4CAF50 0%, #45a049 100%)',
            color: 'white',
            position: 'relative',
            overflow: 'hidden',
            '&::before': {
              content: '""',
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              background: 'radial-gradient(circle at 30% 20%, rgba(255,255,255,0.1) 0%, transparent 50%)',
              pointerEvents: 'none'
            }
          }}
        >
          {/* Banner lớn cố định, không có nút tắt */}
          <Box sx={{ position: 'relative', zIndex: 1 }}>
            <CheckCircle 
              sx={{ 
                fontSize: 80, 
                mb: 2, 
                filter: 'drop-shadow(0 4px 8px rgba(255,255,255,0.3))',
                animation: 'pulse 2s infinite'
              }} 
            />
            <Typography variant="h3" gutterBottom fontWeight="bold">
              Đặt hàng thành công!
            </Typography>
            <Typography variant="h6" sx={{ opacity: 0.9, mb: 2 }}>
              Cảm ơn bạn đã mua sắm tại TechHub
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, alignItems: 'center' }}>
              <Chip 
                label={`Mã đơn hàng: ${orderDetails.orderNumber}`}
                sx={{ 
                  bgcolor: 'rgba(255,255,255,0.2)', 
                  color: 'white', 
                  fontSize: '1.1rem',
                  fontWeight: 'bold',
                  border: '1px solid rgba(255,255,255,0.3)'
                }}
              />
              <Chip 
                label={`Phương thức: ${orderDetails.paymentMethod === 'COD' ? 'Thanh toán khi nhận hàng' : 
                         orderDetails.paymentMethod === 'BANK_TRANSFER' ? 'Chuyển khoản ngân hàng' : 
                         orderDetails.paymentMethod === 'VNPAY' ? 'VNPay' : orderDetails.paymentMethod}`}
                sx={{ 
                  bgcolor: 'rgba(255,255,255,0.15)', 
                  color: 'white', 
                  fontSize: '0.9rem',
                  fontWeight: 'normal',
                  border: '1px solid rgba(255,255,255,0.2)'
                }}
              />
            </Box>
          </Box>
        </Paper>
      )}

      {/* Pending external payment banner (VNPay/MoMo) */}
      {redirectUrl && orderDetails && (orderDetails.paymentMethod === 'VNPAY' || orderDetails.paymentMethod === 'MOMO') && (
        <Paper elevation={2} sx={{ p: 3, borderRadius: 2, mb: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 2 }}>
            <Box>
              <Typography variant="h6" fontWeight="bold">Tiếp tục thanh toán</Typography>
              <Typography variant="body2" color="text.secondary">
                Đơn hàng đang chờ thanh toán qua {orderDetails.paymentMethod}. Nhấn nút bên phải để tới cổng thanh toán.
              </Typography>
            </Box>
            <Button variant="contained" color="primary" size="large" endIcon={<ArrowForward />} onClick={() => window.location.href = redirectUrl}>
              Thanh toán ngay
            </Button>
          </Box>
        </Paper>
      )}

      <Grid container spacing={4}>
        {/* Order Details */}
        <Grid item xs={12} md={8}>
          <Paper elevation={2} sx={{ p: 4, borderRadius: 3, mb: 4 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
              <Receipt sx={{ fontSize: 32, color: 'primary.main', mr: 2 }} />
              <Typography variant="h5" fontWeight="bold">
                Chi tiết đơn hàng
              </Typography>
            </Box>
            
            <Divider sx={{ mb: 3 }} />
            
            {/* Order Items */}
            <Box sx={{ mb: 4 }}>
              {orderDetails.items.map((item) => (
                <Card 
                  key={item.id} 
                  sx={{ 
                    mb: 2, 
                    borderRadius: 3, 
                    boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                    transition: 'all 0.3s ease',
                    '&:hover': {
                      transform: 'translateY(-2px)',
                      boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
                    }
                  }}
                >
                  <CardContent sx={{ p: 3 }}>
                    <Grid container spacing={3} alignItems="center">
                      <Grid item xs={3} sm={2}>
                        <Avatar
                          src={item.product.imageUrl}
                          variant="rounded"
                          sx={{ 
                            width: 80, 
                            height: 80, 
                            borderRadius: 2,
                            boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
                          }}
                        />
                      </Grid>
                      <Grid item xs={9} sm={7}>
                        <Typography variant="h6" fontWeight="bold" gutterBottom>
                          {item.product.name}
                        </Typography>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                          <Chip 
                            label={`Số lượng: ${item.quantity}`}
                            size="small"
                            color="primary"
                            variant="outlined"
                          />
                        </Box>
                        <Typography variant="body2" color="text.secondary">
                          Đơn giá: {formatPrice(item.price / item.quantity)}
                        </Typography>
                      </Grid>
                      <Grid item xs={12} sm={3} sx={{ textAlign: { xs: 'left', sm: 'right' } }}>
                        <Typography variant="h6" color="primary.main" fontWeight="bold">
                          {formatPrice(item.price)}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          Tổng: {formatPrice(item.price * item.quantity)}
                        </Typography>
                      </Grid>
                    </Grid>
                  </CardContent>
                </Card>
              ))}
            </Box>

            {/* Order Summary */}
            <Box sx={{ bgcolor: 'grey.50', p: 3, borderRadius: 2 }}>
              <Typography variant="h6" fontWeight="bold" gutterBottom>
                Tổng cộng
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Tạm tính:
                  </Typography>
                </Grid>
                <Grid item xs={6} sx={{ textAlign: 'right' }}>
                  <Typography variant="body2">
                    {formatPrice(getOrderPriceBreakdown(orderDetails).subtotal)}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Thuế:
                  </Typography>
                </Grid>
                <Grid item xs={6} sx={{ textAlign: 'right' }}>
                  <Typography variant="body2">
                    {formatPrice(getOrderPriceBreakdown(orderDetails).taxAmount)}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Phí vận chuyển:
                  </Typography>
                </Grid>
                <Grid item xs={6} sx={{ textAlign: 'right' }}>
                  <Typography variant="body2">
                    {formatPrice(getOrderPriceBreakdown(orderDetails).shippingAmount)}
                  </Typography>
                </Grid>
                
                {getOrderPriceBreakdown(orderDetails).discountAmount > 0 && (
                  <>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="success.main">
                        Giảm giá:
                      </Typography>
                    </Grid>
                    <Grid item xs={6} sx={{ textAlign: 'right' }}>
                      <Typography variant="body2" color="success.main">
                        -{formatPrice(getOrderPriceBreakdown(orderDetails).discountAmount)}
                      </Typography>
                    </Grid>
                  </>
                )}
                
                <Grid item xs={12}>
                  <Divider sx={{ my: 1 }} />
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="h6" fontWeight="bold">
                    Tổng cộng:
                  </Typography>
                </Grid>
                <Grid item xs={6} sx={{ textAlign: 'right' }}>
                  <Typography variant="h6" color="primary.main" fontWeight="bold">
                    {formatPrice(getOrderPriceBreakdown(orderDetails).totalAmount)}
                  </Typography>
                </Grid>
              </Grid>
            </Box>
          </Paper>
        </Grid>

        {/* Order Info & Next Steps */}
        <Grid item xs={12} md={4}>
          {/* Order Information */}
          <Paper elevation={2} sx={{ p: 4, borderRadius: 3, mb: 4 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
              <Assignment sx={{ fontSize: 28, color: 'primary.main', mr: 2 }} />
              <Typography variant="h6" fontWeight="bold">
                Thông tin đơn hàng
              </Typography>
            </Box>
            
            <Box sx={{ mb: 3 }}>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Mã đơn hàng
              </Typography>
              <Typography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
                {orderDetails.orderNumber}
              </Typography>
              
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Ngày đặt hàng
              </Typography>
              <Typography variant="body1" sx={{ mb: 2 }}>
                {formatDate(orderDetails.createdAt)}
              </Typography>
              
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Phương thức thanh toán
              </Typography>
              <Chip 
                label={orderDetails.paymentMethod} 
                color="primary" 
                variant="outlined"
                sx={{ mb: 2 }}
              />
              
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Địa chỉ giao hàng
              </Typography>
              <Typography variant="body2" sx={{ mb: 2 }}>
                {orderDetails.shippingAddress}
              </Typography>
            </Box>
          </Paper>

          {/* Next Steps */}
          <Paper elevation={2} sx={{ p: 4, borderRadius: 3 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
              <LocalShipping sx={{ fontSize: 28, color: 'primary.main', mr: 2 }} />
              <Typography variant="h6" fontWeight="bold">
                Bước tiếp theo
              </Typography>
            </Box>
            
            <Stepper orientation="vertical" sx={{ mt: 2 }}>
              <Step active={true} completed={false}>
                <StepLabel
                  sx={{
                    '& .MuiStepLabel-label': {
                      fontWeight: 'bold',
                      color: 'primary.main'
                    }
                  }}
                >
                  <Typography variant="body1" fontWeight="medium">
                    Chờ xác nhận
                  </Typography>
                </StepLabel>
                <StepContent>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    Chúng tôi sẽ liên hệ xác nhận đơn hàng trong vòng 24h
                  </Typography>
                  <Chip 
                    label="Đang xử lý" 
                    size="small" 
                    color="primary" 
                    variant="outlined"
                  />
                </StepContent>
              </Step>
              
              <Step active={false} completed={false}>
                <StepLabel>
                  <Typography variant="body1" fontWeight="medium">
                    Chuẩn bị hàng
                  </Typography>
                </StepLabel>
                <StepContent>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    Đơn hàng sẽ được đóng gói và chuẩn bị giao
                  </Typography>
                  <Chip 
                    label="Chờ xử lý" 
                    size="small" 
                    color="default" 
                    variant="outlined"
                  />
                </StepContent>
              </Step>
              
              <Step active={false} completed={false}>
                <StepLabel>
                  <Typography variant="body1" fontWeight="medium">
                    Giao hàng
                  </Typography>
                </StepLabel>
                <StepContent>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    Sản phẩm sẽ được giao đến địa chỉ bạn đã cung cấp
                  </Typography>
                  <Chip 
                    label="Chờ xử lý" 
                    size="small" 
                    color="default" 
                    variant="outlined"
                  />
                </StepContent>
              </Step>
            </Stepper>
          </Paper>
        </Grid>
      </Grid>

      {/* Action Buttons */}
      <Paper 
        elevation={2} 
        sx={{ 
          p: 4, 
          borderRadius: 3, 
          mt: 4,
          background: 'linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)',
          border: '1px solid rgba(0,0,0,0.05)'
        }}
      >
        <Typography variant="h6" textAlign="center" fontWeight="bold" sx={{ mb: 3 }}>
          Bạn muốn làm gì tiếp theo?
        </Typography>
        <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, gap: 2, justifyContent: 'center', flexWrap: 'wrap' }}>
          <Button
            variant="contained"
            size="large"
            startIcon={<Print />}
            onClick={handlePrintInvoice}
            sx={{
              borderRadius: 3,
              px: 4,
              py: 1.5,
              fontSize: '1.1rem',
              fontWeight: 'bold',
              background: 'linear-gradient(135deg, #4CAF50 0%, #45a049 100%)',
              boxShadow: '0 4px 12px rgba(76, 175, 80, 0.3)',
              transition: 'all 0.3s ease',
              '&:hover': {
                background: 'linear-gradient(135deg, #45a049 0%, #3d8b40 100%)',
                boxShadow: '0 6px 16px rgba(76, 175, 80, 0.4)',
                transform: 'translateY(-2px)',
              }
            }}
          >
            In hóa đơn
          </Button>
          
          <Button
            variant="contained"
            size="large"
            startIcon={<Receipt />}
            onClick={() => navigate('/orders')}
            sx={{
              borderRadius: 3,
              px: 4,
              py: 1.5,
              fontSize: '1.1rem',
              fontWeight: 'bold',
              boxShadow: '0 4px 12px rgba(25, 118, 210, 0.3)',
              transition: 'all 0.3s ease',
              '&:hover': {
                boxShadow: '0 6px 16px rgba(25, 118, 210, 0.4)',
                transform: 'translateY(-2px)',
              }
            }}
          >
            Xem đơn hàng
          </Button>
          
          <Button
            variant="outlined"
            size="large"
            startIcon={<ShoppingBag />}
            onClick={() => navigate('/products')}
            sx={{
              borderRadius: 3,
              px: 4,
              py: 1.5,
              fontSize: '1.1rem',
              fontWeight: 'bold',
              borderWidth: 2,
              transition: 'all 0.3s ease',
              '&:hover': {
                borderWidth: 2,
                transform: 'translateY(-2px)',
                boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
              }
            }}
          >
            Tiếp tục mua sắm
          </Button>
          
          <Button
            variant="outlined"
            size="large"
            startIcon={<Home />}
            onClick={() => navigate('/')}
            sx={{
              borderRadius: 3,
              px: 4,
              py: 1.5,
              fontSize: '1.1rem',
              fontWeight: 'bold',
              borderWidth: 2,
              transition: 'all 0.3s ease',
              '&:hover': {
                borderWidth: 2,
                transform: 'translateY(-2px)',
                boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
              }
            }}
          >
            Về trang chủ
          </Button>
        </Box>
        
        {/* Notification Preference Checkbox */}
        <Box sx={{ 
          display: 'flex', 
          justifyContent: 'center', 
          mt: 3,
          pt: 3,
          borderTop: '1px solid rgba(0,0,0,0.1)'
        }}>
          <FormControlLabel
            control={
              <Checkbox
                checked={disableOrderNotification}
                onChange={handleNotificationPreferenceChange}
                sx={{
                  color: 'primary.main',
                  '&.Mui-checked': {
                    color: 'primary.main',
                  },
                }}
              />
            }
            label={
              <Typography variant="body2" color="text.secondary">
                Không hiển thị thông báo đặt hàng thành công
              </Typography>
            }
            sx={{
              '& .MuiFormControlLabel-label': {
                fontSize: '0.9rem',
                color: 'text.secondary',
              }
            }}
          />
        </Box>
      </Paper>
    </Container>
    </PaymentResultErrorBoundary>
  );
};

export default PaymentResultPage; 