import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

export interface UserPermissionChangeEvent {
  type: string;
  userId: string;
  username: string;
  oldRole: string;
  newRole: string;
  reason: string;
  changedBy: string;
  timestamp: number;
  changeTime: string;
}

export interface ForceLogoutEvent {
  type: string;
  reason: string;
  timestamp: number;
}

export interface UserNotificationEvent {
  type?: string;
  title?: string;
  message?: string;
  data?: any;
  timestamp?: number;
  [key: string]: any;
}

const DEBUG_WS = false;

type StompSubscription = { unsubscribe: () => void } | null;

class WebSocketService {
  private stompClient: any = null;
  private isConnected: boolean = false;
  private reconnectAttempts: number = 0;
  private maxReconnectAttempts: number = 3;
  private reconnectDelay: number = 10000;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private currentUserId: string | null = null;

  // Callbacks
  private onPermissionChange: ((event: UserPermissionChangeEvent) => void) | null = null;
  private onForceLogout: ((event: ForceLogoutEvent) => void) | null = null;
  private onAdminNotification: ((event: UserPermissionChangeEvent) => void) | null = null;
  private notificationListeners: Array<(event: UserNotificationEvent) => void> = [];

  constructor() {
    // Don't auto-connect, wait for user to be available
  }

  private init() {
    try {
      // Create SockJS factory for STOMP (required by newer @stomp/stompjs)
      // Use relative path so Vite proxy routes to backend (8081)
      const socketFactory = () => new SockJS('/ws');
      this.stompClient = Stomp.over(socketFactory);
      
      // Properly disable debug logging (must be a function)
      this.stompClient.debug = DEBUG_WS ? (msg: any) => console.log(msg) : () => {};
      
      this.connect();
    } catch (error) {
      if (DEBUG_WS) console.error('WebSocket initialization failed:', error);
      this.scheduleReconnect();
    }
  }

  private connect(headers: Record<string, unknown> = {}) {
    if (this.stompClient) {
      try {
        // Try common token keys
        const token =
          localStorage.getItem('auth_token') ||
          localStorage.getItem('token') ||
          localStorage.getItem('jwt');
        if (token) {
          headers['Authorization'] = `Bearer ${token}`;
        } else if (DEBUG_WS) {
          console.warn('No auth token found for WebSocket connection');
        }
        
        headers['heart-beat'] = '10000,10000';
        
        this.stompClient.connect(
          headers,
          (_frame: unknown) => {
            if (DEBUG_WS) console.log('WebSocket connected successfully');
            this.isConnected = true;
            this.reconnectAttempts = 0;
            this.reconnectDelay = 1000;
            if (this.currentUserId) {
              this.subscribeToUserTopics(this.currentUserId);
            }
          },
          (error: unknown) => {
            if (DEBUG_WS) console.error('WebSocket connection error:', error);
            this.isConnected = false;
            const e = error as { status?: number; headers?: { message?: string } } | undefined
            if (e && (e.status === 401 || e.headers?.message?.includes('401'))) {
              if (DEBUG_WS) console.error('WS Authentication failed - not reconnecting');
              return;
            }
            this.scheduleReconnect();
          }
        );
      } catch (error) {
        if (DEBUG_WS) console.error('Error in WebSocket connect:', error);
        this.isConnected = false;
        this.scheduleReconnect();
      }
    }
  }

  private scheduleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      if (DEBUG_WS) console.log(`Scheduling WebSocket reconnect attempt ${this.reconnectAttempts} in ${this.reconnectDelay}ms`);
      setTimeout(() => {
        this.connect();
      }, this.reconnectDelay);
    } else {
      if (DEBUG_WS) console.error('Max WebSocket reconnection attempts reached');
    }
  }

  private subscribeToUserTopics(userId: string) {
    if (DEBUG_WS) console.log('Subscribing to WebSocket topics for user:', userId, 'with role:', this.getCurrentUser()?.role);
    if (DEBUG_WS) console.log('WebSocket connection status:', this.isConnected);
    if (DEBUG_WS) console.log('Stomp client available:', !!this.stompClient);

    // Subscribe to Spring user-destination mapping (principal based)
    const permissionChangeSubUserDest = this.stompClient.subscribe(
      `/user/permission-change`,
      (message: { body: string }) => {
        try {
          const event: UserPermissionChangeEvent = JSON.parse(message.body);
          if (DEBUG_WS) console.log('Permission change (user-dest) notification received:', event);
          if (this.onPermissionChange) {
            this.onPermissionChange(event);
          }
        } catch (error) {
          if (DEBUG_WS) console.error('Error parsing permission change (user-dest) message:', error);
        }
      }
    );

    const notificationsSub = this.stompClient.subscribe(
      `/user/${userId}/queue/notifications`,
      (message: { body: string }) => {
        try {
          const event: UserNotificationEvent = JSON.parse(message.body);
          if (typeof event.data === 'string') {
            try {
              event.data = JSON.parse(event.data);
            } catch {
              // keep as string
            }
          }
          this.notificationListeners.forEach((listener) => listener(event));
        } catch (error) {
          if (DEBUG_WS) console.error('Error parsing notification message:', error);
        }
      }
    );

    // Subscribe to user-specific permission change notifications (by id path)
    const permissionChangeSubById = this.stompClient.subscribe(
      `/user/${userId}/permission-change`,
      (message: { body: string }) => {
        try {
          const event: UserPermissionChangeEvent = JSON.parse(message.body);
          if (DEBUG_WS) console.log('Permission change notification received:', event);
          
          if (this.onPermissionChange) {
            this.onPermissionChange(event);
          }
        } catch (error) {
          if (DEBUG_WS) console.error('Error parsing permission change message:', error);
        }
      }
    );

    // Subscribe to force logout notifications
    const forceLogoutSubById = this.stompClient.subscribe(
      `/user/${userId}/force-logout`,
      (message: { body: string }) => {
        try {
          const event: ForceLogoutEvent = JSON.parse(message.body);
          if (DEBUG_WS) console.log('Force logout notification received:', event);
          
          if (this.onForceLogout) {
            this.onForceLogout(event);
          }
        } catch (error) {
          if (DEBUG_WS) console.error('Error parsing force logout message:', error);
        }
      }
    );

    const forceLogoutSubUserDest = this.stompClient.subscribe(
      `/user/force-logout`,
      (message: { body: string }) => {
        try {
          const event: ForceLogoutEvent = JSON.parse(message.body);
        if (DEBUG_WS) console.log('Force logout (user-dest) notification received:', event);
          if (this.onForceLogout) {
            this.onForceLogout(event);
          }
        } catch (error) {
          if (DEBUG_WS) console.error('Error parsing force logout (user-dest) message:', error);
        }
      }
    );

    // Subscribe to admin notifications (if user is admin)
    if (this.getCurrentUser()?.role === 'ADMIN') {
      const adminSub = this.stompClient.subscribe(
        '/topic/admin/permission-changes',
        (message: any) => {
          try {
            const event: UserPermissionChangeEvent = JSON.parse(message.body);
            if (DEBUG_WS) console.log('Admin notification received:', event);
            
            if (this.onAdminNotification) {
              this.onAdminNotification(event);
            }
          } catch (error) {
            if (DEBUG_WS) console.error('Error parsing admin notification message:', error);
          }
        }
      );

      this.subscriptions.set('admin', adminSub);
    }

    this.subscriptions.set('permission-change-id', permissionChangeSubById);
    this.subscriptions.set('permission-change-user', permissionChangeSubUserDest);
    this.subscriptions.set('force-logout-id', forceLogoutSubById);
    this.subscriptions.set('force-logout-user', forceLogoutSubUserDest);
    this.subscriptions.set('user-notifications', notificationsSub);
    
    if (DEBUG_WS) console.log('WebSocket subscriptions completed for user:', userId);
    if (DEBUG_WS) console.log('Total subscriptions:', this.subscriptions.size);
  }

  private getCurrentUser(): any {
    try {
      // Use the same key as AuthContext
      const userStr = localStorage.getItem('auth_user');
      if (userStr) {
        return JSON.parse(userStr);
      }
    } catch (error) {
      if (DEBUG_WS) console.error('Error parsing user from localStorage:', error);
    }
    return null;
  }

  // Public methods
  public connectUser(userId: string) {
    this.currentUserId = userId;
    if (DEBUG_WS) console.log('Connecting WebSocket for user:', userId);
    
    if (!this.stompClient) {
      // Initialize WebSocket if not already done
      this.init();
    } else if (this.isConnected) {
      // Re-subscribe with new user ID
      this.unsubscribeAll();
      this.subscribeToUserTopics(userId);
    }
  }

  public disconnect() {
    this.unsubscribeAll();
    
    if (this.stompClient) {
      this.stompClient.disconnect();
      this.stompClient = null;
    }
    
    this.isConnected = false;
    this.currentUserId = null;
  }

  private unsubscribeAll() {
    this.subscriptions.forEach((subscription) => {
      if (subscription) {
        subscription.unsubscribe();
      }
    });
    this.subscriptions.clear();
  }

  // Event handlers
  public onPermissionChangeEvent(callback: (event: UserPermissionChangeEvent) => void) {
    this.onPermissionChange = callback;
  }

  public onForceLogoutEvent(callback: (event: ForceLogoutEvent) => void) {
    this.onForceLogout = callback;
  }

  public onAdminNotificationEvent(callback: (event: UserPermissionChangeEvent) => void) {
    this.onAdminNotification = callback;
  }

  public onNotificationEvent(callback: (event: UserNotificationEvent) => void) {
    this.notificationListeners.push(callback);
    return () => {
      this.notificationListeners = this.notificationListeners.filter((listener) => listener !== callback);
    };
  }

  // Utility methods
  public isWebSocketConnected(): boolean {
    return this.isConnected;
  }

  public getConnectionStatus(): string {
    if (this.isConnected) {
      return 'connected';
    } else if (this.reconnectAttempts > 0) {
      return 'reconnecting';
    } else {
      return 'disconnected';
    }
  }

  public getCurrentUserInfo(): any {
    const user = this.getCurrentUser();
    if (user) {
      return {
        id: user.id,
        username: user.username,
        role: user.role,
        connected: this.isConnected
      };
    }
    return null;
  }

  public getConnectionDetails(): any {
    return {
      isConnected: this.isConnected,
      stompClient: !!this.stompClient,
      subscriptions: this.subscriptions.size,
      reconnectAttempts: this.reconnectAttempts,
      endpoint: '/ws'
    };
  }
}

// Export singleton instance
export const websocketService = new WebSocketService();

// Auto-cleanup on page unload
window.addEventListener('beforeunload', () => {
  websocketService.disconnect();
});

export default websocketService;
