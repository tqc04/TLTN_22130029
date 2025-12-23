import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Typography,
  Alert,
  CircularProgress,
  Stepper,
  Step,
  StepLabel,
  Card,
  CardContent,
  Grid,
  Chip,
  IconButton,
} from '@mui/material';
import {
  ShoppingCart,
  CheckCircle,
  Error,
  Add,
  Remove,
  Close,
  Security,
} from '@mui/icons-material';
import { useCart } from '../contexts/CartContext';
import { useAuth } from '../contexts/AuthContext';
import { useNotification } from '../contexts/NotificationContext';
import { apiService } from '../services/api';
import { formatPrice } from '../utils/priceUtils';

interface ProductPurchaseFlowProps {
  product: {
    id: number;
    name: string;
    price: number;
    stockQuantity: number;
    imageUrl?: string;
  };
  open: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

const steps = ['Quantity Selection', 'Stock Check', 'Add to Cart'];

const ProductPurchaseFlow: React.FC<ProductPurchaseFlowProps> = ({
  product,
  open,
  onClose,
  onSuccess,
}) => {
  const { addToCart } = useCart();
  const { isAuthenticated } = useAuth();
  const { notify } = useNotification();
  
  const [activeStep, setActiveStep] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(false);
  const [stockAvailable, setStockAvailable] = useState(true);
  const [stockQuantity, setStockQuantity] = useState(product.stockQuantity);
  const [error, setError] = useState<string | null>(null);

  // Reset state when dialog opens
  useEffect(() => {
    if (open) {
      setActiveStep(0);
      setQuantity(1);
      setError(null);
      setStockAvailable(true);
      setStockQuantity(product.stockQuantity);
    }
  }, [open, product]);

  const handleQuantityChange = (increment: boolean) => {
    if (increment) {
      setQuantity(prev => Math.min(prev + 1, stockQuantity));
    } else {
      setQuantity(prev => Math.max(prev - 1, 1));
    }
  };

  const handleNext = async () => {
    if (activeStep === 0) {
      // Check stock availability
      setLoading(true);
      try {
        const response = await apiService.getProduct(String(product.id));
        if (response.success) {
          const currentStock = response.data.stockQuantity;
          setStockQuantity(currentStock);
          
          if (currentStock < quantity) {
            setStockAvailable(false);
            setError(`Only ${currentStock} items available in stock`);
            return;
          }
          
          setStockAvailable(true);
          setError(null);
          setActiveStep(1);
        }
      } catch (error) {
        setError('Failed to check stock availability');
      } finally {
        setLoading(false);
      }
    } else if (activeStep === 1) {
      setActiveStep(2);
    }
  };

  const handleAddToCart = async () => {
    if (!isAuthenticated) {
      notify('Please login to add items to cart', 'warning');
      return;
    }

    setLoading(true);
    try {
      const success = await addToCart(String(product.id), quantity);
      if (success) {
        notify(`Added ${quantity} ${quantity === 1 ? 'item' : 'items'} to cart`, 'success');
        onSuccess?.();
        onClose();
      } else {
        setError('Failed to add item to cart');
      }
    } catch (error) {
      setError('Failed to add item to cart');
    } finally {
      setLoading(false);
    }
  };

  const handleBack = () => {
    setActiveStep(prev => Math.max(prev - 1, 0));
  };

  const getStepContent = (step: number) => {
    switch (step) {
      case 0:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>
              Select Quantity
            </Typography>
            <Card sx={{ mb: 2 }}>
              <CardContent>
                <Grid container spacing={2} alignItems="center">
                  <Grid item xs={3}>
                    <img
                      src={product.imageUrl || 'https://via.placeholder.com/80x80'}
                      alt={product.name}
                      style={{ width: '100%', height: 'auto', borderRadius: 8 }}
                    />
                  </Grid>
                  <Grid item xs={9}>
                    <Typography variant="subtitle1" fontWeight="bold">
                      {product.name}
                    </Typography>
                    <Typography variant="h6" color="primary.main" fontWeight="bold">
                      {formatPrice(product.price)}
                    </Typography>
                    <Chip
                      label={`${stockQuantity} in stock`}
                      color={stockQuantity > 10 ? 'success' : stockQuantity > 0 ? 'warning' : 'error'}
                      size="small"
                      sx={{ mt: 1 }}
                    />
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
            
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 2 }}>
              <IconButton
                onClick={() => handleQuantityChange(false)}
                disabled={quantity <= 1}
                size="large"
              >
                <Remove />
              </IconButton>
              <TextField
                value={quantity}
                onChange={(e) => {
                  const value = parseInt(e.target.value) || 1;
                  setQuantity(Math.max(1, Math.min(value, stockQuantity)));
                }}
                type="number"
                inputProps={{ min: 1, max: stockQuantity }}
                sx={{ width: 80, textAlign: 'center' }}
              />
              <IconButton
                onClick={() => handleQuantityChange(true)}
                disabled={quantity >= stockQuantity}
                size="large"
              >
                <Add />
              </IconButton>
            </Box>
            
            <Box sx={{ mt: 2, textAlign: 'center' }}>
              <Typography variant="h6" color="primary.main" fontWeight="bold">
                Total: {formatPrice(product.price * quantity)}
              </Typography>
            </Box>
          </Box>
        );
        
      case 1:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>
              Stock Verification
            </Typography>
            {stockAvailable ? (
              <Alert severity="success" icon={<CheckCircle />}>
                <Typography variant="subtitle1" fontWeight="bold">
                  Stock Available!
                </Typography>
                <Typography variant="body2">
                  {quantity} {quantity === 1 ? 'item' : 'items'} ready to add to cart
                </Typography>
              </Alert>
            ) : (
              <Alert severity="error" icon={<Error />}>
                <Typography variant="subtitle1" fontWeight="bold">
                  Insufficient Stock
                </Typography>
                <Typography variant="body2">
                  {error}
                </Typography>
              </Alert>
            )}
          </Box>
        );
        
      case 2:
        return (
          <Box>
            <Typography variant="h6" gutterBottom>
              Add to Cart
            </Typography>
            <Alert severity="info" icon={<ShoppingCart />}>
              <Typography variant="subtitle1" fontWeight="bold">
                Ready to Add
              </Typography>
              <Typography variant="body2">
                {quantity} x {product.name} - {formatPrice(product.price * quantity)}
              </Typography>
            </Alert>
            
            <Box sx={{ mt: 2 }}>
              <Typography variant="body2" color="text.secondary">
                <Security sx={{ fontSize: 16, verticalAlign: 'middle', mr: 0.5 }} />
                Secure checkout with encrypted payment processing
              </Typography>
            </Box>
          </Box>
        );
        
      default:
        return null;
    }
  };

  const canProceed = () => {
    if (activeStep === 0) {
      return quantity > 0 && quantity <= stockQuantity;
    }
    if (activeStep === 1) {
      return stockAvailable;
    }
    return true;
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      PaperProps={{
        sx: { borderRadius: 3 }
      }}
    >
      <DialogTitle sx={{ pb: 1 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h6" fontWeight="bold">
            Purchase {product.name}
          </Typography>
          <IconButton onClick={onClose} size="small">
            <Close />
          </IconButton>
        </Box>
      </DialogTitle>
      
      <DialogContent>
        <Stepper activeStep={activeStep} sx={{ mb: 3 }}>
          {steps.map((label) => (
            <Step key={label}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>
        
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        
        {getStepContent(activeStep)}
      </DialogContent>
      
      <DialogActions sx={{ p: 3, pt: 1 }}>
        <Button
          onClick={onClose}
          disabled={loading}
        >
          Cancel
        </Button>
        
        {activeStep > 0 && (
          <Button
            onClick={handleBack}
            disabled={loading}
          >
            Back
          </Button>
        )}
        
        {activeStep === steps.length - 1 ? (
          <Button
            onClick={handleAddToCart}
            variant="contained"
            disabled={loading || !canProceed()}
            startIcon={loading ? <CircularProgress size={20} /> : <ShoppingCart />}
          >
            {loading ? 'Adding...' : 'Add to Cart'}
          </Button>
        ) : (
          <Button
            onClick={handleNext}
            variant="contained"
            disabled={loading || !canProceed()}
          >
            {loading ? <CircularProgress size={20} /> : 'Next'}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};

export default ProductPurchaseFlow; 