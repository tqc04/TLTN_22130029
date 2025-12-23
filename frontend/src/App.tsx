import React, { Suspense } from 'react'
import { Skeleton, Box, Typography } from '@mui/material'
import { Routes, Route, useLocation } from 'react-router-dom'

import { CartProvider } from './contexts/CartContext'
import { FavoritesProvider } from './contexts/FavoritesContext'
import { CompareProvider } from './contexts/CompareContext'
import { useAuth } from './contexts/AuthContext'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'
import Footer from './components/Footer'
import ChatWidget from './components/ChatWidget'
import ToastNotification from './components/ToastNotification'
import ErrorBoundary from './components/ErrorBoundary'
import HomePage from './pages/HomePage'
import AdminLayout from './admin/AdminLayout'
import OrdersAdminPage from './admin/OrdersAdminPage'
import ProductsAdminPage from './admin/ProductsAdminPage'
import UsersAdminPage from './admin/UsersAdminPage'
import VoucherAdminPage from './admin/VoucherAdminPage'
import AnalyticsAdminPage from './admin/AnalyticsAdminPage'
import InventoryAdminPage from './admin/InventoryAdminPage'
import ChatLogsPage from './admin/ChatLogsPage'
import WarrantyAdminPage from './admin/WarrantyAdminPage'
import ReviewsAdminPage from './admin/ReviewsAdminPage'

// Loading fallback component for better UX
const LoadingFallback = () => (
  <Box
    display="flex"
    flexDirection="column"
    alignItems="center"
    justifyContent="center"
    minHeight="100vh"
    gap={2}
  >
    <Skeleton variant="circular" width={60} height={60} />
    <Typography variant="h6" color="text.secondary">
      Đang tải...
    </Typography>
  </Box>
)

// Lazy load components for better performance
const ProductsPage = React.lazy(() => import('./pages/ProductsPage'))
const ProductDetailPage = React.lazy(() => import('./pages/ProductDetailPage'))
const ShoppingCartPage = React.lazy(() => import('./pages/ShoppingCartPage'))
const CheckoutPage = React.lazy(() => import('./pages/CheckoutPage'))
const OrdersPage = React.lazy(() => import('./pages/OrdersPage'))
const AdminPage = React.lazy(() => import('./pages/AdminPage'))
const LoginPage = React.lazy(() => import('./pages/LoginPage'))
const SignUpPage = React.lazy(() => import('./pages/SignUpPage'))
const ForgotPasswordPage = React.lazy(() => import('./pages/ForgotPasswordPage'))
const ProfilePage = React.lazy(() => import('./pages/ProfilePage'))
const FavoritesPage = React.lazy(() => import('./pages/FavoritesPage'))
const WishlistPage = React.lazy(() => import('./pages/WishlistPage'))
const CompareProductsPage = React.lazy(() => import('./pages/CompareProductsPage'))
const ReviewsPage = React.lazy(() => import('./pages/ReviewsPage'))
const ChatbotPage = React.lazy(() => import('./pages/ChatbotPage'))
const SupportPage = React.lazy(() => import('./pages/SupportPage'))
const PaymentResultPage = React.lazy(() => import('./pages/PaymentResultPage'))
const VNPayReturnPage = React.lazy(() => import('./pages/VNPayReturnPage'))
const VerifyEmailPage = React.lazy(() => import('./pages/VerifyEmailPage'))
const OAuth2SuccessPage = React.lazy(() => import('./pages/OAuth2SuccessPage'))
const OAuth2CompleteSignupPage = React.lazy(() => import('./pages/OAuth2CompleteSignupPage'))
const PhoneSignUpPage = React.lazy(() => import('./pages/PhoneSignUpPage'))
const SaleProductsPage = React.lazy(() => import('./pages/SaleProductsPage'))
const MLTestPage = React.lazy(() => import('./pages/MLTestPage'))
const ResetPasswordPage = React.lazy(() => import('./pages/ResetPasswordPage'))
const VouchersPage = React.lazy(() => import('./pages/VouchersPage'))

// Component để wrap CartProvider với userId từ AuthContext
const AppContent: React.FC = () => {
  const { user } = useAuth()
  const location = useLocation()
  const authPaths = ['/login', '/signup', '/forgot-password', '/verify-email']
  const isAuthPage = authPaths.includes(location.pathname)
  const isAdminPage = location.pathname.startsWith('/admin')
  
  // Toast notification state
  const [toast, setToast] = React.useState<{
    open: boolean;
    type: 'success' | 'error' | 'warning';
    message: string;
  }>({
    open: false,
    type: 'success',
    message: ''
  });

  // Listen for toast events
  React.useEffect(() => {
    const handleShowToast = (event: CustomEvent) => {
      setToast({
        open: true,
        type: event.detail.type,
        message: event.detail.message
      });
    };

    window.addEventListener('showToast', handleShowToast as EventListener);
    return () => window.removeEventListener('showToast', handleShowToast as EventListener);
  }, []);
  
  return (
    <ErrorBoundary>
      <CompareProvider>
        <FavoritesProvider userId={user?.id}>
          <CartProvider userId={user?.id}>
        {/* Toast Notification */}
        <ToastNotification
          open={toast.open}
          type={toast.type}
          message={toast.message}
          onClose={() => setToast({ ...toast, open: false })}
        />
        
        {!isAdminPage && <Navbar />}
        <Box sx={{
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          background: isAuthPage
            ? 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
            : 'none',
          alignItems: isAuthPage ? 'center' : 'stretch',
          justifyContent: isAuthPage ? 'center' : 'flex-start',
          py: isAuthPage ? 8 : 0,
          pt: isAuthPage ? 8 : (isAdminPage ? 0 : (location.pathname === '/' ? 0 : '40px'))
        }}>
          <Suspense fallback={<LoadingFallback />}>
            <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/products" element={<ProductsPage />} />
            <Route path="/products/:productId" element={<ProductDetailPage />} />
            {/* Support both /product/:id and /products/:id */}
            <Route path="/product/:productId" element={<ProductDetailPage />} />
            <Route path="/sale" element={<SaleProductsPage />} />
            <Route path="/cart" element={<ShoppingCartPage />} />
            <Route path="/checkout" element={<CheckoutPage />} />
            <Route path="/orders" element={<OrdersPage />} />
            <Route path="/admin" element={
              <ProtectedRoute requireRole={['ADMIN', 'PRODUCT_MANAGER', 'USER_MANAGER', 'MODERATOR', 'REPAIR_TECHNICIAN']}>
                <AdminLayout />
              </ProtectedRoute>
            }>
              <Route index element={<AdminPage />} />
              <Route path="orders" element={
                <ProtectedRoute requireRole={['ADMIN', 'MODERATOR']}>
                  <OrdersAdminPage />
                </ProtectedRoute>
              } />
              <Route path="warranty" element={
                <ProtectedRoute requireRole={['ADMIN', 'REPAIR_TECHNICIAN']}>
                  <WarrantyAdminPage />
                </ProtectedRoute>
              } />
              <Route path="products" element={
                <ProtectedRoute requireRole={['ADMIN', 'PRODUCT_MANAGER']}>
                  <ProductsAdminPage />
                </ProtectedRoute>
              } />
              <Route path="users" element={
                <ProtectedRoute requireRole={['ADMIN', 'USER_MANAGER']}>
                  <UsersAdminPage />
                </ProtectedRoute>
              } />
              <Route path="vouchers" element={
                <ProtectedRoute requireRole={['ADMIN', 'MODERATOR']}>
                  <VoucherAdminPage />
                </ProtectedRoute>
              } />
              <Route path="analytics" element={
                <ProtectedRoute requireRole={['ADMIN']}>
                  <AnalyticsAdminPage />
                </ProtectedRoute>
              } />
              <Route path="inventory" element={
                <ProtectedRoute requireRole={['ADMIN', 'MODERATOR']}>
                  <InventoryAdminPage />
                </ProtectedRoute>
              } />
              <Route path="chat-logs" element={
                <ProtectedRoute requireRole={['ADMIN']}>
                  <ChatLogsPage />
                </ProtectedRoute>
              } />
              <Route path="reviews" element={
                <ProtectedRoute requireRole={['ADMIN', 'MODERATOR']}>
                  <ReviewsAdminPage />
                </ProtectedRoute>
              } />
            </Route>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignUpPage />} />
            <Route path="/signup-phone" element={<PhoneSignUpPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/favorites" element={<FavoritesPage />} />
            <Route path="/wishlist" element={<WishlistPage />} />
            <Route path="/compare" element={<CompareProductsPage />} />
            <Route path="/reviews" element={<ReviewsPage />} />
            <Route path="/vouchers" element={<VouchersPage />} />
            <Route path="/ai-assistant" element={<ChatbotPage />} />
            <Route path="/support" element={<SupportPage />} />
            <Route path="/verify-email" element={<VerifyEmailPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />
            {/* Google/Facebook OAuth2 callback */}
            <Route path="/oauth2-success" element={<OAuth2SuccessPage />} />
            <Route path="/oauth2/complete-signup" element={<OAuth2CompleteSignupPage />} />
            <Route path="/vn_pay/vnpayReturn" element={<VNPayReturnPage />} />
            <Route path="/payment-result" element={<PaymentResultPage />} />
            <Route path="/ml-test" element={<MLTestPage />} />
            </Routes>
          </Suspense>
        </Box>
        {!isAuthPage && !isAdminPage && <ChatWidget />}
        {!isAuthPage && !isAdminPage && <Footer />}
          </CartProvider>
        </FavoritesProvider>
      </CompareProvider>
    </ErrorBoundary>
  )
}

function App() {
  return <AppContent />
}

export default App
