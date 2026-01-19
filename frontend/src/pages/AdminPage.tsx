import React, { useState, useEffect } from 'react';
import {
  Container,
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
  Button,
  Chip,
  Alert,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper
} from '@mui/material';
import {
  People,
  Inventory,
  AttachMoney,
  ShoppingCart,
  Analytics,
  ArrowForward,
} from '@mui/icons-material';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { apiService } from '../services/api';

const AdminPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    totalUsers: 0,
    totalProducts: 0,
    totalOrders: 0,
    totalRevenue: 0,
    ordersToday: 0,
    revenueToday: 0,
    pendingOrders: 0,
    processingOrders: 0,
    recentOrders: [] as any[],
    topProducts: [],
    lowStockItems: 0
  });

  useEffect(() => {
    console.log('AdminPage - Component mounted, loading dashboard data...');
    loadDashboardData();
  }, [user]);

  const loadDashboardData = async () => {
    try {
      console.log('AdminPage - Loading dashboard data...');
      
      // Load common data first
      let usersCount = 0;
      let productsCount = 0;
      let lowStock = 0;

      // 1. Fetch Users (if authorized)
      if (user?.role === 'ADMIN' || user?.role === 'USER_MANAGER') {
        try {
          const usersRes = await apiService.getUsers();
          // Fix: Check for content array or totalElements if paginated
          if (usersRes.data && typeof usersRes.data.totalElements === 'number') {
             usersCount = usersRes.data.totalElements;
          } else if (Array.isArray(usersRes.data)) {
             usersCount = usersRes.data.length;
          } else if (usersRes.data && Array.isArray(usersRes.data.content)) {
             // Fallback for some pagination structures
             usersCount = usersRes.data.totalElements || usersRes.data.content.length;
          }
        } catch (e) { console.error("Failed to fetch users", e); }
      }

      // 2. Fetch Products (if authorized)
      if (user?.role === 'ADMIN' || user?.role === 'PRODUCT_MANAGER' || user?.role === 'MODERATOR') {
        try {
          const productsRes = await apiService.adminGetProducts(0, 1000); // Fetch more to check stock
          productsCount = productsRes.data?.totalElements || 0;
          
          // Calculate low stock items (quantity < 10)
          if (productsRes.data?.content) {
             lowStock = productsRes.data.content.filter((p: any) => p.quantity < 10).length;
          }
        } catch (e) { console.error("Failed to fetch products", e); }
      }

      // 3. Fetch Operational Stats (Revenue, Orders, Recent Activity)
      // This is the new endpoint we created for "Dynamic Data"
      if (user?.role === 'ADMIN' || user?.role === 'MODERATOR' || user?.role === 'STAFF') {
        try {
          const statsRes = await apiService.getDashboardStats();
          if (statsRes.success && statsRes.data) {
             const d = statsRes.data;
             setStats({
               totalUsers: usersCount,
               totalProducts: productsCount,
               totalOrders: d.totalOrders || 0,
               totalRevenue: d.totalRevenue || 0,
               ordersToday: d.ordersToday || 0,
               revenueToday: d.revenueToday || 0,
               pendingOrders: d.pendingOrders || 0,
               processingOrders: d.processingOrders || 0,
               recentOrders: d.recentOrders || [],
               topProducts: [],
               lowStockItems: lowStock
             });
             return; // Exit early as we set everything
          }
        } catch (e) { console.error("Failed to fetch dashboard stats", e); }
      }

      // Fallback update if stats call failed or unauthorized for stats
      setStats(prev => ({
        ...prev,
        totalUsers: usersCount,
        totalProducts: productsCount,
        lowStockItems: lowStock
      }));

    } catch (error) {
      console.error('Error loading dashboard data:', error);
    }
  };

  const StatCard = ({ title, value, icon, color, onClick, subValue }: any) => (
    <Card 
      sx={{ 
        height: '100%', 
        cursor: onClick ? 'pointer' : 'default',
        transition: 'all 0.3s ease',
        '&:hover': onClick ? { transform: 'translateY(-4px)', boxShadow: '0 8px 25px rgba(0,0,0,0.15)' } : {}
      }}
      onClick={onClick}
    >
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box>
            <Typography variant="h4" sx={{ fontWeight: 900, color: color, mb: 1 }}>
              {value}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 600 }}>
              {title}
            </Typography>
            {subValue && (
                <Typography variant="caption" sx={{ color: 'text.secondary', mt: 1, display: 'block' }}>
                    {subValue}
                </Typography>
            )}
          </Box>
          <Box sx={{ 
            p: 2, 
            borderRadius: 2, 
            bgcolor: `${color}15`, 
            color: color,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            {icon}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );

  const QuickActionCard = ({ title, description, icon, color, onClick }: any) => (
    <Card 
      sx={{ 
        height: '100%', 
        cursor: 'pointer',
        transition: 'all 0.3s ease',
        '&:hover': { 
          transform: 'translateY(-4px)', 
          boxShadow: '0 8px 25px rgba(0,0,0,0.15)',
          bgcolor: `${color}05`
        }
      }}
      onClick={onClick}
    >
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <Box sx={{ 
            p: 1.5, 
            borderRadius: 2, 
            bgcolor: `${color}15`, 
            color: color,
            mr: 2
          }}>
            {icon}
          </Box>
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            {title}
          </Typography>
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {description}
        </Typography>
        <Button 
          variant="outlined" 
          size="small" 
          endIcon={<ArrowForward />}
          sx={{ color: color, borderColor: color }}
        >
          Manage
        </Button>
      </CardContent>
    </Card>
  );

  // Check if user has admin-like role
  const hasAdminAccess = user && ['ADMIN', 'PRODUCT_MANAGER', 'USER_MANAGER', 'MODERATOR'].includes(user.role);
  
  if (!user || !hasAdminAccess) {
    return (
      <Container maxWidth="md" sx={{ py: 8, textAlign: 'center' }}>
        <Alert severity="error" sx={{ mb: 3 }}>
          Access denied. Admin privileges required.
        </Alert>
        <Button variant="contained" onClick={() => navigate('/')}>
          Go to Home
        </Button>
      </Container>
    );
  }

  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h3" sx={{ fontWeight: 900, mb: 1, background: 'linear-gradient(45deg, #667eea 0%, #764ba2 100%)', backgroundClip: 'text', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
          Admin Dashboard
        </Typography>
        <Typography variant="h6" color="text.secondary">
          Welcome back, {user.firstName}! Here's what's happening with your store today.
        </Typography>
      </Box>

      {/* Stats Cards - Hiển thị theo role */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {/* Users Card */}
        {(user?.role === 'ADMIN' || user?.role === 'USER_MANAGER') && (
          <Grid item xs={12} sm={6} md={3}>
            <StatCard
              title="Total Users"
              value={stats.totalUsers.toLocaleString()}
              icon={<People sx={{ fontSize: 28 }} />}
              color="#3b82f6"
              onClick={() => navigate('/admin/users')}
            />
          </Grid>
        )}
        
        {/* Products Card */}
        {(user?.role === 'ADMIN' || user?.role === 'PRODUCT_MANAGER') && (
          <Grid item xs={12} sm={6} md={3}>
            <StatCard
              title="Total Products"
              value={stats.totalProducts.toLocaleString()}
              subValue={`${stats.lowStockItems} low stock items`}
              icon={<Inventory sx={{ fontSize: 28 }} />}
              color="#10b981"
              onClick={() => navigate('/admin/products')}
            />
          </Grid>
        )}
        
        {/* Orders Card */}
        {(user?.role === 'ADMIN' || user?.role === 'MODERATOR') && (
          <Grid item xs={12} sm={6} md={3}>
            <StatCard
              title="Total Orders"
              value={stats.totalOrders.toLocaleString()}
              subValue={`${stats.ordersToday} today`}
              icon={<ShoppingCart sx={{ fontSize: 28 }} />}
              color="#f59e0b"
              onClick={() => navigate('/admin/orders')}
            />
          </Grid>
        )}
        
        {/* Revenue Card */}
        {user?.role === 'ADMIN' && (
          <Grid item xs={12} sm={6} md={3}>
            <StatCard
              title="Total Revenue"
              value={stats.totalRevenue.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' })}
              subValue={`+${stats.revenueToday.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' })} today`}
              icon={<AttachMoney sx={{ fontSize: 28 }} />}
              color="#ef4444"
            />
          </Grid>
        )}
      </Grid>

      {/* Quick Actions - Hiển thị theo role */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {(user?.role === 'ADMIN' || user?.role === 'PRODUCT_MANAGER') && (
          <Grid item xs={12} md={6}>
            <QuickActionCard
              title="Manage Products"
              description="Add, edit, or remove products from your inventory"
              icon={<Inventory />}
              color="#10b981"
              onClick={() => navigate('/admin/products')}
            />
          </Grid>
        )}
        
        {(user?.role === 'ADMIN' || user?.role === 'MODERATOR') && (
          <Grid item xs={12} md={6}>
            <QuickActionCard
              title="Process Orders"
              description="View and manage customer orders and shipments"
              icon={<ShoppingCart />}
              color="#f59e0b"
              onClick={() => navigate('/admin/orders')}
            />
          </Grid>
        )}
        
        {(user?.role === 'ADMIN' || user?.role === 'USER_MANAGER') && (
          <Grid item xs={12} md={6}>
            <QuickActionCard
              title="User Management"
              description="Manage user accounts, roles, and permissions"
              icon={<People />}
              color="#3b82f6"
              onClick={() => navigate('/admin/users')}
            />
          </Grid>
        )}
        
        {user?.role === 'ADMIN' && (
          <Grid item xs={12} md={6}>
            <QuickActionCard
              title="Analytics & Reports"
              description="View detailed sales reports, charts, and business insights"
              icon={<Analytics />}
              color="#8b5cf6"
              onClick={() => navigate('/admin/analytics')}
            />
          </Grid>
        )}
      </Grid>

      {/* Recent Activity & Quick Stats */}
      <Grid container spacing={3}>
        {/* Recent Orders Table */}
        <Grid item xs={12} md={8}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
                <Typography variant="h6" sx={{ fontWeight: 700 }}>
                  Recent Orders (Live)
                </Typography>
                <Button size="small" endIcon={<ArrowForward />} onClick={() => navigate('/admin/orders')}>
                  View All
                </Button>
              </Box>
              
              {stats.recentOrders.length > 0 ? (
                <TableContainer component={Paper} elevation={0}>
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell>Order ID</TableCell>
                                <TableCell>Customer</TableCell>
                                <TableCell>Amount</TableCell>
                                <TableCell>Status</TableCell>
                                <TableCell>Date</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {stats.recentOrders.map((order: any) => (
                                <TableRow key={order.id} hover>
                                    <TableCell>{order.orderNumber}</TableCell>
                                    <TableCell>{order.userId}</TableCell>
                                    <TableCell>{order.totalAmount?.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' })}</TableCell>
                                    <TableCell>
                                        <Chip 
                                            label={order.status} 
                                            size="small" 
                                            color={
                                                order.status === 'COMPLETED' || order.status === 'DELIVERED' ? 'success' :
                                                order.status === 'PENDING' ? 'warning' :
                                                order.status === 'CANCELLED' ? 'error' : 'primary'
                                            } 
                                        />
                                    </TableCell>
                                    <TableCell>{new Date(order.createdAt).toLocaleDateString()}</TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
              ) : (
                <Box sx={{ minHeight: 300, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <Typography variant="body2" color="text.secondary">
                    No recent activity to display
                    </Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Operational Quick Stats */}
        <Grid item xs={12} md={4}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>
                Operational Status
              </Typography>
              <Stack spacing={2}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">
                    Pending Orders (Action Required)
                  </Typography>
                  <Chip label={stats.pendingOrders} size="small" color={stats.pendingOrders > 0 ? "warning" : "default"} />
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">
                    Processing Orders
                  </Typography>
                  <Chip label={stats.processingOrders} size="small" color="info" />
                </Box>
                 <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">
                    Orders Today
                  </Typography>
                  <Chip label={stats.ordersToday} size="small" color="primary" />
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">
                    Low Stock Items
                  </Typography>
                  <Chip label={stats.lowStockItems} size="small" color={stats.lowStockItems > 0 ? "error" : "success"} />
                </Box>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Container>
  );
};

export default AdminPage;