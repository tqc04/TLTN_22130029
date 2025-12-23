import React from 'react';
import {
  Card,
  CardContent,
  CardActions,
  Typography,
  Button,
  Box,
  Chip,
  Rating,
  IconButton,
  Tooltip
} from '@mui/material';
import {
  Favorite,
  FavoriteBorder,
  ShoppingCart,
  Visibility,
  Compare
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import LazyImage from './LazyImage';
import { Product } from '../services/productService';
import { transformImageUrl } from '../utils/imageUtils';
import { formatPrice } from '../utils/priceUtils';

interface ProductCardProps {
  product: Product;
  onAddToCart?: (productId: string) => void;
  onToggleFavorite?: (productId: string) => void;
  onViewDetails?: (productId: string) => void;
  onAddToCompare?: (productId: string) => void;
  isFavorite?: boolean;
  isInCompare?: boolean;
  showActions?: boolean;
  variant?: 'default' | 'compact' | 'detailed';
}

const ProductCard: React.FC<ProductCardProps> = ({
  product,
  onAddToCart,
  onToggleFavorite,
  onViewDetails,
  onAddToCompare,
  isFavorite = false,
  isInCompare = false,
  showActions = true,
  variant = 'default'
}) => {
  const navigate = useNavigate();
  const {
    id,
    name,
    price,
    salePrice,
    rating: averageRating,
    reviewCount,
    images,
    isOnSale,
    isFeatured,
    stockQuantity,
    brand,
    category
  } = product;

  const displayPrice = salePrice || price;
  const discountPercentage = salePrice && price > salePrice 
    ? Math.round(((price - salePrice) / price) * 100) 
    : 0;

  const isOutOfStock = stockQuantity <= 0;

  const handleAddToCart = () => {
    if (!isOutOfStock && onAddToCart) {
      onAddToCart(id);
    }
  };

  const handleToggleFavorite = () => {
    if (onToggleFavorite) {
      onToggleFavorite(id);
    }
  };

  const handleViewDetails = () => {
    if (onViewDetails) {
      onViewDetails(id);
    } else {
      navigate(`/products/${id}`);
    }
  };

  const handleAddToCompare = () => {
    if (onAddToCompare) {
      onAddToCompare(id);
    }
  };

  const getCardHeight = () => {
    switch (variant) {
      case 'compact':
        return 200;
      case 'detailed':
        return 400;
      default:
        return 300;
    }
  };

  const getImageHeight = () => {
    switch (variant) {
      case 'compact':
        return 120;
      case 'detailed':
        return 200;
      default:
        return 250; // Tăng chiều cao để có tỷ lệ vuông đẹp hơn
    }
  };

  return (
    <Card
      sx={{
        height: getCardHeight(),
        display: 'flex',
        flexDirection: 'column',
        position: 'relative',
        transition: 'transform 0.3s ease-in-out, box-shadow 0.3s ease-in-out',
        '&:hover': {
          transform: 'translateY(-8px)',
          boxShadow: '0 8px 25px rgba(0,0,0,0.15)'
        },
        borderRadius: 3,
        border: 'none',
        backgroundColor: 'background.paper',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        overflow: 'hidden'
      }}
    >
      {/* Product Image with Lazy Loading */}
      <Box sx={{ position: 'relative', cursor: 'pointer' }} onClick={handleViewDetails}>
        <LazyImage
          src={transformImageUrl(images?.[0]?.imageUrl || product.imageUrl) || 'https://via.placeholder.com/300x200/4f46e5/ffffff?text=No+Image'}
          alt={name}
          width="100%"
          height={getImageHeight()}
          style={{
            objectFit: 'cover',
            borderTopLeftRadius: '12px',
            borderTopRightRadius: '12px'
          }}
          placeholder="https://via.placeholder.com/300x200/4f46e5/ffffff?text=Loading..."
          fadeIn={true}
          fadeInDuration={300}
        />
        
        {/* Badges */}
        <Box
          sx={{
            position: 'absolute',
            top: 8,
            left: 8,
            display: 'flex',
            flexDirection: 'column',
            gap: 1
          }}
        >
          {isOnSale && (
            <Chip
              label={`-${discountPercentage}%`}
              size="small"
              sx={{ 
                fontWeight: 'bold',
                background: 'linear-gradient(45deg, #f44336, #ff5722)',
                color: 'white',
                boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
              }}
            />
          )}
          {isFeatured && (
            <Chip
              label="Featured"
              size="small"
              sx={{ 
                fontWeight: 'bold',
                background: 'linear-gradient(45deg, #2196f3, #21cbf3)',
                color: 'white',
                boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
              }}
            />
          )}
        </Box>

        {/* Action Buttons */}
        <Box
          sx={{
            position: 'absolute',
            top: 8,
            right: 8,
            display: 'flex',
            flexDirection: 'column',
            gap: 1,
            opacity: 0.8,
            transition: 'opacity 0.3s ease-in-out',
            '&:hover': {
              opacity: 1
            }
          }}
        >
          <Tooltip title={isFavorite ? 'Remove from favorites' : 'Add to favorites'}>
            <IconButton
              size="small"
              onClick={handleToggleFavorite}
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
              {isFavorite ? <Favorite color="error" /> : <FavoriteBorder />}
            </IconButton>
          </Tooltip>
          
          <Tooltip title="Add to compare">
            <IconButton
              size="small"
              onClick={handleAddToCompare}
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
              <Compare color={isInCompare ? 'primary' : 'inherit'} />
            </IconButton>
          </Tooltip>
        </Box>

        {/* Stock Status Overlay */}
        {isOutOfStock && (
          <Box
            sx={{
              position: 'absolute',
              bottom: 0,
              left: 0,
              right: 0,
              backgroundColor: 'rgba(0, 0, 0, 0.7)',
              color: 'white',
              textAlign: 'center',
              padding: 1
            }}
          >
            <Typography variant="body2" fontWeight="bold">
              Out of Stock
            </Typography>
          </Box>
        )}
      </Box>

      {/* Product Content */}
      <CardContent sx={{ 
        flexGrow: 1, 
        padding: 3,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between'
      }}>
        {/* Brand and Category */}
        <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
          {brand && (
            <Typography variant="caption" color="text.secondary">
              {typeof brand === 'string' ? brand : String((brand as unknown as { name?: string }).name || '')}
            </Typography>
          )}
          {category && (
            <Typography variant="caption" color="text.secondary">
              • {typeof category === 'string' ? category : String((category as unknown as { name?: string }).name || '')}
            </Typography>
          )}
        </Box>

        {/* Product Name */}
        <Typography
          variant="h6"
          component="h3"
          onClick={handleViewDetails}
          sx={{
            fontSize: '1.1rem',
            fontWeight: 700,
            mb: 1,
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            lineHeight: 1.3,
            color: 'text.primary',
            cursor: 'pointer',
            '&:hover': {
              color: 'primary.main',
              textDecoration: 'underline'
            }
          }}
        >
          {name}
        </Typography>

        {/* Rating */}
        {averageRating > 0 && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
            <Rating
              value={averageRating}
              precision={0.1}
              size="small"
              readOnly
            />
            <Typography variant="caption" color="text.secondary">
              ({reviewCount})
            </Typography>
          </Box>
        )}

        {/* Price */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
          <Typography
            variant="h5"
            color="primary"
            sx={{ 
              fontWeight: 'bold', 
              fontSize: '1.3rem',
              background: 'linear-gradient(45deg, #1976d2, #42a5f5)',
              backgroundClip: 'text',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent'
            }}
          >
            {formatPrice(displayPrice)}
          </Typography>
          {salePrice && price > salePrice && (
            <Typography
              variant="body2"
              color="text.secondary"
              sx={{ textDecoration: 'line-through', fontSize: '0.875rem' }}
            >
              {formatPrice(price)}
            </Typography>
          )}
          {discountPercentage > 0 && (
            <Chip 
              label={`-${discountPercentage}%`} 
              size="small" 
              color="error" 
              sx={{ 
                fontSize: '0.75rem', 
                height: 24, 
                fontWeight: 'bold',
                background: 'linear-gradient(45deg, #f44336, #ff5722)',
                color: 'white'
              }}
            />
          )}
        </Box>

        {/* Stock Status */}
        {!isOutOfStock && stockQuantity < 10 && (
          <Typography variant="caption" color="warning.main">
            Only {stockQuantity} left in stock
          </Typography>
        )}
      </CardContent>

      {/* Actions */}
      {showActions && (
        <CardActions sx={{ padding: 2, paddingTop: 0, gap: 1 }}>
          <Button
            variant="contained"
            fullWidth
            onClick={handleAddToCart}
            disabled={isOutOfStock}
            startIcon={<ShoppingCart />}
            sx={{ 
              mb: 1,
              background: 'linear-gradient(45deg, #1976d2, #42a5f5)',
              '&:hover': {
                background: 'linear-gradient(45deg, #1565c0, #1976d2)',
              },
              '&:disabled': {
                background: '#e0e0e0',
                color: '#9e9e9e'
              }
            }}
          >
            {isOutOfStock ? 'Out of Stock' : 'Add to Cart'}
          </Button>
          
          <Button
            variant="outlined"
            fullWidth
            onClick={handleViewDetails}
            startIcon={<Visibility />}
            sx={{
              borderColor: '#1976d2',
              color: '#1976d2',
              '&:hover': {
                borderColor: '#1565c0',
                backgroundColor: 'rgba(25, 118, 210, 0.04)'
              }
            }}
          >
            View Details
          </Button>
        </CardActions>
      )}
    </Card>
  );
};

export default ProductCard;
