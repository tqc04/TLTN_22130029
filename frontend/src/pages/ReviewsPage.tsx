import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Container,
  Grid,
  Card,
  CardContent,
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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Tabs,
  Tab,
  Paper,
  Tooltip,
} from '@mui/material';
import {
  ThumbUp,
  ThumbDown,
  Edit,
  Delete,
  Add,
  Search,
  Verified,
  Send,
  Close,
} from '@mui/icons-material';
import { useAuth } from '../contexts/AuthContext';
import { apiService } from '../services/api';

interface Review {
  id: number;
  productId: string;
  productName: string;
  productImage: string;
  userId: string;
  userName: string;
  userAvatar?: string | null;
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
  category: string;
}

const FALLBACK_PRODUCT_ID = '1';
const FALLBACK_USER_ID = '1';
const FALLBACK_IMAGE = 'https://via.placeholder.com/400x300?text=No+Image';
const FALLBACK_NAME = 'User';

const formatDate = (value?: string | Date): string => {
  if (!value) {
    return new Date().toISOString().split('T')[0];
  }

  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return new Date().toISOString().split('T')[0];
  }

  return parsedDate.toISOString().split('T')[0];
};

const ReviewsPage: React.FC = () => {
  const { t } = useTranslation();
  const { isAuthenticated, user } = useAuth();
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [tabValue, setTabValue] = useState(0);
  const [openReviewDialog, setOpenReviewDialog] = useState(false);
  const [filterCategory, setFilterCategory] = useState('all');
  const [sortBy, setSortBy] = useState('recent');
  const [searchTerm, setSearchTerm] = useState('');

  const categories = [
    { value: 'all', label: t('categories.allCategories') },
    { value: 'Electronics', label: t('categories.electronics') },
    { value: 'Audio', label: t('categories.audio') },
    { value: 'Mobile', label: t('categories.mobile') },
    { value: 'Computers', label: t('categories.computers') },
    { value: 'Gaming', label: t('categories.gaming') },
  ];

  useEffect(() => {
    const fetchReviews = async () => {
      try {
        setLoading(true);
        let response;

        const safeUserId = user?.id ?? FALLBACK_USER_ID;

        if (isAuthenticated && user?.id) {
          // Fetch user's reviews if authenticated
          response = await apiService.getMyReviews(safeUserId);
        } else {
          // Fetch all reviews or reviews by product
          // For demo, fetch reviews for product ID 1
          response = await apiService.getProductReviews(FALLBACK_PRODUCT_ID);
        }

        if (response.success && response.data) {
          // Transform API data to match component interface
          const reviewsData = (response.data as any).content || response.data || [];
          const transformedReviews: Review[] = reviewsData.map((review: any, index: number) => {
            const safeProductId = String(review.productId ?? review.product?.id ?? FALLBACK_PRODUCT_ID);
            const safeUserId = String(review.userId ?? review.user?.id ?? FALLBACK_USER_ID);

            return {
              id: Number(review.id ?? index + 1),
              productId: safeProductId,
              productName: review.productName || review.product?.name || 'Product',
              productImage: review.productImage || review.product?.imageUrl || FALLBACK_IMAGE,
              userId: safeUserId,
              userName: review.userName || review.user?.name || FALLBACK_NAME,
              userAvatar: review.userAvatar || review.user?.avatarUrl || review.user?.profileImageUrl || null,
              rating: review.rating || 5,
              title: review.title || 'Review',
              content: review.content || 'No content available',
              date: formatDate(review.date || review.createdAt),
              helpful: review.helpful || 0,
              notHelpful: review.notHelpful || 0,
              verified: review.verified || false,
              images: review.images || [],
              pros: review.pros || [],
              cons: review.cons || [],
              category: review.category || 'General',
            };
          });
          setReviews(transformedReviews);
        } else {
          setReviews([]);
        }
      } catch (error) {
        console.error('Error fetching reviews:', error);
        setReviews([]);
      } finally {
        setLoading(false);
      }
    };

    fetchReviews();
  }, [isAuthenticated, user]);

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const handleOpenReviewDialog = (_product?: { id: number; name: string; imageUrl?: string }) => {
    setOpenReviewDialog(true);
  };

  const handleCloseReviewDialog = () => {
    setOpenReviewDialog(false);
  };

  const handleHelpful = async (reviewId: number) => {
    try {
      const response = await apiService.markReviewHelpful(reviewId);
      if (response.success) {
        setReviews(prev =>
          prev.map(review =>
            review.id === reviewId
              ? { ...review, helpful: review.helpful + 1 }
              : review
          )
        );
      }
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

  const filteredReviews = reviews.filter(review => {
    const matchesCategory = filterCategory === 'all' || review.category === filterCategory;
    const matchesSearch = review.productName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         review.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         review.content.toLowerCase().includes(searchTerm.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  const sortedReviews = [...filteredReviews].sort((a, b) => {
    switch (sortBy) {
      case 'recent':
        return new Date(b.date).getTime() - new Date(a.date).getTime();
      case 'rating':
        return b.rating - a.rating;
      case 'helpful':
        return b.helpful - a.helpful;
      default:
        return 0;
    }
  });

  const myReviews = sortedReviews.filter(review => review.userId === (user?.id ?? ''));
  const allReviews = sortedReviews;
  const averageRating = reviews.length
    ? (
        reviews.reduce((sum, review) => sum + review.rating, 0) / reviews.length
      ).toFixed(1)
    : '0.0';


  if (!isAuthenticated) {
    return (
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Alert severity="info" sx={{ borderRadius: 3 }}>
          {t('reviews.loginRequired')}
        </Alert>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4, pt: '40px' }}>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" component="h1" sx={{ fontWeight: 700, mb: 2, color: 'text.primary' }}>
          {t('navigation.reviews')}
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
          Read and write reviews to help other customers make informed decisions
        </Typography>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Paper sx={{ 
            borderRadius: 2, 
            p: 3, 
            textAlign: 'center',
            boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
            border: '1px solid',
            borderColor: 'divider'
          }}>
            <Typography variant="h4" sx={{ fontWeight: 700, color: 'primary.main', mb: 1 }}>
              {reviews.length}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {t('common.totalReviews')}
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Paper sx={{ 
            borderRadius: 2, 
            p: 3, 
            textAlign: 'center',
            boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
            border: '1px solid',
            borderColor: 'divider'
          }}>
            <Typography variant="h4" sx={{ fontWeight: 700, color: 'success.main', mb: 1 }}>
              {averageRating}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {t('common.averageRating')}
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Paper sx={{ 
            borderRadius: 2, 
            p: 3, 
            textAlign: 'center',
            boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
            border: '1px solid',
            borderColor: 'divider'
          }}>
            <Typography variant="h4" sx={{ fontWeight: 700, color: 'warning.main', mb: 1 }}>
              {myReviews.length}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              My Reviews
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Paper sx={{ 
            borderRadius: 2, 
            p: 3, 
            textAlign: 'center',
            boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
            border: '1px solid',
            borderColor: 'divider'
          }}>
            <Typography variant="h4" sx={{ fontWeight: 700, color: 'info.main', mb: 1 }}>
              {reviews.filter(review => review.verified).length}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Verified Reviews
            </Typography>
          </Paper>
        </Grid>
      </Grid>

      {/* Tabs */}
      <Paper sx={{ borderRadius: 3, mb: 4 }}>
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
          <Tab label="All Reviews" />
          <Tab label="My Reviews" />
          <Tab label="Write Review" />
        </Tabs>

        {/* All Reviews Tab */}
        {tabValue === 0 && (
          <Box sx={{ p: 3 }}>
            {/* Filters */}
            <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
              <TextField
                placeholder="Search reviews..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                size="small"
                sx={{ minWidth: 200 }}
                InputProps={{
                  startAdornment: <Search sx={{ mr: 1, color: 'action.active' }} />,
                }}
              />
              <FormControl size="small" sx={{ minWidth: 150 }}>
                <InputLabel>Category</InputLabel>
                <Select
                  value={filterCategory}
                  label="Category"
                  onChange={(e) => setFilterCategory(e.target.value)}
                >
                  {categories.map((category) => (
                    <MenuItem key={category.value} value={category.value}>
                      {category.label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 150 }}>
                <InputLabel>Sort By</InputLabel>
                <Select
                  value={sortBy}
                  label="Sort By"
                  onChange={(e) => setSortBy(e.target.value)}
                >
                  <MenuItem value="recent">Most Recent</MenuItem>
                  <MenuItem value="rating">Highest Rated</MenuItem>
                  <MenuItem value="helpful">Most Helpful</MenuItem>
                </Select>
              </FormControl>
            </Box>

            {/* Reviews List */}
            {loading ? (
              <Grid container spacing={3}>
                {[...Array(4)].map((_, index) => (
                  <Grid item xs={12} key={index}>
                    <Card sx={{ borderRadius: 3 }}>
                      <CardContent>
                        <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
                          <Skeleton variant="circular" width={50} height={50} />
                          <Box sx={{ flexGrow: 1 }}>
                            <Skeleton variant="text" width="60%" />
                            <Skeleton variant="text" width="40%" />
                          </Box>
                        </Box>
                        <Skeleton variant="text" />
                        <Skeleton variant="text" />
                        <Skeleton variant="text" width="80%" />
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>
            ) : (
              <List>
                {allReviews.map((review) => (
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
                                <Tooltip title="Verified Purchase">
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
                            <Typography variant="h6" sx={{ fontWeight: 'bold', color: 'primary.main' }}>
                              {review.productName}
                            </Typography>
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
                                  Pros:
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
                                  Cons:
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

                        {/* Review Images */}
                        {review.images && review.images.length > 0 && (
                          <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
                            {review.images.map((image, index) => (
                              <img
                                key={index}
                                src={image}
                                alt={`Review ${index + 1}`}
                                style={{
                                  width: 80,
                                  height: 80,
                                  objectFit: 'cover',
                                  borderRadius: 8,
                                }}
                              />
                            ))}
                          </Box>
                        )}

                        {/* Review Actions */}
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                          <Button
                            size="small"
                            startIcon={<ThumbUp />}
                            onClick={() => handleHelpful(review.id)}
                          >
                            Helpful ({review.helpful})
                          </Button>
                          <Button
                            size="small"
                            startIcon={<ThumbDown />}
                            onClick={() => handleNotHelpful(review.id)}
                          >
                            Not Helpful ({review.notHelpful})
                          </Button>
                          {review.userId === user?.id && (
                            <>
                              <IconButton size="small" color="primary">
                                <Edit />
                              </IconButton>
                              <IconButton size="small" color="error">
                                <Delete />
                              </IconButton>
                            </>
                          )}
                        </Box>
                      </CardContent>
                    </Card>
                  </ListItem>
                ))}
              </List>
            )}
          </Box>
        )}

        {/* My Reviews Tab */}
        {tabValue === 1 && (
          <Box sx={{ p: 3 }}>
            <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 3 }}>
              My Reviews ({myReviews.length})
            </Typography>
            
            {myReviews.length === 0 ? (
              <Alert severity="info" sx={{ borderRadius: 3 }}>
                You haven't written any reviews yet. Start by reviewing products you've purchased!
              </Alert>
            ) : (
              <List>
                {myReviews.map((review) => (
                  <ListItem key={review.id} sx={{ display: 'block', mb: 2 }}>
                    <Card sx={{ borderRadius: 3 }}>
                      <CardContent>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                          <img
                            src={review.productImage}
                            alt={review.productName}
                            style={{
                              width: 60,
                              height: 60,
                              objectFit: 'cover',
                              borderRadius: 8,
                            }}
                          />
                          <Box sx={{ flexGrow: 1 }}>
                            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                              {review.productName}
                            </Typography>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              <Rating value={review.rating} readOnly size="small" />
                              <Typography variant="body2" color="text.secondary">
                                {review.date}
                              </Typography>
                            </Box>
                          </Box>
                          <Box>
                            <IconButton size="small" color="primary">
                              <Edit />
                            </IconButton>
                            <IconButton size="small" color="error">
                              <Delete />
                            </IconButton>
                          </Box>
                        </Box>
                        <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1 }}>
                          {review.title}
                        </Typography>
                        <Typography variant="body1" sx={{ mb: 2 }}>
                          {review.content}
                        </Typography>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                          <Chip
                            icon={<ThumbUp />}
                            label={`${review.helpful} helpful`}
                            size="small"
                            color="primary"
                            variant="outlined"
                          />
                          <Chip
                            icon={<ThumbDown />}
                            label={`${review.notHelpful} not helpful`}
                            size="small"
                            color="error"
                            variant="outlined"
                          />
                        </Box>
                      </CardContent>
                    </Card>
                  </ListItem>
                ))}
              </List>
            )}
          </Box>
        )}

        {/* Write Review Tab */}
        {tabValue === 2 && (
          <Box sx={{ p: 3 }}>
            <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 3 }}>
              Write a Review
            </Typography>
            
            <Alert severity="info" sx={{ borderRadius: 3, mb: 3 }}>
              You can only review products you have purchased. Browse your order history to find products to review.
            </Alert>
            
            <Button
              variant="contained"
              startIcon={<Add />}
              onClick={() => handleOpenReviewDialog()}
              sx={{ borderRadius: 2 }}
            >
              Write New Review
            </Button>
          </Box>
        )}
      </Paper>

      {/* Write Review Dialog */}
      <Dialog
        open={openReviewDialog}
        onClose={handleCloseReviewDialog}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
              Write a Review
            </Typography>
            <IconButton onClick={handleCloseReviewDialog}>
              <Close />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={3} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel>Select Product</InputLabel>
                <Select label="Select Product">
                  <MenuItem value="1">Gaming Laptop Pro</MenuItem>
                  <MenuItem value="2">Wireless Headphones</MenuItem>
                  <MenuItem value="3">Smartphone X Pro</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <Typography component="legend">Rating</Typography>
              <Rating size="large" />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Review Title"
                placeholder="Summarize your experience"
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Review Content"
                multiline
                rows={4}
                placeholder="Share your detailed experience with this product..."
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Pros (optional)"
                placeholder="What you liked about this product"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Cons (optional)"
                placeholder="What could be improved"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseReviewDialog}>Cancel</Button>
          <Button variant="contained" startIcon={<Send />}>
            Submit Review
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default ReviewsPage; 