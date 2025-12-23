import React, { useState, useEffect } from 'react';
import {
  Container,
  Grid,
  Card,
  CardContent,
  CardMedia,
  Typography,
  Button,
  Box,
  IconButton,
  Chip,
  Rating,
  Alert,
  Skeleton,
  Tooltip,
} from '@mui/material';
import {
  Favorite,
  ShoppingCart,
  Delete,
  Visibility,
  TrendingUp,
  TrendingDown,
} from '@mui/icons-material';
import { useTranslation } from 'react-i18next';
import { useCart } from '../contexts/CartContext';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/api';

interface WishlistItem {
  id: number;
  productId: string;
  name: string;
  description: string;
  price: number;
  originalPrice?: number;
  image: string;
  category: string;
  brand: string;
  rating: number;
  reviews: number;
  stock: number;
  addedDate: string;
  discount?: number;
  isNew?: boolean;
  isOnSale?: boolean;
}

const WishlistPage: React.FC = () => {
  const { t } = useTranslation();
  const { addToCart } = useCart();
  const { isAuthenticated } = useAuth();
  const [wishlistItems, setWishlistItems] = useState<WishlistItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedItems, setSelectedItems] = useState<number[]>([]);

  useEffect(() => {
    const fetchWishlistItems = async () => {
      if (!isAuthenticated) {
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        const currentUser = JSON.parse(localStorage.getItem('auth_user') || '{}');
        const userId = currentUser.id || currentUser.userId || 1;

        // Fetch wishlist items from favorites API
        const response = await apiService.getFavorites(userId);
        if (response.success && response.data) {
          // Transform API data to match component interface
          const transformedItems: WishlistItem[] = response.data.map((product: any, index: number) => ({
            id: index + 1,
            productId: product.id,
            name: product.name,
            description: product.description || 'No description available',
            price: product.price || 0,
            originalPrice: product.compareAtPrice || product.price,
            image: product.imageUrl || 'https://via.placeholder.com/400x300?text=No+Image',
            category: product.category || 'General',
            brand: product.brand || 'Unknown Brand',
            rating: product.averageRating || product.rating || 4.5,
            reviews: product.reviewCount || 0,
            stock: product.stockQuantity || 0,
            addedDate: product.createdAt ? new Date(product.createdAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
            discount: product.salePrice ? Math.round(100 * (1 - product.salePrice / product.price)) : 0,
            isNew: product.isFeatured || false,
            isOnSale: product.isOnSale || false,
          }));
          setWishlistItems(transformedItems);
        } else {
          setWishlistItems([]);
        }
      } catch (error) {
        console.error('Error fetching wishlist items:', error);
        setWishlistItems([]);
      } finally {
        setLoading(false);
      }
    };

    fetchWishlistItems();
  }, [isAuthenticated]);

  const handleRemoveFromWishlist = async (itemId: number) => {
    try {
      const item = wishlistItems.find(item => item.id === itemId);
      if (!item) return;

      const currentUser = JSON.parse(localStorage.getItem('auth_user') || '{}');
      const userId = currentUser.id || currentUser.userId || 1;

      // Call API to remove from favorites
      await apiService.removeFromFavorites(item.productId, userId);
      setWishlistItems(prev => prev.filter(item => item.id !== itemId));
    } catch (error) {
      console.error('Error removing from wishlist:', error);
      // Still remove from UI for better UX
      setWishlistItems(prev => prev.filter(item => item.id !== itemId));
    }
  };

  const handleAddToCart = (item: WishlistItem) => {
    addToCart(item.productId, 1);
  };

  // Select item functionality removed - not used

  const handleSelectAll = () => {
    if (selectedItems.length === wishlistItems.length) {
      setSelectedItems([]);
    } else {
      setSelectedItems(wishlistItems.map(item => item.id));
    }
  };

  const handleRemoveSelected = async () => {
    try {
      const currentUser = JSON.parse(localStorage.getItem('auth_user') || '{}');
      const userId = currentUser.id || currentUser.userId || 1;

      // Remove all selected items from API
      await Promise.all(
        selectedItems.map(async (itemId) => {
          const item = wishlistItems.find(item => item.id === itemId);
          if (item) {
            await apiService.removeFromFavorites(item.productId, userId);
          }
        })
      );

      setWishlistItems(prev => prev.filter(item => !selectedItems.includes(item.id)));
      setSelectedItems([]);
    } catch (error) {
      console.error('Error removing selected items:', error);
      // Still remove from UI for better UX
      setWishlistItems(prev => prev.filter(item => !selectedItems.includes(item.id)));
      setSelectedItems([]);
    }
  };

  const handleAddSelectedToCart = () => {
    selectedItems.forEach(itemId => {
      const item = wishlistItems.find(wishlistItem => wishlistItem.id === itemId);
      if (item) {
        addToCart(item.productId, 1);
      }
    });
  };

  const getPriceChange = (item: WishlistItem) => {
    if (!item.originalPrice) return null;
    const change = ((item.price - item.originalPrice) / item.originalPrice) * 100;
    return change;
  };

  if (!isAuthenticated) {
    return (
      <Container maxWidth="lg" sx={{ py: 4, pt: '40px' }}>
        <Alert severity="info" sx={{ borderRadius: 3 }}>
          {t('wishlist.loginRequired')}
        </Alert>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4, pt: '40px' }}>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h3" component="h1" sx={{ fontWeight: 'bold', mb: 1 }}>
          {t('wishlist.title')}
        </Typography>
        <Typography variant="h6" color="text.secondary">
          {t('wishlist.empty')}
        </Typography>
      </Box>

      {/* Actions Bar */}
      {wishlistItems.length > 0 && (
        <Card sx={{ borderRadius: 3, mb: 4 }}>
          <CardContent>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Button
                  variant="outlined"
                  onClick={handleSelectAll}
                  size="small"
                >
                  {selectedItems.length === wishlistItems.length ? t('common.cancel') : t('common.edit')}
                </Button>
                <Typography variant="body2" color="text.secondary">
                  {selectedItems.length} of {wishlistItems.length} selected
                </Typography>
              </Box>
              
              <Box sx={{ display: 'flex', gap: 1 }}>
                {selectedItems.length > 0 && (
                  <>
                    <Button
                      variant="contained"
                      startIcon={<ShoppingCart />}
                      onClick={handleAddSelectedToCart}
                      size="small"
                    >
                      {t('cart.addToCart')}
                    </Button>
                    <Button
                      variant="outlined"
                      color="error"
                      startIcon={<Delete />}
                      onClick={handleRemoveSelected}
                      size="small"
                    >
                      {t('common.delete')}
                    </Button>
                  </>
                )}
              </Box>
            </Box>
          </CardContent>
        </Card>
      )}

      {/* Wishlist Items */}
      {loading ? (
        <Grid container spacing={3}>
          {[...Array(4)].map((_, index) => (
            <Grid item xs={12} sm={6} md={4} lg={3} key={index}>
              <Card sx={{ borderRadius: 3 }}>
                <Skeleton variant="rectangular" height={200} />
                <CardContent>
                  <Skeleton variant="text" height={24} />
                  <Skeleton variant="text" height={20} />
                  <Skeleton variant="text" height={20} />
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      ) : wishlistItems.length === 0 ? (
        <Card sx={{ borderRadius: 3, textAlign: 'center', py: 8 }}>
          <CardContent>
            <Favorite sx={{ fontSize: 80, color: 'text.secondary', mb: 3 }} />
            <Typography variant="h5" sx={{ fontWeight: 'bold', mb: 2 }}>
              {t('wishlist.empty')}
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
              {t('wishlist.loginRequired')}
            </Typography>
            <Button
              variant="contained"
              size="large"
              href="/products"
              sx={{ borderRadius: 2, textTransform: 'none' }}
            >
              {t('products.title')}
            </Button>
          </CardContent>
        </Card>
      ) : (
        <Grid container spacing={3}>
          {wishlistItems.map((item) => (
            <Grid item xs={12} sm={6} md={4} lg={3} key={item.id}>
              <Card 
                sx={{ 
                  borderRadius: 3,
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  transition: 'transform 0.2s ease, box-shadow 0.2s ease',
                  '&:hover': {
                    transform: 'translateY(-4px)',
                    boxShadow: '0 8px 25px rgba(0,0,0,0.15)',
                  },
                }}
              >
                {/* Product Image */}
                <Box sx={{ position: 'relative' }}>
                  <CardMedia
                    component="img"
                    height="200"
                    image={item.image}
                    alt={item.name}
                    sx={{ objectFit: 'cover' }}
                  />
                  
                  {/* Badges */}
                  <Box sx={{ position: 'absolute', top: 12, left: 12, display: 'flex', flexDirection: 'column', gap: 1 }}>
                    {item.isNew && (
                      <Chip label="NEW" color="success" size="small" />
                    )}
                    {item.discount && (
                      <Chip label={`-${item.discount}%`} color="error" size="small" />
                    )}
                    {item.isOnSale && (
                      <Chip label="SALE" color="warning" size="small" />
                    )}
                  </Box>

                  {/* Price Change Indicator */}
                  {getPriceChange(item) && (
                    <Box sx={{ position: 'absolute', top: 12, right: 12 }}>
                      <Chip
                        icon={getPriceChange(item)! < 0 ? <TrendingDown /> : <TrendingUp />}
                        label={`${getPriceChange(item)! > 0 ? '+' : ''}${getPriceChange(item)!.toFixed(1)}%`}
                        color={getPriceChange(item)! < 0 ? 'success' : 'error'}
                        size="small"
                      />
                    </Box>
                  )}

                  {/* Action Buttons */}
                  <Box sx={{ position: 'absolute', bottom: 12, right: 12, display: 'flex', gap: 1 }}>
                    <Tooltip title={t('common.edit')}>
                      <IconButton
                        sx={{
                          backgroundColor: 'rgba(255, 255, 255, 0.9)',
                          '&:hover': { backgroundColor: 'white' },
                        }}
                        size="small"
                      >
                        <Visibility />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title={t('wishlist.removeFromWishlist')}>
                      <IconButton
                        sx={{
                          backgroundColor: 'rgba(255, 255, 255, 0.9)',
                          '&:hover': { backgroundColor: 'white' },
                        }}
                        size="small"
                        color="error"
                        onClick={() => handleRemoveFromWishlist(item.id)}
                      >
                        <Delete />
                      </IconButton>
                    </Tooltip>
                  </Box>
                </Box>

                {/* Product Info */}
                <CardContent sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column' }}>
                  <Typography variant="caption" color="primary.main" sx={{ fontWeight: 'bold', mb: 1 }}>
                    {item.brand}
                  </Typography>
                  
                  <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1, lineHeight: 1.3 }}>
                    {item.name}
                  </Typography>
                  
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2, flexGrow: 1 }}>
                    {item.description.length > 80 
                      ? `${item.description.substring(0, 80)}...` 
                      : item.description
                    }
                  </Typography>

                  {/* Rating */}
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <Rating value={item.rating} precision={0.1} size="small" readOnly />
                    <Typography variant="body2" color="text.secondary" sx={{ ml: 1 }}>
                      ({item.reviews})
                    </Typography>
                  </Box>

                  {/* Price */}
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                    <Typography variant="h6" sx={{ fontWeight: 'bold', color: 'primary.main' }}>
                      ${item.price}
                    </Typography>
                    {item.originalPrice && (
                      <Typography
                        variant="body2"
                        sx={{
                          textDecoration: 'line-through',
                          color: 'text.secondary',
                        }}
                      >
                        ${item.originalPrice}
                      </Typography>
                    )}
                  </Box>

                  {/* Stock Status */}
                  <Box sx={{ mb: 2 }}>
                    <Chip
                      label={item.stock > 10 ? t('products.inStock') : item.stock > 0 ? 'Low Stock' : t('products.outOfStock')}
                      color={item.stock > 10 ? 'success' : item.stock > 0 ? 'warning' : 'error'}
                      size="small"
                    />
                  </Box>

                  {/* Added Date */}
                  <Typography variant="caption" color="text.secondary" sx={{ mb: 2, display: 'block' }}>
                    Added on {new Date(item.addedDate).toLocaleDateString()}
                  </Typography>

                  {/* Action Buttons */}
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <Button
                      variant="contained"
                      startIcon={<ShoppingCart />}
                      onClick={() => handleAddToCart(item)}
                      disabled={item.stock === 0}
                      fullWidth
                      sx={{ borderRadius: 2 }}
                    >
                      {t('cart.addToCart')}
                    </Button>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Wishlist Summary */}
      {wishlistItems.length > 0 && (
        <Card sx={{ borderRadius: 3, mt: 4 }}>
          <CardContent>
            <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>
              {t('wishlist.title')}
            </Typography>
            <Grid container spacing={3}>
              <Grid item xs={12} sm={6} md={3}>
                <Box sx={{ textAlign: 'center' }}>
                  <Typography variant="h4" sx={{ fontWeight: 'bold', color: 'primary.main' }}>
                    {wishlistItems.length}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {t('common.info')}
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Box sx={{ textAlign: 'center' }}>
                  <Typography variant="h4" sx={{ fontWeight: 'bold', color: 'success.main' }}>
                    ${wishlistItems.reduce((sum, item) => sum + item.price, 0).toFixed(2)}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {t('cart.total')}
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Box sx={{ textAlign: 'center' }}>
                  <Typography variant="h4" sx={{ fontWeight: 'bold', color: 'warning.main' }}>
                    {wishlistItems.filter(item => item.discount).length}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Sale
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Box sx={{ textAlign: 'center' }}>
                  <Typography variant="h4" sx={{ fontWeight: 'bold', color: 'info.main' }}>
                    {wishlistItems.filter(item => item.isNew).length}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    New
                  </Typography>
                </Box>
              </Grid>
            </Grid>
          </CardContent>
        </Card>
      )}
    </Container>
  );
};

export default WishlistPage; 