import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Box,
  Grid,
  Card,
  CardContent,
  CardActions,
  Button,
  Chip,
  Tabs,
  Tab,
  Alert,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Divider,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  LocalOffer,
  CheckCircle,
  Cancel,
  Info,
  ShoppingCart,
  CalendarToday,
  Person,
  Close,
} from '@mui/icons-material';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/api';
import { useNotification } from '../contexts/NotificationContext';
import { useNavigate } from 'react-router-dom';

interface Voucher {
  id: number;
  code: string;
  name: string;
  description: string;
  type: 'PERCENTAGE' | 'FIXED_AMOUNT';
  value: number;
  minOrderAmount?: number;
  maxDiscountAmount?: number;
  startDate: string;
  endDate: string;
  usageLimit?: number;
  usageCount: number;
  usageLimitPerUser?: number;
  isActive: boolean;
  isPublic: boolean;
  freeShipping?: boolean;
}

interface UserVoucher {
  userVoucherId: number;
  voucherId: number;
  voucherCode: string;
  obtainedAt: string;
  isUsed: boolean;
  usedAt?: string;
  orderId?: number;
  orderNumber?: string;
  voucher?: Voucher;
}

const VouchersPage: React.FC = () => {
  const { user } = useAuth();
  const { notify } = useNotification();
  const navigate = useNavigate();
  const [tabValue, setTabValue] = useState(0);
  const [publicVouchers, setPublicVouchers] = useState<Voucher[]>([]);
  const [userAvailableVouchers, setUserAvailableVouchers] = useState<UserVoucher[]>([]);
  const [userAllVouchers, setUserAllVouchers] = useState<UserVoucher[]>([]);
  const [loading, setLoading] = useState(false);
  const [claimingVoucherId, setClaimingVoucherId] = useState<number | null>(null);
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);
  const [selectedVoucher, setSelectedVoucher] = useState<Voucher | null>(null);

  useEffect(() => {
    loadPublicVouchers();
    if (user?.id) {
      loadUserVouchers();
    }
  }, [user?.id]);

  const loadPublicVouchers = async () => {
    try {
      setLoading(true);
      const response = await apiService.getPublicVouchers();
      if (response.success && response.data) {
        // Chuẩn hóa field boolean từ backend (active/public -> isActive/isPublic)
        const rawVouchers = response.data as any[];
        const normalized: Voucher[] = rawVouchers.map((v) => ({
          ...v,
          isActive: v.isActive ?? v.active ?? false,
          isPublic: v.isPublic ?? v.public ?? false,
        }));
        setPublicVouchers(normalized);
      }
    } catch (error) {
      console.error('Error loading public vouchers:', error);
      notify('Không thể tải danh sách voucher', 'error');
    } finally {
      setLoading(false);
    }
  };

  const loadUserVouchers = async () => {
    if (!user?.id) return;
    
    try {
      // Load available vouchers
      const availableResponse = await apiService.getUserAvailableVouchers(user.id);
      if (availableResponse.success && availableResponse.data) {
        setUserAvailableVouchers(availableResponse.data as UserVoucher[]);
      }

      // Load all vouchers
      const allResponse = await apiService.getUserVouchers(user.id);
      if (allResponse.success && allResponse.data) {
        setUserAllVouchers(allResponse.data as UserVoucher[]);
      }
    } catch (error) {
      console.error('Error loading user vouchers:', error);
    }
  };

  const handleClaimVoucher = async (voucherId: number) => {
    if (!user?.id) {
      notify('Vui lòng đăng nhập để lấy voucher', 'warning');
      navigate('/login', { state: { returnTo: '/vouchers' } });
      return;
    }

    try {
      setClaimingVoucherId(voucherId);
      const response = await apiService.claimVoucher(user.id, voucherId);
      
      if (response.success) {
        notify('Đã lấy voucher thành công!', 'success');
        // Reload vouchers
        loadPublicVouchers();
        loadUserVouchers();
      } else {
        notify(response.message || 'Không thể lấy voucher', 'error');
      }
    } catch (error: any) {
      console.error('Error claiming voucher:', error);
      notify(error.message || 'Không thể lấy voucher', 'error');
    } finally {
      setClaimingVoucherId(null);
    }
  };

  const handleViewDetails = (voucher: Voucher) => {
    setSelectedVoucher(voucher);
    setDetailDialogOpen(true);
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const isVoucherClaimed = (voucherId: number): boolean => {
    return userAllVouchers.some(uv => uv.voucherId === voucherId);
  };

  const canClaimVoucher = (voucher: Voucher): boolean => {
    if (!user?.id) return false;
    if (isVoucherClaimed(voucher.id)) return false;
    if (!voucher.isActive || !voucher.isPublic) return false;
    
    const now = new Date();
    const startDate = new Date(voucher.startDate);
    const endDate = new Date(voucher.endDate);
    
    return now >= startDate && now <= endDate;
  };

  const VoucherCard: React.FC<{ voucher: Voucher; showClaimButton?: boolean }> = ({ 
    voucher, 
    showClaimButton = false 
  }) => {
    const claimed = isVoucherClaimed(voucher.id);
    const canClaim = canClaimVoucher(voucher);

    return (
      <Card
        sx={{
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          borderRadius: 3,
          border: '2px solid',
          borderColor: claimed ? 'success.main' : 'divider',
          position: 'relative',
          transition: 'all 0.3s ease',
          '&:hover': {
            transform: 'translateY(-4px)',
            boxShadow: 6,
          },
        }}
      >
        {/* Voucher Header */}
        <Box
          sx={{
            background: claimed
              ? 'linear-gradient(135deg, #10b981 0%, #059669 100%)'
              : 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)',
            color: 'white',
            p: 2,
            borderRadius: '12px 12px 0 0',
          }}
        >
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 0.5 }}>
                {voucher.code}
              </Typography>
              <Typography variant="body2" sx={{ opacity: 0.9 }}>
                {voucher.name}
              </Typography>
            </Box>
            {claimed && (
              <Chip
                icon={<CheckCircle />}
                label="Đã lấy"
                size="small"
                sx={{
                  bgcolor: 'rgba(255,255,255,0.2)',
                  color: 'white',
                  fontWeight: 'bold',
                }}
              />
            )}
          </Box>

          {/* Discount Value */}
          <Box sx={{ mt: 2, display: 'flex', alignItems: 'baseline', gap: 1 }}>
            <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
              {voucher.type === 'PERCENTAGE' ? `${voucher.value}%` : formatCurrency(voucher.value)}
            </Typography>
            {voucher.type === 'PERCENTAGE' && voucher.maxDiscountAmount && (
              <Typography variant="body2" sx={{ opacity: 0.9 }}>
                (Tối đa {formatCurrency(voucher.maxDiscountAmount)})
              </Typography>
            )}
          </Box>
        </Box>

        <CardContent sx={{ flexGrow: 1, p: 2 }}>
          {/* Description */}
          {voucher.description && (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2, minHeight: 40 }}>
              {voucher.description}
            </Typography>
          )}

          {/* Conditions */}
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {voucher.minOrderAmount && voucher.minOrderAmount > 0 && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <ShoppingCart fontSize="small" color="action" />
                <Typography variant="caption" color="text.secondary">
                  Đơn tối thiểu: {formatCurrency(voucher.minOrderAmount)}
                </Typography>
              </Box>
            )}

            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <CalendarToday fontSize="small" color="action" />
              <Typography variant="caption" color="text.secondary">
                HSD: {formatDate(voucher.endDate)}
              </Typography>
            </Box>

            {voucher.usageLimitPerUser && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Person fontSize="small" color="action" />
                <Typography variant="caption" color="text.secondary">
                  {voucher.usageLimitPerUser} lần/người
                </Typography>
              </Box>
            )}

            {voucher.freeShipping && (
              <Chip
                label="Miễn phí vận chuyển"
                size="small"
                color="success"
                sx={{ width: 'fit-content', mt: 0.5 }}
              />
            )}
          </Box>
        </CardContent>

        <CardActions sx={{ p: 2, pt: 0, gap: 1 }}>
          <Button
            size="small"
            startIcon={<Info />}
            onClick={() => handleViewDetails(voucher)}
          >
            Chi tiết
          </Button>
          {showClaimButton && (
            <Button
              variant="contained"
              size="small"
              fullWidth
              disabled={claimed || (user?.id && !canClaim) || claimingVoucherId === voucher.id}
              onClick={() => handleClaimVoucher(voucher.id)}
              sx={{ ml: 'auto' }}
            >
              {claimingVoucherId === voucher.id ? (
                <CircularProgress size={16} />
              ) : claimed ? (
                'Đã lấy'
              ) : (
                'Lấy ngay'
              )}
            </Button>
          )}
        </CardActions>
      </Card>
    );
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 1 }}>
          <LocalOffer sx={{ mr: 1, verticalAlign: 'middle' }} />
          Voucher
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Lấy voucher và sử dụng khi thanh toán để được giảm giá
        </Typography>
      </Box>

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={tabValue} onChange={(_, newValue) => setTabValue(newValue)}>
          <Tab label={`Voucher có sẵn (${publicVouchers.length})`} />
          <Tab label={`Voucher của tôi - Chưa dùng (${userAvailableVouchers.length})`} />
          <Tab label={`Tất cả voucher (${userAllVouchers.length})`} />
        </Tabs>
      </Box>

      {/* Tab Content */}
      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : (
        <>
          {/* Tab 0: Public Vouchers */}
          {tabValue === 0 && (
            <>
              {!user?.id && (
                <Alert severity="info" sx={{ mb: 3, borderRadius: 2 }}>
                  Vui lòng đăng nhập để lấy voucher
                </Alert>
              )}
              {publicVouchers.length === 0 ? (
                <Alert severity="info" sx={{ borderRadius: 2 }}>
                  Hiện chưa có voucher nào khả dụng
                </Alert>
              ) : (
                <Grid container spacing={3}>
                  {publicVouchers.map((voucher) => (
                    <Grid item xs={12} sm={6} md={4} key={voucher.id}>
                      <VoucherCard voucher={voucher} showClaimButton={true} />
                    </Grid>
                  ))}
                </Grid>
              )}
            </>
          )}

          {/* Tab 1: Available Vouchers (Not Used) */}
          {tabValue === 1 && (
            <>
              {!user?.id ? (
                <Alert severity="warning" sx={{ borderRadius: 2 }}>
                  Vui lòng đăng nhập để xem voucher của bạn
                </Alert>
              ) : userAvailableVouchers.length === 0 ? (
                <Alert severity="info" sx={{ borderRadius: 2 }}>
                  Bạn chưa có voucher nào chưa sử dụng
                </Alert>
              ) : (
                <Grid container spacing={3}>
                  {userAvailableVouchers.map((userVoucher) => (
                    <Grid item xs={12} sm={6} md={4} key={userVoucher.userVoucherId}>
                      {userVoucher.voucher && (
                        <VoucherCard voucher={userVoucher.voucher} showClaimButton={false} />
                      )}
                    </Grid>
                  ))}
                </Grid>
              )}
            </>
          )}

          {/* Tab 2: All User Vouchers */}
          {tabValue === 2 && (
            <>
              {!user?.id ? (
                <Alert severity="warning" sx={{ borderRadius: 2 }}>
                  Vui lòng đăng nhập để xem voucher của bạn
                </Alert>
              ) : userAllVouchers.length === 0 ? (
                <Alert severity="info" sx={{ borderRadius: 2 }}>
                  Bạn chưa có voucher nào
                </Alert>
              ) : (
                <Grid container spacing={3}>
                  {userAllVouchers.map((userVoucher) => (
                    <Grid item xs={12} sm={6} md={4} key={userVoucher.userVoucherId}>
                      {userVoucher.voucher && (
                        <Card
                          sx={{
                            height: '100%',
                            borderRadius: 3,
                            border: '2px solid',
                            borderColor: userVoucher.isUsed ? 'grey.300' : 'success.main',
                            opacity: userVoucher.isUsed ? 0.7 : 1,
                          }}
                        >
                          <Box
                            sx={{
                              background: userVoucher.isUsed
                                ? 'linear-gradient(135deg, #9ca3af 0%, #6b7280 100%)'
                                : 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
                              color: 'white',
                              p: 2,
                              borderRadius: '12px 12px 0 0',
                            }}
                          >
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                              <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                                {userVoucher.voucher.code}
                              </Typography>
                              <Chip
                                icon={userVoucher.isUsed ? <Cancel /> : <CheckCircle />}
                                label={userVoucher.isUsed ? 'Đã dùng' : 'Chưa dùng'}
                                size="small"
                                sx={{
                                  bgcolor: 'rgba(255,255,255,0.2)',
                                  color: 'white',
                                }}
                              />
                            </Box>
                            <Typography variant="h5" sx={{ fontWeight: 'bold', mt: 1 }}>
                              {userVoucher.voucher.type === 'PERCENTAGE'
                                ? `${userVoucher.voucher.value}%`
                                : formatCurrency(userVoucher.voucher.value)}
                            </Typography>
                          </Box>
                          <CardContent>
                            <Typography variant="body2" color="text.secondary">
                              Lấy ngày: {formatDate(userVoucher.obtainedAt)}
                            </Typography>
                            {userVoucher.isUsed && userVoucher.usedAt && (
                              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                                Dùng ngày: {formatDate(userVoucher.usedAt)}
                              </Typography>
                            )}
                            {userVoucher.isUsed && userVoucher.orderNumber && (
                              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                                Đơn hàng: {userVoucher.orderNumber}
                              </Typography>
                            )}
                          </CardContent>
                        </Card>
                      )}
                    </Grid>
                  ))}
                </Grid>
              )}
            </>
          )}
        </>
      )}

      {/* Voucher Detail Dialog */}
      <Dialog
        open={detailDialogOpen}
        onClose={() => setDetailDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
              Chi tiết voucher
            </Typography>
            <IconButton onClick={() => setDetailDialogOpen(false)} size="small">
              <Close />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          {selectedVoucher && (
            <>
              <Box
                sx={{
                  background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)',
                  color: 'white',
                  p: 3,
                  borderRadius: 2,
                  mb: 3,
                  textAlign: 'center',
                }}
              >
                <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 1 }}>
                  {selectedVoucher.code}
                </Typography>
                <Typography variant="h6" sx={{ mb: 2 }}>
                  {selectedVoucher.name}
                </Typography>
                <Typography variant="h3" sx={{ fontWeight: 'bold' }}>
                  {selectedVoucher.type === 'PERCENTAGE'
                    ? `${selectedVoucher.value}%`
                    : formatCurrency(selectedVoucher.value)}
                </Typography>
                {selectedVoucher.type === 'PERCENTAGE' && selectedVoucher.maxDiscountAmount && (
                  <Typography variant="body2" sx={{ mt: 1, opacity: 0.9 }}>
                    Tối đa {formatCurrency(selectedVoucher.maxDiscountAmount)}
                  </Typography>
                )}
              </Box>

              <Divider sx={{ my: 2 }} />

              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                {selectedVoucher.description && (
                  <Box>
                    <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>
                      Mô tả
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {selectedVoucher.description}
                    </Typography>
                  </Box>
                )}

                <Box>
                  <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>
                    Điều kiện sử dụng
                  </Typography>
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                    {selectedVoucher.minOrderAmount && selectedVoucher.minOrderAmount > 0 && (
                      <Typography variant="body2" color="text.secondary">
                        • Đơn hàng tối thiểu: {formatCurrency(selectedVoucher.minOrderAmount)}
                      </Typography>
                    )}
                    <Typography variant="body2" color="text.secondary">
                      • Hiệu lực từ: {formatDate(selectedVoucher.startDate)} đến{' '}
                      {formatDate(selectedVoucher.endDate)}
                    </Typography>
                    {selectedVoucher.usageLimitPerUser && (
                      <Typography variant="body2" color="text.secondary">
                        • Giới hạn: {selectedVoucher.usageLimitPerUser} lần/người
                      </Typography>
                    )}
                    {selectedVoucher.usageLimit && (
                      <Typography variant="body2" color="text.secondary">
                        • Tổng số lượng: {selectedVoucher.usageLimit} voucher
                      </Typography>
                    )}
                    {selectedVoucher.freeShipping && (
                      <Typography variant="body2" color="success.main" sx={{ fontWeight: 'bold' }}>
                        • Miễn phí vận chuyển
                      </Typography>
                    )}
                  </Box>
                </Box>

                <Box>
                  <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>
                    Thống kê
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Đã sử dụng: {selectedVoucher.usageCount} /{' '}
                    {selectedVoucher.usageLimit || 'Không giới hạn'}
                  </Typography>
                </Box>
              </Box>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailDialogOpen(false)}>Đóng</Button>
          {selectedVoucher && user?.id && canClaimVoucher(selectedVoucher) && !isVoucherClaimed(selectedVoucher.id) && (
            <Button
              variant="contained"
              onClick={() => {
                handleClaimVoucher(selectedVoucher.id);
                setDetailDialogOpen(false);
              }}
              disabled={claimingVoucherId === selectedVoucher.id}
            >
              {claimingVoucherId === selectedVoucher.id ? 'Đang lấy...' : 'Lấy voucher'}
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default VouchersPage;

