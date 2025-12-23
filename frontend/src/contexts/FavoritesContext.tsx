import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react'
import { apiService, Product } from '../services/api'
import { useAuth } from './AuthContext'
import { useNotification } from './NotificationContext'
import { useTranslation } from 'react-i18next'
import NProgress from 'nprogress'

// Favorites Context Type
interface FavoritesContextType {
  favorites: Product[]
  favoritesLoading: boolean
  addToFavorites: (productId: string) => Promise<boolean>
  removeFromFavorites: (productId: string) => Promise<boolean>
  toggleFavorite: (productId: string) => Promise<boolean>
  isFavorite: (productId: string) => boolean
  loadFavorites: () => Promise<void>
}

// Create context
const FavoritesContext = createContext<FavoritesContextType | undefined>(undefined)

// Hook to use favorites context
export const useFavorites = () => {
  const context = useContext(FavoritesContext)
  if (!context) {
    throw new Error('useFavorites must be used within a FavoritesProvider')
  }
  return context
}

// Favorites Provider Props
interface FavoritesProviderProps {
  children: ReactNode
  userId?: string // Add userId prop
}

// Favorites Provider Component
export const FavoritesProvider: React.FC<FavoritesProviderProps> = ({ children, userId }) => {
  const [favorites, setFavorites] = useState<Product[]>([])
  const [favoritesLoading, setFavoritesLoading] = useState(false)
  const { isAuthenticated } = useAuth()
  const { notify } = useNotification()
  const { t } = useTranslation()

  // Load favorites from backend
  const loadFavorites = useCallback(async () => {
    if (!isAuthenticated || !userId) return
    
    try {
      setFavoritesLoading(true)
      const response = await apiService.getFavorites(userId)

      if (response.success) {
        setFavorites(response.data || [])
      } else {
        // If API returns error but not exception, set empty array
        setFavorites([])
      }
    } catch (error: any) {
      // Only log error if it's not a 500 (service might not be available)
      if (error?.response?.status !== 500) {
        console.error('Failed to load favorites:', error)
      }
      // Set empty array on error to prevent UI issues
      setFavorites([])
    } finally {
      setFavoritesLoading(false)
    }
  }, [isAuthenticated, userId])

  // Load favorites on component mount and when authentication or userId changes
  useEffect(() => {
    if (isAuthenticated && userId) {
      loadFavorites()
    } else {
      setFavorites([])
    }
  }, [isAuthenticated, userId, loadFavorites])

  // Add product to favorites
  const addToFavorites = async (productId: string): Promise<boolean> => {
    if (!isAuthenticated || !userId) {
      notify(t('favorites.errors.loginRequired'), 'warning')
      return false
    }

    try {
      NProgress.start()
      const response = await apiService.addToFavorites(productId, userId)
      if (response.success) {
        // Reload favorites to get updated list
        await loadFavorites()
        notify(t('favorites.success.added'), 'success')
        return true
      }
      return false
    } catch (error: unknown) {
      console.error('Failed to add to favorites:', error)
      const axiosError = error as { response?: { data?: { message?: string } } }
      const errorMessage = axiosError?.response?.data?.message || t('favorites.errors.addFailed')
      notify(errorMessage, 'error')
      return false
    } finally {
      NProgress.done()
    }
  }

  // Remove product from favorites
  const removeFromFavorites = async (productId: string): Promise<boolean> => {
    if (!isAuthenticated || !userId) {
      return false
    }

    try {
      NProgress.start()
      const response = await apiService.removeFromFavorites(productId, userId)
      if (response.success) {
        // Update local state immediately for better UX
        setFavorites(prev => prev.filter(product => product.id !== productId))
        notify(t('favorites.success.removed'), 'info')
        return true
      }
      return false
    } catch (error: unknown) {
      console.error('Failed to remove from favorites:', error)
      const axiosError = error as { response?: { data?: { message?: string } } }
      const errorMessage = axiosError?.response?.data?.message || t('favorites.errors.removeFailed')
      notify(errorMessage, 'error')
      return false
    } finally {
      NProgress.done()
    }
  }

  // Check if product is favorite
  const isFavorite = (productId: string): boolean => {
    return favorites.some(product => product.id === productId)
  }

  // Toggle favorite status
  const toggleFavorite = async (productId: string): Promise<boolean> => {
    if (isFavorite(productId)) {
      return await removeFromFavorites(productId)
    } else {
      return await addToFavorites(productId)
    }
  }

  const value: FavoritesContextType = {
    favorites,
    favoritesLoading,
    addToFavorites,
    removeFromFavorites,
    toggleFavorite,
    isFavorite,
    loadFavorites
  }

  return (
    <FavoritesContext.Provider value={value}>
      {children}
    </FavoritesContext.Provider>
  )
}
