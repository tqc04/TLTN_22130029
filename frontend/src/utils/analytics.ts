// Analytics utility for tracking user behavior and events
// This file provides analytics functionality for the e-commerce platform

interface AnalyticsEvent {
  action: string
  category: string
  label?: string
  value?: number
  customParameters?: Record<string, unknown>
}

interface UserProperties {
  userId?: string
  userRole?: string
  isAuthenticated?: boolean
  preferredLanguage?: string
  [key: string]: unknown
}

class AnalyticsService {
  private isEnabled: boolean = true
  private isDevelopment: boolean = import.meta.env.DEV

  constructor() {
    // Disable analytics in development unless explicitly enabled
    if (this.isDevelopment && !import.meta.env.VITE_ENABLE_ANALYTICS_IN_DEV) {
      this.isEnabled = false
    }

    // Initialize analytics when the service is created
    this.initialize()
  }

  private initialize(): void {
    if (!this.isEnabled) {
      console.log('Analytics disabled (development mode)')
      return
    }

    // Initialize Google Analytics, Mixpanel, or other analytics services here
    console.log('Analytics service initialized')

    // Example: Initialize Google Analytics 4
    // gtag('config', 'GA_MEASUREMENT_ID')
  }

    // Track page views
  trackPageView(pagePath: string, pageTitle?: string): void {
    if (!this.isEnabled) return

    console.log('Page View:', { pagePath, pageTitle })

    // Example: Google Analytics page view tracking
    // gtag('config', pagePath, {
    //   page_title: pageTitle,
    //   page_location: window.location.href
    // })
  }

  // Track custom events
  trackEvent(event: AnalyticsEvent): void {
    if (!this.isEnabled) return

    console.log('Event:', event)

    // Example: Google Analytics event tracking
    // gtag('event', event.action, {
    //   event_category: event.category,
    //   event_label: event.label,
    //   value: event.value,
    //   ...event.customParameters
    // })
  }

  // Track e-commerce specific events
  trackEcommerceEvent(action: string, productInfo?: {
    id: string
    name: string
    category?: string
    price?: number
    quantity?: number
  }): void {
    if (!this.isEnabled) return

    this.trackEvent({
      action,
      category: 'ecommerce',
      label: productInfo?.name,
      value: productInfo?.price,
      customParameters: productInfo
    })
  }

  // Track user interactions
  trackUserInteraction(action: string, element?: string, additionalData?: Record<string, unknown>): void {
    if (!this.isEnabled) return

    this.trackEvent({
      action,
      category: 'user_interaction',
      label: element,
      customParameters: additionalData
    })
  }

  // Track performance metrics
  trackPerformance(name: string, value: number, unit: string = 'ms'): void {
    if (!this.isEnabled) return

    this.trackEvent({
      action: 'performance',
      category: 'performance',
      label: name,
      value,
      customParameters: { unit }
    })
  }

  // Set user properties
  setUserProperties(properties: UserProperties): void {
    if (!this.isEnabled) return

    console.log('User Properties:', properties)

    // Example: Set user properties in analytics service
    // gtag('config', 'GA_MEASUREMENT_ID', {
    //   custom_map: properties
    // })
  }

  // Track errors
  trackError(error: Error, context?: string): void {
    if (!this.isEnabled) return

    this.trackEvent({
      action: 'error',
      category: 'error',
      label: error.message,
      customParameters: {
        stack: error.stack,
        context,
        userAgent: navigator.userAgent,
        url: window.location.href
      }
    })
  }

  // Track search events
  trackSearch(searchTerm: string, resultsCount?: number, filters?: Record<string, unknown>): void {
    if (!this.isEnabled) return

    this.trackEvent({
      action: 'search',
      category: 'search',
      label: searchTerm,
      value: resultsCount,
      customParameters: filters
    })
  }

  // Track conversion events (purchases, sign-ups, etc.)
  trackConversion(action: string, value?: number, additionalData?: Record<string, unknown>): void {
    if (!this.isEnabled) return

    this.trackEvent({
      action,
      category: 'conversion',
      value,
      customParameters: additionalData
    })
  }

  // Method to enable/disable analytics (useful for user privacy settings)
  setEnabled(enabled: boolean): void {
    this.isEnabled = enabled
    console.log('Analytics', enabled ? 'enabled' : 'disabled')
  }

  // Get current analytics status
  isAnalyticsEnabled(): boolean {
    return this.isEnabled
  }
}

// Create and export singleton instance
export const analytics = new AnalyticsService()

// Export types for use in other parts of the application
export type { AnalyticsEvent, UserProperties }

// Export convenience functions for common tracking operations
export const trackPageView = (pagePath: string, pageTitle?: string) =>
  analytics.trackPageView(pagePath, pageTitle)

export const trackEvent = (event: AnalyticsEvent) =>
  analytics.trackEvent(event)

export const trackEcommerceEvent = (action: string, productInfo?: {
  id: string
  name: string
  category?: string
  price?: number
  quantity?: number
}) => analytics.trackEcommerceEvent(action, productInfo)

export const trackUserInteraction = (action: string, element?: string, additionalData?: Record<string, unknown>) =>
  analytics.trackUserInteraction(action, element, additionalData)

export const trackError = (error: Error, context?: string) =>
  analytics.trackError(error, context)

export const trackSearch = (searchTerm: string, resultsCount?: number, filters?: Record<string, unknown>) =>
  analytics.trackSearch(searchTerm, resultsCount, filters)

export const trackConversion = (action: string, value?: number, additionalData?: Record<string, unknown>) =>
  analytics.trackConversion(action, value, additionalData)

export const setUserProperties = (properties: UserProperties) =>
  analytics.setUserProperties(properties)

export default analytics