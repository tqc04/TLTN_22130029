import React, { useState, useEffect } from 'react';
import {
  Popper,
  Paper,
  Box,
  Typography,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  ListItemIcon,
  IconButton,
  Divider,
  Button,
  Chip,
  CircularProgress,
} from '@mui/material';
import {
  NotificationsNone,
  Payment,
  LocalOffer,
  Settings,
  Build,
  ShoppingBag,
  Close,
  DoneAll,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { apiService, Notification } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { websocketService } from '../services/websocketService';

interface NotificationDropdownProps {
  anchorEl: HTMLElement | null;
  open: boolean;
  onClose: () => void;
  onNotificationClick?: (notification: Notification) => void;
}

const NotificationDropdown: React.FC<NotificationDropdownProps> = ({
  anchorEl,
  open,
  onClose,
  onNotificationClick,
}) => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (open && user?.id) {
      loadNotifications();
      loadUnreadCount();
    }
  }, [open, user?.id]);

  // Listen to WebSocket notifications for real-time updates
  useEffect(() => {
    if (!user?.id) return;

    const handleWebSocketNotification = () => {
      // Reload notifications when new one arrives via WebSocket
      if (open) {
        loadNotifications();
        loadUnreadCount();
      }
    };

    // Register WebSocket listener
    const unsubscribe = websocketService.onNotificationEvent(handleWebSocketNotification);

    // Cleanup on unmount
    return () => {
      if (unsubscribe) unsubscribe();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id, open]);

  const loadNotifications = async () => {
    if (!user?.id) return;
    setLoading(true);
    try {
      const response = await apiService.getUserNotifications(user.id);
      if (response.success && response.data) {
        // Parse data field if it's a string
        const parsed = response.data.map((notif: any) => {
          if (typeof notif.data === 'string' && notif.data) {
            try {
              notif.data = JSON.parse(notif.data);
            } catch {
              // Keep as string if parsing fails
            }
          }
          return notif;
        });
        
        // Sort by createdAt descending (newest first)
        const sorted = [...parsed].sort((a, b) => {
          const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
          const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
          return dateB - dateA;
        });
        setNotifications(sorted);
      }
    } catch (error) {
      console.error('Error loading notifications:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadUnreadCount = async () => {
    if (!user?.id) return;
    try {
      const response = await apiService.getUnreadCount(user.id);
      if (response.success && response.data !== undefined) {
        setUnreadCount(response.data);
      }
    } catch (error) {
      console.error('Error loading unread count:', error);
    }
  };

  const handleNotificationClick = async (notification: Notification) => {
    console.log('=== NOTIFICATION CLICK START ===');
    console.log('Notification clicked:', notification);
    console.log('Notification ID:', notification.id);
    console.log('Notification type:', notification.type);
    console.log('Notification data:', notification.data);
    console.log('Will navigate after marking as read...');
    
    try {
      // Mark as read if not read
      if (!notification.isRead) {
        try {
          console.log('Marking notification as read...');
          await apiService.markNotificationAsRead(notification.id);
          setNotifications(prev =>
            prev.map(n =>
              n.id === notification.id ? { ...n, isRead: true } : n
            )
          );
          setUnreadCount(prev => Math.max(0, prev - 1));
          console.log('Notification marked as read successfully');
        } catch (error) {
          console.error('Error marking notification as read:', error);
        }
      } else {
        console.log('Notification already read, skipping mark as read');
      }

      // Close popup first
      console.log('Closing popup...');
      onClose();

      // Navigate based on notification type and data
      // Use setTimeout to ensure popup closes before navigation
      setTimeout(() => {
        try {
          console.log('=== NAVIGATION TIMEOUT FIRED ===');
          console.log('About to navigate, onNotificationClick exists:', !!onNotificationClick);
          console.log('Navigate function exists:', !!navigate);
          
          // Always navigate, but also call custom handler if provided
          if (onNotificationClick) {
            console.log('Calling custom onNotificationClick handler');
            try {
              onNotificationClick(notification);
            } catch (customError) {
              console.error('Error in custom onNotificationClick handler:', customError);
            }
          }
          
          // Always navigate regardless of custom handler
          console.log('Calling navigateFromNotification');
          navigateFromNotification(notification);
          console.log('=== NAVIGATION COMPLETED ===');
        } catch (error) {
          console.error('=== NAVIGATION ERROR ===');
          console.error('Error navigating from notification:', error);
          console.error('Error stack:', error instanceof Error ? error.stack : 'No stack');
          // Fallback: navigate to orders page
          console.log('Fallback: navigating to /orders');
          try {
            navigate('/orders');
          } catch (fallbackError) {
            console.error('Fallback navigation also failed:', fallbackError);
          }
        }
      }, 150);
    } catch (error) {
      console.error('=== HANDLE NOTIFICATION CLICK ERROR ===');
      console.error('Unexpected error in handleNotificationClick:', error);
      // Still try to navigate
      try {
        navigate('/orders');
      } catch (navError) {
        console.error('Navigation failed:', navError);
      }
    }
    console.log('=== NOTIFICATION CLICK END ===');
  };

  const navigateFromNotification = (notification: Notification): void => {
    // Parse data if it's a string
    let data: Record<string, any> = {};
    if (typeof notification.data === 'string' && notification.data) {
      try {
        data = JSON.parse(notification.data);
      } catch (e) {
        console.warn('Failed to parse notification data:', e);
        data = {};
      }
    } else if (notification.data && typeof notification.data === 'object') {
      data = notification.data;
    }
    
    // Extract base type from notification type (e.g., ORDER_CONFIRMED -> ORDER)
    const baseType = notification.type.split('_')[0];
    
    console.log('Navigating from notification:', {
      type: notification.type,
      baseType,
      data,
      title: notification.title,
      message: notification.message
    });
    
    // Try to extract orderNumber from message if data is null
    let extractedOrderNumber: string | null = null;
    if (!data.orderNumber && notification.message) {
      // Try multiple patterns to extract order number
      const patterns = [
        /ORD-([\w-]+)/i,                    // ORD-1765087363180-749658f6
        /order[:\s]+([\w-]+)/i,             // order: 12345 or order 12345
        /#([\w-]+)/i,                       // #12345
        /order\s+([\w-]+)/i,                // order 12345
        /ORD-([\d]+-[\w]+)/i                // ORD-1765087363180-749658f6 (with dash)
      ];
      
      for (const pattern of patterns) {
        const match = notification.message.match(pattern);
        if (match) {
          extractedOrderNumber = match[1] || match[0];
          console.log('Extracted orderNumber from message:', extractedOrderNumber, 'using pattern:', pattern);
          break;
        }
      }
    }
    
    switch (baseType) {
      case 'ORDER':
        // Navigate to orders page, optionally with orderId filter
        console.log('=== ORDER CASE ===');
        console.log('data.orderId:', data.orderId);
        console.log('data.orderNumber:', data.orderNumber);
        console.log('extractedOrderNumber:', extractedOrderNumber);
        
        if (data.orderId) {
          const url = `/orders?orderId=${encodeURIComponent(data.orderId)}`;
          console.log('Navigating to (with orderId):', url);
          try {
            navigate(url);
            console.log('Navigate called successfully with orderId');
          } catch (err) {
            console.error('Navigate failed with orderId:', err);
            navigate('/orders');
          }
        } else if (data.orderNumber || extractedOrderNumber) {
          const orderNumber = data.orderNumber || extractedOrderNumber;
          const url = `/orders?orderNumber=${encodeURIComponent(orderNumber!)}`;
          console.log('Navigating to (with orderNumber):', url);
          try {
            navigate(url);
            console.log('Navigate called successfully with orderNumber');
          } catch (err) {
            console.error('Navigate failed with orderNumber:', err);
            navigate('/orders');
          }
        } else {
          console.log('Navigating to /orders (default - no orderId/orderNumber found)');
          try {
            navigate('/orders');
            console.log('Navigate called successfully (default)');
          } catch (err) {
            console.error('Navigate failed (default):', err);
          }
        }
        console.log('=== ORDER CASE END ===');
        break;
      case 'PAYMENT':
        // Navigate to orders page for payment notifications
        if (data.orderId) {
          console.log('Navigating to /orders with orderId:', data.orderId);
          navigate(`/orders?orderId=${encodeURIComponent(data.orderId)}`);
        } else if (data.orderNumber || extractedOrderNumber) {
          const orderNumber = data.orderNumber || extractedOrderNumber;
          console.log('Navigating to /orders with orderNumber:', orderNumber);
          navigate(`/orders?orderNumber=${encodeURIComponent(orderNumber!)}`);
        } else {
          console.log('Navigating to /orders (default)');
          navigate('/orders');
        }
        break;
      case 'PRODUCT':
      case 'PRICE':
        // For PRICE_DROP, PRODUCT_IN_STOCK, PRODUCT_OUT_OF_STOCK
        if (data.productId) {
          console.log('Navigating to /product/', data.productId);
          navigate(`/product/${encodeURIComponent(data.productId)}`);
        } else {
          console.log('Navigating to /products (default)');
          navigate('/products');
        }
        break;
      case 'PROMOTION':
        if (data.productId) {
          console.log('Navigating to /product/', data.productId);
          navigate(`/product/${encodeURIComponent(data.productId)}`);
        } else {
          console.log('Navigating to /products (default)');
          navigate('/products');
        }
        break;
      case 'WARRANTY':
        console.log('Navigating to /profile');
        navigate('/profile');
        break;
      default:
        // Default to orders page
        console.log('=== DEFAULT CASE ===');
        console.log('Navigating to /orders (default case)');
        try {
          navigate('/orders');
          console.log('Navigate called successfully (default case)');
        } catch (err) {
          console.error('Navigate failed (default case):', err);
        }
    }
    console.log('=== NAVIGATE FROM NOTIFICATION END ===');
  };

  const handleMarkAllAsRead = async () => {
    if (!user?.id) return;
    try {
      const response = await apiService.markAllNotificationsAsRead(user.id);
      if (response.success) {
        setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
        setUnreadCount(0);
      }
    } catch (error) {
      console.error('Error marking all as read:', error);
    }
  };

  const handleDeleteNotification = async (e: React.MouseEvent, notificationId: number) => {
    e.stopPropagation();
    try {
      const response = await apiService.deleteNotification(notificationId);
      if (response.success) {
        setNotifications(prev => prev.filter(n => n.id !== notificationId));
        // Update unread count if notification was unread
        const notification = notifications.find(n => n.id === notificationId);
        if (notification && !notification.isRead) {
          setUnreadCount(prev => Math.max(0, prev - 1));
        }
      }
    } catch (error) {
      console.error('Error deleting notification:', error);
    }
  };

  const getNotificationIcon = (type: string) => {
    // Extract base type from notification type (e.g., ORDER_CONFIRMED -> ORDER)
    const baseType = type.split('_')[0];
    
    switch (baseType) {
      case 'ORDER':
        return <ShoppingBag color="primary" />;
      case 'PAYMENT':
        return <Payment color="success" />;
      case 'PRODUCT':
      case 'PRICE':
        return <LocalOffer color="warning" />;
      case 'PROMOTION':
        return <LocalOffer color="warning" />;
      case 'WARRANTY':
        return <Build color="info" />;
      case 'SYSTEM':
      case 'ACCOUNT':
      case 'PASSWORD':
      case 'SECURITY':
        return <Settings color="action" />;
      default:
        return <NotificationsNone color="action" />;
    }
  };

  const formatTime = (dateString?: string) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Vừa xong';
    if (diffMins < 60) return `${diffMins} phút trước`;
    if (diffHours < 24) return `${diffHours} giờ trước`;
    if (diffDays < 7) return `${diffDays} ngày trước`;
    return date.toLocaleDateString('vi-VN');
  };

  return (
    <Popper
      open={open}
      anchorEl={anchorEl}
      placement="bottom-end"
      style={{ zIndex: 1300 }}
      modifiers={[
        {
          name: 'offset',
          options: {
            offset: [0, 8],
          },
        },
      ]}
    >
      <Paper
        sx={{
          width: 380,
          maxHeight: 500,
          display: 'flex',
          flexDirection: 'column',
          boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
          borderRadius: 2,
          overflow: 'hidden',
        }}
      >
        {/* Header */}
        <Box
          sx={{
            p: 2,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            borderBottom: 1,
            borderColor: 'divider',
            bgcolor: 'background.paper',
          }}
        >
          <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
            Thông báo
          </Typography>
          <Box sx={{ display: 'flex', gap: 1 }}>
            {unreadCount > 0 && (
              <Button
                size="small"
                startIcon={<DoneAll />}
                onClick={handleMarkAllAsRead}
                sx={{ textTransform: 'none' }}
              >
                Đọc tất cả
              </Button>
            )}
            <IconButton size="small" onClick={onClose}>
              <Close />
            </IconButton>
          </Box>
        </Box>

        {/* Notifications List */}
        <Box sx={{ overflowY: 'auto', maxHeight: 400 }}>
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
              <CircularProgress size={24} />
            </Box>
          ) : notifications.length === 0 ? (
            <Box sx={{ textAlign: 'center', p: 4 }}>
              <NotificationsNone sx={{ fontSize: 48, color: 'text.secondary', mb: 1 }} />
              <Typography variant="body2" color="text.secondary">
                Không có thông báo
              </Typography>
            </Box>
          ) : (
            <List sx={{ p: 0 }}>
              {notifications.map((notification, index) => (
                <React.Fragment key={notification.id}>
                  <ListItem
                    disablePadding
                    sx={{
                      bgcolor: notification.isRead ? 'transparent' : 'action.hover',
                      '&:hover': {
                        bgcolor: 'action.selected',
                      },
                    }}
                  >
                    <ListItemButton
                      onClick={(e) => {
                        console.log('=== LIST ITEM BUTTON CLICKED ===');
                        console.log('Event:', e);
                        console.log('Notification ID:', notification.id);
                        e.preventDefault();
                        e.stopPropagation();
                        console.log('Prevented default and stopped propagation');
                        console.log('Calling handleNotificationClick...');
                        try {
                          handleNotificationClick(notification);
                        } catch (error) {
                          console.error('=== ERROR IN LIST ITEM BUTTON CLICK ===');
                          console.error('Error calling handleNotificationClick:', error);
                        }
                        console.log('=== LIST ITEM BUTTON CLICK HANDLER END ===');
                      }}
                      sx={{ py: 1.5, px: 2, cursor: 'pointer' }}
                    >
                      <ListItemIcon sx={{ minWidth: 40 }}>
                        {getNotificationIcon(notification.type)}
                      </ListItemIcon>
                      <ListItemText
                        primary={
                          <Box 
                            component="span"
                            sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
                          >
                            <Typography
                              component="span"
                              variant="subtitle2"
                              sx={{
                                fontWeight: notification.isRead ? 400 : 600,
                                flex: 1,
                              }}
                            >
                              {notification.title}
                            </Typography>
                            {!notification.isRead && (
                              <Chip
                                label="Mới"
                                size="small"
                                color="primary"
                                sx={{ height: 18, fontSize: '0.65rem' }}
                              />
                            )}
                          </Box>
                        }
                        secondary={
                          <Box component="span">
                            <Typography
                              component="span"
                              variant="body2"
                              color="text.secondary"
                              sx={{
                                display: '-webkit-box',
                                WebkitLineClamp: 2,
                                WebkitBoxOrient: 'vertical',
                                overflow: 'hidden',
                                mb: 0.5,
                              }}
                            >
                              {notification.message}
                            </Typography>
                            <Typography 
                              component="span"
                              variant="caption" 
                              color="text.secondary"
                              sx={{ display: 'block' }}
                            >
                              {formatTime(notification.createdAt || notification.sentAt)}
                            </Typography>
                          </Box>
                        }
                      />
                      <IconButton
                        size="small"
                        onClick={(e) => handleDeleteNotification(e, notification.id)}
                        sx={{ ml: 1 }}
                      >
                        <Close fontSize="small" />
                      </IconButton>
                    </ListItemButton>
                  </ListItem>
                  {index < notifications.length - 1 && <Divider />}
                </React.Fragment>
              ))}
            </List>
          )}
        </Box>

        {/* Footer */}
        {notifications.length > 0 && (
          <Box
            sx={{
              p: 1.5,
              borderTop: 1,
              borderColor: 'divider',
              textAlign: 'center',
            }}
          >
            <Button
              fullWidth
              size="small"
              onClick={() => {
                navigate('/orders');
                onClose();
              }}
              sx={{ textTransform: 'none' }}
            >
              Xem tất cả
            </Button>
          </Box>
        )}
      </Paper>
    </Popper>
  );
};

export default NotificationDropdown;

