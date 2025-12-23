export type NotificationType = 'success' | 'error' | 'warning' | 'info'

interface NotificationOptions {
  duration?: number
}

class NotificationService {
  // Loading state management
  private loadingStates = new Map<string, boolean>()
  private loadingCallbacks = new Map<string, (() => void)[]>()

  // Simple notification method using browser notification or console
  success(message: string, options?: NotificationOptions) {
    
    this.showToast(message, 'success', options?.duration)
  }

  error(message: string, options?: NotificationOptions) {
    console.error('❌ Error:', message)
    this.showToast(message, 'error', options?.duration)
  }

  warning(message: string, options?: NotificationOptions) {
    console.warn('⚠️ Warning:', message)
    this.showToast(message, 'warning', options?.duration)
  }

  info(message: string, options?: NotificationOptions) {
    console.info('ℹ️ Info:', message)
    this.showToast(message, 'info', options?.duration)
  }

  private showToast(message: string, type: NotificationType, duration: number = 5000) {
    // Create a simple toast element
    const toast = document.createElement('div')
    toast.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      padding: 12px 24px;
      border-radius: 4px;
      color: white;
      font-family: Arial, sans-serif;
      font-size: 14px;
      z-index: 9999;
      max-width: 400px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      animation: slideIn 0.3s ease-out;
      word-wrap: break-word;
    `

    // Set background color based on type
    const colors = {
      success: '#4caf50',
      error: '#f44336',
      warning: '#ff9800',
      info: '#2196f3'
    }
    toast.style.backgroundColor = colors[type]

    toast.textContent = message
    document.body.appendChild(toast)

    // Add CSS animation
    if (!document.getElementById('toast-styles')) {
      const style = document.createElement('style')
      style.id = 'toast-styles'
      style.textContent = `
        @keyframes slideIn {
          from { transform: translateX(100%); opacity: 0; }
          to { transform: translateX(0); opacity: 1; }
        }
        @keyframes slideOut {
          from { transform: translateX(0); opacity: 1; }
          to { transform: translateX(100%); opacity: 0; }
        }
      `
      document.head.appendChild(style)
    }

    // Auto remove after specified duration
    setTimeout(() => {
      toast.style.animation = 'slideOut 0.3s ease-in'
      setTimeout(() => {
        if (document.body.contains(toast)) {
          document.body.removeChild(toast)
        }
      }, 300)
    }, duration)
  }

  setLoading(key: string, isLoading: boolean) {
    this.loadingStates.set(key, isLoading)
    
    // Notify all callbacks for this key
    const callbacks = this.loadingCallbacks.get(key) || []
    callbacks.forEach(callback => callback())
  }

  isLoading(key: string): boolean {
    return this.loadingStates.get(key) || false
  }

  onLoadingChange(key: string, callback: () => void) {
    if (!this.loadingCallbacks.has(key)) {
      this.loadingCallbacks.set(key, [])
    }
    this.loadingCallbacks.get(key)?.push(callback)

    // Return cleanup function
    return () => {
      const callbacks = this.loadingCallbacks.get(key) || []
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    }
  }

  // Utility methods for common operations
  async handleAsyncOperation<T>(
    operation: () => Promise<T>,
    loadingKey: string,
    successMessage?: string,
    errorMessage?: string
  ): Promise<T | null> {
    try {
      this.setLoading(loadingKey, true)
      const result = await operation()
      
      if (successMessage) {
        this.success(successMessage)
      }
      
      return result
    } catch (error: unknown) {
      const fallback = typeof error === 'object' && error && 'message' in error ? String((error as { message?: string }).message) : 'An error occurred'
      const message = errorMessage || fallback
      this.error(message)
      return null
    } finally {
      this.setLoading(loadingKey, false)
    }
  }
}

// Export singleton instance
export const notificationService = new NotificationService()
export default notificationService 