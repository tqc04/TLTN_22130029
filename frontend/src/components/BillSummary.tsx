import React from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Divider,
  Chip,
  Grid,
} from '@mui/material';
import {
  Receipt,
  LocalOffer,
  LocalShipping,
  CreditCard,
} from '@mui/icons-material';

interface BillSummaryProps {
  subtotal?: number;
  discount?: number;
  shipping?: number;
  tax?: number;
  total?: number;
  voucherCode?: string;
  voucherDiscount?: number;
}

const BillSummary: React.FC<BillSummaryProps> = ({
  subtotal = 0,
  discount = 0,
  shipping = 0,
  tax = 0,
  total = 0,
  voucherCode,
  voucherDiscount = 0,
}) => {
  const formatCurrency = (amount: number) => {
    // Ensure amount is a valid number
    const safeAmount = Number(amount) || 0;
    return safeAmount.toLocaleString('vi-VN') + '₫';
  };

  return (
    <Card sx={{ borderRadius: 3, boxShadow: '0 4px 20px rgba(0,0,0,0.1)' }}>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
          <Receipt color="primary" />
          <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
            Chi tiết thanh toán
          </Typography>
        </Box>

        <Grid container spacing={2}>
          {/* Subtotal */}
          <Grid item xs={8}>
            <Typography variant="body1">Tổng tiền hàng:</Typography>
          </Grid>
          <Grid item xs={4} sx={{ textAlign: 'right' }}>
            <Typography variant="body1" sx={{ fontWeight: 'bold' }}>
              {formatCurrency(subtotal)}
            </Typography>
          </Grid>

          {/* Voucher Discount */}
          {voucherCode && voucherDiscount && voucherDiscount > 0 && (
            <>
              <Grid item xs={8}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <LocalOffer fontSize="small" color="success" />
                  <Typography variant="body2" color="success.main">
                    Mã giảm giá ({voucherCode}):
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={4} sx={{ textAlign: 'right' }}>
                <Typography variant="body2" color="success.main" sx={{ fontWeight: 'bold' }}>
                  -{formatCurrency(voucherDiscount)}
                </Typography>
              </Grid>
            </>
          )}

          {/* Other Discounts */}
          {discount > 0 && discount !== voucherDiscount && (
            <>
              <Grid item xs={8}>
                <Typography variant="body2" color="text.secondary">
                  Giảm giá khác:
                </Typography>
              </Grid>
              <Grid item xs={4} sx={{ textAlign: 'right' }}>
                <Typography variant="body2" color="text.secondary">
                  -{formatCurrency(discount - (voucherDiscount || 0))}
                </Typography>
              </Grid>
            </>
          )}

          {/* Shipping */}
          <Grid item xs={8}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <LocalShipping fontSize="small" color="info" />
              <Typography variant="body1">Phí vận chuyển:</Typography>
            </Box>
          </Grid>
          <Grid item xs={4} sx={{ textAlign: 'right' }}>
            <Typography variant="body1">
              {shipping === 0 ? (
                <Chip label="Miễn phí" size="small" color="success" />
              ) : (
                formatCurrency(shipping)
              )}
            </Typography>
          </Grid>

          {/* Tax */}
          <Grid item xs={8}>
            <Typography variant="body1">Thuế VAT (10%):</Typography>
          </Grid>
          <Grid item xs={4} sx={{ textAlign: 'right' }}>
            <Typography variant="body1">
              {formatCurrency(tax)}
            </Typography>
          </Grid>

          {/* Divider */}
          <Grid item xs={12}>
            <Divider sx={{ my: 1 }} />
          </Grid>

          {/* Total */}
          <Grid item xs={8}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <CreditCard fontSize="small" color="primary" />
              <Typography variant="h6" sx={{ fontWeight: 'bold', color: 'primary.main' }}>
                Tổng thanh toán:
              </Typography>
            </Box>
          </Grid>
          <Grid item xs={4} sx={{ textAlign: 'right' }}>
            <Typography variant="h6" sx={{ fontWeight: 'bold', color: 'primary.main' }}>
              {formatCurrency(total)}
            </Typography>
          </Grid>
        </Grid>

        {/* Payment Breakdown Summary */}
        <Box sx={{ mt: 3, p: 2, bgcolor: 'grey.50', borderRadius: 2 }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 'bold' }}>
            PHÂN TÍCH THANH TOÁN
          </Typography>
          <Box sx={{ mt: 1, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="body2">Giá ban đầu:</Typography>
            <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
              {formatCurrency(subtotal + tax + shipping)}
            </Typography>
          </Box>

          {voucherDiscount && voucherDiscount > 0 && (
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'success.main' }}>
              <Typography variant="body2">Giảm giá voucher:</Typography>
              <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                -{formatCurrency(voucherDiscount)}
              </Typography>
            </Box>
          )}

          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 1 }}>
            <Typography variant="body1" sx={{ fontWeight: 'bold' }}>Thành tiền:</Typography>
            <Typography variant="body1" sx={{ fontWeight: 'bold', color: 'primary.main' }}>
              {formatCurrency(total)}
            </Typography>
          </Box>
        </Box>

        {/* Terms and Conditions */}
        <Box sx={{ mt: 2, p: 2, bgcolor: 'info.light', borderRadius: 2, border: '1px solid', borderColor: 'info.main' }}>
          <Typography variant="caption" sx={{ fontWeight: 'bold', color: 'info.dark' }}>
            💡 Lưu ý:
          </Typography>
          <Typography variant="caption" color="info.dark" sx={{ display: 'block', mt: 0.5 }}>
            • Giá đã bao gồm VAT
            • Miễn phí vận chuyển cho đơn hàng trên 500.000₫
            • Áp dụng chính sách đổi trả trong 30 ngày
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};

export default BillSummary;
