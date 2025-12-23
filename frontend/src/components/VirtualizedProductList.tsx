import React from 'react';
import {
  Box,
  Typography,
  CircularProgress,
  Alert,
  Grid
} from '@mui/material';
import ProductCard from './ProductCard';
import { Product } from '../services/productService';

interface VirtualizedProductListProps {
  products: Product[];
  loading?: boolean;
  error?: string;
  onAddToCart?: (productId: string) => void;
  onToggleFavorite?: (productId: string) => void;
  onViewDetails?: (productId: string) => void;
  onAddToCompare?: (productId: string) => void;
  favorites?: number[];
  compareList?: number[];
  columns?: number;
  height?: number;
  itemHeight?: number;
  itemWidth?: number;
  showPagination?: boolean;
  onLoadMore?: () => void;
  hasMore?: boolean;
}

// Simple grid item component
const GridItem: React.FC<{
  product: Product;
  onAddToCart?: (productId: string) => void;
  onToggleFavorite?: (productId: string) => void;
  onViewDetails?: (productId: string) => void;
  onAddToCompare?: (productId: string) => void;
  isFavorite?: boolean;
  isInCompare?: boolean;
  showActions?: boolean;
}> = ({
  product,
  onAddToCart,
  onToggleFavorite,
  onViewDetails,
  onAddToCompare,
  isFavorite = false,
  isInCompare = false,
  showActions = true
}) => {
  return (
    <Grid item xs={12} sm={6} md={4} lg={4}>
      <ProductCard
        product={product}
        onAddToCart={onAddToCart}
        onToggleFavorite={onToggleFavorite}
        onViewDetails={onViewDetails}
        onAddToCompare={onAddToCompare}
        isFavorite={isFavorite}
        isInCompare={isInCompare}
        showActions={showActions}
        variant="default"
      />
    </Grid>
  );
};

const VirtualizedProductList: React.FC<VirtualizedProductListProps> = ({
  products,
  loading = false,
  error,
  onAddToCart,
  onToggleFavorite,
  onViewDetails,
  onAddToCompare,
  favorites = [],
  compareList = [],
  columns = 4,
  height = 600,
  itemHeight = 400,
  itemWidth = 300,
  showPagination = false,
  onLoadMore,
  hasMore = false
}) => {
  // Mark unused props with underscore to satisfy lint rules
  void columns; void height; void itemHeight; void itemWidth; void showPagination; void onLoadMore; void hasMore;
  // Display all products instead of pagination
  const displayedProducts = products;

  if (error) {
    return (
      <Box sx={{ padding: 3, textAlign: 'center' }}>
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      </Box>
    );
  }

  if (loading && products.length === 0) {
    return (
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: 400 
      }}>
        <CircularProgress />
      </Box>
    );
  }

  if (products.length === 0) {
    return (
      <Box sx={{ padding: 3, textAlign: 'center' }}>
        <Typography variant="h6" color="text.secondary">
          No products found
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ width: '100%' }}>
      <Grid container spacing={3} sx={{ padding: 2 }}>
        {displayedProducts.map((product) => (
          <GridItem
            key={product.id}
            product={product}
            onAddToCart={onAddToCart}
            onToggleFavorite={onToggleFavorite}
            onViewDetails={onViewDetails}
            onAddToCompare={onAddToCompare}
            isFavorite={favorites.includes(product.id)}
            isInCompare={compareList.includes(product.id)}
            showActions={true}
          />
        ))}
      </Grid>
    </Box>
  );
};

export default VirtualizedProductList;
