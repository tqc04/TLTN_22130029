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
    recentOrders: [],
    topProducts: [],
    userGrowth: [],
  });

  useEffect(() => {
    console.log('AdminPage - Component mounted, loading dashboard data...');
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      console.log('AdminPage - Loading dashboard data...');
      
      // Load data based on user role
      if (user?.role === 'ADMIN') {
        // Admin can see everything
        const [usersRes, productsRes, ordersRes] = await Promise.all([
          apiService.getUsers(),
          apiService.adminGetProducts(0, 1),
          apiService.adminGetOrders(0, 1),
        ]);
        
        setStats({
          totalUsers: Array.isArray(usersRes.data) ? usersRes.data.length : 0,
          totalProducts: productsRes.data?.totalElements || 0,
          totalOrders: ordersRes.data?.totalElements || 0,
          totalRevenue: 0,
          recentOrders: [],
          topProducts: [],
          userGrowth: [],
        });
      } else if (user?.role === 'PRODUCT_MANAGER') {
        // Product Manager only sees product-related data
        const productsRes = await apiService.adminGetProducts(0, 1);
        
        setStats({
          totalUsers: 0, // Not accessible
          totalProducts: productsRes.data?.totalElements || 0,
          totalOrders: 0, // Not accessible
          totalRevenue: 0,
          recentOrders: [],
          topProducts: [],
          userGrowth: [],
        });
      } else if (user?.role === 'USER_MANAGER') {
        // User Manager only sees user-related data
        const usersRes = await apiService.getUsers();
        
        setStats({
          totalUsers: Array.isArray(usersRes.data) ? usersRes.data.length : 0,
          totalProducts: 0, // Not accessible
          totalOrders: 0, // Not accessible
          totalRevenue: 0,
          recentOrders: [],
          topProducts: [],
          userGrowth: [],
        });
      } else if (user?.role === 'MODERATOR') {
        // Moderator sees orders and inventory
        const [ordersRes, productsRes] = await Promise.all([
          apiService.adminGetOrders(0, 1),
          apiService.adminGetProducts(0, 1),
        ]);
        
        setStats({
          totalUsers: 0, // Not accessible
          totalProducts: productsRes.data?.totalElements || 0,
          totalOrders: ordersRes.data?.totalElements || 0,
          totalRevenue: 0,
          recentOrders: [],
          topProducts: [],
          userGrowth: [],
        });
      }
      
      console.log('AdminPage - Dashboard data loaded for role:', user?.role);
    } catch (error) {
      console.error('Error loading dashboard data:', error);
    }
  };

  const StatCard = ({ title, value, icon, color, onClick }: any) => (
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
          Welcome back, {user.firstName}! Here's what's happening with your store.
        </Typography>
      </Box>

      {/* Stats Cards - Hiển thị theo role */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {/* Users Card - Chỉ hiển thị cho ADMIN và USER_MANAGER */}
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
        
        {/* Products Card - Chỉ hiển thị cho ADMIN và PRODUCT_MANAGER */}
        {(user?.role === 'ADMIN' || user?.role === 'PRODUCT_MANAGER') && (
          <Grid item xs={12} sm={6} md={3}>
            <StatCard
              title="Total Products"
              value={stats.totalProducts.toLocaleString()}
              icon={<Inventory sx={{ fontSize: 28 }} />}
              color="#10b981"
              onClick={() => navigate('/admin/products')}
            />
          </Grid>
        )}
        
        {/* Orders Card - Chỉ hiển thị cho ADMIN và MODERATOR */}
        {(user?.role === 'ADMIN' || user?.role === 'MODERATOR') && (
          <Grid item xs={12} sm={6} md={3}>
            <StatCard
              title="Total Orders"
              value={stats.totalOrders.toLocaleString()}
              icon={<ShoppingCart sx={{ fontSize: 28 }} />}
              color="#f59e0b"
              onClick={() => navigate('/admin/orders')}
            />
          </Grid>
        )}
        
        {/* Revenue Card - Chỉ hiển thị cho ADMIN */}
        {user?.role === 'ADMIN' && (
          <Grid item xs={12} sm={6} md={3}>
            <StatCard
              title="Total Revenue"
              value={`$${stats.totalRevenue.toLocaleString()}`}
              icon={<AttachMoney sx={{ fontSize: 28 }} />}
              color="#ef4444"
            />
          </Grid>
        )}
      </Grid>

      {/* Quick Actions - Hiển thị theo role */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {/* Products Management - Chỉ hiển thị cho ADMIN và PRODUCT_MANAGER */}
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
        
        {/* Orders Management - Chỉ hiển thị cho ADMIN và MODERATOR */}
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
        
        {/* User Management - Chỉ hiển thị cho ADMIN và USER_MANAGER */}
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
        
        {/* Analytics - Chỉ hiển thị cho ADMIN */}
        {user?.role === 'ADMIN' && (
          <Grid item xs={12} md={6}>
            <QuickActionCard
              title="Analytics & Reports"
              description="View sales reports, user analytics, and insights"
              icon={<Analytics />}
              color="#8b5cf6"
              onClick={() => navigate('/admin/analytics')}
            />
          </Grid>
        )}
      </Grid>

      {/* Recent Activity */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
                <Typography variant="h6" sx={{ fontWeight: 700 }}>
                  Recent Activity
                </Typography>
                <Button size="small" endIcon={<ArrowForward />}>
                  View All
                </Button>
              </Box>
              <Box sx={{ minHeight: 300, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Typography variant="body2" color="text.secondary">
                  No recent activity to display
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={4}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 3 }}>
                Quick Stats
              </Typography>
              <Stack spacing={2}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">
                    Orders Today
                  </Typography>
                  <Chip label="0" size="small" color="primary" />
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">
                    New Users
                  </Typography>
                  <Chip label="0" size="small" color="success" />
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Typography variant="body2" color="text.secondary">
                    Low Stock Items
                  </Typography>
                  <Chip label="0" size="small" color="warning" />
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