import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { apiService } from '../services/api';
import { websocketService, UserPermissionChangeEvent, ForceLogoutEvent } from '../services/websocketService';

export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  roles?: string[];
  isActive?: boolean;
  createdAt: string;
  // Optional fields used across pages
  phoneNumber?: string;
  address?: string;
  dateOfBirth?: string;
  avatarUrl?: string;
  profileImageUrl?: string;
  isEmailVerified?: boolean;
  personalizationEnabled?: boolean;
  chatbotEnabled?: boolean;
  recommendationEnabled?: boolean;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (username: string, password: string) => Promise<boolean>;
  logout: (reason?: string) => void;
  setUser: (user: User | null) => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: React.ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('auth_token'));
  const [isLoading, setIsLoading] = useState(false);

  const isAuthenticated = !!user && !!token;
  
  // Improved admin role detection - handle different case formats and admin-like roles
  const isAdmin = (() => {
    if (!user) return false;
    
    const role = user.role;
    const roles = user.roles || [];
    
    // Check exact matches for ADMIN role
    if (role === 'ADMIN' || role === 'admin' || role === 'Admin') return true;
    if (roles.includes('ADMIN') || roles.includes('admin') || roles.includes('Admin')) return true;
    
    // Check for admin-like roles that should have access to admin panel
    const adminLikeRoles = ['PRODUCT_MANAGER', 'USER_MANAGER', 'MODERATOR', 'SUPPORT', 'REPAIR_TECHNICIAN'];
    if (adminLikeRoles.includes(role)) return true;
    if (roles.some(r => adminLikeRoles.includes(r))) return true;
    
    // Check if role contains 'admin' (case insensitive)
    if (typeof role === 'string' && role.toLowerCase().includes('admin')) return true;
    if (roles.some(r => typeof r === 'string' && r.toLowerCase().includes('admin'))) return true;
    
    return false;
  })();
  
  // Admin role detection with logging in development only
  if (import.meta.env.DEV) {
    console.log('AuthContext - User:', user);
    console.log('AuthContext - isAdmin:', isAdmin);
  }

  // Helper function to determine if a role change is a downgrade
  const isRoleDowngrade = useCallback((oldRole: string, newRole: string): boolean => {
    const rolePriority: { [key: string]: number } = {
      'ADMIN': 6,
      'MODERATOR': 5,
      'PRODUCT_MANAGER': 4,
      'USER_MANAGER': 3,
      'SUPPORT': 2,
      'USER': 1
    };
    
    const oldPriority = rolePriority[oldRole] || 0;
    const newPriority = rolePriority[newRole] || 0;
    
    // Nếu từ USER lên bất kỳ role nào khác thì không phải hạ quyền
    if (oldRole === 'USER' && newRole !== 'USER') {
      return false;
    }
    
    return newPriority < oldPriority;
  }, []);

  // Toast notification function
  const showToastNotification = (type: 'success' | 'error' | 'warning', message: string) => {
    // Create a custom event to show toast
    const event = new CustomEvent('showToast', {
      detail: { type, message }
    });
    window.dispatchEvent(event);
  };

  
  const logout = useCallback((reason?: string) => {
    // Disconnect WebSocket
    websocketService.disconnect();

    // Clear localStorage
    localStorage.removeItem('auth_token');
    localStorage.removeItem('jwt');
    localStorage.removeItem('auth_user');

    // Broadcast logout event to all tabs using BroadcastChannel
    try {
      const channel = new BroadcastChannel('auth_logout');
      channel.postMessage({ type: 'LOGOUT', reason: reason || 'User logged out' });
      channel.close();
    } catch (e) {
      // Fallback to localStorage event if BroadcastChannel not supported
      localStorage.setItem('auth_logout_event', JSON.stringify({ 
        timestamp: Date.now(), 
        reason: reason || 'User logged out' 
      }));
      localStorage.removeItem('auth_logout_event');
    }

    // Clear state
    setToken(null);
    setUser(null);
  }, []);

  // Removed unused WebSocket connection checker to satisfy lint rules

  const setupWebSocketListeners = useCallback(() => {
    // Listen for permission change events - ENABLED for real-time role changes
    websocketService.onPermissionChangeEvent((event: UserPermissionChangeEvent) => {

      // Update user role in state if it's the current user
      if (user && user.id === event.userId) {
        setUser(prevUser => prevUser ? { ...prevUser, role: event.newRole } : null);

        // Check if this is a role downgrade that requires logout
        if (event.type === 'ROLE_CHANGE') {
          const isDowngrade = isRoleDowngrade(event.oldRole, event.newRole);
          
          console.log('Role change detected:', {
            oldRole: event.oldRole,
            newRole: event.newRole,
            isDowngrade,
            user: user?.username
          });

          if (isDowngrade) {
            // Show notification about role downgrade
            const message = `🚨 Quyền hạn của bạn đã bị hạ từ ${event.oldRole} xuống ${event.newRole}. Lý do: ${event.reason || 'Không rõ'}. Bạn sẽ bị đăng xuất sau 3 giây.`;

            // Show toast notification
            showToastNotification('error', message);

            // Force logout immediately for role downgrade
            setTimeout(() => {
              logout();
              window.location.href = '/login';
            }, 3000); // Delay 3 seconds to show notification
            return;
          } else {
            // Regular role change notification (upgrade or lateral change)
            const message = `🎉 Quyền hạn của bạn đã được nâng từ ${event.oldRole} lên ${event.newRole}! ${event.reason ? `Lý do: ${event.reason}.` : ''} Trang sẽ tự động tải lại sau 2 giây.`;
            showToastNotification('success', message);
            
            // Update localStorage with new role immediately
            const updatedUser = { ...user, role: event.newRole };
            localStorage.setItem('auth_user', JSON.stringify(updatedUser));
            
            // Also update auth_token if needed to refresh JWT claims
            const currentToken = localStorage.getItem('auth_token');
            if (currentToken) {
              // Force token refresh by re-logging (optional, depends on backend)
              console.log('Role upgraded, localStorage updated');
            }
            
            // Reload page after 2 seconds to apply new permissions
            setTimeout(() => {
              window.location.reload();
            }, 2000);
          }
        }
      }
    });

    // Listen for force logout events (separate from role changes)
    websocketService.onForceLogoutEvent((event: ForceLogoutEvent) => {
      // Show notification about role downgrade
      alert(`Bạn đã bị đăng xuất: ${event.reason}`);
      
      // Force logout
      logout();
    });

    // Listen for admin notifications
    websocketService.onAdminNotificationEvent((event: UserPermissionChangeEvent) => {
      // Show admin notification
      const message = `User ${event.username} đã được thay đổi quyền từ ${event.oldRole} thành ${event.newRole} bởi ${event.changedBy}`;
      alert(message);
    });

    // Add custom event listener as backup for force logout
    const handleForceLogout = (e: CustomEvent) => {
      alert(`Bạn đã bị đăng xuất: ${e.detail.reason}`);
      logout();
    };
    
    window.addEventListener('forceLogout', handleForceLogout as EventListener);
    
    // Return cleanup function
    return () => {
      window.removeEventListener('forceLogout', handleForceLogout as EventListener);
    };
  }, [user, isRoleDowngrade, logout]);

  const initializeAuth = useCallback(() => {
    const storedToken = localStorage.getItem('auth_token') || localStorage.getItem('jwt');
    const storedUser = localStorage.getItem('auth_user');

    if (!storedToken) {
      return;
    }

    setToken(storedToken);

    // If we already have a cached user, use it
    if (storedUser) {
      try {
        const userData = JSON.parse(storedUser);
        setUser(userData);
        
        // Connect WebSocket for the existing user
        if (userData.id) {
          websocketService.connectUser(userData.id);
        }
        
        // Validate token in background without affecting UI
        apiService
          .getUserProfile()
          .then((response) => {
            const userData = response.data;
            if (userData) {
              const userWithRoles = {
                ...userData,
                roles: userData.role ? [userData.role] : [],
                isActive: true,
              };
              localStorage.setItem('auth_user', JSON.stringify(userWithRoles));
              setUser(userWithRoles);
            }
          })
          .catch((error) => {
            // Only logout if token is actually invalid
            if (error.response?.status === 401 && (
              error.response?.data?.message?.includes('token expired') ||
              error.response?.data?.message?.includes('invalid token') ||
              error.response?.data?.error?.includes('Unauthorized')
            )) {
              logout();
            }
          });
        return;
      } catch (error) {
        console.error('Error parsing stored user:', error);
        localStorage.removeItem('auth_user');
      }
    }

    // Only fetch user profile if we don't have cached user
    apiService
      .getUserProfile()
      .then((response) => {
        const userData = response.data;
        if (userData) {
          const userWithRoles = {
            ...userData,
            roles: userData.role ? [userData.role] : [],
            isActive: true,
          };
          localStorage.setItem('auth_user', JSON.stringify(userWithRoles));
          setUser(userWithRoles);
          
          // Connect WebSocket for the fetched user
          if (userData.id) {
            websocketService.connectUser(userData.id);
          }
          return;
        }
      })
      .catch((error) => {
        // Only logout if token is actually invalid
        if (error.response?.status === 401 && (
          error.response?.data?.message?.includes('token expired') ||
          error.response?.data?.message?.includes('invalid token') ||
          error.response?.data?.error?.includes('Unauthorized')
        )) {
          logout();
        }
      });
  }, []);

  const login = async (username: string, password: string): Promise<boolean> => {
    try {
      setIsLoading(true);

      // Call login API
      const response = await apiService.login(username, password);

      if (response.success && response.data.token && response.data.user) {
        const { token: authToken, user: userData } = response.data;
        
        // Add roles array if not present (for backward compatibility)
        const userWithRoles = {
          ...userData,
          roles: userData.role ? [userData.role] : [],
          isActive: true
        };

        // Store in localStorage (đồng bộ key với Google login)
        localStorage.setItem('jwt', authToken);
        localStorage.setItem('auth_token', authToken); // giữ lại cho tương thích cũ
        localStorage.setItem('auth_user', JSON.stringify(userWithRoles));

        // Update state
        setToken(authToken);
        setUser(userWithRoles);

        // Connect WebSocket for the new user
        websocketService.connectUser(userWithRoles.id);

        return true;
      }

      return false;
    } catch (error) {
      console.error('Login failed:', error);
      return false;
    } finally {
      setIsLoading(false);
    }
  };

  // Initialize authentication state
  // Listen for logout events from other tabs
  useEffect(() => {
    // Listen to BroadcastChannel
    const channel = new BroadcastChannel('auth_logout');
    channel.onmessage = (event) => {
      if (event.data.type === 'LOGOUT') {
        console.log('Received logout broadcast:', event.data.reason);
        logout();
        window.location.href = '/login';
      }
    };

    // Listen to localStorage events (fallback)
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === 'auth_logout_event' && e.newValue) {
        try {
          const data = JSON.parse(e.newValue);
          console.log('Received logout event from other tab:', data.reason);
          logout();
          window.location.href = '/login';
        } catch (error) {
          console.error('Error parsing logout event:', error);
        }
      }
    };

    window.addEventListener('storage', handleStorageChange);

    return () => {
      channel.close();
      window.removeEventListener('storage', handleStorageChange);
    };
  }, [logout]);

  useEffect(() => {
    initializeAuth();
  }, [initializeAuth]);

  // Setup WebSocket listeners when user is available
  useEffect(() => {
    if (user && user.id) {
      const cleanup = setupWebSocketListeners();

      // Global role change checker - check every 10 seconds for role downgrades
      // This ensures logout happens even when user is not on admin page
      let roleCheckInterval: number | undefined;
      let roleCheckInFlight = false;

      roleCheckInterval = window.setInterval(async () => {
        try {
          if (document.hidden) return; // pause when tab not visible
          if (roleCheckInFlight) return; // avoid overlap
          if (!user || !user.role) return; // no user to check
          
          roleCheckInFlight = true;

          const response = await apiService.getUserProfile();
          const currentRole = user.role?.toUpperCase();
          const updatedRole = response.data.role?.toUpperCase();
          
          // Skip if role hasn't changed
          if (updatedRole === currentRole) {
            roleCheckInFlight = false;
            return;
          }
          
          console.log('🔄 Global role check - Role changed:', {
            oldRole: currentRole,
            newRole: updatedRole,
            userId: user.id
          });
          
          // Check if this is a role downgrade
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
          
          // If downgrade → logout with notification
          if (isDowngrade) {
            console.log('🚨 GLOBAL ROLE DOWNGRADE DETECTED:', {
              oldRole: currentRole,
              newRole: updatedRole
            });
            
            // Show notification
            const downgradeMessage = updatedRole === 'USER' 
              ? `🚨 Bạn đã bị hạ quyền từ ${currentRole} về USER. Bạn sẽ bị đăng xuất trong 3 giây.`
              : `🚨 Bạn đã bị hạ quyền từ ${currentRole} xuống ${updatedRole}. Bạn sẽ bị đăng xuất trong 3 giây.`;
            
            showToastNotification('error', downgradeMessage);
            
            // Logout after 3 seconds
            setTimeout(() => {
              logout(`Role downgraded from ${currentRole} to ${updatedRole}`);
              window.location.href = '/login';
            }, 3000);
            return;
          }
          
          // If upgrade from USER → update role silently
          if (currentRole === 'USER' && updatedRole !== 'USER') {
            console.log('✅ Role upgraded from USER to', updatedRole);
            const updatedUser = {
              ...response.data,
              roles: response.data.role ? [response.data.role] : [],
              isActive: true,
            };
            localStorage.setItem('auth_user', JSON.stringify(updatedUser));
            setUser(updatedUser);
            roleCheckInFlight = false;
            return;
          }
          
          // If role changed but not downgrade (e.g., PRODUCT_MANAGER → USER_MANAGER)
          // Still logout if not from USER
          if (currentRole !== 'USER' && updatedRole !== currentRole && !isDowngrade) {
            console.log('🚨 Role changed (not downgrade) from', currentRole, 'to', updatedRole);
            showToastNotification('error', `🚨 Quyền của bạn đã bị thay đổi từ ${currentRole} sang ${updatedRole}. Bạn sẽ bị đăng xuất trong 3 giây.`);
            
            setTimeout(() => {
              logout(`Role changed from ${currentRole} to ${updatedRole}`);
              window.location.href = '/login';
            }, 3000);
            return;
          }
          
          // Otherwise, just update the role
          const updatedUser = {
            ...response.data,
            roles: response.data.role ? [response.data.role] : [],
            isActive: true,
          };
          localStorage.setItem('auth_user', JSON.stringify(updatedUser));
          setUser(updatedUser);
          
        } catch (error) {
          // Silent fail for role checking
          console.error('Error checking role:', error);
        } finally {
          roleCheckInFlight = false;
        }
      }, 10000); // Check every 10 seconds
      
      // Cleanup WebSocket listeners and role check interval
      return () => {
        if (cleanup) {
          cleanup();
        }
        if (roleCheckInterval) {
          clearInterval(roleCheckInterval);
        }
      };
    }
  }, [user, logout, setUser, showToastNotification, setupWebSocketListeners]);

  // Function to refresh user info from database
  const refreshUser = useCallback(async (): Promise<void> => {
    try {
      const response = await apiService.getUserProfile()
      if (response.data && response.success) {
        const updatedUser = {
          ...response.data,
          roles: response.data.role ? [response.data.role] : [],
          isActive: true,
        }
        localStorage.setItem('auth_user', JSON.stringify(updatedUser))
        setUser(updatedUser)
        console.log('✅ User info refreshed:', {
          role: updatedUser.role,
          avatarUrl: updatedUser.avatarUrl,
          profileImageUrl: updatedUser.profileImageUrl,
          userId: updatedUser.id
        })
      }
    } catch (error) {
      console.error('Failed to refresh user info:', error)
      throw error
    }
  }, [])

  const value: AuthContextType = {
    user,
    token,
    isLoading,
    isAuthenticated,
    isAdmin,
    login,
    logout,
    setUser,
    refreshUser
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}; 