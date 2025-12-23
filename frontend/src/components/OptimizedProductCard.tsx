import React, { useCallback } from 'react';
import {
  Card,
  CardContent,
  CardActions,
  CardMedia,
  Typography,
  Button,
  Box,
  Chip,
  Rating,
  IconButton,
  Tooltip,
  Zoom,
} from '@mui/material';
import {
  ShoppingCart,
  Favorite,
  FavoriteBorder,
  Visibility,
  Compare,
} from '@mui/icons-material';
import { Product } from '../services/productService';
import { formatPrice } from '../utils/priceUtils';

interface OptimizedProductCardProps {
  product: Product;
  onAddToCart: (productId: string) => void;
  onToggleFavorite: (productId: string) => void;
  onViewDetails: (productId: string) => void;
  onAddToCompare: (productId: string) => void;
  isFavorite: (productId: string) => boolean;
  isInCompare?: (productId: string) => boolean;
}

const OptimizedProductCard: React.FC<OptimizedProductCardProps> = React.memo(({
  product,
  onAddToCart,
  onToggleFavorite,
  onViewDetails,
  onAddToCompare,
  isFavorite,
  isInCompare
}) => {
  const {
    id,
    name,
    price,
    salePrice,
    imageUrl,
    category,
    brand,
    stockQuantity,
    rating: averageRating,
    reviewCount,
    images,
    isOnSale,
    isFeatured,
    isNew
  } = product;

  // Chỉ hiển thị giá sale khi isOnSale = true và salePrice < price
  const hasValidSale = isOnSale && salePrice && salePrice < price;
  const displayPrice = hasValidSale ? salePrice : price;
  const discountPercentage = hasValidSale 
    ? Math.round(((price - salePrice) / price) * 100) 
    : 0;
  const isOutOfStock = stockQuantity <= 0;
  const isFav = isFavorite(id);
  const inCompare = isInCompare ? isInCompare(id) : false;

  // Memoize handlers để tránh re-render
  const handleAddToCartClick = useCallback(() => {
    onAddToCart(id);
  }, [id, onAddToCart]);

  const handleToggleFavoriteClick = useCallback(() => {
    onToggleFavorite(id);
  }, [id, onToggleFavorite]);

  const handleViewDetailsClick = useCallback(() => {
    onViewDetails(id);
  }, [id, onViewDetails]);

  const handleAddToCompareClick = useCallback(() => {
    onAddToCompare(id);
  }, [id, onAddToCompare]);

  return (
    <Zoom in={true} timeout={300}>
      <Card
        sx={{
          height: '100%',
          minHeight: 400,
          display: 'flex',
          flexDirection: 'column',
          position: 'relative',
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
          '&:hover': {
            transform: 'translateY(-8px)',
            boxShadow: '0 12px 40px rgba(0,0,0,0.15)',
          },
          borderRadius: 3,
          border: 'none',
          backgroundColor: 'background.paper',
          boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
          overflow: 'hidden'
        }}
      >
        {/* Product Image */}
        <Box sx={{ position: 'relative', height: 160, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: '#f5f5f5' }}>
          <CardMedia
            component="img"
            image={images?.[0]?.imageUrl || imageUrl || 'https://via.placeholder.com/250x160/4f46e5/ffffff?text=No+Image'}
            alt={name}
            sx={{
              width: '70%',
              height: '70%',
              objectFit: 'contain',
              transition: 'transform 0.3s ease',
              '&:hover': {
                transform: 'scale(1.1)'
              }
            }}
          />
          
          {/* Badges */}
          <Box sx={{ position: 'absolute', top: 12, left: 12, display: 'flex', flexDirection: 'column', gap: 1 }}>
            {isNew && (
              <Chip
                label="NEW"
                size="small"
                sx={{ 
                  fontWeight: 'bold',
                  background: 'linear-gradient(45deg, #4caf50, #8bc34a)',
                  color: 'white',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.2)'
                }}
              />
            )}
            {isOnSale && (
              <Chip
                label={`-${discountPercentage}%`}
                size="small"
                sx={{ 
                  fontWeight: 'bold',
                  background: 'linear-gradient(45deg, #f44336, #ff5722)',
                  color: 'white',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.2)'
                }}
              />
            )}
            {isFeatured && (
              <Chip
                label="FEATURED"
                size="small"
                sx={{ 
                  fontWeight: 'bold',
                  background: 'linear-gradient(45deg, #2196f3, #21cbf3)',
                  color: 'white',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.2)'
                }}
              />
            )}
          </Box>

          {/* Action Buttons */}
          <Box sx={{ position: 'absolute', top: 12, right: 12, display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Tooltip title={isFav ? 'Remove from favorites' : 'Add to favorites'}>
              <IconButton
                size="small"
                onClick={handleToggleFavoriteClick}
                sx={{
                  backgroundColor: 'rgba(255, 255, 255, 0.95)',
                  backdropFilter: 'blur(10px)',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
                  '&:hover': {
                    backgroundColor: 'rgba(255, 255, 255, 1)',
                    transform: 'scale(1.1)'
                  },
                  transition: 'all 0.2s ease-in-out'
                }}
              >
                {isFav ? <Favorite color="error" /> : <FavoriteBorder />}
              </IconButton>
            </Tooltip>
            
            <Tooltip title={inCompare ? "Đã thêm vào so sánh - Click để xem" : "So sánh sản phẩm"}>
              <IconButton
                size="small"
                onClick={handleAddToCompareClick}
                sx={{
                  backgroundColor: inCompare ? 'primary.main' : 'rgba(255, 255, 255, 0.95)',
                  color: inCompare ? 'white' : 'primary.main',
                  backdropFilter: 'blur(10px)',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
                  '&:hover': {
                    backgroundColor: 'primary.main',
                    color: 'white',
                    transform: 'scale(1.1)'
                  },
                  transition: 'all 0.2s ease-in-out'
                }}
              >
                <Compare />
              </IconButton>
            </Tooltip>
          </Box>

          {/* Stock Status Overlay */}
          {isOutOfStock && (
            <Box
              sx={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                backgroundColor: 'rgba(0,0,0,0.7)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'white',
                fontWeight: 'bold',
                fontSize: '1.2rem'
              }}
            >
              OUT OF STOCK
            </Box>
          )}
        </Box>

        {/* Product Info */}
        <CardContent sx={{
          flexGrow: 1,
          p: 2,
          display: 'flex',
          flexDirection: 'column'
        }}>
          {/* Category & Brand */}
          <Box sx={{ mb: 1, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            {category && (
              <Chip
                label={category}
                size="small"
                variant="outlined"
                sx={{ fontSize: '0.75rem' }}
              />
            )}
            {brand && (
              <Chip
                label={brand}
                size="small"
                variant="outlined"
                sx={{ fontSize: '0.75rem' }}
              />
            )}
          </Box>

          {/* Product Name */}
          <Typography
            variant="h6"
            component="h3"
            sx={{
              fontSize: '0.85rem',
              fontWeight: 600,
              mb: 0.5,
              display: '-webkit-box',
              WebkitLineClamp: 2,
              WebkitBoxOrient: 'vertical',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              lineHeight: 1.2,
              color: 'text.primary'
            }}
          >
            {name}
          </Typography>

          {/* Rating */}
          {(averageRating || 0) > 0 && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
              <Rating
                value={averageRating || 0}
                precision={0.1}
                size="small"
                readOnly
                sx={{ color: '#ffc107' }}
              />
              <Typography variant="body2" color="text.secondary">
                ({reviewCount || 0})
              </Typography>
            </Box>
          )}

          {/* Price */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 1 }}>
            <Typography
              variant="h5"
              sx={{ 
                fontWeight: 'bold', 
                fontSize: '0.95rem',
                background: 'linear-gradient(45deg, #1976d2, #42a5f5)',
                backgroundClip: 'text',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent'
              }}
            >
              {formatPrice(displayPrice)}
            </Typography>
            {hasValidSale && (
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{ textDecoration: 'line-through', fontSize: '0.75rem' }}
              >
                {formatPrice(price)}
              </Typography>
            )}
          </Box>

          {/* Stock Status */}
          {!isOutOfStock && stockQuantity < 10 && (
            <Typography variant="caption" color="warning.main" sx={{ fontWeight: 'bold' }}>
              Only {stockQuantity} left in stock
            </Typography>
          )}
        </CardContent>

        {/* Actions */}
        <CardActions sx={{ p: 2, pt: 0, gap: 0.5 }}>
          <Button
            variant="contained"
            fullWidth
            onClick={handleAddToCartClick}
            disabled={isOutOfStock}
            startIcon={<ShoppingCart />}
            sx={{
              background: 'linear-gradient(45deg, #1976d2, #42a5f5)',
              mt: 'auto',
              '&:hover': {
                background: 'linear-gradient(45deg, #1565c0, #1976d2)',
              },
              '&:disabled': {
                background: '#e0e0e0',
                color: '#9e9e9e'
              },
              fontWeight: 'bold',
              py: 1,
              fontSize: { xs: '0.75rem', md: '0.8rem' }
            }}
          >
            {isOutOfStock ? 'Hết hàng' : 'Thêm vào giỏ'}
          </Button>
          
          <Button
            variant="outlined"
            fullWidth
            onClick={handleViewDetailsClick}
            startIcon={<Visibility />}
            sx={{
              borderColor: '#1976d2',
              color: '#1976d2',
              '&:hover': {
                borderColor: '#1565c0',
                backgroundColor: 'rgba(25, 118, 210, 0.04)'
              },
              fontWeight: 'bold',
              py: 1,
              fontSize: { xs: '0.75rem', md: '0.8rem' }
            }}
          >
            Xem chi tiết
          </Button>
        </CardActions>
      </Card>
    </Zoom>
  );
});

OptimizedProductCard.displayName = 'OptimizedProductCard';

export default OptimizedProductCard;
