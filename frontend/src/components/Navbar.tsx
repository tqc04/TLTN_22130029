import React, { useState, memo } from 'react'
import { useTranslation } from 'react-i18next'
import {
  AppBar,
  Toolbar,
  Typography,
  Button,
  Box,
  Avatar,
  Menu,
  MenuItem,
  Divider,
  Chip,
  IconButton,
  Badge,
  TextField,
  InputAdornment,
  Paper,
  MenuList,
  ClickAwayListener,
  Popper,
  Grow,
  useTheme,
  useMediaQuery,
  Drawer,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemButton,
  Stack,
} from '@mui/material'
import {
  Search,
  ShoppingCart,
  Favorite,
  Notifications,
  AccountCircle,
  Logout,
  AdminPanelSettings,
  Menu as MenuIcon,
  Home,
  Store,
  Receipt,
  Category,
  LocalOffer,
  Support,
  TrendingUp,
  Close,
  Compare,
} from '@mui/icons-material'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useCart } from '../contexts/CartContext'
import { useFavorites } from '../contexts/FavoritesContext'
import { useCompare } from '../contexts/CompareContext'
import LanguageSwitcher from './LanguageSwitcher'
import NotificationDropdown from './NotificationDropdown'
import { apiService } from '../services/api'
import productService from '../services/productService'
import { Product } from '../services/productService'

const Navbar: React.FC = memo(() => {
  const { t } = useTranslation()
  const location = useLocation()
  const navigate = useNavigate()
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('md'))
  const { isAuthenticated, isAdmin, user, logout } = useAuth()
  const { getCartCount } = useCart()
  const { favorites } = useFavorites()
  const { compareProducts } = useCompare()

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const [categoriesOpen, setCategoriesOpen] = useState(false)
  const [categoriesAnchorEl, setCategoriesAnchorEl] = useState<null | HTMLElement>(null)
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false)
  const [searchValue, setSearchValue] = useState('')
  const [searchSuggestions, setSearchSuggestions] = useState<Product[]>([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [searchAnchorEl, setSearchAnchorEl] = useState<HTMLElement | null>(null)
  const searchInputRef = React.useRef<HTMLInputElement>(null)
  const cartCount = getCartCount()
  const favoriteCount = favorites.length
  const compareCount = compareProducts.length
  const [notificationCount, setNotificationCount] = useState(0)
  const [notificationAnchorEl, setNotificationAnchorEl] = useState<HTMLElement | null>(null)
  const notificationOpen = Boolean(notificationAnchorEl)

  // Load unread notification count
  React.useEffect(() => {
    if (isAuthenticated && user?.id) {
      const loadUnreadCount = async () => {
        try {
          const response = await apiService.getUnreadCount(user.id)
          if (response.success && response.data !== undefined) {
            setNotificationCount(response.data)
          }
        } catch (error) {
          console.error('Error loading unread count:', error)
        }
      }
      loadUnreadCount()
      // Refresh every 30 seconds
      const interval = setInterval(loadUnreadCount, 30000)
      return () => clearInterval(interval)
    } else {
      setNotificationCount(0)
    }
  }, [isAuthenticated, user?.id])

  // Sync search value with URL params when on products page
  React.useEffect(() => {
    if (location.pathname === '/products') {
      const searchParams = new URLSearchParams(location.search)
      const searchParam = searchParams.get('search')
      if (searchParam && searchParam !== searchValue) {
        setSearchValue(searchParam)
      }
    }
  }, [location, searchValue])

  // Fetch autocomplete suggestions when user types
  React.useEffect(() => {
    const fetchSuggestions = async () => {
      if (searchValue.trim().length >= 2) {
        try {
          const suggestions = await productService.autocomplete(searchValue, 5)
          setSearchSuggestions(suggestions)
          setShowSuggestions(suggestions.length > 0)
        } catch (error) {
          console.error('Error fetching suggestions:', error)
          setSearchSuggestions([])
          setShowSuggestions(false)
        }
      } else {
        setSearchSuggestions([])
        setShowSuggestions(false)
      }
    }

    const timeoutId = setTimeout(fetchSuggestions, 300) // Debounce 300ms
    return () => clearTimeout(timeoutId)
  }, [searchValue])

  const handleUserMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget)
  }

  const handleUserMenuClose = () => {
    setAnchorEl(null)
  }

  const handleCategoriesClick = (event: React.MouseEvent<HTMLElement>) => {
    setCategoriesAnchorEl(event.currentTarget)
    setCategoriesOpen(!categoriesOpen)
  }

  const handleCategoriesClose = () => {
    setCategoriesOpen(false)
  }

  const handleLogout = () => {
    logout()
    handleUserMenuClose()
    navigate('/')
  }

  const handleSearch = () => {
    if (searchValue.trim()) {
      // Navigate to products page with search query
      navigate(`/products?search=${encodeURIComponent(searchValue.trim())}`)
      setShowSuggestions(false)
      // Clear search after navigation (optional)
      // setSearchValue('')
    }
  }

  const handleSuggestionClick = (product: Product) => {
    setSearchValue('')
    setShowSuggestions(false)
    navigate(`/products/${product.id}`)
  }

  const handleSearchFocus = (event: React.FocusEvent<HTMLInputElement>) => {
    setSearchAnchorEl(event.currentTarget)
    if (searchSuggestions.length > 0) {
      setShowSuggestions(true)
    }
  }

  const handleSearchBlur = () => {
    // Delay to allow click on suggestion
    setTimeout(() => {
      setShowSuggestions(false)
    }, 200)
  }

  const handleSearchKeyPress = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter') {
      handleSearch()
    }
  }

  const isActive = (path: string) => location.pathname === path

  const categories = [
    { name: t('categories.electronics'), icon: '💻', path: '/products?category=electronics' },
    { name: t('categories.gaming'), icon: '🎮', path: '/products?category=gaming' },
    { name: t('categories.computers'), icon: '🖥️', path: '/products?category=computers' },
    { name: t('categories.mobile'), icon: '📱', path: '/products?category=mobile' },
    { name: t('categories.audio'), icon: '🎧', path: '/products?category=audio' },
    { name: t('categories.accessories'), icon: '🔌', path: '/products?category=accessories' },
  ]

  const mobileNavItems = [
    { label: t('navigation.home'), path: '/', icon: <Home />, public: true },
    { label: t('navigation.products'), path: '/products', icon: <Store />, public: true },
    { label: 'Voucher', path: '/vouchers', icon: <LocalOffer />, public: true },
    { label: t('navigation.support'), path: '/support', icon: <Support />, public: true },
    { label: t('navigation.favorites'), path: '/favorites', icon: <Favorite />, public: false },
    { label: t('navigation.compare'), path: '/compare', icon: <Compare />, public: false },
    { label: t('navigation.profile'), path: '/profile', icon: <AccountCircle />, public: false },
    { label: t('navigation.orders'), path: '/orders', icon: <Receipt />, public: false },
    { label: t('navigation.admin'), path: '/admin', icon: <AdminPanelSettings />, public: false, adminOnly: true },
  ]

  const filteredMobileNavItems = mobileNavItems.filter(item =>
      item.public ||
      (isAuthenticated && !item.adminOnly) ||
      (isAuthenticated && item.adminOnly && isAdmin)
  )

  return (
      <AppBar
          position="fixed"
          sx={{
            zIndex: (theme) => theme.zIndex.drawer + 1,
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            boxShadow: '0 4px 20px rgba(0,0,0,0.1)',
          }}
      >
        <Toolbar sx={{ py: 1 }}>
          {/* Mobile Menu Button */}
          {isMobile && (
              <IconButton
                  color="inherit"
                  onClick={() => setMobileDrawerOpen(true)}
                  sx={{ mr: 1 }}
              >
                <MenuIcon />
              </IconButton>
          )}

          {/* Logo */}
          <Box
              component={Link}
              to="/"
              sx={{
                display: 'flex',
                alignItems: 'center',
                textDecoration: 'none',
                color: 'inherit',
                mr: { xs: 1, md: 4 }
              }}
          >
              <Typography
                variant={isMobile ? "h6" : "h5"}
                component="div"
                sx={{
                  fontWeight: 'bold',
                  background: 'linear-gradient(45deg, #fff, #f0f0f0)',
                  backgroundClip: 'text',
                  WebkitBackgroundClip: 'text',
                  color: 'transparent',
                }}
            >
              {t('common.brandName')}
            </Typography>
          </Box>

          {/* Desktop Navigation */}
          {!isMobile && (
              <>
                {/* Categories Button */}
                <Button
                    color="inherit"
                    startIcon={<Category />}
                    onClick={handleCategoriesClick}
                    sx={{
                      textTransform: 'none',
                      mr: 2,
                      borderRadius: 2,
                      '&:hover': {
                        backgroundColor: 'rgba(255, 255, 255, 0.1)',
                      }
                    }}
                >
                  {t('navigation.categories')}
                </Button>

                {/* Navigation Links */}
                <Box sx={{ display: 'flex', gap: 1, mr: 'auto' }}>
                  <Button
                      component={Link}
                      to="/"
                      color="inherit"
                      sx={{
                        textTransform: 'none',
                        borderRadius: 2,
                        backgroundColor: isActive('/') ? 'rgba(255, 255, 255, 0.15)' : 'transparent',
                        '&:hover': {
                          backgroundColor: 'rgba(255, 255, 255, 0.1)',
                        },
                      }}
                  >
                    {t('navigation.home')}
                  </Button>
                  <Button
                      component={Link}
                      to="/products"
                      color="inherit"
                      sx={{
                        textTransform: 'none',
                        borderRadius: 2,
                        backgroundColor: isActive('/products') ? 'rgba(255, 255, 255, 0.15)' : 'transparent',
                        '&:hover': {
                          backgroundColor: 'rgba(255, 255, 255, 0.1)',
                        },
                      }}
                  >
                    {t('navigation.products')}
                  </Button>
                  <Button
                      component={Link}
                      to="/vouchers"
                      color="inherit"
                      startIcon={<LocalOffer />}
                      sx={{
                        textTransform: 'none',
                        borderRadius: 2,
                        backgroundColor: isActive('/vouchers') ? 'rgba(255, 255, 255, 0.15)' : 'transparent',
                        '&:hover': {
                          backgroundColor: 'rgba(255, 255, 255, 0.1)',
                        },
                      }}
                  >
                    Voucher
                  </Button>
                  <Button
                      component={Link}
                      to="/support"
                      color="inherit"
                      sx={{
                        textTransform: 'none',
                        borderRadius: 2,
                        backgroundColor: isActive('/support') ? 'rgba(255, 255, 255, 0.15)' : 'transparent',
                        '&:hover': {
                          backgroundColor: 'rgba(255, 255, 255, 0.1)',
                        },
                      }}
                  >
                    {t('navigation.support')}
                  </Button>
                </Box>
              </>
          )}

          {/* Search Bar - Optimized for all pages */}
          <Box sx={{ 
            flexGrow: 1, 
            maxWidth: { xs: '100%', sm: 400, md: 500 }, 
            mx: { xs: 1, md: 2 },
            display: { xs: 'none', sm: 'flex' },
            alignItems: 'center'
          }}>
            <Box sx={{ 
              display: 'flex', 
              width: '100%',
              bgcolor: 'white',
              borderRadius: 3,
              overflow: 'hidden',
              boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
              '&:hover': {
                boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
              },
              '&:focus-within': {
                boxShadow: '0 4px 16px rgba(102, 126, 234, 0.3)',
              }
            }}>
              <Box sx={{ position: 'relative', width: '100%' }}>
                <TextField
                    inputRef={searchInputRef}
                    fullWidth
                    size="small"
                    placeholder={t('common.search') || "Tìm kiếm sản phẩm..."}
                    value={searchValue}
                    onChange={(e) => setSearchValue(e.target.value)}
                    onKeyPress={handleSearchKeyPress}
                    onFocus={handleSearchFocus}
                    onBlur={handleSearchBlur}
                    InputProps={{
                      startAdornment: (
                          <InputAdornment position="start" sx={{ ml: 1 }}>
                            <Search sx={{ color: '#667eea' }} />
                          </InputAdornment>
                      ),
                      endAdornment: searchValue && (
                          <InputAdornment position="end">
                            <IconButton
                                size="small"
                                onClick={() => {
                                  setSearchValue('')
                                  setShowSuggestions(false)
                                }}
                                sx={{ mr: 0.5 }}
                            >
                              <Close fontSize="small" />
                            </IconButton>
                          </InputAdornment>
                      ),
                      sx: {
                        '& fieldset': {
                          border: 'none',
                        },
                      }
                    }}
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        backgroundColor: 'transparent',
                        '&:hover': {
                          backgroundColor: 'transparent',
                        },
                        '&.Mui-focused': {
                          backgroundColor: 'transparent',
                        },
                      },
                      '& .MuiOutlinedInput-input': {
                        py: 1.2,
                      },
                    }}
                />
                
                {/* Search Suggestions Dropdown */}
                {showSuggestions && searchSuggestions.length > 0 && searchAnchorEl && (
                    <Popper
                        open={showSuggestions}
                        anchorEl={searchAnchorEl}
                        placement="bottom-start"
                        style={{ 
                          // Widen suggestion box for clearer product info
                          width: Math.max(searchAnchorEl?.offsetWidth || 400, 450),
                          zIndex: 1300,
                          marginTop: 4
                        }}
                    >
                      <Paper
                          elevation={8}
                          sx={{
                            minWidth: 450,
                            maxHeight: 500,
                            overflow: 'auto',
                            borderRadius: 2,
                            mt: 0.5,
                            border: '1px solid #e0e0e0',
                          }}
                      >
                        <Box sx={{ p: 1 }}>
                          <Typography variant="caption" sx={{ px: 2, py: 1, color: 'text.secondary', fontWeight: 600 }}>
                            Sản phẩm gợi ý
                          </Typography>
                          <Divider sx={{ mb: 1 }} />
                          {searchSuggestions.map((product) => (
                              <Box
                                  key={product.id}
                                  onClick={() => handleSuggestionClick(product)}
                                  sx={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 2,
                                    p: 1.5,
                                    borderRadius: 1,
                                    cursor: 'pointer',
                                    '&:hover': {
                                      bgcolor: '#f5f5f5',
                                    },
                                  }}
                              >
                                <Box
                                    sx={{
                                      width: 60,
                                      height: 60,
                                      borderRadius: 1,
                                      overflow: 'hidden',
                                      bgcolor: '#f0f0f0',
                                      flexShrink: 0,
                                    }}
                                >
                                  {product.imageUrl ? (
                                      <img
                                          src={product.imageUrl}
                                          alt={product.name}
                                          style={{
                                            width: '100%',
                                            height: '100%',
                                            objectFit: 'cover',
                                          }}
                                          onError={(e) => {
                                            (e.target as HTMLImageElement).src = 'https://via.placeholder.com/60'
                                          }}
                                      />
                                  ) : (
                                      <Box sx={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                        <Store sx={{ color: '#ccc' }} />
                                      </Box>
                                  )}
                                </Box>
                                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                                  <Typography
                                      variant="body2"
                                      sx={{
                                        fontWeight: 500,
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        whiteSpace: 'nowrap',
                                      }}
                                  >
                                    {product.name}
                                  </Typography>
                                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
                                    {product.salePrice && product.salePrice < product.price ? (
                                        <>
                                          <Typography variant="body2" sx={{ color: 'error.main', fontWeight: 600 }}>
                                            {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(product.salePrice)}
                                          </Typography>
                                          <Typography variant="caption" sx={{ color: 'text.secondary', textDecoration: 'line-through' }}>
                                            {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(product.price)}
                                          </Typography>
                                        </>
                                    ) : (
                                        <Typography variant="body2" sx={{ color: 'error.main', fontWeight: 600 }}>
                                          {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(product.price)}
                                        </Typography>
                                    )}
                                  </Stack>
                                </Box>
                              </Box>
                          ))}
                          {searchValue.trim() && (
                              <>
                                <Divider sx={{ my: 1 }} />
                                <Box
                                    onClick={handleSearch}
                                    sx={{
                                      p: 1.5,
                                      textAlign: 'center',
                                      cursor: 'pointer',
                                      borderRadius: 1,
                                      bgcolor: '#f5f5f5',
                                      '&:hover': {
                                        bgcolor: '#e0e0e0',
                                      },
                                    }}
                                >
                                  <Typography variant="body2" sx={{ fontWeight: 600, color: 'primary.main' }}>
                                    Xem tất cả kết quả cho "{searchValue}"
                                  </Typography>
                                </Box>
                              </>
                          )}
                        </Box>
                      </Paper>
                    </Popper>
                )}
              </Box>
              <Button
                  onClick={handleSearch}
                  disabled={!searchValue.trim()}
                  sx={{
                    minWidth: { xs: 80, md: 100 },
                    borderRadius: '0 3px 3px 0',
                    bgcolor: '#667eea',
                    color: 'white',
                    fontWeight: 600,
                    textTransform: 'none',
                    px: { xs: 2, md: 3 },
                    '&:hover': {
                      bgcolor: '#5568d3',
                    },
                    '&:disabled': {
                      bgcolor: '#ccc',
                      color: '#999',
                    }
                  }}
              >
                {t('common.search') || 'Search'}
              </Button>
            </Box>
          </Box>

          {/* Right Side Actions */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {/* Compare Icon */}
            {compareCount > 0 && (
              <IconButton
                color="inherit"
                component={Link}
                to="/compare"
                sx={{
                  '&:hover': {
                    backgroundColor: 'rgba(255, 255, 255, 0.1)',
                  }
                }}
              >
                <Badge badgeContent={compareCount} color="primary">
                  <Compare />
                </Badge>
              </IconButton>
            )}

            {/* Favorites Icon */}
            {isAuthenticated && (
                <IconButton
                    color="inherit"
                    component={Link}
                    to="/favorites"
                    sx={{
                      '&:hover': {
                        backgroundColor: 'rgba(255, 255, 255, 0.1)',
                      }
                    }}
                >
                  <Badge badgeContent={favoriteCount} color="error">
                    <Favorite />
                  </Badge>
                </IconButton>
            )}

            {/* Shopping Cart */}
            <IconButton
                color="inherit"
                component={Link}
                to="/cart"
                sx={{
                  '&:hover': {
                    backgroundColor: 'rgba(255, 255, 255, 0.1)',
                  }
                }}
            >
              <Badge badgeContent={cartCount} color="error">
                <ShoppingCart />
              </Badge>
            </IconButton>

            {/* Language Switcher */}
            <LanguageSwitcher />

            {/* Notifications */}
            {isAuthenticated && (
              <>
                <IconButton
                  color="inherit"
                  onClick={(e) => setNotificationAnchorEl(e.currentTarget)}
                  sx={{
                    '&:hover': {
                      backgroundColor: 'rgba(255, 255, 255, 0.1)',
                    }
                  }}
                >
                  <Badge badgeContent={notificationCount > 0 ? notificationCount : undefined} color="error">
                    <Notifications />
                  </Badge>
                </IconButton>
                <NotificationDropdown
                  anchorEl={notificationAnchorEl}
                  open={notificationOpen}
                  onClose={() => setNotificationAnchorEl(null)}
                  onNotificationClick={() => {
                    // Refresh unread count after clicking
                    if (user?.id) {
                      apiService.getUnreadCount(user.id).then(response => {
                        if (response.success && response.data !== undefined) {
                          setNotificationCount(response.data)
                        }
                      })
                    }
                  }}
                />
              </>
            )}

            {/* User Authentication */}
            {isAuthenticated ? (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, ml: 1 }}>
                                     {!isMobile && (isAdmin || user?.role === 'PRODUCT_MANAGER' || user?.role === 'USER_MANAGER' || user?.role === 'MODERATOR') && (
                       <Button
                           component={Link}
                           to="/admin"
                           color="inherit"
                           startIcon={<AdminPanelSettings />}
                           sx={{
                             textTransform: 'none',
                             borderRadius: 2,
                             backgroundColor: isActive('/admin') ? 'rgba(255, 255, 255, 0.15)' : 'transparent',
                             '&:hover': {
                               backgroundColor: 'rgba(255, 255, 255, 0.1)',
                             },
                           }}
                       >
                         {t('navigation.admin')}
                       </Button>
                   )}

                  {!isMobile && (
                      <Chip
                          label={user?.role || 'USER'}
                          size="small"
                          color={isAdmin ? 'warning' : 'secondary'}
                          sx={{
                            color: 'white',
                            bgcolor: isAdmin ? 'orange' : 'rgba(255, 255, 255, 0.2)',
                            fontWeight: 'bold'
                          }}
                      />
                  )}

                  <IconButton
                      onClick={handleUserMenuOpen}
                      sx={{
                        color: 'white',
                        '&:hover': {
                          backgroundColor: 'rgba(255, 255, 255, 0.1)',
                        }
                      }}
                  >
                    <Avatar
                        src={user?.avatarUrl || user?.profileImageUrl}
                        sx={{
                          width: 32,
                          height: 32,
                          bgcolor: 'secondary.main',
                          border: '2px solid rgba(255,255,255,0.3)'
                        }}
                    >
                      {user?.firstName?.[0] || user?.username?.[0] || 'U'}
                    </Avatar>
                  </IconButton>
                </Box>
            ) : (
                <Button
                    component={Link}
                    to="/login"
                    color="inherit"
                    variant="outlined"
                    sx={{
                      textTransform: 'none',
                      borderRadius: 3,
                      borderColor: 'rgba(255, 255, 255, 0.5)',
                      fontWeight: 'bold',
                      '&:hover': {
                        borderColor: 'white',
                        backgroundColor: 'rgba(255, 255, 255, 0.1)',
                      },
                    }}
                >
                  {t('common.login')}
                </Button>
            )}
          </Box>
        </Toolbar>

        {/* Categories Menu */}
        <Popper
            open={categoriesOpen}
            anchorEl={categoriesAnchorEl}
            role={undefined}
            placement="bottom-start"
            transition
            disablePortal
            sx={{ zIndex: 1300 }}
        >
          {({ TransitionProps, placement }) => (
              <Grow
                  {...TransitionProps}
                  style={{
                    transformOrigin: placement === 'bottom-start' ? 'left top' : 'left bottom',
                  }}
              >
                <Paper
                    elevation={8}
                    sx={{
                      mt: 1,
                      minWidth: 250,
                      borderRadius: 2,
                      overflow: 'hidden',
                    }}
                >
                  <ClickAwayListener onClickAway={handleCategoriesClose}>
                    <MenuList autoFocusItem={categoriesOpen} dense>
                      <MenuItem sx={{ py: 1.5, borderBottom: '1px solid #eee' }}>
                        <ListItemIcon>
                          <TrendingUp />
                        </ListItemIcon>
                        <ListItemText primary={t('categories.allCategories')} />
                      </MenuItem>
                      {categories.map((category) => (
                          <MenuItem
                              key={category.name}
                              component={Link}
                              to={category.path}
                              onClick={handleCategoriesClose}
                              sx={{ py: 1 }}
                          >
                            <ListItemText
                                primary={
                                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                    <span>{category.icon}</span>
                                    <span>{category.name}</span>
                                  </Box>
                                }
                            />
                          </MenuItem>
                      ))}
                      <Divider />
                      <MenuItem
                          component={Link}
                          to="/products"
                          onClick={handleCategoriesClose}
                          sx={{ py: 1.5, color: 'primary.main', fontWeight: 'bold' }}
                      >
                        <ListItemIcon>
                          <LocalOffer sx={{ color: 'primary.main' }} />
                        </ListItemIcon>
                        <ListItemText primary={t('categories.viewAllProducts')} />
                      </MenuItem>
                    </MenuList>
                  </ClickAwayListener>
                </Paper>
              </Grow>
          )}
        </Popper>

        {/* User Menu */}
        <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleUserMenuClose}
            onClick={handleUserMenuClose}
            PaperProps={{
              elevation: 8,
              sx: {
                overflow: 'visible',
                filter: 'drop-shadow(0px 2px 8px rgba(0,0,0,0.32))',
                mt: 1.5,
                minWidth: 220,
                borderRadius: 2,
                '&:before': {
                  content: '""',
                  display: 'block',
                  position: 'absolute',
                  top: 0,
                  right: 14,
                  width: 10,
                  height: 10,
                  bgcolor: 'background.paper',
                  transform: 'translateY(-50%) rotate(45deg)',
                  zIndex: 0,
                },
              },
            }}
            transformOrigin={{ horizontal: 'right', vertical: 'top' }}
            anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        >
          <MenuItem disabled sx={{ py: 2 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Avatar 
                src={user?.avatarUrl || user?.profileImageUrl}
                sx={{ bgcolor: 'primary.main' }}
              >
                {user?.firstName?.[0] || user?.username?.[0] || 'U'}
              </Avatar>
              <Box>
                <Typography variant="subtitle1" fontWeight="bold">
                  {user?.firstName} {user?.lastName}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  @{user?.username}
                </Typography>
              </Box>
            </Box>
          </MenuItem>
          <Divider />
          <MenuItem component={Link} to="/profile">
            <AccountCircle sx={{ mr: 2 }} />
            {t('navigation.profile')}
          </MenuItem>
          <MenuItem component={Link} to="/orders">
            <Receipt sx={{ mr: 2 }} />
            {t('navigation.orders')}
          </MenuItem>
          <MenuItem component={Link} to="/favorites">
            <Favorite sx={{ mr: 2 }} />
            {t('navigation.favorites')}
          </MenuItem>
          <MenuItem component={Link} to="/vouchers">
            <LocalOffer sx={{ mr: 2 }} />
            Voucher
          </MenuItem>
          <Divider />
          <MenuItem component={Link} to="/support">
            <Support sx={{ mr: 2 }} />
            {t('navigation.support')}
          </MenuItem>
          <Divider />
          <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
            <Logout sx={{ mr: 2 }} />
            {t('common.logout')}
          </MenuItem>
        </Menu>

        {/* Mobile Drawer */}
        <Drawer
            anchor="left"
            open={mobileDrawerOpen}
            onClose={() => setMobileDrawerOpen(false)}
            sx={{
              '& .MuiDrawer-paper': {
                width: 280,
                bgcolor: 'background.paper',
              },
            }}
        >
          <Box sx={{ p: 2, bgcolor: 'primary.main', color: 'white' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Typography variant="h6" fontWeight="bold">
                {t('common.brandName')}
              </Typography>
              <IconButton onClick={() => setMobileDrawerOpen(false)} sx={{ color: 'white' }}>
                <Close />
              </IconButton>
            </Box>
            {isAuthenticated && user && (
                <Box sx={{ mt: 2, display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Avatar sx={{ bgcolor: 'secondary.main' }}>
                    {user.firstName?.[0] || user.username?.[0] || 'U'}
                  </Avatar>
                  <Box>
                    <Typography variant="body2" fontWeight="bold">
                      {user.firstName} {user.lastName}
                    </Typography>
                    <Chip label={user.role} size="small" sx={{ bgcolor: 'rgba(255,255,255,0.2)', color: 'white' }} />
                  </Box>
                </Box>
            )}
          </Box>

          <Divider />

          {/* Mobile Search Bar */}
          <Box sx={{ p: 2 }}>
            <Box sx={{ 
              display: 'flex', 
              width: '100%',
              bgcolor: 'white',
              borderRadius: 3,
              overflow: 'hidden',
              boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
              border: '1px solid #e0e0e0'
            }}>
              <TextField
                  fullWidth
                  size="small"
                  placeholder={t('common.search') || "Tìm kiếm sản phẩm..."}
                  value={searchValue}
                  onChange={(e) => setSearchValue(e.target.value)}
                  onKeyPress={handleSearchKeyPress}
                  onFocus={handleSearchFocus}
                  InputProps={{
                    startAdornment: (
                        <InputAdornment position="start" sx={{ ml: 1 }}>
                          <Search sx={{ color: '#667eea' }} />
                        </InputAdornment>
                    ),
                    endAdornment: searchValue && (
                        <InputAdornment position="end">
                          <IconButton
                              size="small"
                              onClick={() => {
                                setSearchValue('')
                                setShowSuggestions(false)
                              }}
                              sx={{ mr: 0.5 }}
                          >
                            <Close fontSize="small" />
                          </IconButton>
                        </InputAdornment>
                    ),
                    sx: {
                      '& fieldset': {
                        border: 'none',
                      },
                    }
                  }}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      backgroundColor: 'transparent',
                    },
                    '& .MuiOutlinedInput-input': {
                      py: 1.2,
                    },
                  }}
              />
              <Button
                  onClick={() => {
                    handleSearch()
                    setMobileDrawerOpen(false)
                  }}
                  disabled={!searchValue.trim()}
                  sx={{
                    minWidth: 80,
                    borderRadius: '0 3px 3px 0',
                    bgcolor: '#667eea',
                    color: 'white',
                    fontWeight: 600,
                    textTransform: 'none',
                    px: 2,
                    '&:hover': {
                      bgcolor: '#5568d3',
                    },
                    '&:disabled': {
                      bgcolor: '#ccc',
                      color: '#999',
                    }
                  }}
              >
                {t('common.search') || 'Search'}
              </Button>
            </Box>
            
            {/* Mobile Search Suggestions */}
            {showSuggestions && searchSuggestions.length > 0 && (
                <Paper
                    elevation={2}
                    sx={{
                      mt: 1,
                      maxHeight: 400,
                      overflow: 'auto',
                      borderRadius: 2,
                      border: '1px solid #e0e0e0',
                    }}
                >
                  <Box sx={{ p: 1 }}>
                    <Typography variant="caption" sx={{ px: 2, py: 1, color: 'text.secondary', fontWeight: 600, display: 'block' }}>
                      Sản phẩm gợi ý
                    </Typography>
                    <Divider sx={{ mb: 1 }} />
                    {searchSuggestions.map((product) => (
                        <Box
                            key={product.id}
                            onClick={() => {
                              handleSuggestionClick(product)
                              setMobileDrawerOpen(false)
                            }}
                            sx={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: 2,
                              p: 1.5,
                              borderRadius: 1,
                              cursor: 'pointer',
                              '&:hover': {
                                bgcolor: '#f5f5f5',
                              },
                            }}
                        >
                          <Box
                              sx={{
                                width: 50,
                                height: 50,
                                borderRadius: 1,
                                overflow: 'hidden',
                                bgcolor: '#f0f0f0',
                                flexShrink: 0,
                              }}
                          >
                            {product.imageUrl ? (
                                <img
                                    src={product.imageUrl}
                                    alt={product.name}
                                    style={{
                                      width: '100%',
                                      height: '100%',
                                      objectFit: 'cover',
                                    }}
                                    onError={(e) => {
                                      (e.target as HTMLImageElement).src = 'https://via.placeholder.com/50'
                                    }}
                                />
                            ) : (
                                <Box sx={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                  <Store sx={{ color: '#ccc', fontSize: 24 }} />
                                </Box>
                            )}
                          </Box>
                          <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                            <Typography
                                variant="body2"
                                sx={{
                                  fontWeight: 500,
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis',
                                  whiteSpace: 'nowrap',
                                  fontSize: '0.875rem',
                                }}
                            >
                              {product.name}
                            </Typography>
                            <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
                              {product.salePrice && product.salePrice < product.price ? (
                                  <>
                                    <Typography variant="caption" sx={{ color: 'error.main', fontWeight: 600, fontSize: '0.75rem' }}>
                                      {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(product.salePrice)}
                                    </Typography>
                                    <Typography variant="caption" sx={{ color: 'text.secondary', textDecoration: 'line-through', fontSize: '0.7rem' }}>
                                      {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(product.price)}
                                    </Typography>
                                  </>
                              ) : (
                                  <Typography variant="caption" sx={{ color: 'error.main', fontWeight: 600, fontSize: '0.75rem' }}>
                                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(product.price)}
                                  </Typography>
                              )}
                            </Stack>
                          </Box>
                        </Box>
                    ))}
                    {searchValue.trim() && (
                        <>
                          <Divider sx={{ my: 1 }} />
                          <Box
                              onClick={() => {
                                handleSearch()
                                setMobileDrawerOpen(false)
                              }}
                              sx={{
                                p: 1.5,
                                textAlign: 'center',
                                cursor: 'pointer',
                                borderRadius: 1,
                                bgcolor: '#f5f5f5',
                                '&:hover': {
                                  bgcolor: '#e0e0e0',
                                },
                              }}
                          >
                            <Typography variant="body2" sx={{ fontWeight: 600, color: 'primary.main', fontSize: '0.875rem' }}>
                              Xem tất cả kết quả cho "{searchValue}"
                            </Typography>
                          </Box>
                        </>
                    )}
                  </Box>
                </Paper>
            )}
          </Box>

          <Divider />

          <List>
            {filteredMobileNavItems.map((item) => (
                <ListItemButton
                    key={item.path}
                    component={Link}
                    to={item.path}
                    onClick={() => setMobileDrawerOpen(false)}
                    selected={isActive(item.path)}
                >
                  <ListItemIcon>{item.icon}</ListItemIcon>
                  <ListItemText primary={item.label} />
                </ListItemButton>
            ))}
          </List>

          <Divider />

          <List>
            <ListItem>
              <ListItemText primary={t('categories.categories')} secondary={t('categories.browseByCategory')} />
            </ListItem>
            {categories.map((category) => (
                <ListItemButton
                    key={category.name}
                    component={Link}
                    to={category.path}
                    onClick={() => setMobileDrawerOpen(false)}
                    sx={{ pl: 4 }}
                >
                  <ListItemText
                      primary={`${category.icon} ${category.name}`}
                  />
                </ListItemButton>
            ))}
          </List>

          {isAuthenticated && (
              <>
                <Divider />
                <List>
                  <ListItemButton onClick={handleLogout}>
                    <ListItemIcon><Logout /></ListItemIcon>
                    <ListItemText primary={t('navigation.logout')} />
                  </ListItemButton>
                </List>
              </>
          )}
        </Drawer>
      </AppBar>
  )
})

export default Navbar