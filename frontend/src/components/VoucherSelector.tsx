import React, { useState, useEffect } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
  Chip,
  CircularProgress,
  Alert,
  IconButton,
  Divider
} from '@mui/material'
import {
  Close as CloseIcon,
  LocalOffer as OfferIcon,
  CheckCircle as CheckCircleIcon
} from '@mui/icons-material'
import { apiService } from '../services/api'
import { formatPrice } from '../utils/priceUtils'
import { useAuth } from '../contexts/AuthContext'

interface Voucher {
  id: number
  code: string
  name: string
  description?: string
  type: 'PERCENTAGE' | 'FIXED'
  value: number
  maxDiscountAmount?: number
  minOrderAmount?: number
  startDate: string
  endDate: string
  isActive: boolean
  isPublic: boolean
  freeShipping?: boolean
  usageLimitPerUser?: number
}

interface VoucherSelectorProps {
  open: boolean
  onClose: () => void
  onSelectVoucher: (voucherCode: string) => void
  cartTotal: number
  appliedVoucherCode?: string | null
}

const VoucherSelector: React.FC<VoucherSelectorProps> = ({
  open,
  onClose,
  onSelectVoucher,
  cartTotal,
  appliedVoucherCode
}) => {
  const { user } = useAuth()
  const [vouchers, setVouchers] = useState<Voucher[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open) {
      loadVouchers()
    }
  }, [open, user?.id])

  const loadVouchers = async () => {
    if (!user?.id) {
      setError('Vui lòng đăng nhập để xem voucher')
      return
    }
    
    setLoading(true)
    setError(null)
    try {
      const userVouchersResponse = await apiService.getUserAvailableVouchers(user.id)
      if (userVouchersResponse.success && userVouchersResponse.data) {
        const userVouchers = userVouchersResponse.data as any[]
        // Extract voucher info from user vouchers
        const validVouchers: Voucher[] = userVouchers
          .filter(uv => uv.voucher && !uv.isUsed)
          .map(uv => {
            const v = uv.voucher
            return {
              id: v.id,
              code: uv.voucherCode || v.code,
              name: v.name,
              description: v.description,
              type: v.type === 'FIXED_AMOUNT' ? 'FIXED' : v.type,
              value: v.value,
              maxDiscountAmount: v.maxDiscountAmount,
              minOrderAmount: v.minOrderAmount,
              startDate: v.startDate,
              endDate: v.endDate,
              isActive: v.isActive ?? v.active ?? true,
              isPublic: v.isPublic ?? v.public ?? true,
              freeShipping: v.freeShipping,
              usageLimitPerUser: v.usageLimitPerUser,
            } as Voucher
          })
          .filter(v => {
            // Filter out expired vouchers
            const now = new Date()
            const startDate = new Date(v.startDate)
            const endDate = new Date(v.endDate)
            return now >= startDate && now <= endDate
          })
        setVouchers(validVouchers)
      } else {
        setError('Không thể tải danh sách voucher')
      }
    } catch (err: any) {
      console.error('Error loading vouchers:', err)
      setError(err.message || 'Không thể tải danh sách voucher')
    } finally {
      setLoading(false)
    }
  }

  const calculateDiscount = (voucher: Voucher): number => {
    if (voucher.type === 'PERCENTAGE') {
      const discount = (cartTotal * voucher.value) / 100
      return voucher.maxDiscountAmount 
        ? Math.min(discount, voucher.maxDiscountAmount)
        : discount
    } else {
      return Math.min(voucher.value, cartTotal)
    }
  }

  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    })
  }

  const handleSelect = (voucherCode: string) => {
    onSelectVoucher(voucherCode)
    onClose()
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      PaperProps={{
        sx: {
          borderRadius: 3,
          maxHeight: '90vh'
        }
      }}
    >
      <DialogTitle sx={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center',
        pb: 1
      }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <OfferIcon color="primary" />
          <Typography variant="h6" fontWeight="bold">
            Chọn mã giảm giá
          </Typography>
        </Box>
        <IconButton onClick={onClose} size="small">
          <CloseIcon />
        </IconButton>
      </DialogTitle>

      <Divider />

      <DialogContent sx={{ pt: 2 }}>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        ) : error ? (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        ) : vouchers.length === 0 ? (
          <Alert severity="info">
            Hiện chưa có voucher nào khả dụng cho đơn hàng của bạn
          </Alert>
        ) : (
          <Grid container spacing={2}>
            {vouchers.map((voucher) => {
              const discount = calculateDiscount(voucher)
              const isApplied = appliedVoucherCode === voucher.code
              const canApply = cartTotal >= (voucher.minOrderAmount || 0)

              return (
                <Grid item xs={12} key={voucher.id}>
                  <Card
                    sx={{
                      border: isApplied 
                        ? '2px solid #4caf50' 
                        : '1px solid #e0e0e0',
                      borderRadius: 2,
                      cursor: canApply ? 'pointer' : 'not-allowed',
                      opacity: canApply ? 1 : 0.6,
                      transition: 'all 0.2s',
                      '&:hover': canApply ? {
                        boxShadow: 4,
                        transform: 'translateY(-2px)'
                      } : {},
                      background: isApplied 
                        ? 'linear-gradient(135deg, #e8f5e9 0%, #ffffff 100%)'
                        : 'white'
                    }}
                    onClick={() => canApply && handleSelect(voucher.code)}
                  >
                    <CardContent>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <Box sx={{ flex: 1 }}>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                            <Typography variant="h6" fontWeight="bold" color="primary">
                              {voucher.code}
                            </Typography>
                            {isApplied && (
                              <Chip
                                icon={<CheckCircleIcon />}
                                label="Đã áp dụng"
                                color="success"
                                size="small"
                              />
                            )}
                            {voucher.freeShipping && (
                              <Chip
                                label="Miễn phí ship"
                                color="secondary"
                                size="small"
                              />
                            )}
                          </Box>

                          <Typography variant="body1" fontWeight="bold" sx={{ mb: 0.5 }}>
                            {voucher.name}
                          </Typography>

                          {voucher.description && (
                            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                              {voucher.description}
                            </Typography>
                          )}

                          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mt: 1 }}>
                            {voucher.minOrderAmount && voucher.minOrderAmount > 0 && (
                              <Typography 
                                variant="caption" 
                                color={canApply ? "text.secondary" : "error.main"}
                                fontWeight={!canApply ? "bold" : "normal"}
                              >
                                Đơn tối thiểu: {formatPrice(voucher.minOrderAmount)}
                                {!canApply && ` (Cần thêm ${formatPrice(voucher.minOrderAmount - cartTotal)})`}
                              </Typography>
                            )}
                            <Typography variant="caption" color="text.secondary">
                              HSD: {formatDate(voucher.endDate)}
                            </Typography>
                          </Box>
                          {!canApply && voucher.minOrderAmount && voucher.minOrderAmount > 0 && (
                            <Alert severity="warning" sx={{ mt: 1, fontSize: '0.75rem', py: 0.5 }}>
                              Đơn hàng của bạn chưa đủ điều kiện để sử dụng voucher này
                            </Alert>
                          )}
                        </Box>

                        <Box sx={{ 
                          textAlign: 'right', 
                          ml: 2,
                          minWidth: 120
                        }}>
                          <Typography 
                            variant="h5" 
                            fontWeight="bold" 
                            color="primary"
                            sx={{ mb: 0.5 }}
                          >
                            {voucher.type === 'PERCENTAGE' 
                              ? `${voucher.value}%`
                              : formatPrice(voucher.value)
                            }
                          </Typography>
                          {voucher.type === 'PERCENTAGE' && voucher.maxDiscountAmount && (
                            <Typography variant="caption" color="text.secondary">
                              Tối đa {formatPrice(voucher.maxDiscountAmount)}
                            </Typography>
                          )}
                          <Typography variant="body2" color="success.main" sx={{ mt: 1 }}>
                            Giảm {formatPrice(discount)}
                          </Typography>
                        </Box>
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              )
            })}
          </Grid>
        )}
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} variant="outlined">
          Đóng
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default VoucherSelector

