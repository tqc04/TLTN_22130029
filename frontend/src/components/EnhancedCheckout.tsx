import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  Stepper,
  Step,
  StepLabel,
  Button,
  Grid,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  CircularProgress,
  List,
  ListItem,
  ListItemText,
  ListItemAvatar,
  Avatar,
  RadioGroup,
  FormControlLabel,
  Radio,
  FormHelperText,
} from '@mui/material';
import {
  CheckCircle,
  Security,
  CreditCard,
  AccountBalance,
  Money,
  ArrowForward,
  ArrowBack,
} from '@mui/icons-material';
import { useCart } from '../contexts/CartContext';
import { useAuth } from '../contexts/AuthContext';
import { useNotification } from '../contexts/NotificationContext';
import { apiService } from '../services/api';
import { formatPrice } from '../utils/priceUtils';

const steps = ['Cart Review', 'Shipping Address', 'Payment Method', 'Order Confirmation'];

interface EnhancedCheckoutProps {
  onOrderSuccess?: (orderNumber: string) => void;
  onCancel?: () => void;
}

const EnhancedCheckout: React.FC<EnhancedCheckoutProps> = ({
  onOrderSuccess,
  onCancel,
}) => {
  const { cart, cartLoading, clearCart } = useCart();
  const { user } = useAuth();
  const { notify } = useNotification();
  
  const [activeStep, setActiveStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [orderNumber, setOrderNumber] = useState<string | null>(null);
  
  // Form states
  const [shippingAddress, setShippingAddress] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    address: '',
    provinceId: '',
    communeCode: '',
    provinceName: '',
    communeName: '',
    note: '',
  });
  
  const [paymentMethod, setPaymentMethod] = useState('credit_card');
  const [sameAsBilling] = useState(true);
  const [shippingFee, setShippingFee] = useState<number>(0);
  const [provinces, setProvinces] = useState<{ code: string; name: string }[]>([]);
  const [communes, setCommunes] = useState<{ code: string; name: string }[]>([]);
  
  // Validation states
  const [errors, setErrors] = useState<{[key: string]: string}>({});

  useEffect(() => {
    loadProvinces();
  }, []);

  const loadProvinces = async () => {
    try {
      const response = await apiService.getProvinces();
      if (response.success) {
        setProvinces((response.data as { code: string; name: string }[]) || []);
      } else {
        console.warn('Failed to load provinces:', response.message);
        // Show user-friendly error message
        notify(response.message || 'Unable to load shipping locations. Please try again later.', 'warning');
        // Provide fallback data if available
        setProvinces([]);
      }
    } catch (error) {
      console.error('Failed to load provinces:', error);
      notify('Unable to load shipping locations. Please try again later.', 'warning');
    }
  };

  const loadCommunes = async (provinceCode: string) => {
    try {
      const response = await apiService.getCommunesByProvince(provinceCode);
      if (response.success) {
        setCommunes((response.data as { code: string; name: string }[]) || []);
      } else {
        console.warn('Failed to load communes:', response.message);
        notify(response.message || 'Unable to load districts. Please try again later.', 'warning');
        setCommunes([]);
      }
    } catch (error) {
      console.error('Failed to load communes:', error);
      notify('Unable to load districts. Please try again later.', 'warning');
    }
  };

  const validateStep = (step: number): boolean => {
    const newErrors: {[key: string]: string} = {};

    switch (step) {
      case 0:
        if (!cart || cart.items.length === 0) {
          newErrors.cart = 'Cart is empty';
        }
        break;
        
      case 1:
        if (!shippingAddress.firstName.trim()) {
          newErrors.firstName = 'First name is required';
        }
        if (!shippingAddress.lastName.trim()) {
          newErrors.lastName = 'Last name is required';
        }
        if (!shippingAddress.email.trim()) {
          newErrors.email = 'Email is required';
        } else if (!/\S+@\S+\.\S+/.test(shippingAddress.email)) {
          newErrors.email = 'Invalid email format';
        }
        if (!shippingAddress.phone.trim()) {
          newErrors.phone = 'Phone number is required';
        }
        if (!shippingAddress.address.trim()) {
          newErrors.address = 'Address is required';
        }
        if (!shippingAddress.provinceId) {
          newErrors.province = 'Province is required';
        }
        if (!shippingAddress.communeCode) {
          newErrors.commune = 'Commune is required';
        }
        break;
        
      case 2:
        if (!paymentMethod) {
          newErrors.payment = 'Payment method is required';
        }
        break;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleNext = async () => {
    if (!validateStep(activeStep)) {
      return;
    }

    if (activeStep === 0) {
      // Validate stock before proceeding
      setLoading(true);
      try {
        const stockValidation = await validateStock();
        if (!stockValidation.valid) {
          notify(`Stock issue: ${stockValidation.message}`, 'error');
          return;
        }
        // Stock validation successful
      } catch (error) {
        notify('Failed to validate stock', 'error');
        return;
      } finally {
        setLoading(false);
      }
    }

    if (activeStep === 1) {
      // Calculate shipping fee
      setLoading(true);
      try {
        const fee = await calculateShippingFee();
        setShippingFee(fee);
      } catch (error) {
        notify('Failed to calculate shipping fee', 'error');
        return;
      } finally {
        setLoading(false);
      }
    }

    setActiveStep(prev => prev + 1);
  };

  const handleBack = () => {
    setActiveStep(prev => Math.max(prev - 1, 0));
  };

  const validateStock = async (): Promise<{valid: boolean, message: string}> => {
    if (!cart) return { valid: false, message: 'Cart is empty' };

    for (const item of cart.items) {
      try {
        const response = await apiService.getProduct(item.productId);
        if (response.success) {
          const currentStock = response.data.stockQuantity;
          if (currentStock < item.quantity) {
            return {
              valid: false,
              message: `Insufficient stock for ${response.data.name}. Available: ${currentStock}, Requested: ${item.quantity}`
            };
          }
        }
      } catch (error) {
        return { valid: false, message: 'Failed to validate stock' };
      }
    }
    return { valid: true, message: '' };
  };

  const calculateShippingFee = async (): Promise<number> => {
    // Mock shipping fee calculation
    // In real implementation, this would call shipping API
    return 50000; // 50,000 VND
  };

  const handlePlaceOrder = async () => {
    if (!cart || !user) {
      notify('Unable to place order - missing data', 'error');
      return;
    }

    setLoading(true);
    try {
      const fullShippingAddress = [
        shippingAddress.address,
        shippingAddress.communeName,
        shippingAddress.provinceName
      ].filter(Boolean).join(', ');

      const orderData = {
        // userId removed - will be obtained from JWT on backend
        items: cart.items.map(item => ({
          productId: item.productId,
          quantity: item.quantity
        })),
        shippingAddress: fullShippingAddress,
        billingAddress: sameAsBilling ? fullShippingAddress : fullShippingAddress,
        paymentMethod: paymentMethod,
        shippingFee: shippingFee,
        note: shippingAddress.note,
        // Add voucher information if applied
        voucherCode: cart.voucherCode,
        voucherId: cart.voucherId,
        discountAmount: cart.discountAmount
      };

      const orderResponse = await apiService.createOrder(orderData);

      if (orderResponse.success) {
        setOrderNumber(orderResponse.data.orderNumber);
        setActiveStep(steps.length);
        
        // Only clear cart immediately for COD orders
        // For online payments, cart will be cleared after payment confirmation
        if (paymentMethod === 'cod' || paymentMethod === 'COD' || paymentMethod === 'cash_on_delivery') {
          await clearCart();
          notify('Order placed successfully! Pay on delivery.', 'success');
        } else {
          // For online payments, don't clear cart yet
          notify('Order created! Please complete payment to finalize.', 'info');
        }
        
        onOrderSuccess?.(orderResponse.data.orderNumber);
      } else {
        notify('Failed to place order', 'error');
      }
    } catch (error) {
      console.error('Order creation failed:', error);
      notify('Failed to place order', 'error');
    } finally {
      setLoading(false);
    }
  };

  const getStepContent = (step: number) => {
    switch (step) {
      case 0:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>
              Review Your Cart
            </Typography>
            {cart && cart.items.length > 0 ? (
              <List>
                {cart.items.map((item, index) => (
                  <ListItem key={index} divider>
                    <ListItemAvatar>
                      <Avatar src={item.productImage} />
                    </ListItemAvatar>
                    <ListItemText
                      primary={item.productName}
                      secondary={`Quantity: ${item.quantity} | Price: ${formatPrice(item.productPrice)}`}
                    />
                    <Typography variant="subtitle1" fontWeight="bold">
                      {formatPrice(item.productPrice * item.quantity)}
                    </Typography>
                  </ListItem>
                ))}
              </List>
            ) : (
              <Alert severity="warning">
                Your cart is empty
              </Alert>
            )}
            
            {cart && (
              <Box sx={{ mt: 2, p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                <Typography variant="h6" gutterBottom>
                  Order Summary
                </Typography>
                <Grid container spacing={1}>
                  <Grid item xs={6}>
                    <Typography>Subtotal:</Typography>
                  </Grid>
                  <Grid item xs={6} textAlign="right">
                    <Typography>{formatPrice(cart.subtotal || 0)}</Typography>
                  </Grid>

                  {/* Voucher Discount */}
                  {cart.voucherCode && cart.discountAmount > 0 && (
                    <>
                      <Grid item xs={6}>
                        <Typography color="success.main">Voucher ({cart.voucherCode}):</Typography>
                      </Grid>
                      <Grid item xs={6} textAlign="right">
                        <Typography color="success.main">-{formatPrice(cart.discountAmount)}</Typography>
                      </Grid>
                    </>
                  )}

                  <Grid item xs={6}>
                    <Typography>Shipping:</Typography>
                  </Grid>
                  <Grid item xs={6} textAlign="right">
                    <Typography>{formatPrice(shippingFee)}</Typography>
                  </Grid>

                  <Grid item xs={12}>
                    <Box sx={{ borderTop: '1px solid', borderColor: 'divider', pt: 1, mt: 1 }}>
                      <Grid container>
                        <Grid item xs={6}>
                          <Typography variant="h6" fontWeight="bold">Total:</Typography>
                        </Grid>
                        <Grid item xs={6} textAlign="right">
                          <Typography variant="h6" fontWeight="bold">
                            {formatPrice(cart.totalAmount || (cart.subtotal + shippingFee - (cart.discountAmount || 0)))}
                          </Typography>
                        </Grid>
                      </Grid>
                    </Box>
                  </Grid>
                </Grid>
              </Box>
            )}
          </Box>
        );

      case 1:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>
              Shipping Address
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={6}>
                <TextField
                  fullWidth
                  label="First Name"
                  value={shippingAddress.firstName}
                  onChange={(e) => setShippingAddress(prev => ({ ...prev, firstName: e.target.value }))}
                  error={!!errors.firstName}
                  helperText={errors.firstName}
                />
              </Grid>
              <Grid item xs={6}>
                <TextField
                  fullWidth
                  label="Last Name"
                  value={shippingAddress.lastName}
                  onChange={(e) => setShippingAddress(prev => ({ ...prev, lastName: e.target.value }))}
                  error={!!errors.lastName}
                  helperText={errors.lastName}
                />
              </Grid>
              <Grid item xs={6}>
                <TextField
                  fullWidth
                  label="Email"
                  type="email"
                  value={shippingAddress.email}
                  onChange={(e) => setShippingAddress(prev => ({ ...prev, email: e.target.value }))}
                  error={!!errors.email}
                  helperText={errors.email}
                />
              </Grid>
              <Grid item xs={6}>
                <TextField
                  fullWidth
                  label="Phone"
                  value={shippingAddress.phone}
                  onChange={(e) => setShippingAddress(prev => ({ ...prev, phone: e.target.value }))}
                  error={!!errors.phone}
                  helperText={errors.phone}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Address"
                  multiline
                  rows={2}
                  value={shippingAddress.address}
                  onChange={(e) => setShippingAddress(prev => ({ ...prev, address: e.target.value }))}
                  error={!!errors.address}
                  helperText={errors.address}
                />
              </Grid>
              <Grid item xs={6}>
                <FormControl fullWidth error={!!errors.province}>
                  <InputLabel>Province</InputLabel>
                  <Select
                    value={shippingAddress.provinceId}
                    onChange={(e) => {
                      const province = provinces.find(p => p.code === e.target.value);
                      setShippingAddress(prev => ({
                        ...prev,
                        provinceId: e.target.value,
                        provinceName: province?.name || '',
                        communeCode: '',
                        communeName: ''
                      }));
                      if (e.target.value) {
                        loadCommunes(e.target.value);
                      }
                    }}
                  >
                    {provinces.map((province) => (
                      <MenuItem key={province.code} value={province.code}>
                        {province.name}
                      </MenuItem>
                    ))}
                  </Select>
                  {errors.province && <FormHelperText>{errors.province}</FormHelperText>}
                </FormControl>
              </Grid>
              <Grid item xs={6}>
                <FormControl fullWidth error={!!errors.commune}>
                  <InputLabel>Commune</InputLabel>
                  <Select
                    value={shippingAddress.communeCode}
                    onChange={(e) => {
                      const commune = communes.find(c => c.code === e.target.value);
                      setShippingAddress(prev => ({
                        ...prev,
                        communeCode: e.target.value,
                        communeName: commune?.name || ''
                      }));
                    }}
                    disabled={!shippingAddress.provinceId}
                  >
                    {communes.map((commune) => (
                      <MenuItem key={commune.code} value={commune.code}>
                        {commune.name}
                      </MenuItem>
                    ))}
                  </Select>
                  {errors.commune && <FormHelperText>{errors.commune}</FormHelperText>}
                </FormControl>
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Note (Optional)"
                  multiline
                  rows={2}
                  value={shippingAddress.note}
                  onChange={(e) => setShippingAddress(prev => ({ ...prev, note: e.target.value }))}
                />
              </Grid>
            </Grid>
          </Box>
        );

      case 2:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>
              Payment Method
            </Typography>
            <FormControl component="fieldset" error={!!errors.payment}>
              <RadioGroup
                value={paymentMethod}
                onChange={(e) => setPaymentMethod(e.target.value)}
              >
                <FormControlLabel
                  value="credit_card"
                  control={<Radio />}
                  label={
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <CreditCard />
                      <Typography>Credit Card</Typography>
                    </Box>
                  }
                />
                <FormControlLabel
                  value="bank_transfer"
                  control={<Radio />}
                  label={
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <AccountBalance />
                      <Typography>Bank Transfer</Typography>
                    </Box>
                  }
                />
                <FormControlLabel
                  value="cod"
                  control={<Radio />}
                  label={
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Money />
                      <Typography>Cash on Delivery</Typography>
                    </Box>
                  }
                />
              </RadioGroup>
              {errors.payment && <FormHelperText>{errors.payment}</FormHelperText>}
            </FormControl>
            
            <Alert severity="info" sx={{ mt: 2 }}>
              <Security sx={{ mr: 1 }} />
              Your payment information is secure and encrypted
            </Alert>
          </Box>
        );

      default:
        return null;
    }
  };

  if (cartLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (activeStep === steps.length) {
    return (
      <Box sx={{ textAlign: 'center', p: 4 }}>
        <CheckCircle sx={{ fontSize: 80, color: 'success.main', mb: 2 }} />
        <Typography variant="h4" gutterBottom fontWeight="bold">
          Order Confirmed!
        </Typography>
        <Typography variant="h6" color="text.secondary" sx={{ mb: 3 }}>
          Order Number: {orderNumber}
        </Typography>
        <Typography variant="body1" sx={{ mb: 3 }}>
          Thank you for your purchase. You will receive an email confirmation shortly.
        </Typography>
        <Button
          variant="contained"
          onClick={() => window.location.href = '/orders'}
        >
          View My Orders
        </Button>
      </Box>
    );
  }

  return (
    <Paper sx={{ p: 3 }}>
      <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
        {steps.map((label) => (
          <Step key={label}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>

      {getStepContent(activeStep)}

      <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 4 }}>
        <Button
          onClick={onCancel}
          disabled={loading}
        >
          Cancel
        </Button>
        
        <Box>
          {activeStep > 0 && (
            <Button
              onClick={handleBack}
              disabled={loading}
              sx={{ mr: 1 }}
            >
              <ArrowBack sx={{ mr: 1 }} />
              Back
            </Button>
          )}
          
          {activeStep === steps.length - 1 ? (
            <Button
              onClick={handlePlaceOrder}
              variant="contained"
              disabled={loading}
              startIcon={loading ? <CircularProgress size={20} /> : <CheckCircle />}
            >
              {loading ? 'Placing Order...' : 'Place Order'}
            </Button>
          ) : (
            <Button
              onClick={handleNext}
              variant="contained"
              disabled={loading}
              endIcon={<ArrowForward />}
            >
              {loading ? <CircularProgress size={20} /> : 'Next'}
            </Button>
          )}
        </Box>
      </Box>
    </Paper>
  );
};

export default EnhancedCheckout; 