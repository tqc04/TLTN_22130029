import React from 'react'
import { Box, Drawer, List, ListItemButton, ListItemIcon, ListItemText, Toolbar, AppBar, Typography, IconButton, Divider, Avatar, Chip } from '@mui/material'
import { Dashboard, People, Inventory, ShoppingCart, LocalOffer, Analytics, Store, ExitToApp, Build, Chat, RateReview } from '@mui/icons-material'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const drawerWidth = 260

const AdminLayout: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout, refreshUser } = useAuth()
  
  // Refresh user info from database when entering admin page
  React.useEffect(() => {
    // Refresh immediately when entering admin page
    refreshUser().catch(error => {
      console.error('Failed to refresh user info on mount:', error)
    })
    
    // Also refresh periodically to catch role changes
    const interval = setInterval(() => {
      refreshUser().catch(error => {
        console.error('Failed to refresh user info:', error)
      })
    }, 5000) // Every 5 seconds
    
    return () => clearInterval(interval)
  }, [refreshUser])
  
  // Debug logging on component mount
  React.useEffect(() => {
    console.log('AdminLayout component mounted')
    console.log('Current user:', user)
    console.log('Current location:', location.pathname)
    console.log('User role:', user?.role)
    console.log('Has admin access:', hasAdminAccess(user?.role))
  }, [user, location.pathname])

  // Helper function to check if user has admin access (including PRODUCT_MANAGER, etc.)
  const hasAdminAccess = (role: string | undefined): boolean => {
    if (!role) return false
    const adminRoles = ['ADMIN', 'PRODUCT_MANAGER', 'USER_MANAGER', 'MODERATOR', 'REPAIR_TECHNICIAN']
    return adminRoles.includes(role.toUpperCase())
  }

  // Role check when accessing admin pages
  React.useEffect(() => {
    if (user && !hasAdminAccess(user.role)) {
      console.log('🚨 ACCESS DENIED: User does not have admin access, redirecting to login');
      console.log('User role:', user.role);
      
      // Show notification before logout
      const event = new CustomEvent('showToast', {
        detail: { 
          type: 'error', 
          message: '🚨 Bạn không có quyền truy cập trang admin. Bạn sẽ bị đăng xuất.' 
        }
      });
      window.dispatchEvent(event);
      
      // Delay logout to show notification
      setTimeout(() => {
        logout();
        navigate('/login');
      }, 2000);
    }
  }, [user, logout, navigate])

  // Real-time role check every 5 seconds
  React.useEffect(() => {
    if (!user || !hasAdminAccess(user.role)) {
      return; // Already no admin access, no need to check
    }

    const interval = setInterval(async () => {
      try {
        // Refresh user info from database
        await refreshUser()
        
        // Get updated user from localStorage (refreshUser already updated it)
        const storedUser = localStorage.getItem('auth_user')
        if (storedUser) {
          const updatedUser = JSON.parse(storedUser)
          const updatedRole = updatedUser.role?.toUpperCase()
          const currentRole = user.role?.toUpperCase()
          
          // Skip if role hasn't changed
          if (updatedRole === currentRole) {
            return;
          }
          
          console.log('🔄 Role change detected:', {
            oldRole: currentRole,
            newRole: updatedRole,
            wasUser: currentRole === 'USER',
            isUser: updatedRole === 'USER'
          });
          
          // Logic 1: Từ USER lên role khác → Cho phép, không logout (nâng quyền)
          if (currentRole === 'USER' && updatedRole !== 'USER') {
            console.log('✅ Role upgraded from USER to', updatedRole, '- No logout required');
            // refreshUser already updated the user state, no action needed
            return;
          }
          
          // Logic 2: Kiểm tra hạ quyền (từ role cao xuống role thấp hơn, hoặc từ role khác về USER)
          const rolePriority: { [key: string]: number } = {
            'ADMIN': 6,
            'MODERATOR': 5,
            'PRODUCT_MANAGER': 4,
            'USER_MANAGER': 3,
            'SUPPORT': 2,
            'USER': 1
          };
          
          const oldPriority = rolePriority[currentRole] || 0;
          const newPriority = rolePriority[updatedRole] || 0;
          const isDowngrade = currentRole !== 'USER' && (newPriority < oldPriority || updatedRole === 'USER');
          
          // Nếu bị hạ quyền → Logout ngay với thông báo
          if (isDowngrade) {
            console.log('🚨 ROLE DOWNGRADE DETECTED:', {
              oldRole: currentRole,
              newRole: updatedRole,
              oldPriority,
              newPriority
            });
            
            // Hiển thị thông báo hạ quyền rõ ràng
            const downgradeMessage = updatedRole === 'USER' 
              ? `🚨 Bạn đã bị hạ quyền từ ${currentRole} về USER. Bạn sẽ bị đăng xuất trong 3 giây.`
              : `🚨 Bạn đã bị hạ quyền từ ${currentRole} xuống ${updatedRole}. Bạn sẽ bị đăng xuất trong 3 giây.`;
            
            const event = new CustomEvent('showToast', {
              detail: { 
                type: 'error', 
                message: downgradeMessage
              }
            });
            window.dispatchEvent(event);
            
            // Logout sau 3 giây để user đọc được thông báo (will broadcast to all tabs)
            setTimeout(() => {
              logout(`Role downgraded from ${currentRole} to ${updatedRole}`);
              navigate('/login');
            }, 3000);
            return;
          }
          
          // Logic 3: Từ role khác (không phải USER) bị thay đổi sang role khác (không phải hạ quyền) → Logout ngay
          if (currentRole !== 'USER' && updatedRole !== currentRole && !isDowngrade) {
            console.log('🚨 Role changed from', currentRole, 'to', updatedRole, '- Logout required');
            
            // Show notification
            const event = new CustomEvent('showToast', {
              detail: { 
                type: 'error', 
                message: `🚨 Quyền của bạn đã bị thay đổi từ ${currentRole} sang ${updatedRole}. Bạn sẽ bị đăng xuất trong 3 giây.` 
              }
            });
            window.dispatchEvent(event);
            
            // Logout sau 3 giây (will broadcast to all tabs)
            setTimeout(() => {
              logout(`Role changed from ${currentRole} to ${updatedRole}`);
              navigate('/login');
            }, 3000);
            return;
          }
          
          // Logic 4: Nếu role thay đổi và không còn admin access (fallback)
          if (updatedRole !== currentRole && !hasAdminAccess(updatedRole) && currentRole !== 'USER') {
            console.log('🚨 ROLE CHANGED: User lost admin access');
            console.log('Old role:', currentRole, 'New role:', updatedRole);
            
            // Show notification
            const event = new CustomEvent('showToast', {
              detail: { 
                type: 'error', 
                message: `🚨 Quyền admin của bạn đã bị thu hồi (từ ${currentRole} về ${updatedRole}). Bạn sẽ bị đăng xuất trong 3 giây.` 
              }
            });
            window.dispatchEvent(event);
            
            // Force logout sau 3 giây (will broadcast to all tabs)
            setTimeout(() => {
              logout('Admin access revoked');
              navigate('/login');
            }, 3000);
          }
        }
      } catch (error) {
        console.error('Failed to check role update:', error)
      }
    }, 5000); // Check every 5 seconds

    return () => clearInterval(interval);
  }, [user, logout, navigate, refreshUser])


  const menuItems = [
    {
      label: 'Dashboard',
      icon: <Dashboard />,
      path: '/admin',
      color: '#3b82f6',
      roles: ['ADMIN', 'PRODUCT_MANAGER', 'USER_MANAGER', 'MODERATOR', 'REPAIR_TECHNICIAN']
    },
    {
      label: 'Orders',
      icon: <ShoppingCart />,
      path: '/admin/orders',
      color: '#f59e0b',
      roles: ['ADMIN', 'MODERATOR']
    },
    {
      label: 'Warranty',
      icon: <Build />,
      path: '/admin/warranty',
      color: '#06b6d4',
      roles: ['ADMIN', 'REPAIR_TECHNICIAN']
    },
    {
      label: 'Products',
      icon: <Inventory />,
      path: '/admin/products',
      color: '#10b981',
      roles: ['ADMIN', 'PRODUCT_MANAGER']
    },
    { 
      label: 'Users', 
      icon: <People />, 
      path: '/admin/users',
      color: '#8b5cf6',
      roles: ['ADMIN', 'USER_MANAGER']
    },
    { 
      label: 'Vouchers', 
      icon: <LocalOffer />, 
      path: '/admin/vouchers',
      color: '#ef4444',
      roles: ['ADMIN', 'MODERATOR']
    },
    { 
      label: 'Analytics', 
      icon: <Analytics />, 
      path: '/admin/analytics',
      color: '#06b6d4',
      roles: ['ADMIN']
    },
    { 
      label: 'Inventory', 
      icon: <Inventory />, 
      path: '/admin/inventory',
      color: '#84cc16',
      roles: ['ADMIN', 'MODERATOR']
    },
    { 
      label: 'Chat Logs', 
      icon: <Chat />, 
      path: '/admin/chat-logs',
      color: '#ec4899',
      roles: ['ADMIN']
    },
    { 
      label: 'Reviews', 
      icon: <RateReview />, 
      path: '/admin/reviews',
      color: '#f59e0b',
      roles: ['ADMIN', 'MODERATOR']
    },
  ]

  // Filter menu items theo role của user
  const filteredMenuItems = menuItems.filter(item => 
    item.roles.includes(user?.role || 'USER')
  );

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      {/* App Bar */}
      <AppBar 
        position="fixed" 
        sx={{ 
          zIndex: (theme) => theme.zIndex.drawer + 1, 
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          boxShadow: '0 8px 32px rgba(102, 126, 234, 0.3)',
          backdropFilter: 'blur(10px)'
        }}
      >
        <Toolbar sx={{ px: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Store sx={{ fontSize: 32, color: 'white' }} />
            <Typography variant="h5" sx={{ fontWeight: 900, letterSpacing: 0.5, color: 'white' }}>
              TechHub Admin
            </Typography>
          </Box>
          
          <Box sx={{ flexGrow: 1 }} />
          
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Chip 
              label={user?.role || 'Admin'} 
              size="small" 
              sx={{ 
                bgcolor: 'rgba(255,255,255,0.2)', 
                color: 'white', 
                fontWeight: 700,
                border: '1px solid rgba(255,255,255,0.3)'
              }} 
            />
            <Avatar 
              sx={{ 
                width: 36, 
                height: 36, 
                bgcolor: 'rgba(255,255,255,0.2)',
                border: '2px solid rgba(255,255,255,0.3)'
              }}
            >
              {user?.firstName?.charAt(0) || 'A'}
            </Avatar>
            <IconButton 
              color="inherit" 
              onClick={() => navigate('/')}
              sx={{ 
                bgcolor: 'rgba(255,255,255,0.1)',
                '&:hover': { bgcolor: 'rgba(255,255,255,0.2)' }
              }}
            >
              <Store />
            </IconButton>
            <IconButton 
              color="inherit" 
              onClick={handleLogout}
              sx={{ 
                bgcolor: 'rgba(255,255,255,0.1)',
                '&:hover': { bgcolor: 'rgba(255,255,255,0.2)' }
              }}
            >
              <ExitToApp />
            </IconButton>
          </Box>
        </Toolbar>
      </AppBar>

      {/* Sidebar */}
      <Drawer
        variant="permanent"
        sx={{
          width: drawerWidth,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: { 
            width: drawerWidth, 
            boxSizing: 'border-box', 
            borderRight: '1px solid rgba(0,0,0,0.08)', 
            background: 'linear-gradient(180deg, #ffffff 0%, #f8fafc 100%)',
            boxShadow: '4px 0 20px rgba(0,0,0,0.05)'
          },
        }}
      >
        <Toolbar />
        <Box sx={{ px: 3, py: 2 }}>
          <Typography variant="overline" sx={{ 
            color: 'text.secondary', 
            fontWeight: 800, 
            letterSpacing: 1.5,
            fontSize: '0.75rem'
          }}>
            ADMIN NAVIGATION
          </Typography>
        </Box>
        
        <List sx={{ px: 2 }}>
          {filteredMenuItems.map((item) => {
            // Kiểm tra user có quyền truy cập menu này không
            const hasAccess = item.roles.includes(user?.role || 'USER');
            
            return (
              <ListItemButton
                key={item.path}
                selected={location.pathname === item.path}
                onClick={() => {
                  if (hasAccess) {
                    navigate(item.path);
                  } else {
                    // Hiển thị thông báo không có quyền
                    const event = new CustomEvent('showToast', {
                      detail: { 
                        type: 'warning', 
                        message: `🚫 Bạn không có quyền truy cập ${item.label}` 
                      }
                    });
                    window.dispatchEvent(event);
                  }
                }}
                disabled={!hasAccess}
                sx={{
                  borderRadius: 2,
                  mb: 0.5,
                  transition: 'all 0.3s ease',
                  opacity: hasAccess ? 1 : 0.4, // Làm nhòe menu không được phép
                  filter: hasAccess ? 'none' : 'grayscale(50%)', // Thêm hiệu ứng nhòe
                  cursor: hasAccess ? 'pointer' : 'not-allowed',
                  '&.Mui-selected': {
                    bgcolor: hasAccess ? `${item.color}15` : 'rgba(0,0,0,0.05)',
                    color: hasAccess ? item.color : 'text.disabled',
                    '&:hover': {
                      bgcolor: hasAccess ? `${item.color}20` : 'rgba(0,0,0,0.05)',
                    },
                    '& .MuiListItemIcon-root': {
                      color: hasAccess ? item.color : 'text.disabled',
                    },
                  },
                  '&:hover': {
                    bgcolor: hasAccess ? 'rgba(0,0,0,0.04)' : 'rgba(0,0,0,0.02)',
                    transform: hasAccess ? 'translateX(4px)' : 'none',
                  },
                  '&.Mui-disabled': {
                    opacity: 0.4,
                    color: 'text.disabled',
                    '& .MuiListItemIcon-root': {
                      color: 'text.disabled',
                    },
                  }
                }}
              >
                <ListItemIcon sx={{ 
                  minWidth: 40,
                  color: hasAccess 
                    ? (location.pathname === item.path ? item.color : 'text.secondary')
                    : 'text.disabled'
                }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText 
                  primary={item.label} 
                  primaryTypographyProps={{
                    fontWeight: location.pathname === item.path ? 700 : 600,
                    fontSize: '0.9rem',
                    color: hasAccess ? 'inherit' : 'text.disabled'
                  }}
                />
              </ListItemButton>
            );
          })}
        </List>
        
        <Divider sx={{ my: 2, mx: 2 }} />
        
        <Box sx={{ px: 3, py: 2 }}>
          <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
            Logged in as: {user?.firstName} {user?.lastName}
          </Typography>
        </Box>
      </Drawer>

      {/* Main Content */}
      <Box component="main" sx={{ 
        flexGrow: 1, 
        background: 'linear-gradient(135deg, #f8fafc 0%, #ffffff 100%)',
        minHeight: '100vh'
      }}>
        <Toolbar />
        <Box sx={{ p: 3 }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  )
}

export default AdminLayout


