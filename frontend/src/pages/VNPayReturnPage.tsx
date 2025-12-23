import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Box, CircularProgress, Typography, Alert } from '@mui/material';

const VNPayReturnPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const handleVNPayReturn = async () => {
      try {
        // Parse VNPay return parameters
        const vnpParams: Record<string, string> = {};
        searchParams.forEach((value, key) => {
          vnpParams[key] = value;
        });

        console.log('VNPay return parameters:', vnpParams);

        // Check VNPay response code
        const responseCode = vnpParams.vnp_ResponseCode;
        const orderNumber = vnpParams.vnp_TxnRef;
        const transactionNo = vnpParams.vnp_TransactionNo;
        const amount = vnpParams.vnp_Amount;

        // VNPay response code '00' means success
        const isSuccess = responseCode === '00';

        if (isSuccess && orderNumber) {
          // Payment successful - redirect to success page
          const message = `Thanh toán thành công với mã giao dịch ${transactionNo}. Số tiền: ${parseInt(amount || '0') / 100} VND`;
          const redirectUrl = `/payment-result?orderNumber=${orderNumber}&success=true&message=${encodeURIComponent(message)}&paymentMethod=VNPAY`;
          navigate(redirectUrl);
        } else {
          // Payment failed or cancelled
          let errorMessage = 'Thanh toán không thành công';
          if (responseCode) {
            errorMessage += ` (Mã lỗi: ${responseCode})`;
          }
          if (vnpParams.vnp_OrderInfo) {
            errorMessage += `. ${vnpParams.vnp_OrderInfo}`;
          }

          const redirectUrl = `/payment-result?success=false&message=${encodeURIComponent(errorMessage)}&paymentMethod=VNPAY`;
          navigate(redirectUrl);
        }
      } catch (err: any) {
        console.error('VNPay return error:', err);
        setError(err.message || 'Có lỗi xảy ra khi xử lý thanh toán');

        // Redirect to PaymentResultPage with error
        setTimeout(() => {
          navigate('/payment-result?success=false&message=' + encodeURIComponent(err.message || 'Thanh toán thất bại'));
        }, 2000);
      } finally {
        setLoading(false);
      }
    };

    handleVNPayReturn();
  }, [searchParams, navigate]);

  if (loading) {
    return (
      <Box
        display="flex"
        flexDirection="column"
        alignItems="center"
        justifyContent="center"
        minHeight="100vh"
        gap={2}
      >
        <CircularProgress />
        <Typography>Đang xử lý thanh toán...</Typography>
      </Box>
    );
  }

  if (error) {
    return (
      <Box
        display="flex"
        flexDirection="column"
        alignItems="center"
        justifyContent="center"
        minHeight="100vh"
        gap={2}
      >
        <Alert severity="error">{error}</Alert>
        <Typography>Đang chuyển hướng...</Typography>
      </Box>
    );
  }

  return null;
};

export default VNPayReturnPage;
