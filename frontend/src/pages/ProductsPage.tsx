import React, { useState, useEffect, useCallback } from 'react';
import {
  Container,
  Typography,
  Box,
  TextField,
  InputAdornment,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Paper,
  Breadcrumbs,
  Link as MuiLink,
  Grid,
  Card,
  CardContent,
  IconButton,
  Skeleton,
  Pagination,
} from '@mui/material';
import { Search, Clear, Home } from '@mui/icons-material';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useCart } from '../contexts/CartContext';
import { useFavorites } from '../contexts/FavoritesContext';
import { useCompare } from '../contexts/CompareContext';
import { useDebounce } from '../hooks/useDebounce';
// import { useNProgress } from '../hooks/useNProgress';
import productService from '../services/productService';
import { Product } from '../services/productService';
import OptimizedProductCard from '../components/OptimizedProductCard';

const ProductsPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { addToCart } = useCart();
  const { favorites, addToFavorites, removeFromFavorites } = useFavorites();
  const { addToCompare, isInCompare, canAddMore } = useCompare();
  // const { startProgress, completeProgress } = useNProgress();

  // State management
  const [filteredProducts, setFilteredProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState(searchParams.get('search') || '');
  const [selectedCategory, setSelectedCategory] = useState(searchParams.get('category') || 'all');
  const [selectedBrand, setSelectedBrand] = useState('all');
  const [priceRange, setPriceRange] = useState<number[]>([0, 1000000000]);
  const [sortBy, setSortBy] = useState(searchParams.get('sort') || 'featured');
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(24); // Số sản phẩm mỗi trang
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  type Category = { id: number; name: string };
  type Brand = { id: number; name: string };
  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  
  // Special filters from URL
  const [filterType, setFilterType] = useState<string | null>(null); // 'sale', 'new', 'bestselling', 'recommended'
  const [saleTimeSlot, setSaleTimeSlot] = useState<string | null>(null);
  
  // Page title based on filter
  const [pageTitle, setPageTitle] = useState('Tất cả sản phẩm');

  // Debounced search
  const debouncedSearchTerm = useDebounce(searchTerm, 500);
  
  // Read special filters from URL on mount
  useEffect(() => {
    const sale = searchParams.get('sale');
    const sort = searchParams.get('sort');
    const saleTime = searchParams.get('saleTime');
    const recommended = searchParams.get('recommended');
    const category = searchParams.get('category');
    
    if (sale === 'true') {
      setFilterType('sale');
      setPageTitle('🔥 Flash Sale - Giảm giá sốc');
      if (saleTime) {
        setSaleTimeSlot(saleTime);
        setPageTitle(`🔥 Flash Sale ${saleTime} - Giảm giá sốc`);
      }
    } else if (sort === 'newest') {
      setFilterType('new');
      setPageTitle('✨ Sản phẩm mới nhất');
      setSortBy('createdAt-desc');
    } else if (sort === 'bestselling') {
      setFilterType('bestselling');
      setPageTitle('🏆 Sản phẩm bán chạy nhất');
      setSortBy('soldCount-desc');
    } else if (recommended === 'true') {
      setFilterType('recommended');
      setPageTitle('💡 Gợi ý cho bạn');
    } else if (category) {
      setSelectedCategory(category);
      setPageTitle(`Danh mục: ${category}`);
    } else {
      setFilterType(null);
      setPageTitle('Tất cả sản phẩm');
    }
  }, [searchParams]);
  
  // Load products based on filter type
  const loadProducts = useCallback(async () => {
    setLoading(true);
    try {
      let response;
      
      // Check for special filter types from URL
      if (filterType === 'sale') {
        // Load sale products
        response = await productService.getSaleProducts({
          page: currentPage,
          size: pageSize,
          search: debouncedSearchTerm,
          category: selectedCategory !== 'all' ? selectedCategory : undefined,
          brand: selectedBrand !== 'all' ? selectedBrand : undefined,
          saleTimeSlot: saleTimeSlot || undefined
        });
      } else if (filterType === 'new') {
        // Load newest products
        response = await productService.getNewProducts({
          page: currentPage,
          size: pageSize,
          search: debouncedSearchTerm,
          category: selectedCategory !== 'all' ? selectedCategory : undefined,
          brand: selectedBrand !== 'all' ? selectedBrand : undefined
        });
      } else if (filterType === 'bestselling') {
        // Load best selling products
        response = await productService.getBestSellingProducts({
          page: currentPage,
          size: pageSize,
          search: debouncedSearchTerm,
          category: selectedCategory !== 'all' ? selectedCategory : undefined,
          brand: selectedBrand !== 'all' ? selectedBrand : undefined
        });
      } else {
        // Default: load all products with filters
        response = await productService.getProducts({
          page: currentPage,
          size: pageSize,
          search: debouncedSearchTerm,
          category: selectedCategory !== 'all' ? selectedCategory : undefined,
          brand: selectedBrand !== 'all' ? selectedBrand : undefined,
          minPrice: priceRange[0] > 0 ? priceRange[0] : undefined,
          maxPrice: priceRange[1] < 1000000000 ? priceRange[1] : undefined,
          sortBy: sortBy
        });
      }
      
      setFilteredProducts(response.content || []);
      setTotalElements(response.totalElements || 0);
      setTotalPages(response.totalPages || 0);
    } catch (error) {
      console.error('Error loading products:', error);
      setFilteredProducts([]);
      setTotalElements(0);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  }, [debouncedSearchTerm, selectedCategory, selectedBrand, priceRange, sortBy, currentPage, pageSize, filterType, saleTimeSlot]);
  
  // Reset to page 0 when filters change
  useEffect(() => {
    setCurrentPage(0);
  }, [debouncedSearchTerm, selectedCategory, selectedBrand, priceRange, sortBy]);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  // Load categories and brands
  const loadCategoriesAndBrands = useCallback(async () => {
    try {
      const [categoriesData, brandsData] = await Promise.all([
        productService.getCategories(),
        productService.getBrands()
      ]);
      setCategories(categoriesData);
      setBrands(brandsData);
    } catch (error) {
      console.error('Error loading categories and brands:', error);
    }
  }, []);

  useEffect(() => {
    loadCategoriesAndBrands();
  }, [loadCategoriesAndBrands]);

  // Read search from URL params on mount and when URL changes
  useEffect(() => {
    const searchFromUrl = searchParams.get('search');
    if (searchFromUrl && searchFromUrl !== searchTerm) {
      setSearchTerm(searchFromUrl);
    }
  }, [searchParams]);

  // Update URL when search term changes (debounced) - but only if different from URL
  useEffect(() => {
    const currentSearchInUrl = searchParams.get('search') || '';
    if (debouncedSearchTerm !== currentSearchInUrl) {
      if (debouncedSearchTerm) {
        setSearchParams({ search: debouncedSearchTerm }, { replace: true });
      } else {
        setSearchParams({}, { replace: true });
      }
    }
  }, [debouncedSearchTerm, setSearchParams, searchParams]);

  // Handlers
  const handleAddToCart = useCallback(async (productId: string) => {
    try {
      // Optimistic update - không cần await để UI responsive hơn
      addToCart(productId, 1);
    } catch (error) {
      console.error('Error adding to cart:', error);
    }
  }, [addToCart]);

  const handleToggleFavorite = useCallback(async (productId: string) => {
    try {
      if (favorites.some(fav => fav.id === productId)) {
        await removeFromFavorites(productId);
      } else {
        await addToFavorites(productId);
      }
    } catch (error) {
      console.error('Error toggling favorite:', error);
    }
  }, [favorites, addToFavorites, removeFromFavorites]);

  const handleViewDetails = useCallback((productId: string) => {
    navigate(`/product/${productId}`);
  }, [navigate]);

  const handleAddToCompare = useCallback((productId: string) => {
    const product = filteredProducts.find(p => p.id === productId);
    if (product) {
      if (isInCompare(productId)) {
        // Product already in compare, navigate to compare page
        navigate('/compare');
      } else if (canAddMore) {
        addToCompare(product);
      } else {
        // Show notification that max products reached
        const event = new CustomEvent('showToast', {
          detail: {
            type: 'warning',
            message: 'Bạn đã chọn tối đa 4 sản phẩm. Vui lòng xóa một sản phẩm để thêm mới.'
          }
        });
        window.dispatchEvent(event);
      }
    }
  }, [filteredProducts, isInCompare, canAddMore, addToCompare, navigate]);

  const handleClearFilters = useCallback(() => {
    setSearchTerm('');
    setSelectedCategory('all');
    setSelectedBrand('all');
    setPriceRange([0, 1000000000]);
    setSortBy('featured');
  }, []);

  const isFavorite = useCallback((productId: string) => favorites.some(fav => fav.id === productId), [favorites]);

  // Loading Skeleton
  const ProductSkeleton = () => (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Skeleton variant="rectangular" height={280} />
      <CardContent sx={{ flexGrow: 1 }}>
        <Skeleton variant="text" height={24} sx={{ mb: 1 }} />
        <Skeleton variant="text" height={20} sx={{ mb: 1 }} />
        <Skeleton variant="text" height={32} sx={{ mb: 2 }} />
        <Skeleton variant="rectangular" height={40} />
                    </CardContent>
                  </Card>
  );

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: '#f8f9fa' }}>
      <Container maxWidth="xl" sx={{ py: 4 }}>
        {/* Breadcrumbs */}
        <Breadcrumbs sx={{ mb: 3 }}>
          <MuiLink component={Link} to="/" color="inherit" sx={{ display: 'flex', alignItems: 'center' }}>
            <Home sx={{ mr: 0.5 }} fontSize="inherit" />
            {t('navigation.home')}
          </MuiLink>
          <Typography color="text.primary">{t('navigation.products')}</Typography>
        </Breadcrumbs>

        {/* Header */}
        <Box sx={{ mb: 4 }}>
          <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 2, color: 'text.primary' }}>
            {t('productsPage.title')}
            </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
            {t('productsPage.subtitle')}
                  </Typography>
                </Box>

        {/* Filters */}
        <Paper sx={{ p: 3, mb: 4, borderRadius: 3, boxShadow: '0 4px 20px rgba(0,0,0,0.08)' }}>
          <Grid container spacing={3} alignItems="center">
            {/* Search */}
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                placeholder={t('productsPage.searchPlaceholder')}
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
                      <IconButton onClick={() => setSearchTerm('')} size="small">
                        <Clear />
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
                    sx={{ 
                  '& .MuiOutlinedInput-root': {
                      borderRadius: 2,
                  }
                }}
              />
            </Grid>

            {/* Category Filter */}
            <Grid item xs={12} sm={6} md={2}>
              <FormControl fullWidth>
                <InputLabel>{t('productsPage.category')}</InputLabel>
                <Select
                  value={selectedCategory}
                  onChange={(e) => setSelectedCategory(e.target.value)}
                  label={t('productsPage.category')}
                  sx={{ borderRadius: 2 }}
                >
                  <MenuItem value="all">{t('productsPage.allCategories')}</MenuItem>
                  {categories.map((category: Category) => (
                    <MenuItem key={category.id} value={category.name}>
                      {category.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>

            {/* Brand Filter */}
            <Grid item xs={12} sm={6} md={2}>
              <FormControl fullWidth>
                <InputLabel>{t('productsPage.brand')}</InputLabel>
                <Select
                  value={selectedBrand}
                  onChange={(e) => setSelectedBrand(e.target.value)}
                  label={t('productsPage.brand')}
                  sx={{ borderRadius: 2 }}
                >
                  <MenuItem value="all">{t('productsPage.allBrands')}</MenuItem>
                  {brands.map((brand: Brand) => (
                    <MenuItem key={brand.id} value={brand.name}>
                      {brand.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>

            {/* Sort Filter */}
            <Grid item xs={12} sm={6} md={2}>
              <FormControl fullWidth>
                <InputLabel>{t('productsPage.sortBy')}</InputLabel>
                <Select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                  label={t('productsPage.sortBy')}
                  sx={{ borderRadius: 2 }}
                >
                  <MenuItem value="featured">{t('productsPage.sortOptions.featured')}</MenuItem>
                  <MenuItem value="price-asc">{t('productsPage.sortOptions.priceAsc')}</MenuItem>
                  <MenuItem value="price-desc">{t('productsPage.sortOptions.priceDesc')}</MenuItem>
                  <MenuItem value="name-asc">{t('productsPage.sortOptions.nameAsc')}</MenuItem>
                  <MenuItem value="name-desc">{t('productsPage.sortOptions.nameDesc')}</MenuItem>
                  <MenuItem value="newest">{t('productsPage.sortOptions.newest')}</MenuItem>
                </Select>
              </FormControl>
            </Grid>

          {/* Clear Filters */}
            <Grid item xs={12} sm={6} md={2}>
            <Button
              variant="outlined"
                onClick={handleClearFilters}
                startIcon={<Clear />}
              fullWidth
                sx={{ borderRadius: 2, py: 1.5 }}
              >
                {t('productsPage.clearFilters')}
              </Button>
            </Grid>
          </Grid>
        </Paper>

        {/* Results Header */}
        <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 0.5 }}>
              {loading ? t('productsPage.loading') : t('productsPage.resultsFound', { count: totalElements })}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {t('productsPage.totalProducts', { count: totalElements })}
            </Typography>
          </Box>
        </Box>

        {/* Products Grid */}
        {loading ? (
          <Grid container spacing={1.5}>
            {Array.from({ length: 10 }).map((_, index) => (
              <Grid item xs={12} sm={6} md={2.4} key={index}>
                <ProductSkeleton />
              </Grid>
            ))}
          </Grid>
        ) : filteredProducts.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 8 }}>
            <Typography variant="h5" color="text.secondary" sx={{ mb: 2 }}>
              {t('productsPage.noResultsTitle')}
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
              {t('productsPage.noResultsSubtitle')}
            </Typography>
            <Button
              variant="contained"
              onClick={handleClearFilters}
              startIcon={<Clear />}
              sx={{ borderRadius: 2 }}
            >
              {t('productsPage.clearAll')}
            </Button>
          </Box>
        ) : (
          <>
            <Grid container spacing={1.5}>
              {filteredProducts.map((product) => (
                <Grid item xs={12} sm={6} md={2.4} key={product.id}>
                  <OptimizedProductCard 
                    product={product}
                    onAddToCart={handleAddToCart}
                    onToggleFavorite={handleToggleFavorite}
                    onViewDetails={handleViewDetails}
                    onAddToCompare={handleAddToCompare}
                    isFavorite={isFavorite}
                    isInCompare={isInCompare}
                  />
                </Grid>
              ))}
            </Grid>
            
            {/* Pagination */}
            {totalPages > 1 && (
              <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4, mb: 2 }}>
                <Pagination
                  count={totalPages}
                  page={currentPage + 1}
                  onChange={(_, value) => {
                    setCurrentPage(value - 1);
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                  }}
                  color="primary"
                  size="large"
                  showFirstButton
                  showLastButton
                />
              </Box>
            )}
          </>
        )}
      </Container>
    </Box>
  );
};

export default ProductsPage; 