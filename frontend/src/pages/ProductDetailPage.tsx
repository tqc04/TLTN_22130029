import React, { useState, useEffect } from 'react';
import { transformImageUrl } from '../utils/imageUtils';
import {
  Container,
  Grid,
  Card,
  CardContent,
  CardMedia,
  Typography,
  Button,
  Box,
  Rating,
  TextField,
  Avatar,
  Chip,
  Alert,
  Skeleton,
  List,
  ListItem,
  ListItemText,
  IconButton,
  Tabs,
  Tab,
  Paper,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableRow,
  Breadcrumbs,
  Link,
} from '@mui/material';
import {
  Star,
  ShoppingCart,
  Favorite,
  FavoriteBorder,
  Share,
  Compare,
  Add,
  Remove,
  Send,
  Close,
  ThumbUp,
  ThumbDown,
  Verified,
  Check,
  Home,
  NavigateNext,
  Security,
  LocalShipping,
  Support,
  Refresh,
  Facebook,
  Twitter,
  WhatsApp,
  ZoomIn,
  ZoomOut,
  ChevronLeft,
  ChevronRight,
} from '@mui/icons-material';
import { useParams, useNavigate, Link as RouterLink } from 'react-router-dom';
import { useCart } from '../contexts/CartContext';
import { useAuth } from '../contexts/AuthContext';
import { apiService, Product, ProductVariant } from '../services/api';

interface ProductWithImages extends Product {
  images?: Array<{
    id: number
    imageUrl: string
    alt?: string
    isPrimary?: boolean
  }>
  categoryId?: number
  brandId?: number
}
import { useFavorites } from '../contexts/FavoritesContext';

interface Review {
  id: number;
  userId: string;
  userName: string;
  userAvatar: string;
  rating: number;
  title: string;
  content: string;
  date: string;
  helpful: number;
  notHelpful: number;
  verified: boolean;
  images?: string[];
  pros?: string[];
  cons?: string[];
}

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`product-tabpanel-${index}`}
      aria-labelledby={`product-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const ProductDetailPage: React.FC = () => {
  const { productId } = useParams<{ productId: string }>();
  const navigate = useNavigate();
  const { addToCart } = useCart();
  const { user } = useAuth();
  const { toggleFavorite, isFavorite } = useFavorites();
  
  const [product, setProduct] = useState<ProductWithImages | null>(null);
  const [variants, setVariants] = useState<ProductVariant[]>([]);
  const [selectedVariant, setSelectedVariant] = useState<ProductVariant | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [relatedProducts, setRelatedProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const [selectedImage, setSelectedImage] = useState(0);
  
  // Handle image change with animation
  const handleImageChange = (index: number) => {
    setImageFade(false);
    setTimeout(() => {
      setSelectedImage(index);
      setImageFade(true);
    }, 150);
  };
  const [tabValue, setTabValue] = useState(0);
  const [compareItems, setCompareItems] = useState<string[]>([]);
  const [openReviewDialog, setOpenReviewDialog] = useState(false);
  const [openCompareDialog, setOpenCompareDialog] = useState(false);
  const [openShareDialog, setOpenShareDialog] = useState(false);
  const [imageZoomOpen, setImageZoomOpen] = useState(false);
  const [zoomedImage, setZoomedImage] = useState('');
  const [zoomLevel, setZoomLevel] = useState(1);
  const [imagePosition, setImagePosition] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [imageFade, setImageFade] = useState(true);
  const [reviewForm, setReviewForm] = useState({
    rating: 0,
    title: '',
    content: '',
    pros: '',
    cons: '',
  });


  useEffect(() => {
    if (!productId) return;
    
    const loadProductData = async () => {
      try {
        setLoading(true);
        
        // Load product details
        const productResponse = await apiService.getProduct(productId);
        const productData = productResponse.data;

        // If category and brand are IDs, fetch their names
        if (productData.categoryId && !productData.category) {
          try {
            // TODO: Fetch category name from category service
            productData.category = `Category ${productData.categoryId}`;
          } catch (error) {
            console.warn('Could not fetch category name:', error);
          }
        }

        if (productData.brandId && !productData.brand) {
          try {
            // TODO: Fetch brand name from brand service
            productData.brand = `Brand ${productData.brandId}`;
          } catch (error) {
            console.warn('Could not fetch brand name:', error);
          }
        }

        // Load product images, variants, reviews, and summary in parallel for better performance
        const [imagesResponse, variantsResponse, reviewsResponse, summaryResponse] = await Promise.allSettled([
          apiService.getProductImages(productId).catch(() => ({ success: false, data: [] })),
          apiService.getProductVariants(productId).catch(() => ({ success: false, data: [] })),
          apiService.getProductReviews(productId, 0, 10).catch(() => ({ success: false, data: [] })),
          apiService.getReviewSummary(productId).catch(() => ({ success: false, data: null }))
        ]);

        // Process images
        if (imagesResponse.status === 'fulfilled' && imagesResponse.value.success && imagesResponse.value.data) {
          productData.images = imagesResponse.value.data.map((img: any) => ({
            id: img.id,
            imageUrl: img.imageUrl,
            isPrimary: img.isPrimary || false,
            displayOrder: img.displayOrder || 0,
            altText: img.altText || productData.name
          }));
        }

        setProduct(productData);
        
        // Process variants
        if (variantsResponse.status === 'fulfilled' && variantsResponse.value.success) {
          const variants = variantsResponse.value.data || [];
          setVariants(variants);
          if (variants.length > 0) {
            const defaultVariant = variants.find((v: ProductVariant) => v.isDefault) || variants[0];
            setSelectedVariant(defaultVariant);
          }
        }

        // Process reviews
        if (reviewsResponse.status === 'fulfilled' && reviewsResponse.value.success && reviewsResponse.value.data) {
          const reviewsData = (reviewsResponse.value.data as any).content || reviewsResponse.value.data || [];
          const transformedReviews: Review[] = reviewsData.map((review: any, index: number) => ({
            id: review.id || index + 1,
            userId: review.userId || 1,
            userName: review.userName || review.user?.firstName + ' ' + review.user?.lastName || 'User',
            userAvatar: review.userAvatar || review.user?.avatarUrl || review.user?.profileImageUrl || null,
            rating: review.rating || 5,
            title: review.title || 'Review',
            content: review.content || review.comment || 'No content available',
            date: review.date || review.createdAt ? new Date(review.createdAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
            helpful: review.helpful || 0,
            notHelpful: review.notHelpful || 0,
            verified: review.verified || review.isVerifiedPurchase || false,
            images: review.images || [],
            pros: Array.isArray(review.pros) ? review.pros : (review.pros ? review.pros.split(',').map((p: string) => p.trim()) : []),
            cons: Array.isArray(review.cons) ? review.cons : (review.cons ? review.cons.split(',').map((c: string) => c.trim()) : []),
          }));
          setReviews(transformedReviews);
        }

        // Process review summary
        if (summaryResponse.status === 'fulfilled' && summaryResponse.value.success && summaryResponse.value.data) {
          const summary = summaryResponse.value.data as any;
          if (productData && summary.averageRating) {
            productData.averageRating = summary.averageRating;
            productData.reviewCount = summary.totalReviews;
            setProduct({ ...productData }); // Update product with new rating
          }
        }

        // Load similar products using Content-Based Filtering (Recommendation API)
        try {
          const similarResponse = await apiService.getSimilarProducts(productId, 8);
          if (similarResponse.success && similarResponse.data && Array.isArray(similarResponse.data)) {
            // Transform ProductRecommendation to Product format and filter out current product
            const transformed = similarResponse.data
              .filter((rec: any) => {
                const recId = rec.productId || rec.id;
                return recId && recId !== productId;
              })
              .map((rec: any) => {
                const product: Product = {
                  id: rec.productId || rec.id,
                  name: rec.productName || rec.name || 'Sản phẩm',
                  price: rec.price ? (typeof rec.price === 'number' ? rec.price : parseFloat(rec.price)) : 0,
                  salePrice: rec.salePrice || undefined,
                  imageUrl: rec.productImage || rec.imageUrl || rec.image || '',
                  description: rec.description || '',
                  category: rec.category || '',
                  brand: rec.brand || '',
                  averageRating: rec.averageRating || rec.rating || 0,
                  reviewCount: rec.reviewCount || rec.reviews || 0,
                  categoryId: rec.categoryId || undefined,
                  brandId: rec.brandId || undefined,
                  stockQuantity: rec.stockQuantity || 0,
                  sku: rec.sku || '',
                  isActive: true,
                  createdAt: rec.createdAt || new Date().toISOString(),
                  updatedAt: rec.updatedAt || new Date().toISOString()
                };
                return product;
              });
            setRelatedProducts(transformed.slice(0, 4)); // Limit to 4 products
          } else {
            // Fallback to old API if recommendation service is not available
            try {
              const relatedResponse = await apiService.getRelatedProducts(productId, 4);
              if (relatedResponse.success && relatedResponse.data) {
                const filtered = relatedResponse.data
                  .filter((p: Product) => p.id !== productId)
                  .map((p: Product) => {
                    if (p.categoryId && !p.category) {
                      p.category = `Category ${p.categoryId}`;
                    }
                    if (p.brandId && !p.brand) {
                      p.brand = `Brand ${p.brandId}`;
                    }
                    return p;
                  });
                setRelatedProducts(filtered);
              }
            } catch (fallbackError) {
              console.error('Error loading fallback related products:', fallbackError);
            }
          }
        } catch (error) {
          console.error('Error loading similar products:', error);
          // Fallback to old API
          try {
            const relatedResponse = await apiService.getRelatedProducts(productId, 4);
            if (relatedResponse.success && relatedResponse.data) {
              const filtered = relatedResponse.data
                .filter((p: Product) => p.id !== productId);
              setRelatedProducts(filtered);
            }
          } catch (fallbackError) {
            console.error('Error loading fallback related products:', fallbackError);
          }
        }

        setLoading(false);
        
        // Log user behavior for AI recommendation
        if (user && productResponse.data?.id) {
          try {
            await apiService.logUserBehavior(user.id, productResponse.data.id, 'VIEW');
          } catch (error) {
          // Behavior tracking is non-critical but log for observability
          console.error('Failed to track VIEW behavior:', error);
          }
        }
      } catch (error) {
        console.error('Error loading product:', error);
        setLoading(false);
      }
    };

    if (productId) {
      loadProductData();
    }
  }, [productId, user]);

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const handleAddToCart = () => {
    if (product) {
      // Always use product.id, and include variantId if a variant is selected
      const variantId = selectedVariant?.id;
      addToCart(product.id, quantity, variantId);
      if (user) {
        void apiService.logUserBehavior(user.id, product.id, 'ADD_TO_CART');
      }
    }
  };

  const handleVariantSelect = (variant: ProductVariant) => {
    setSelectedVariant(variant);
    setQuantity(1); // Reset quantity when variant changes
  };

  const getAvailableColors = () => {
    const colors = new Set(variants.map(v => v.color).filter(Boolean));
    return Array.from(colors);
  };

  const getAvailableSizes = () => {
    const sizes = new Set(variants.map(v => v.size).filter(Boolean));
    return Array.from(sizes);
  };

  const getCurrentPrice = () => {
    // Nếu có variant được chọn, dùng giá của variant
    if (selectedVariant) {
      return selectedVariant.price || 0;
    }
    // Chỉ dùng salePrice nếu isOnSale = true và salePrice < price
    if (product?.isOnSale && product?.salePrice && product.salePrice < product.price) {
      return product.salePrice;
    }
    return product?.price || 0;
  };

  const getOriginalPrice = () => {
    // Lấy giá gốc (không phải salePrice)
    if (selectedVariant) {
      return selectedVariant.price || 0;
    }
    return product?.price || 0;
  };

  const getCurrentStock = () => {
    return selectedVariant?.stockQuantity || product?.stockQuantity || 0;
  };

  const handleToggleFavorite = async () => {
    if (product) {
      await toggleFavorite(product.id);
    }
  };

  const handleAddToCompare = () => {
    if (product) {
      setCompareItems(prev => 
        prev.includes(product.id) 
          ? prev.filter(id => id !== product.id)
          : [...prev, product.id]
      );
    }
  };

  const handleQuantityChange = (increment: boolean) => {
    if (increment) {
      setQuantity(prev => Math.min(prev + 1, getCurrentStock()));
    } else {
      setQuantity(prev => Math.max(prev - 1, 1));
    }
  };

  const handleReviewSubmit = async () => {
    // Check if user is logged in
    if (!user) {
      alert('Vui lòng đăng nhập để viết đánh giá!');
      navigate('/login');
      return;
    }

    if (!product) return;

    // Validate review form
    if (!reviewForm.rating || reviewForm.rating < 1 || reviewForm.rating > 5) {
      alert('Vui lòng chọn số sao đánh giá (1-5)!');
      return;
    }

    if (!reviewForm.title || reviewForm.title.trim().length < 5) {
      alert('Tiêu đề đánh giá phải có ít nhất 5 ký tự!');
      return;
    }

    if (!reviewForm.content || reviewForm.content.trim().length < 10) {
      alert('Nội dung đánh giá phải có ít nhất 10 ký tự!');
      return;
    }

    try {
      const reviewData: any = {
        userId: user.id,
        productId: product.id,
        rating: reviewForm.rating,
        title: reviewForm.title.trim(),
        content: reviewForm.content.trim(),
      };

      // Only add pros and cons if they have content
      if (reviewForm.pros && reviewForm.pros.trim()) {
        reviewData.pros = reviewForm.pros.trim();
      }
      if (reviewForm.cons && reviewForm.cons.trim()) {
        reviewData.cons = reviewForm.cons.trim();
      }

      const response = await apiService.createReview(reviewData);

      if (!response.success) {
        throw new Error(response.message || 'Failed to create review');
      }

      // Reset form and close dialog
      setReviewForm({
        rating: 0,
        title: '',
        content: '',
        pros: '',
        cons: '',
      });
      setOpenReviewDialog(false);

      // Reload reviews from server to show the new review immediately
      if (productId) {
        try {
          const reviewsResponse = await apiService.getProductReviews(productId);
          if (reviewsResponse.success && reviewsResponse.data) {
            const reviewsData = (reviewsResponse.data as any).content || reviewsResponse.data || [];
            const transformedReviews: Review[] = reviewsData.map((review: any, index: number) => ({
              id: review.id || index + 1,
              userId: review.userId || user.id,
              userName: review.userName || review.user?.firstName + ' ' + review.user?.lastName || 'User',
              userAvatar: review.userAvatar || review.user?.avatarUrl || review.user?.profileImageUrl || null,
              rating: review.rating || 5,
              title: review.title || 'Review',
              content: review.content || review.comment || 'No content available',
              date: review.date || review.createdAt ? new Date(review.createdAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
              helpful: review.helpful || review.helpfulVotes || 0,
              notHelpful: review.notHelpful || 0,
              verified: review.verified || review.isVerifiedPurchase || false,
              images: review.images || [],
              pros: Array.isArray(review.pros) ? review.pros : (review.pros ? review.pros.split(',').map((p: string) => p.trim()) : []),
              cons: Array.isArray(review.cons) ? review.cons : (review.cons ? review.cons.split(',').map((c: string) => c.trim()) : []),
            }));
            setReviews(transformedReviews);
          }

          // Reload review summary to update rating
          const summaryResponse = await apiService.getReviewSummary(productId);
          if (summaryResponse.success && summaryResponse.data) {
            const summary = summaryResponse.data as any;
            if (product && summary.averageRating) {
              setProduct({
                ...product,
                averageRating: summary.averageRating,
                reviewCount: summary.totalReviews,
              });
            }
          }
        } catch (error) {
          console.error('Error reloading reviews:', error);
        }
      }

      // Show success message
      alert('✅ Đánh giá của bạn đã được gửi thành công!\n\nĐánh giá đã được hiển thị ngay trên trang sản phẩm để các khách hàng khác có thể xem.');
    } catch (error: any) {
      console.error('Error submitting review:', error);
      
      // Show specific error message
      if (error.response?.status === 403) {
        alert('❌ Bạn cần đăng nhập để viết đánh giá!\n\nVui lòng đăng nhập và thử lại.');
        navigate('/login');
      } else if (error.response?.status === 400) {
        alert('❌ Có lỗi xảy ra: ' + (error.response.data?.error || 'Vui lòng kiểm tra lại thông tin đánh giá!'));
      } else {
        alert('❌ Không thể gửi đánh giá!\n\nVui lòng thử lại sau hoặc liên hệ admin nếu lỗi vẫn tiếp tục.');
      }
    }
  };

  const handleHelpful = async (reviewId: number) => {
    try {
      await apiService.markReviewHelpful(reviewId);
      setReviews(prev =>
        prev.map(review =>
          review.id === reviewId
            ? { ...review, helpful: review.helpful + 1 }
            : review
        )
      );
    } catch (error) {
      console.error('Error marking review helpful:', error);
    }
  };

  const handleNotHelpful = (reviewId: number) => {
    setReviews(prev =>
      prev.map(review =>
        review.id === reviewId
          ? { ...review, notHelpful: review.notHelpful + 1 }
          : review
      )
    );
  };

  const handleShare = (platform: string) => {
    if (!product) return;

    const url = window.location.href;
    const text = `Check out this product: ${product.name}`;
    const encodedUrl = encodeURIComponent(url);
    const encodedText = encodeURIComponent(text);

    let shareUrl = '';

    switch (platform) {
      case 'facebook':
        shareUrl = `https://www.facebook.com/sharer/sharer.php?u=${encodedUrl}`;
        break;
      case 'twitter':
        shareUrl = `https://twitter.com/intent/tweet?url=${encodedUrl}&text=${encodedText}`;
        break;
      case 'whatsapp':
        shareUrl = `https://wa.me/?text=${encodedText} ${encodedUrl}`;
        break;
      default:
        // Copy to clipboard as fallback
        navigator.clipboard.writeText(url);
        return;
    }

    window.open(shareUrl, '_blank', 'width=600,height=400');
    setOpenShareDialog(false);
  };

  if (loading) {
    return (
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Grid container spacing={4}>
          <Grid item xs={12} md={6}>
            <Skeleton variant="rectangular" height={400} />
          </Grid>
          <Grid item xs={12} md={6}>
            <Skeleton variant="text" height={40} />
            <Skeleton variant="text" height={30} />
            <Skeleton variant="text" height={30} />
            <Skeleton variant="text" height={100} />
          </Grid>
        </Grid>
      </Container>
    );
  }

  if (!product) {
    return (
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Alert severity="error" sx={{ borderRadius: 3 }}>
          Không tìm thấy sản phẩm.
        </Alert>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      {/* Breadcrumbs */}
      <Breadcrumbs separator={<NavigateNext fontSize="small" />} sx={{ mb: 3 }}>
        <Link component={RouterLink} to="/" underline="hover" color="inherit">
          <Home fontSize="small" sx={{ mr: 0.5 }} />
          Trang chủ
        </Link>
        <Link component={RouterLink} to="/products" underline="hover" color="inherit">
          Sản phẩm
        </Link>
        <Typography color="text.primary">{product.name}</Typography>
      </Breadcrumbs>

      {/* Product Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h3" component="h1" sx={{ fontWeight: 'bold', mb: 1 }}>
          {product.name}
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
          {product.brand && (
            <Chip
              label={product.brand}
              variant="outlined"
              size="small"
              sx={{ fontWeight: 600 }}
            />
          )}
          {product.category && (
            <Chip
              label={product.category}
              variant="outlined"
              size="small"
              color="primary"
              sx={{ fontWeight: 600 }}
            />
          )}
        </Box>
      </Box>

      <Grid container spacing={4}>
        {/* Product Images */}
        <Grid item xs={12} md={6}>
          <Box
            sx={{
              position: 'relative',
              borderRadius: 3,
              mb: 2,
              overflow: 'hidden',
              cursor: 'pointer',
              height: 400,
              backgroundColor: '#f5f5f5',
              '&:hover': {
                boxShadow: '0 8px 25px rgba(0,0,0,0.15)',
              },
            }}
            onClick={() => {
              const currentImage = selectedImage === 0
                ? (transformImageUrl(product.imageUrl) || 'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=800')
                : (transformImageUrl(product.images?.[selectedImage - 1]?.imageUrl) || `https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=800&${selectedImage}`);
              setZoomedImage(currentImage);
              setZoomLevel(1);
              setImagePosition({ x: 0, y: 0 });
              setImageZoomOpen(true);
            }}
            onKeyDown={(e) => {
              if (e.key === 'ArrowLeft' && selectedImage > 0) {
                e.preventDefault();
                handleImageChange(selectedImage - 1);
              } else if (e.key === 'ArrowRight' && selectedImage < (product.images?.length || 0)) {
                e.preventDefault();
                handleImageChange(selectedImage + 1);
              }
            }}
            tabIndex={0}
          >
            {/* Main Image with Fade Animation */}
            <Box
              sx={{
                position: 'relative',
                width: '100%',
                height: '100%',
                opacity: imageFade ? 1 : 0,
                transition: 'opacity 0.3s ease-in-out',
              }}
            >
              <CardMedia
                component="img"
                height="100%"
                image={selectedImage === 0
                  ? transformImageUrl(product.imageUrl) || 'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=800'
                  : transformImageUrl(product.images?.[selectedImage - 1]?.imageUrl) || `https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=800&${selectedImage}`
                }
                alt={product.name}
                sx={{ 
                  objectFit: 'cover',
                  width: '100%',
                  height: '100%',
                }}
              />
            </Box>
            
            {/* Navigation Arrows */}
            {product.images && product.images.length > 0 && (
              <>
                {selectedImage > 0 && (
                  <IconButton
                    onClick={(e) => {
                      e.stopPropagation();
                      handleImageChange(selectedImage - 1);
                    }}
                    sx={{
                      position: 'absolute',
                      left: 8,
                      top: '50%',
                      transform: 'translateY(-50%)',
                      backgroundColor: 'rgba(255,255,255,0.9)',
                      '&:hover': {
                        backgroundColor: 'rgba(255,255,255,1)',
                      },
                      zIndex: 2,
                    }}
                  >
                    <ChevronLeft />
                  </IconButton>
                )}
                {selectedImage < (product.images?.length || 0) && (
                  <IconButton
                    onClick={(e) => {
                      e.stopPropagation();
                      handleImageChange(selectedImage + 1);
                    }}
                    sx={{
                      position: 'absolute',
                      right: 8,
                      top: '50%',
                      transform: 'translateY(-50%)',
                      backgroundColor: 'rgba(255,255,255,0.9)',
                      '&:hover': {
                        backgroundColor: 'rgba(255,255,255,1)',
                      },
                      zIndex: 2,
                    }}
                  >
                    <ChevronRight />
                  </IconButton>
                )}
              </>
            )}
            
            {/* Zoom Icon Overlay */}
            <Box
              sx={{
                position: 'absolute',
                bottom: 16,
                right: 16,
                backgroundColor: 'rgba(0,0,0,0.6)',
                borderRadius: '50%',
                p: 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 2,
              }}
            >
              <ZoomIn sx={{ color: 'white', fontSize: 24 }} />
            </Box>
          </Box>
          
          {/* Thumbnail Images */}
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            {/* Main image thumbnail */}
            <Card
              sx={{
                width: 80,
                height: 80,
                cursor: 'pointer',
                border: selectedImage === 0 ? '2px solid' : '1px solid',
                borderColor: selectedImage === 0 ? 'primary.main' : 'divider',
                borderRadius: 2,
                overflow: 'hidden',
              }}
              onClick={() => handleImageChange(0)}
            >
              <CardMedia
                component="img"
                height="100%"
                image={transformImageUrl(product.imageUrl) || 'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=400'}
                alt={product.name}
                sx={{ objectFit: 'cover' }}
              />
            </Card>

            {/* Additional product images */}
            {product.images?.slice(1, 5).map((image, index) => (
              <Card
                key={image.id}
                sx={{
                  width: 80,
                  height: 80,
                  cursor: 'pointer',
                  border: selectedImage === index + 1 ? '2px solid' : '1px solid',
                  borderColor: selectedImage === index + 1 ? 'primary.main' : 'divider',
                  borderRadius: 2,
                  overflow: 'hidden',
                }}
                onClick={() => handleImageChange(index + 1)}
              >
                <CardMedia
                  component="img"
                  height="100%"
                  image={transformImageUrl(image.imageUrl)}
                  alt={`${product.name} ${index + 2}`}
                  sx={{ objectFit: 'cover' }}
                />
              </Card>
            ))}

            {/* Placeholder images if no additional images */}
            {(!product.images || product.images.length < 2) && [1, 2, 3, 4].slice(product.images?.length || 0).map((index) => (
              <Card
                key={`placeholder-${index}`}
                sx={{
                  width: 80,
                  height: 80,
                  cursor: 'pointer',
                  border: '1px solid',
                  borderColor: 'divider',
                  borderRadius: 2,
                  overflow: 'hidden',
                  backgroundColor: '#f5f5f5',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <Typography variant="caption" color="text.secondary">
                  {index + 1}
                </Typography>
              </Card>
            ))}
          </Box>
        </Grid>

        {/* Product Info */}
        <Grid item xs={12} md={6}>
          <Card sx={{ borderRadius: 3, mb: 3 }}>
            <CardContent>
              {/* Rating and Reviews */}
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
                <Rating value={product.averageRating || 4.5} precision={0.1} readOnly />
                <Typography variant="body1" sx={{ fontWeight: 'bold' }}>
                  {product.averageRating || 4.5}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  ({product.reviewCount || 124} đánh giá)
                </Typography>
                <Button
                  variant="text"
                  onClick={() => setTabValue(1)}
                  sx={{ textTransform: 'none' }}
                >
                  Xem tất cả đánh giá
                </Button>
              </Box>

              {/* Price */}
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
                <Typography variant="h3" sx={{ fontWeight: 'bold', color: 'primary.main' }}>
                  {getCurrentPrice().toLocaleString('vi-VN')}₫
                </Typography>
                {getCurrentPrice() < getOriginalPrice() && (
                  <>
                    <Typography
                      variant="h5"
                      sx={{
                        textDecoration: 'line-through',
                        color: 'text.secondary',
                      }}
                    >
                      {getOriginalPrice().toLocaleString('vi-VN')}₫
                    </Typography>
                    <Chip
                      label={`-${Math.round(((getOriginalPrice() - getCurrentPrice()) / getOriginalPrice()) * 100)}%`}
                      color="error"
                      size="small"
                    />
                  </>
                )}
              </Box>

              {/* Variant Selection */}
              {variants.length > 0 && (
                <Box sx={{ mb: 3 }}>
                  <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>
                    Chọn tùy chọn
                  </Typography>
                  
                  {/* Color Selection */}
                  {getAvailableColors().length > 0 && (
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="subtitle1" sx={{ fontWeight: 'bold', mb: 1 }}>
                        Màu sắc: {selectedVariant?.color || 'Chọn màu'}
                      </Typography>
                      <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                        {getAvailableColors().map((color) => (
                          <Chip
                            key={color}
                            label={color}
                            onClick={() => {
                              const variant = variants.find(v => v.color === color);
                              if (variant) handleVariantSelect(variant);
                            }}
                            color={selectedVariant?.color === color ? 'primary' : 'default'}
                            variant={selectedVariant?.color === color ? 'filled' : 'outlined'}
                            sx={{ cursor: 'pointer' }}
                          />
                        ))}
                      </Box>
                    </Box>
                  )}
                  
                  {/* Size Selection */}
                  {getAvailableSizes().length > 0 && (
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="subtitle1" sx={{ fontWeight: 'bold', mb: 1 }}>
                        Kích thước: {selectedVariant?.size || 'Chọn kích thước'}
                      </Typography>
                      <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                        {getAvailableSizes().map((size) => (
                          <Chip
                            key={size}
                            label={size}
                            onClick={() => {
                              const variant = variants.find(v => v.size === size);
                              if (variant) handleVariantSelect(variant);
                            }}
                            color={selectedVariant?.size === size ? 'primary' : 'default'}
                            variant={selectedVariant?.size === size ? 'filled' : 'outlined'}
                            sx={{ cursor: 'pointer' }}
                          />
                        ))}
                      </Box>
                    </Box>
                  )}
                </Box>
              )}

              {/* Stock Status */}
              <Box sx={{ mb: 3 }}>
                <Chip
                  label={getCurrentStock() > 10 ? 'Còn hàng' : getCurrentStock() > 0 ? 'Sắp hết hàng' : 'Hết hàng'}
                  color={getCurrentStock() > 10 ? 'success' : getCurrentStock() > 0 ? 'warning' : 'error'}
                  size="small"
                />
              </Box>

              {/* Quantity Selector */}
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
                <Typography variant="body1" sx={{ fontWeight: 'bold' }}>
                  Số lượng:
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                  <IconButton
                    onClick={() => handleQuantityChange(false)}
                    disabled={quantity <= 1}
                  >
                    <Remove />
                  </IconButton>
                  <Typography sx={{ px: 2, minWidth: 40, textAlign: 'center' }}>
                    {quantity}
                  </Typography>
                  <IconButton
                    onClick={() => handleQuantityChange(true)}
                    disabled={quantity >= getCurrentStock()}
                  >
                    <Add />
                  </IconButton>
                </Box>
              </Box>

              {/* Action Buttons */}
              <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
                <Button
                  variant="contained"
                  size="large"
                  startIcon={<ShoppingCart />}
                  onClick={handleAddToCart}
                  disabled={getCurrentStock() === 0}
                  fullWidth
                  sx={{ borderRadius: 2, py: 1.5 }}
                >
                  Thêm vào giỏ hàng
                </Button>
                <IconButton
                  onClick={handleToggleFavorite}
                  color={isFavorite(product.id) ? 'error' : 'default'}
                  sx={{ border: '1px solid', borderColor: 'divider' }}
                >
                  {isFavorite(product.id) ? <Favorite /> : <FavoriteBorder />}
                </IconButton>
                <IconButton
                  onClick={handleAddToCompare}
                  color={compareItems.includes(product.id) ? 'primary' : 'default'}
                  sx={{ border: '1px solid', borderColor: 'divider' }}
                >
                  <Compare />
                </IconButton>
                <IconButton
                  onClick={() => setOpenShareDialog(true)}
                  sx={{ border: '1px solid', borderColor: 'divider' }}
                >
                  <Share />
                </IconButton>
              </Box>

              {/* Features */}
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 3 }}>
                <Chip icon={<Security />} label="Bảo hành chính hãng" color="info" variant="outlined" />
                <Chip icon={<LocalShipping />} label="Miễn phí vận chuyển" color="info" variant="outlined" />
                <Chip icon={<Support />} label="Hỗ trợ 24/7" color="info" variant="outlined" />
                <Chip icon={<Refresh />} label="Đổi trả 30 ngày" color="info" variant="outlined" />
              </Box>
            </CardContent>
          </Card>

          {/* Quick Actions */}
          <Card sx={{ borderRadius: 3 }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>
                Thao tác nhanh
              </Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <Button
                  variant="outlined"
                  onClick={() => setOpenReviewDialog(true)}
                  startIcon={<Star />}
                  fullWidth
                >
                  Viết đánh giá
                </Button>
                <Button
                  variant="outlined"
                  onClick={() => setOpenCompareDialog(true)}
                  startIcon={<Compare />}
                  fullWidth
                >
                  So sánh sản phẩm tương tự
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Product Details Tabs */}
      <Paper sx={{ borderRadius: 3, mt: 4 }}>
        <Tabs
          value={tabValue}
          onChange={handleTabChange}
          sx={{
            borderBottom: 1,
            borderColor: 'divider',
            '& .MuiTab-root': {
              textTransform: 'none',
              fontWeight: 'bold',
              minWidth: 120,
            },
          }}
        >
          <Tab label="Mô tả sản phẩm" />
          <Tab label="Đánh giá" />
          <Tab label="Thông số kỹ thuật" />
          <Tab label="Tính năng" />
        </Tabs>

        {/* Description Tab */}
        <TabPanel value={tabValue} index={0}>
          <Typography variant="body1" sx={{ lineHeight: 1.8 }}>
            {product.description}
          </Typography>
        </TabPanel>

        {/* Reviews Tab */}
        <TabPanel value={tabValue} index={1}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
              Đánh giá khách hàng ({reviews.length})
            </Typography>
            <Button
              variant="contained"
              startIcon={<Star />}
              onClick={() => setOpenReviewDialog(true)}
            >
              Viết đánh giá
            </Button>
          </Box>

          {reviews.length === 0 ? (
            <Alert severity="info" sx={{ borderRadius: 3 }}>
              Chưa có đánh giá nào. Hãy là người đầu tiên đánh giá sản phẩm này!
            </Alert>
          ) : (
            <List>
              {reviews.map((review) => (
                <ListItem key={review.id} sx={{ display: 'block', mb: 2 }}>
                  <Card sx={{ borderRadius: 3 }}>
                    <CardContent>
                      {/* Review Header */}
                      <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2, mb: 2 }}>
                        <Avatar 
                          src={review.userAvatar || undefined} 
                          sx={{ width: 50, height: 50 }}
                        >
                          {review.userName ? review.userName.split(' ').map((n: string) => n[0]).join('').toUpperCase().slice(0, 2) : 'U'}
                        </Avatar>
                        <Box sx={{ flexGrow: 1 }}>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                              {review.userName}
                            </Typography>
                            {review.verified && (
                              <Tooltip title="Đã mua hàng">
                                <Verified color="primary" fontSize="small" />
                              </Tooltip>
                            )}
                          </Box>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                            <Rating value={review.rating} readOnly size="small" />
                            <Typography variant="body2" color="text.secondary">
                              {review.date}
                            </Typography>
                          </Box>
                        </Box>
                      </Box>

                      {/* Review Content */}
                      <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1 }}>
                        {review.title}
                      </Typography>
                      <Typography variant="body1" sx={{ mb: 2, lineHeight: 1.6 }}>
                        {review.content}
                      </Typography>

                      {/* Pros and Cons */}
                      {(review.pros || review.cons) && (
                        <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                          {review.pros && review.pros.length > 0 && (
                            <Box>
                              <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: 'success.main', mb: 1 }}>
                                Ưu điểm:
                              </Typography>
                              <List dense>
                                {review.pros.map((pro, index) => (
                                  <ListItem key={index} sx={{ py: 0 }}>
                                    <ListItemText primary={pro} />
                                  </ListItem>
                                ))}
                              </List>
                            </Box>
                          )}
                          {review.cons && review.cons.length > 0 && (
                            <Box>
                              <Typography variant="subtitle2" sx={{ fontWeight: 'bold', color: 'error.main', mb: 1 }}>
                                Nhược điểm:
                              </Typography>
                              <List dense>
                                {review.cons.map((con, index) => (
                                  <ListItem key={index} sx={{ py: 0 }}>
                                    <ListItemText primary={con} />
                                  </ListItem>
                                ))}
                              </List>
                            </Box>
                          )}
                        </Box>
                      )}

                      {/* Review Actions */}
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                        <Button
                          size="small"
                          startIcon={<ThumbUp />}
                          onClick={() => handleHelpful(review.id)}
                        >
                          Hữu ích ({review.helpful})
                        </Button>
                        <Button
                          size="small"
                          startIcon={<ThumbDown />}
                          onClick={() => handleNotHelpful(review.id)}
                        >
                          Không hữu ích ({review.notHelpful})
                        </Button>
                      </Box>
                    </CardContent>
                  </Card>
                </ListItem>
              ))}
            </List>
          )}
        </TabPanel>

        {/* Specifications Tab */}
        <TabPanel value={tabValue} index={2}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2, color: 'primary.main' }}>
                Thông tin cơ bản
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableBody>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 'bold', width: '40%' }}>Tên sản phẩm</TableCell>
                      <TableCell>{product.name}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 'bold' }}>Thương hiệu</TableCell>
                      <TableCell>{product.brand}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 'bold' }}>Danh mục</TableCell>
                      <TableCell>{product.category}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 'bold' }}>SKU</TableCell>
                      <TableCell>
                        <Chip label={product.sku} size="small" variant="outlined" />
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 'bold' }}>Trạng thái</TableCell>
                      <TableCell>
                        <Chip
                          label={product.isActive ? 'Đang bán' : 'Ngừng bán'}
                          color={product.isActive ? 'success' : 'error'}
                          size="small"
                        />
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 'bold' }}>Đánh giá</TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Rating value={product.averageRating || 0} size="small" readOnly />
                          <Typography variant="body2">
                            ({product.reviewCount || 0} đánh giá)
                          </Typography>
                        </Box>
                      </TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </TableContainer>
            </Grid>

            <Grid item xs={12} md={6}>
              <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2, color: 'primary.main' }}>
                Thông tin giá cả
              </Typography>
              <TableContainer>
                <Table size="small">
                  <TableBody>
                    {/* Chỉ hiển thị giá gốc nếu có khuyến mãi */}
                    {product.salePrice && product.salePrice < product.price && (
                      <TableRow>
                        <TableCell sx={{ fontWeight: 'bold', width: '40%' }}>Giá gốc</TableCell>
                        <TableCell>
                          <Typography variant="h6" color="text.secondary" sx={{ textDecoration: 'line-through' }}>
                            {product.price.toLocaleString('vi-VN')}₫
                          </Typography>
                        </TableCell>
                      </TableRow>
                    )}
                    {product.salePrice && product.salePrice < product.price && (
                      <TableRow>
                        <TableCell sx={{ fontWeight: 'bold' }}>Giá khuyến mãi</TableCell>
                        <TableCell>
                          <Typography variant="h6" color="error.main" sx={{ fontWeight: 'bold' }}>
                            {product.salePrice.toLocaleString('vi-VN')}₫
                          </Typography>
                        </TableCell>
                      </TableRow>
                    )}
                    <TableRow>
                      <TableCell sx={{ fontWeight: 'bold' }}>Giá</TableCell>
                      <TableCell>
                        <Typography variant="h6" color="primary.main" sx={{ fontWeight: 'bold' }}>
                          {getCurrentPrice().toLocaleString('vi-VN')}₫
                        </Typography>
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 'bold' }}>Tồn kho</TableCell>
                      <TableCell>
                        <Chip
                          label={`${getCurrentStock()} sản phẩm`}
                          color={getCurrentStock() > 10 ? 'success' : getCurrentStock() > 0 ? 'warning' : 'error'}
                          size="small"
                        />
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 'bold' }}>Ngày tạo</TableCell>
                      <TableCell>{new Date(product.createdAt).toLocaleDateString('vi-VN')}</TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </TableContainer>
            </Grid>
          </Grid>
        </TabPanel>

        {/* Features Tab */}
        <TabPanel value={tabValue} index={3}>
          <Grid container spacing={2}>
            {[
              'Chất lượng cao',
              'Giá cả hợp lý',
              'Giao hàng nhanh',
              'Bảo hành chính hãng',
              'Hỗ trợ khách hàng 24/7',
              'Đổi trả miễn phí',
              'Thanh toán an toàn',
              'Vận chuyển toàn quốc',
            ].map((feature, index) => (
              <Grid item xs={12} sm={6} md={4} key={index}>
                <Card sx={{ borderRadius: 2, p: 2 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Check color="success" />
                    <Typography variant="body1">{feature}</Typography>
                  </Box>
                </Card>
              </Grid>
            ))}
          </Grid>
        </TabPanel>
      </Paper>

      {/* Write Review Dialog */}
      <Dialog
        open={openReviewDialog}
        onClose={() => setOpenReviewDialog(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
              Viết đánh giá cho {product.name}
            </Typography>
            <IconButton onClick={() => setOpenReviewDialog(false)}>
              <Close />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={3} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <Typography component="legend">Đánh giá</Typography>
              <Rating
                size="large"
                value={reviewForm.rating}
                onChange={(_event, newValue) => {
                  setReviewForm(prev => ({ ...prev, rating: newValue || 0 }));
                }}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Tiêu đề đánh giá"
                placeholder="Tóm tắt trải nghiệm của bạn"
                value={reviewForm.title}
                onChange={(e) => setReviewForm(prev => ({ ...prev, title: e.target.value }))}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Nội dung đánh giá"
                multiline
                rows={4}
                placeholder="Chia sẻ trải nghiệm chi tiết của bạn về sản phẩm này..."
                value={reviewForm.content}
                onChange={(e) => setReviewForm(prev => ({ ...prev, content: e.target.value }))}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Ưu điểm (tùy chọn)"
                placeholder="Những điều bạn thích về sản phẩm này"
                value={reviewForm.pros}
                onChange={(e) => setReviewForm(prev => ({ ...prev, pros: e.target.value }))}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Nhược điểm (tùy chọn)"
                placeholder="Những điều cần cải thiện"
                value={reviewForm.cons}
                onChange={(e) => setReviewForm(prev => ({ ...prev, cons: e.target.value }))}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenReviewDialog(false)}>Hủy</Button>
          <Button
            variant="contained"
            startIcon={<Send />}
            onClick={handleReviewSubmit}
            disabled={!reviewForm.rating || !reviewForm.title || !reviewForm.content}
          >
            Gửi đánh giá
          </Button>
        </DialogActions>
      </Dialog>

      {/* Compare Dialog */}
      <Dialog
        open={openCompareDialog}
        onClose={() => setOpenCompareDialog(false)}
        maxWidth="lg"
        fullWidth
      >
        <DialogTitle>
          <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
            So sánh sản phẩm
          </Typography>
        </DialogTitle>
        <DialogContent>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
            Chọn sản phẩm để so sánh với {product.name}
          </Typography>
          
          <Grid container spacing={2}>
            {[
              { id: '2', name: 'Tai nghe không dây', price: 199.99, image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400' },
              { id: '3', name: 'Điện thoại thông minh', price: 899.99, image: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400' },
              { id: '4', name: 'Màn hình 4K', price: 399.99, image: 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=400' },
            ].map((item) => (
              <Grid item xs={12} sm={6} md={4} key={item.id}>
                <Card
                  sx={{
                    borderRadius: 3,
                    cursor: 'pointer',
                    border: compareItems.includes(item.id) ? '2px solid' : '1px solid',
                    borderColor: compareItems.includes(item.id) ? 'primary.main' : 'divider',
                  }}
                  onClick={() => handleAddToCompare()}
                >
                  <CardMedia
                    component="img"
                    height="150"
                    image={item.image}
                    alt={item.name}
                    sx={{ objectFit: 'cover' }}
                  />
                  <CardContent>
                    <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1 }}>
                      {item.name}
                    </Typography>
                    <Typography variant="h6" color="primary.main">
                      {item.price.toLocaleString('vi-VN')}₫
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenCompareDialog(false)}>Hủy</Button>
          <Button
            variant="contained"
            onClick={() => {
              setOpenCompareDialog(false);
              navigate('/compare');
            }}
          >
            So sánh đã chọn
          </Button>
        </DialogActions>
      </Dialog>

      {/* Share Dialog */}
      <Dialog
        open={openShareDialog}
        onClose={() => setOpenShareDialog(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
              Chia sẻ sản phẩm
            </Typography>
            <IconButton onClick={() => setOpenShareDialog(false)}>
              <Close />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Chia sẻ {product?.name} với bạn bè của bạn
          </Typography>

          <Grid container spacing={2}>
            <Grid item xs={6}>
              <Button
                fullWidth
                variant="outlined"
                startIcon={<Facebook />}
                onClick={() => handleShare('facebook')}
                sx={{
                  py: 2,
                  borderColor: '#1877f2',
                  color: '#1877f2',
                  '&:hover': {
                    borderColor: '#1877f2',
                    backgroundColor: 'rgba(24, 119, 242, 0.04)',
                  },
                }}
              >
                Facebook
              </Button>
            </Grid>
            <Grid item xs={6}>
              <Button
                fullWidth
                variant="outlined"
                startIcon={<Twitter />}
                onClick={() => handleShare('twitter')}
                sx={{
                  py: 2,
                  borderColor: '#1da1f2',
                  color: '#1da1f2',
                  '&:hover': {
                    borderColor: '#1da1f2',
                    backgroundColor: 'rgba(29, 161, 242, 0.04)',
                  },
                }}
              >
                Twitter
              </Button>
            </Grid>
            <Grid item xs={6}>
              <Button
                fullWidth
                variant="outlined"
                startIcon={<WhatsApp />}
                onClick={() => handleShare('whatsapp')}
                sx={{
                  py: 2,
                  borderColor: '#25d366',
                  color: '#25d366',
                  '&:hover': {
                    borderColor: '#25d366',
                    backgroundColor: 'rgba(37, 211, 102, 0.04)',
                  },
                }}
              >
                WhatsApp
              </Button>
            </Grid>
            <Grid item xs={6}>
              <Button
                fullWidth
                variant="outlined"
                startIcon={<Share />}
                onClick={() => handleShare('copy')}
                sx={{
                  py: 2,
                  borderColor: 'primary.main',
                  color: 'primary.main',
                  '&:hover': {
                    borderColor: 'primary.main',
                    backgroundColor: 'rgba(25, 118, 210, 0.04)',
                  },
                }}
              >
                Copy Link
              </Button>
            </Grid>
          </Grid>
        </DialogContent>
      </Dialog>

      {/* Image Zoom Dialog with Enhanced Features */}
      <Dialog
        open={imageZoomOpen}
        onClose={() => {
          setImageZoomOpen(false);
          setZoomLevel(1);
          setImagePosition({ x: 0, y: 0 });
        }}
        maxWidth={false}
        fullWidth
        PaperProps={{
          sx: {
            backgroundColor: 'rgba(0,0,0,0.95)',
            boxShadow: 'none',
            maxWidth: '100vw',
            maxHeight: '100vh',
            m: 0,
            borderRadius: 0,
          }
        }}
      >
        <DialogContent 
          sx={{ 
            p: 0, 
            position: 'relative',
            height: '100vh',
            overflow: 'hidden',
          }}
          onWheel={(e) => {
            e.preventDefault();
            const delta = e.deltaY > 0 ? -0.1 : 0.1;
            setZoomLevel(prev => Math.max(1, Math.min(5, prev + delta)));
          }}
          onMouseDown={(e) => {
            if (zoomLevel > 1) {
              setIsDragging(true);
              setDragStart({ x: e.clientX - imagePosition.x, y: e.clientY - imagePosition.y });
            }
          }}
          onMouseMove={(e) => {
            if (isDragging && zoomLevel > 1) {
              setImagePosition({
                x: e.clientX - dragStart.x,
                y: e.clientY - dragStart.y,
              });
            }
          }}
          onMouseUp={() => setIsDragging(false)}
          onMouseLeave={() => setIsDragging(false)}
        >
          {/* Close Button */}
          <IconButton
            onClick={() => {
              setImageZoomOpen(false);
              setZoomLevel(1);
              setImagePosition({ x: 0, y: 0 });
            }}
            sx={{
              position: 'absolute',
              top: 16,
              right: 16,
              backgroundColor: 'rgba(255,255,255,0.2)',
              color: 'white',
              '&:hover': {
                backgroundColor: 'rgba(255,255,255,0.3)',
              },
              zIndex: 10,
            }}
          >
            <Close />
          </IconButton>
          
          {/* Zoom Controls */}
          <Box
            sx={{
              position: 'absolute',
              top: 16,
              left: 16,
              display: 'flex',
              gap: 1,
              zIndex: 10,
            }}
          >
            <IconButton
              onClick={() => setZoomLevel(prev => Math.max(1, prev - 0.5))}
              disabled={zoomLevel <= 1}
              sx={{
                backgroundColor: 'rgba(255,255,255,0.2)',
                color: 'white',
                '&:hover': {
                  backgroundColor: 'rgba(255,255,255,0.3)',
                },
                '&:disabled': {
                  backgroundColor: 'rgba(255,255,255,0.1)',
                  color: 'rgba(255,255,255,0.5)',
                },
              }}
            >
              <ZoomOut />
            </IconButton>
            <IconButton
              onClick={() => setZoomLevel(prev => Math.min(5, prev + 0.5))}
              disabled={zoomLevel >= 5}
              sx={{
                backgroundColor: 'rgba(255,255,255,0.2)',
                color: 'white',
                '&:hover': {
                  backgroundColor: 'rgba(255,255,255,0.3)',
                },
                '&:disabled': {
                  backgroundColor: 'rgba(255,255,255,0.1)',
                  color: 'rgba(255,255,255,0.5)',
                },
              }}
            >
              <ZoomIn />
            </IconButton>
          </Box>
          
          {/* Image Container */}
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: '100%',
              height: '100%',
              overflow: 'hidden',
              cursor: zoomLevel > 1 ? (isDragging ? 'grabbing' : 'grab') : 'zoom-in',
            }}
            onClick={(e) => {
              if (zoomLevel === 1 && e.target === e.currentTarget) {
                setZoomLevel(2);
              }
            }}
          >
            <Box
              component="img"
              src={zoomedImage}
              alt={product?.name}
              sx={{
                maxWidth: zoomLevel === 1 ? '90%' : 'none',
                maxHeight: zoomLevel === 1 ? '90%' : 'none',
                width: zoomLevel > 1 ? `${zoomLevel * 100}%` : 'auto',
                height: zoomLevel > 1 ? `${zoomLevel * 100}%` : 'auto',
                objectFit: 'contain',
                borderRadius: '8px',
                transform: zoomLevel > 1 
                  ? `translate(${imagePosition.x}px, ${imagePosition.y}px)` 
                  : 'none',
                transition: zoomLevel === 1 ? 'transform 0.3s ease' : 'none',
                userSelect: 'none',
              }}
            />
          </Box>
          
          {/* Navigation Arrows for Gallery */}
          {(product.images && product.images.length > 0) && (
            <>
              {selectedImage > 0 && (
                <IconButton
                  onClick={() => {
                    const prevIndex = selectedImage - 1;
                    handleImageChange(prevIndex);
                    const prevImage = prevIndex === 0
                      ? (transformImageUrl(product.imageUrl) || 'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=800')
                      : (transformImageUrl(product.images?.[prevIndex - 1]?.imageUrl) || `https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=800&${prevIndex}`);
                    setZoomedImage(prevImage);
                    setZoomLevel(1);
                    setImagePosition({ x: 0, y: 0 });
                  }}
                  sx={{
                    position: 'absolute',
                    left: 16,
                    top: '50%',
                    transform: 'translateY(-50%)',
                    backgroundColor: 'rgba(255,255,255,0.2)',
                    color: 'white',
                    '&:hover': {
                      backgroundColor: 'rgba(255,255,255,0.3)',
                    },
                    zIndex: 10,
                  }}
                >
                  <ChevronLeft />
                </IconButton>
              )}
              {selectedImage <= (product.images?.length || 0) && (
                <IconButton
                  onClick={() => {
                    const nextIndex = selectedImage + 1;
                    handleImageChange(nextIndex);
                    const nextImage = nextIndex === 0
                      ? (transformImageUrl(product.imageUrl) || 'https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=800')
                      : (transformImageUrl(product.images?.[nextIndex - 1]?.imageUrl) || `https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=800&${nextIndex}`);
                    setZoomedImage(nextImage);
                    setZoomLevel(1);
                    setImagePosition({ x: 0, y: 0 });
                  }}
                  sx={{
                    position: 'absolute',
                    right: 16,
                    top: '50%',
                    transform: 'translateY(-50%)',
                    backgroundColor: 'rgba(255,255,255,0.2)',
                    color: 'white',
                    '&:hover': {
                      backgroundColor: 'rgba(255,255,255,0.3)',
                    },
                    zIndex: 10,
                  }}
                >
                  <ChevronRight />
                </IconButton>
              )}
            </>
          )}
          
          {/* Image Counter */}
          {product.images && product.images.length > 0 && (
            <Box
              sx={{
                position: 'absolute',
                bottom: 16,
                left: '50%',
                transform: 'translateX(-50%)',
                backgroundColor: 'rgba(0,0,0,0.6)',
                color: 'white',
                px: 2,
                py: 1,
                borderRadius: 2,
                zIndex: 10,
              }}
            >
              <Typography variant="body2">
                {selectedImage + 1} / {(product.images?.length || 0) + 1}
              </Typography>
            </Box>
          )}
        </DialogContent>
      </Dialog>

      {/* Related Products Section */}
      {relatedProducts.length > 0 && (
        <Box sx={{ mt: 6 }}>
          <Typography variant="h5" sx={{ fontWeight: 'bold', mb: 3, textAlign: 'center' }}>
            Sản phẩm liên quan
          </Typography>
          <Grid container spacing={2}>
            {relatedProducts.map((relatedProduct) => (
              <Grid item xs={12} sm={6} md={3} key={relatedProduct.id}>
                <Card
                  sx={{
                    borderRadius: 3,
                    cursor: 'pointer',
                    transition: 'all 0.3s ease',
                    '&:hover': {
                      transform: 'translateY(-4px)',
                      boxShadow: '0 8px 25px rgba(0,0,0,0.15)',
                    },
                  }}
                  onClick={() => navigate(`/products/${relatedProduct.id}`)}
                >
                  <CardMedia
                    component="img"
                    height="200"
                    image={relatedProduct.imageUrl || 'https://via.placeholder.com/300x200?text=No+Image'}
                    alt={relatedProduct.name}
                    sx={{ objectFit: 'cover' }}
                  />
                  <CardContent sx={{ p: 2 }}>
                    <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1, fontSize: '0.9rem' }}>
                      {relatedProduct.name}
                    </Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                      <Rating value={relatedProduct.averageRating || 0} size="small" readOnly />
                      <Typography variant="body2" color="text.secondary">
                        ({relatedProduct.reviewCount || 0})
                      </Typography>
                    </Box>
                    <Typography variant="h6" color="primary.main" sx={{ fontWeight: 'bold' }}>
                      {relatedProduct.price.toLocaleString('vi-VN')}₫
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Box>
      )}
    </Container>
  );
};

export default ProductDetailPage;