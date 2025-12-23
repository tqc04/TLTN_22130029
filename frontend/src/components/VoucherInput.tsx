import React, { useState, useEffect } from 'react';
import {
  Box,
  TextField,
  Button,
  Typography,
  Alert,
  Chip,
  Collapse,
  IconButton,
  Card,
  CardContent,
  Link,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Divider,
} from '@mui/material';
import {
  ExpandMore,
  ExpandLess,
  LocalOffer,
  CheckCircle,
  Close,
  ArrowForward,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { apiService } from '../services/api';
import { CartItem } from '../contexts/CartContext';

interface VoucherInputProps {
  userId: number;
  cartItems: CartItem[];
  cartTotal: number;
  onVoucherApplied: (voucher: any) => void;
  onVoucherRemoved: () => void;
  appliedVoucher?: any;
}

const VoucherInput: React.FC<VoucherInputProps> = ({
  userId,
  cartItems,
  cartTotal,
  onVoucherApplied,
  onVoucherRemoved,
  appliedVoucher,
}) => {
  const navigate = useNavigate();
  const [voucherCode, setVoucherCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [expanded, setExpanded] = useState(false);
  const [availableVouchers, setAvailableVouchers] = useState<any[]>([]);
  const [userAvailableVouchers, setUserAvailableVouchers] = useState<any[]>([]);
  const [loadingUserVouchers, setLoadingUserVouchers] = useState(false);

  const handleApplyVoucher = async () => {
    if (!voucherCode.trim()) {
      setError('Vui lòng nhập mã voucher');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      console.log('Applying voucher:', voucherCode, 'for user:', userId);

      // Prepare validation request
      const validationRequest = {
        voucherCode: voucherCode.trim(),
        userId: String(userId), // Convert to string as backend expects string
        orderAmount: cartTotal,
        items: cartItems.map(item => ({
          productId: item.productId,
          productName: item.productName,
          categoryId: 1, // Default category ID - in real implementation, get from product service
          brandId: 1,    // Default brand ID - in real implementation, get from product service
          price: item.productPrice,
          quantity: item.quantity,
        })),
      };

      console.log('Validation request:', validationRequest);

      const response = await apiService.validateVoucher(validationRequest);
      console.log('Validation response:', response);

      if (response.success && (response.data as any)?.valid) {
        const data = response.data as any;
        const discountAmount = data.discountAmount;
        setSuccess(`Áp dụng thành công! Giảm ${discountAmount?.toLocaleString('vi-VN')}₫`);

        onVoucherApplied({
          code: data.voucherCode,
          id: data.voucherId,
          discountAmount: discountAmount,
          message: data.message,
        });
        setVoucherCode('');
      } else {
        const errorMsg = (response.data as any)?.message || response.message || 'Mã voucher không hợp lệ';
        console.error('Voucher validation failed:', errorMsg);
        setError(errorMsg);
      }
    } catch (error: any) {
      console.error('Error applying voucher:', error);
      const errorMsg = error.message || error.response?.data?.message || 'Không thể áp dụng mã voucher';
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveVoucher = () => {
    setError('');
    setSuccess('');
    onVoucherRemoved();
  };

  const handleKeyPress = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter' && !loading) {
      handleApplyVoucher();
    }
  };

  const loadAvailableVouchers = async () => {
    try {
      console.log('Loading available vouchers...');
      const response = await apiService.getPublicVouchers();
      console.log('Available vouchers response:', response);

      if (response.success && response.data) {
        setAvailableVouchers(response.data);
        console.log('Loaded vouchers:', response.data.length);
      } else {
        console.error('Failed to load vouchers:', response.message);
      }
    } catch (error) {
      console.error('Error loading available vouchers:', error);
    }
  };

  // Load user's available vouchers
  useEffect(() => {
    if (userId) {
      loadUserAvailableVouchers();
    }
  }, [userId]);

  const loadUserAvailableVouchers = async () => {
    if (!userId) return;
    
    try {
      setLoadingUserVouchers(true);
      const response = await apiService.getUserAvailableVouchers(String(userId));
      if (response.success && response.data) {
        setUserAvailableVouchers(response.data as any[]);
      }
    } catch (error) {
      console.error('Error loading user vouchers:', error);
    } finally {
      setLoadingUserVouchers(false);
    }
  };

  const handleSelectUserVoucher = async (voucherCode: string) => {
    setVoucherCode(voucherCode);
    // Auto apply when selecting from user vouchers
    const validationRequest = {
      voucherCode: voucherCode.trim(),
      userId: String(userId),
      orderAmount: cartTotal,
      items: cartItems.map(item => ({
        productId: item.productId,
        productName: item.productName,
        categoryId: 1,
        brandId: 1,
        price: item.productPrice,
        quantity: item.quantity,
      })),
    };

    try {
      setLoading(true);
      const response = await apiService.validateVoucher(validationRequest);
      if (response.success && (response.data as any)?.valid) {
        const data = response.data as any;
        onVoucherApplied({
          code: data.voucherCode,
          id: data.voucherId,
          discountAmount: data.discountAmount,
          message: data.message,
        });
        setSuccess(`Áp dụng thành công! Giảm ${data.discountAmount?.toLocaleString('vi-VN')}₫`);
        setVoucherCode('');
      } else {
        const errorMsg = (response.data as any)?.message || 'Mã voucher không hợp lệ';
        setError(errorMsg);
      }
    } catch (error: any) {
      setError(error.message || 'Không thể áp dụng mã voucher');
    } finally {
      setLoading(false);
    }
  };

  const handleExpandClick = () => {
    setExpanded(!expanded);
    if (!expanded) {
      if (availableVouchers.length === 0) {
        loadAvailableVouchers();
      }
      if (userAvailableVouchers.length === 0 && userId) {
        loadUserAvailableVouchers();
      }
    }
  };

  return (
    <Card sx={{ borderRadius: 3, mb: 2 }}>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
          <LocalOffer color="primary" />
          <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
            Voucher
          </Typography>
          <IconButton onClick={handleExpandClick} size="small">
            {expanded ? <ExpandLess /> : <ExpandMore />}
          </IconButton>
        </Box>

        {/* Applied Voucher Display */}
        {appliedVoucher && (
          <Box sx={{ mb: 2, p: 2, bgcolor: 'success.light', borderRadius: 2, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <CheckCircle color="success" />
              <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                {appliedVoucher.code} - Giảm {appliedVoucher.discountAmount?.toLocaleString('vi-VN')}₫
              </Typography>
            </Box>
            <IconButton onClick={handleRemoveVoucher} size="small">
              <Close />
            </IconButton>
          </Box>
        )}

        {/* Voucher Input */}
        <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
          <TextField
            fullWidth
            size="small"
            placeholder="Nhập mã giảm giá"
            value={voucherCode}
            onChange={(e) => setVoucherCode(e.target.value.toUpperCase())}
            onKeyPress={handleKeyPress}
            disabled={loading || !!appliedVoucher}
            error={!!error}
            sx={{
              '& .MuiOutlinedInput-root': {
                borderRadius: 2,
              },
            }}
          />
          <Button
            variant="contained"
            onClick={handleApplyVoucher}
            disabled={loading || !voucherCode.trim() || !!appliedVoucher}
            sx={{ borderRadius: 2, minWidth: 100 }}
          >
            {loading ? '...' : 'Áp dụng'}
          </Button>
        </Box>

        {/* Error/Success Messages */}
        {error && (
          <Alert severity="error" sx={{ mb: 1, borderRadius: 2 }}>
            <Typography variant="body2">{error}</Typography>
          </Alert>
        )}

        {success && (
          <Alert severity="success" sx={{ mb: 1, borderRadius: 2 }}>
            <Typography variant="body2">{success}</Typography>
          </Alert>
        )}

        {/* Available Vouchers */}
        <Collapse in={expanded} timeout="auto" unmountOnExit>
          <Box sx={{ mt: 2, pt: 2, borderTop: '1px solid', borderColor: 'divider' }}>
            {/* User's Available Vouchers (Like Shopee) */}
            {userId && (
              <>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>
                    Voucher của tôi ({userAvailableVouchers.length})
                  </Typography>
                  <Link
                    component="button"
                    variant="body2"
                    onClick={() => navigate('/vouchers')}
                    sx={{ display: 'flex', alignItems: 'center', gap: 0.5, textDecoration: 'none' }}
                  >
                    Xem tất cả <ArrowForward fontSize="small" />
                  </Link>
                </Box>

                {loadingUserVouchers ? (
                  <Typography variant="body2" color="text.secondary">
                    Đang tải...
                  </Typography>
                ) : userAvailableVouchers.length === 0 ? (
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                      Bạn chưa có voucher nào. Hãy lấy voucher ngay!
                    </Typography>
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => navigate('/vouchers')}
                      startIcon={<LocalOffer />}
                    >
                      Lấy voucher
                    </Button>
                  </Box>
                ) : (
                  <List dense sx={{ mb: 2, bgcolor: 'grey.50', borderRadius: 2, p: 1 }}>
                    {userAvailableVouchers.slice(0, 3).map((userVoucher: any) => {
                      const voucher = userVoucher.voucher;
                      if (!voucher) return null;
                      
                      return (
                        <ListItem key={userVoucher.userVoucherId} disablePadding>
                          <ListItemButton
                            onClick={() => handleSelectUserVoucher(voucher.code)}
                            disabled={!!appliedVoucher}
                            sx={{
                              borderRadius: 1,
                              mb: 0.5,
                              '&:hover': { bgcolor: 'primary.light', color: 'white' },
                            }}
                          >
                            <ListItemText
                              primary={
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                  <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                                    {voucher.code}
                                  </Typography>
                                  <Chip
                                    label={
                                      voucher.type === 'PERCENTAGE'
                                        ? `${voucher.value}%`
                                        : `${voucher.value?.toLocaleString('vi-VN')}₫`
                                    }
                                    size="small"
                                    color="primary"
                                    sx={{ height: 20, fontSize: '0.7rem' }}
                                  />
                                </Box>
                              }
                              secondary={
                                <Typography variant="caption" color="text.secondary">
                                  {voucher.name}
                                  {voucher.minOrderAmount && voucher.minOrderAmount > 0 && (
                                    <> • Đơn tối thiểu: {voucher.minOrderAmount.toLocaleString('vi-VN')}₫</>
                                  )}
                                </Typography>
                              }
                            />
                          </ListItemButton>
                        </ListItem>
                      );
                    })}
                    {userAvailableVouchers.length > 3 && (
                      <Box sx={{ textAlign: 'center', mt: 1 }}>
                        <Button
                          size="small"
                          onClick={() => navigate('/vouchers')}
                          endIcon={<ArrowForward />}
                        >
                          Xem thêm {userAvailableVouchers.length - 3} voucher
                        </Button>
                      </Box>
                    )}
                  </List>
                )}
                <Divider sx={{ my: 2 }} />
              </>
            )}

            {/* Public Vouchers (for claiming) */}
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>
                Mã giảm giá có sẵn:
              </Typography>
              <Link
                component="button"
                variant="body2"
                onClick={() => navigate('/vouchers')}
                sx={{ display: 'flex', alignItems: 'center', gap: 0.5, textDecoration: 'none' }}
              >
                Lấy thêm <ArrowForward fontSize="small" />
              </Link>
            </Box>

            {availableVouchers.length === 0 ? (
              <Typography variant="body2" color="text.secondary">
                Không có mã giảm giá nào khả dụng
              </Typography>
            ) : (
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {availableVouchers.slice(0, 5).map((voucher: any) => (
                  <Chip
                    key={voucher.id}
                    label={`${voucher.code} - ${voucher.type === 'PERCENTAGE' ? `${voucher.value}%` : `${voucher.value?.toLocaleString('vi-VN')}₫`}`}
                    variant="outlined"
                    color="primary"
                    onClick={() => setVoucherCode(voucher.code)}
                    sx={{ cursor: 'pointer' }}
                  />
                ))}
                {availableVouchers.length > 5 && (
                  <Button
                    size="small"
                    onClick={() => navigate('/vouchers')}
                    endIcon={<ArrowForward />}
                  >
                    Xem thêm
                  </Button>
                )}
              </Box>
            )}
          </Box>
        </Collapse>

        {/* Voucher Terms */}
        <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
          * Mã giảm giá có thể có điều kiện áp dụng. Vui lòng kiểm tra chi tiết trước khi sử dụng.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default VoucherInput;
