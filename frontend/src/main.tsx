import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { ThemeProvider } from '@mui/material/styles'
import CssBaseline from '@mui/material/CssBaseline'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

// Enable React Fast Refresh in development
if (import.meta.env.DEV) {
  import.meta.hot?.accept()
}

// Preload critical resources for better performance
const preloadCriticalResources = () => {
  // Preload fonts
  const fontLink = document.createElement('link')
  fontLink.rel = 'preconnect'
  fontLink.href = 'https://fonts.googleapis.com'
  document.head.appendChild(fontLink)

  const fontLink2 = document.createElement('link')
  fontLink2.rel = 'preconnect'
  fontLink2.href = 'https://fonts.gstatic.com'
  fontLink2.crossOrigin = 'anonymous'
  document.head.appendChild(fontLink2)
}

import App from './App'
import theme from './theme/theme'
import { AuthProvider } from './contexts/AuthContext'
import { NotificationProvider } from './contexts/NotificationContext'

// Optimize bundle by lazy loading heavy libraries
import('./utils/analytics')
import './i18n'
import 'nprogress/nprogress.css'

// Create a client for React Query with optimized performance settings
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 10 * 60 * 1000, // 10 minutes for better caching
      gcTime: 15 * 60 * 1000, // 15 minutes garbage collection time
      retry: 3,
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
      refetchOnWindowFocus: false,
      refetchOnReconnect: true,
    },
    mutations: {
      retry: 1,
    },
  },
})

// Preload critical resources
preloadCriticalResources()

// Register Service Worker for offline support and caching
if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js')
      .then((registration) => {
        console.log('Service Worker registered successfully:', registration.scope);
      })
      .catch((error) => {
        console.log('Service Worker registration failed:', error);
      });
  });
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter
        future={{
          v7_startTransition: true,
          v7_relativeSplatPath: true
        }}
      >
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <AuthProvider>
            <NotificationProvider>
              <App />
            </NotificationProvider>
          </AuthProvider>
        </ThemeProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
)