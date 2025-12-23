import React, { useState, useEffect } from 'react'
import {
  Container,
  Typography,
  Grid,
  Card,
  CardMedia,
  CardContent,
  CardActions,
  Button,
  Box,
  Alert,
  IconButton,
  Chip,
  Skeleton,
  Fab,
  Tooltip,
  TextField,
  InputAdornment,
  Paper
} from '@mui/material'
import {
  Favorite,
  FavoriteBorder,
  ShoppingCart,
  Share,
  Visibility,
  Search,
  Clear
} from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useCart } from '../contexts/CartContext'
import { useFavorites } from '../contexts/FavoritesContext'
import { useDebounce } from '../hooks/useDebounce'
import { useNProgress } from '../hooks/useNProgress'
import { formatPrice } from '../utils/priceUtils'
import { Product, apiService } from '../services/api'

const FavoritesPage: React.FC = () => {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()
  const { addToCart } = useCart()
  const { favorites, favoritesLoading, removeFromFavorites } = useFavorites()
  
  const [searchTerm, setSearchTerm] = useState('')
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null)
  const [enrichedProducts, setEnrichedProducts] = useState<Product[]>([])
  const [enriching, setEnriching] = useState(false)
  
  // Debounced search
  const debouncedSearchTerm = useDebounce(searchTerm, 300)
  
  // NProgress for loading
  useNProgress(favoritesLoading || enriching)

  // Enrich favorites with full product data
  useEffect(() => {
    const enrichFavorites = async () => {
      if (!favorites || favorites.length === 0) {
        setEnrichedProducts([])
        return
      }

      setEnriching(true)
      try {
        const enriched = await Promise.all(
          favorites.map(async (favorite) => {
            // If favorite already has full product data, use it
            if (favorite.category && favorite.category !== 'Uncategorized' && 
                favorite.brand && favorite.brand !== 'Unknown' &&
                favorite.stockQuantity !== undefined) {
              return favorite
            }

            // Otherwise, fetch full product data
            try {
              const productResponse = await apiService.getProduct(favorite.id)
              if (productResponse.success && productResponse.data) {
                // Merge favorite data with product data, preferring product data
                return {
                  ...favorite,
                  ...productResponse.data,
                  // Keep favorite's image if product doesn't have one
                  imageUrl: productResponse.data.imageUrl || 
                           productResponse.data.images?.[0]?.imageUrl || 
                           favorite.imageUrl,
                  // Keep favorite's price if product price is 0
                  price: productResponse.data.price || favorite.price,
                } as Product
              }
            } catch (error) {
              console.warn(`Failed to fetch product ${favorite.id}:`, error)
            }
            return favorite
          })
        )
        setEnrichedProducts(enriched)
      } catch (error) {
        console.error('Failed to enrich favorites:', error)
        setEnrichedProducts(favorites)
      } finally {
        setEnriching(false)
      }
    }

    if (!favoritesLoading) {
      enrichFavorites()
    }
  }, [favorites, favoritesLoading])

  // Filter favorites based on search term
  const filteredFavorites = enrichedProducts.filter(product =>
    product.name.toLowerCase().includes(debouncedSearchTerm.toLowerCase()) ||
    (product.description || '').toLowerCase().includes(debouncedSearchTerm.toLowerCase()) ||
    (product.category || '').toLowerCase().includes(debouncedSearchTerm.toLowerCase()) ||
    (product.brand || '').toLowerCase().includes(debouncedSearchTerm.toLowerCase())
  )

  const handleRemoveFromFavorites = async (productId: string) => {
    await removeFromFavorites(productId)
  }

  const handleAddToCart = async (product: Product) => {
    try {
      await addToCart(product.id, 1)
      setMessage({ type: 'success', text: 'Added to cart successfully' })
      setTimeout(() => setMessage(null), 3000)
    } catch (error: unknown) {
      console.error('Failed to add to cart:', error)
      setMessage({ type: 'error', text: 'Failed to add to cart' })
      setTimeout(() => setMessage(null), 3000)
    }
  }

  const handleProductClick = (productId: string) => {
    navigate(`/products/${productId}`)
  }

  const handleShareProduct = (product: Product) => {
    if (navigator.share) {
      navigator.share({
        title: product.name,
        text: product.description,
        url: `${window.location.origin}/products/${product.id}`
      })
    } else {
      // Fallback for browsers that don't support Web Share API
      navigator.clipboard.writeText(`${window.location.origin}/products/${product.id}`)
      setMessage({
        type: 'success',
        text: 'Product link copied to clipboard'
      })
      setTimeout(() => setMessage(null), 3000)
    }
  }

  const ProductCard: React.FC<{ product: Product }> = ({ product }) => (
    <Card 
      sx={{ 
        height: '100%', 
        display: 'flex', 
        flexDirection: 'column',
        position: 'relative',
        '&:hover': {
          boxShadow: 6,
          transform: 'translateY(-2px)',
          transition: 'all 0.3s ease-in-out'
        }
      }}
    >
      {/* Favorite Button */}
      <IconButton
        sx={{
          position: 'absolute',
          top: 8,
          right: 8,
          backgroundColor: 'rgba(255, 255, 255, 0.9)',
          zIndex: 1,
          '&:hover': {
            backgroundColor: 'rgba(255, 255, 255, 1)'
          }
        }}
        onClick={() => handleRemoveFromFavorites(product.id)}
      >
        <Favorite color="error" />
      </IconButton>

      {/* Product Image */}
      <CardMedia
        component="img"
        height="200"
        image={product.imageUrl || `https://via.placeholder.com/300x200?text=${encodeURIComponent(product.name)}`}
        alt={product.name}
        sx={{ 
          cursor: 'pointer',
          objectFit: 'cover'
        }}
        onClick={() => handleProductClick(product.id)}
      />

      {/* Product Content */}
      <CardContent sx={{ flexGrow: 1 }}>
        <Typography 
          gutterBottom 
          variant="h6" 
          component="h2"
          sx={{ 
            cursor: 'pointer',
            '&:hover': { color: 'primary.main' }
          }}
          onClick={() => handleProductClick(product.id)}
        >
          {product.name}
        </Typography>
        
        <Typography 
          variant="body2" 
          color="text.secondary" 
          sx={{ 
            mb: 2,
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
            overflow: 'hidden'
          }}
        >
          {product.description}
        </Typography>

        <Box display="flex" gap={1} mb={2}>
          <Chip 
            label={product.category} 
            size="small" 
            color="primary" 
            variant="outlined" 
          />
          <Chip 
            label={product.brand} 
            size="small" 
            variant="outlined" 
          />
        </Box>

        <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
          <Typography variant="h6" color="primary">
            {formatPrice(product.price)}
          </Typography>
          {product.rating && (
            <Typography variant="body2" color="text.secondary">
              ⭐ {product.rating}/5
            </Typography>
          )}
        </Box>

        <Typography 
          variant="body2" 
          color={product.stockQuantity > 0 ? 'success.main' : 'error.main'}
        >
          {product.stockQuantity > 0 
            ? `${product.stockQuantity} in stock` 
            : 'Out of stock'
          }
        </Typography>
      </CardContent>

      {/* Actions */}
      <CardActions sx={{ justifyContent: 'space-between', px: 2, pb: 2 }}>
        <Button
          variant="contained"
          startIcon={<ShoppingCart />}
          onClick={() => handleAddToCart(product)}
          disabled={product.stockQuantity <= 0}
          size="small"
        >
          Add to Cart
        </Button>
        
        <Box>
          <Tooltip title="View Details">
            <IconButton 
              size="small"
              onClick={() => handleProductClick(product.id)}
            >
              <Visibility />
            </IconButton>
          </Tooltip>
          <Tooltip title="Share">
            <IconButton 
              size="small"
              onClick={() => handleShareProduct(product)}
            >
              <Share />
            </IconButton>
          </Tooltip>
        </Box>
      </CardActions>
    </Card>
  )

  const LoadingSkeleton: React.FC = () => (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Skeleton variant="rectangular" height={200} />
      <CardContent sx={{ flexGrow: 1 }}>
        <Skeleton variant="text" height={32} width="80%" />
        <Skeleton variant="text" height={20} width="100%" />
        <Skeleton variant="text" height={20} width="60%" />
        <Box display="flex" gap={1} my={1}>
          <Skeleton variant="rounded" height={24} width={60} />
          <Skeleton variant="rounded" height={24} width={60} />
        </Box>
        <Skeleton variant="text" height={24} width="40%" />
      </CardContent>
      <CardActions sx={{ px: 2, pb: 2 }}>
        <Skeleton variant="rounded" height={36} width={120} />
        <Skeleton variant="circular" height={40} width={40} />
      </CardActions>
    </Card>
  )

  if (!isAuthenticated) {
    return (
      <Container maxWidth="md" sx={{ mt: 4, pt: '40px' }}>
        <Alert severity="warning">
          Please log in to view your favorite products.
        </Alert>
      </Container>
    )
  }

  return (
    <Container maxWidth="xl" sx={{ mt: 4, mb: 4, pt: '40px' }}>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={4}>
        <Box>
          <Typography variant="h4" component="h1" gutterBottom>
            My Favorites
          </Typography>
          <Typography variant="body1" color="text.secondary">
            {enrichedProducts.length > 0 
              ? `${enrichedProducts.length} favorite ${enrichedProducts.length === 1 ? 'product' : 'products'}`
              : 'No favorite products yet'
            }
          </Typography>
        </Box>
      </Box>

      {/* Search Bar */}
      {enrichedProducts.length > 0 && (
        <Paper elevation={1} sx={{ mb: 4, p: 2 }}>
          <TextField
            fullWidth
            placeholder="Search your favorites..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Search color="action" />
                </InputAdornment>
              ),
              endAdornment: searchTerm && (
                <InputAdornment position="end">
                  <IconButton size="small" onClick={() => setSearchTerm('')}>
                    <Clear />
                  </IconButton>
                </InputAdornment>
              ),
            }}
            sx={{
              '& .MuiOutlinedInput-root': {
                borderRadius: 2,
                backgroundColor: 'background.default',
              },
            }}
          />
        </Paper>
      )}

      {/* Messages */}
      {message && (
        <Alert severity={message.type} sx={{ mb: 3 }}>
          {message.text}
        </Alert>
      )}

      {/* Content */}
      {favoritesLoading ? (
        <Grid container spacing={3}>
          {[...Array(8)].map((_, index) => (
            <Grid item xs={12} sm={6} md={4} lg={3} key={index}>
              <LoadingSkeleton />
            </Grid>
          ))}
        </Grid>
      ) : enrichedProducts.length === 0 ? (
        <Box textAlign="center" py={8}>
          <FavoriteBorder sx={{ fontSize: 100, color: 'text.secondary', mb: 2 }} />
          <Typography variant="h5" gutterBottom>
            No Favorites Yet
          </Typography>
          <Typography variant="body1" color="text.secondary" mb={3}>
            Start browsing products and add items to your favorites to see them here.
          </Typography>
          <Button
            variant="contained"
            size="large"
            onClick={() => navigate('/products')}
          >
            Browse Products
          </Button>
        </Box>
      ) : filteredFavorites.length === 0 ? (
        <Box textAlign="center" py={8}>
          <Search sx={{ fontSize: 100, color: 'text.secondary', mb: 2 }} />
          <Typography variant="h5" gutterBottom>
            No Results Found
          </Typography>
          <Typography variant="body1" color="text.secondary" mb={3}>
            Try adjusting your search terms to find what you're looking for.
          </Typography>
          <Button
            variant="outlined"
            onClick={() => setSearchTerm('')}
            startIcon={<Clear />}
          >
            Clear Search
          </Button>
        </Box>
      ) : (
        <Grid container spacing={3}>
          {filteredFavorites.map((product) => (
            <Grid item xs={12} sm={6} md={4} lg={3} key={product.id}>
              <ProductCard product={product} />
            </Grid>
          ))}
        </Grid>
      )}

      {/* Floating Action Button */}
      <Fab
        color="primary"
        aria-label="browse products"
        sx={{
          position: 'fixed',
          bottom: 16,
          right: 16,
        }}
        onClick={() => navigate('/products')}
      >
        <ShoppingCart />
      </Fab>
    </Container>
  )
}

export default FavoritesPage 