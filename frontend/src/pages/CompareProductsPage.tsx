import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Button,
  IconButton,
  Chip,
  Rating,
  Grid,
  Card,
  CardMedia,
  Alert,
  Breadcrumbs,
  Link as MuiLink,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Slide,
  Fade,
} from '@mui/material';
import { TransitionProps } from '@mui/material/transitions';
import {
  Delete,
  ShoppingCart,
  Home,
  CompareArrows,
} from '@mui/icons-material';
import { Link, useNavigate } from 'react-router-dom';
import { useCompare } from '../contexts/CompareContext';
import { useCart } from '../contexts/CartContext';
import LazyImage from '../components/LazyImage';
import { Product } from '../services/api';
import { Product as ProductServiceProduct } from '../services/productService';

const Transition = React.forwardRef(function Transition(
  props: TransitionProps & {
    children: React.ReactElement<any, any>;
  },
  ref: React.Ref<unknown>,
) {
  return <Slide direction="up" ref={ref} {...props} />;
});

const CompareProductsPage: React.FC = () => {
  const navigate = useNavigate();
  const { compareProducts, removeFromCompare, clearCompare } = useCompare();
  const { addToCart } = useCart();
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [productToDelete, setProductToDelete] = useState<string | null>(null);
  const [clearDialogOpen, setClearDialogOpen] = useState(false);
  const [removingProductId, setRemovingProductId] = useState<string | null>(null);

  // Redirect to products page if no products to compare
  useEffect(() => {
    if (compareProducts.length === 0) {
      // Small delay to show toast notification first
      const timer = setTimeout(() => {
        navigate('/products');
      }, 1500);
      return () => clearTimeout(timer);
    }
  }, [compareProducts.length, navigate]);

  const handleRemoveClick = (productId: string) => {
    // If only 2 products left, show confirmation
    if (compareProducts.length === 2) {
      setProductToDelete(productId);
      setDeleteDialogOpen(true);
    } else {
      handleConfirmRemove(productId);
    }
  };

  const handleConfirmRemove = (productId: string) => {
    setRemovingProductId(productId);
    // Small delay for animation
    setTimeout(() => {
      removeFromCompare(productId);
      setRemovingProductId(null);
    }, 200);
  };

  const handleClearAllClick = () => {
    setClearDialogOpen(true);
  };

  const handleConfirmClear = () => {
    clearCompare();
    setClearDialogOpen(false);
  };

  if (compareProducts.length < 2) {
    return (
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Breadcrumbs sx={{ mb: 3 }}>
          <MuiLink component={Link} to="/" color="inherit" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <Home fontSize="small" />
            Trang chủ
          </MuiLink>
          <Typography color="text.primary">So sánh sản phẩm</Typography>
        </Breadcrumbs>

        <Box sx={{ textAlign: 'center', py: 8 }}>
          <CompareArrows sx={{ fontSize: 80, color: 'text.secondary', mb: 2 }} />
          <Typography variant="h5" color="text.secondary" sx={{ mb: 2 }}>
            Chưa có sản phẩm để so sánh
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
            Bạn cần chọn ít nhất 2 sản phẩm để so sánh
          </Typography>
          <Button
            variant="contained"
            component={Link}
            to="/products"
            sx={{ borderRadius: 2 }}
          >
            Xem sản phẩm
          </Button>
        </Box>
      </Container>
    );
  }

  // Get all unique attributes from all products
  const getComparisonAttributes = (products: (Product | ProductServiceProduct)[]) => {
    const attributes = new Set<string>();
    products.forEach(product => {
      if (product.name) attributes.add('name');
      if (product.price !== undefined) attributes.add('price');
      if (product.salePrice !== undefined) attributes.add('salePrice');
      if (product.rating !== undefined) attributes.add('rating');
      if (product.reviewCount !== undefined) attributes.add('reviewCount');
      if (product.stockQuantity !== undefined) attributes.add('stockQuantity');
      if (product.brand) attributes.add('brand');
      if (product.category) attributes.add('category');
      if (product.description) attributes.add('description');
      if ('sku' in product && product.sku) attributes.add('sku');
    });
    return Array.from(attributes);
  };

  const attributes = getComparisonAttributes(compareProducts);

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(price);
  };

  const handleAddToCart = (productId: string) => {
    addToCart(productId, 1);
  };

  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Breadcrumbs sx={{ mb: 3 }}>
        <MuiLink component={Link} to="/" color="inherit" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
          <Home fontSize="small" />
          Trang chủ
        </MuiLink>
        <MuiLink component={Link} to="/products" color="inherit">
          Sản phẩm
        </MuiLink>
        <Typography color="text.primary">So sánh sản phẩm</Typography>
      </Breadcrumbs>

      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
          So sánh sản phẩm ({compareProducts.length})
        </Typography>
        <Button
          variant="outlined"
          color="error"
          startIcon={<Delete />}
          onClick={handleClearAllClick}
          disabled={compareProducts.length === 0}
          sx={{ borderRadius: 2 }}
        >
          Xóa tất cả
        </Button>
      </Box>

      {compareProducts.length >= 4 && (
        <Alert severity="info" sx={{ mb: 3 }}>
          Bạn đã chọn tối đa 4 sản phẩm để so sánh. Để thêm sản phẩm mới, hãy xóa một sản phẩm hiện có.
        </Alert>
      )}

      {/* Product Cards Row */}
      <Grid container spacing={2} sx={{ mb: 4 }}>
        {compareProducts.map((product) => (
          <Grid item xs={12} sm={6} md={3} key={product.id}>
            <Fade in={removingProductId !== product.id} timeout={300}>
              <Card 
                sx={{ 
                  height: '100%', 
                  position: 'relative',
                  transition: 'all 0.3s ease-in-out',
                  opacity: removingProductId === product.id ? 0 : 1,
                  transform: removingProductId === product.id ? 'scale(0.8)' : 'scale(1)',
                  '&:hover': {
                    boxShadow: 6,
                    transform: 'translateY(-4px)'
                  }
                }}
              >
                <IconButton
                  sx={{
                    position: 'absolute',
                    top: 8,
                    right: 8,
                    zIndex: 2,
                    bgcolor: 'background.paper',
                    boxShadow: 2,
                    transition: 'all 0.2s ease-in-out',
                    '&:hover': { 
                      bgcolor: 'error.main', 
                      color: 'white',
                      transform: 'scale(1.1)'
                    },
                  }}
                  onClick={() => handleRemoveClick(product.id)}
                  size="small"
                  title="Xóa khỏi so sánh"
                >
                  <Delete />
                </IconButton>
              <CardMedia sx={{ position: 'relative', height: 200 }}>
                <LazyImage
                  src={product.images?.[0]?.imageUrl || product.imageUrl || 'https://via.placeholder.com/300x200'}
                  alt={product.name}
                  width="100%"
                  height="100%"
                  style={{ objectFit: 'cover' }}
                />
              </CardMedia>
              <Box sx={{ p: 2 }}>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1, fontSize: '1rem' }}>
                  {product.name}
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <Rating value={product.rating || 0} readOnly size="small" />
                  <Typography variant="body2" color="text.secondary">
                    ({product.reviewCount || 0})
                  </Typography>
                </Box>
                <Typography variant="h6" color="primary" sx={{ fontWeight: 'bold', mb: 2 }}>
                  {product.salePrice ? formatPrice(product.salePrice) : formatPrice(product.price || 0)}
                  {product.salePrice && product.price && product.price > product.salePrice && (
                    <Typography
                      component="span"
                      variant="body2"
                      sx={{ textDecoration: 'line-through', color: 'text.secondary', ml: 1 }}
                    >
                      {formatPrice(product.price)}
                    </Typography>
                  )}
                </Typography>
                <Button
                  fullWidth
                  variant="contained"
                  startIcon={<ShoppingCart />}
                  onClick={() => handleAddToCart(product.id)}
                  disabled={!product.stockQuantity || product.stockQuantity <= 0}
                  sx={{ borderRadius: 2 }}
                >
                  Thêm vào giỏ
                </Button>
              </Box>
            </Card>
            </Fade>
          </Grid>
        ))}
      </Grid>

      {/* Comparison Table */}
      <TableContainer component={Paper} sx={{ borderRadius: 2, overflow: 'hidden' }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 'bold', bgcolor: 'grey.100', minWidth: 200 }}>
                Thuộc tính
              </TableCell>
              {compareProducts.map((product) => (
                <TableCell
                  key={product.id}
                  align="center"
                  sx={{ fontWeight: 'bold', bgcolor: 'grey.50', minWidth: 200 }}
                >
                  {product.name}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {attributes.map((attr) => (
              <TableRow key={attr}>
                <TableCell sx={{ fontWeight: 'bold', bgcolor: 'grey.50' }}>
                  {attr === 'name' && 'Tên sản phẩm'}
                  {attr === 'price' && 'Giá gốc'}
                  {attr === 'salePrice' && 'Giá khuyến mãi'}
                  {attr === 'rating' && 'Đánh giá'}
                  {attr === 'reviewCount' && 'Số lượt đánh giá'}
                  {attr === 'stockQuantity' && 'Tồn kho'}
                  {attr === 'brand' && 'Thương hiệu'}
                  {attr === 'category' && 'Danh mục'}
                  {attr === 'description' && 'Mô tả'}
                  {attr === 'sku' && 'SKU'}
                </TableCell>
                {compareProducts.map((product) => (
                  <TableCell key={`${product.id}-${attr}`} align="center">
                    {attr === 'name' && product.name}
                    {attr === 'price' && product.price && formatPrice(product.price)}
                    {attr === 'salePrice' && product.salePrice && formatPrice(product.salePrice)}
                    {attr === 'rating' && (
                      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1 }}>
                        <Rating value={product.rating || 0} readOnly size="small" />
                        <Typography variant="body2">({product.rating?.toFixed(1) || 0})</Typography>
                      </Box>
                    )}
                    {attr === 'reviewCount' && product.reviewCount}
                    {attr === 'stockQuantity' && (
                      <Chip
                        label={product.stockQuantity > 0 ? `Còn ${product.stockQuantity}` : 'Hết hàng'}
                        color={product.stockQuantity > 0 ? 'success' : 'error'}
                        size="small"
                      />
                    )}
                    {attr === 'brand' && (typeof product.brand === 'string' ? product.brand : (product.brand && typeof product.brand === 'object' && 'name' in product.brand ? (product.brand as { name: string }).name : ''))}
                    {attr === 'category' && (typeof product.category === 'string' ? product.category : (product.category && typeof product.category === 'object' && 'name' in product.category ? (product.category as { name: string }).name : ''))}
                    {attr === 'description' && (
                      <Typography variant="body2" sx={{ maxWidth: 300, textAlign: 'left' }}>
                        {product.description?.substring(0, 100)}
                        {product.description && product.description.length > 100 && '...'}
                      </Typography>
                    )}
                    {attr === 'sku' && ('sku' in product ? product.sku : '')}
                  </TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Box sx={{ mt: 4, display: 'flex', justifyContent: 'center', gap: 2 }}>
        <Button
          variant="outlined"
          component={Link}
          to="/products"
          sx={{ borderRadius: 2 }}
        >
          Xem thêm sản phẩm
        </Button>
        <Button
          variant="contained"
          onClick={() => navigate(-1)}
          sx={{ borderRadius: 2 }}
        >
          Quay lại
        </Button>
      </Box>

      {/* Confirmation Dialog for removing last product */}
      <Dialog
        open={deleteDialogOpen}
        TransitionComponent={Transition}
        keepMounted
        onClose={() => setDeleteDialogOpen(false)}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
      >
        <DialogTitle id="alert-dialog-title">
          Xác nhận xóa sản phẩm
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="alert-dialog-description">
            Bạn đang có 2 sản phẩm để so sánh. Nếu xóa sản phẩm này, bạn sẽ không thể so sánh nữa vì cần ít nhất 2 sản phẩm.
            <br /><br />
            Bạn có chắc chắn muốn xóa không?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)} color="inherit">
            Hủy
          </Button>
          <Button 
            onClick={() => {
              if (productToDelete) {
                handleConfirmRemove(productToDelete);
              }
              setDeleteDialogOpen(false);
              setProductToDelete(null);
            }} 
            color="error" 
            variant="contained"
            autoFocus
          >
            Xóa
          </Button>
        </DialogActions>
      </Dialog>

      {/* Confirmation Dialog for clearing all */}
      <Dialog
        open={clearDialogOpen}
        TransitionComponent={Transition}
        keepMounted
        onClose={() => setClearDialogOpen(false)}
        aria-labelledby="clear-dialog-title"
        aria-describedby="clear-dialog-description"
      >
        <DialogTitle id="clear-dialog-title">
          Xác nhận xóa tất cả
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="clear-dialog-description">
            Bạn có chắc chắn muốn xóa tất cả {compareProducts.length} sản phẩm khỏi danh sách so sánh không?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setClearDialogOpen(false)} color="inherit">
            Hủy
          </Button>
          <Button 
            onClick={handleConfirmClear} 
            color="error" 
            variant="contained"
            autoFocus
          >
            Xóa tất cả
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default CompareProductsPage;

