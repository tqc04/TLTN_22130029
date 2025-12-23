import React from 'react';
import {
  Box,
  RadioGroup,
  FormControlLabel,
  Radio,
  Typography,
  Card,
  CardContent,
  Alert,
} from '@mui/material';
import {
  CreditCard,
  AccountBalance,
  Money,
  Payment,
} from '@mui/icons-material';

interface PaymentMethodSelectorProps {
  selectedMethod: string;
  creditCardGateway: string;
  selectedBank: string;
  onMethodChange: (method: string) => void;
  onGatewayChange: (gateway: string) => void;
  onBankChange: (bank: string) => void;
}

const PaymentMethodSelector: React.FC<PaymentMethodSelectorProps> = ({
  selectedMethod,
  creditCardGateway,
  selectedBank,
  onMethodChange,
  onGatewayChange,
  onBankChange,
}) => {
  // selectedBank & onBankChange vẫn giữ trong props để tránh breaking change,
  // nhưng hiện tại không còn sử dụng vì đã bỏ Bank Transfer và MoMo.

  return (
    <Card sx={{ maxWidth: 600, mx: 'auto', mt: 2 }}>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Payment Method
        </Typography>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          Select Payment Method
        </Typography>

        <RadioGroup value={selectedMethod} onChange={(e) => onMethodChange(e.target.value)}>
          {/* Credit Card (VNPay) Option */}
          <FormControlLabel
            value="credit_card"
            control={<Radio />}
            label={
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <CreditCard sx={{ mr: 1 }} />
                <Typography>Thanh toán VNPay</Typography>
              </Box>
            }
          />

          {/* Cash on Delivery Option */}
          <FormControlLabel
            value="cod"
            control={<Radio />}
            label={
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <Money sx={{ mr: 1 }} />
                <Typography>Thanh toán khi nhận hàng (COD)</Typography>
              </Box>
            }
          />
        </RadioGroup>

        {/* Sub-options for Credit Card (only VNPay) */}
        {selectedMethod === 'credit_card' && (
          <Box sx={{ ml: 4, mt: 2, p: 2, bgcolor: 'background.paper', borderRadius: 1 }}>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Cổng thanh toán:
            </Typography>
            <RadioGroup value={creditCardGateway} onChange={(e) => onGatewayChange(e.target.value)}>
              <FormControlLabel
                value="vnpay"
                control={<Radio />}
                label={
                  <Box sx={{ display: 'flex', alignItems: 'center' }}>
                    <Payment sx={{ mr: 1, color: '#0055A4' }} />
                    <Typography>VNPay</Typography>
                  </Box>
                }
              />
            </RadioGroup>
            
            <Alert severity="info" sx={{ mt: 2 }}>
              <Typography variant="body2">
                VNPay - Thanh toán qua thẻ ATM, thẻ quốc tế, ví điện tử.
              </Typography>
            </Alert>
          </Box>
        )}

        {/* Information for COD */}
        {selectedMethod === 'cod' && (
          <Box sx={{ ml: 4, mt: 2, p: 2, bgcolor: 'background.paper', borderRadius: 1 }}>
            <Alert severity="info">
              <Typography variant="body2">
                Thanh toán tiền mặt khi nhận hàng. Không cần thanh toán trước.
              </Typography>
            </Alert>
          </Box>
        )}
      </CardContent>
    </Card>
  );
};

export default PaymentMethodSelector; 