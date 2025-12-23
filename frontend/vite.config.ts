
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const API_BASE = env.VITE_API_BASE_URL || 'http://localhost:8080'
  const isDev = mode === 'development'
  const isProd = mode === 'production'

  return {
    plugins: [react()],
    define: {
      global: 'globalThis',
    },
    server: {
      port: 3000,
      host: true,
      hmr: {
        port: 3001,
      },
      proxy: {
        // Direct AI service during development for lower latency and fewer dependencies
        '/api/ai': {
          target: 'http://localhost:8088',
          changeOrigin: true,
          secure: false,
          configure: (proxy, _options) => {
            proxy.on('error', (err, _req, _res) => {
              console.log('proxy error (ai)', err);
            });
            proxy.on('proxyReq', (proxyReq, req, _res) => {
              console.log('Sending Request to AI Target:', req.method, req.url);
              const auth = req.headers['authorization'];
              if (auth) {
                proxyReq.setHeader('authorization', auth);
                proxyReq.setHeader('Authorization', auth);
              }
            });
            proxy.on('proxyRes', (proxyRes, req, _res) => {
              console.log('Received Response from AI Target:', proxyRes.statusCode, req.url);
            });
          },
        },
        // Shipping API - route directly to order-service (workaround for Gateway routing issue)
        '/api/shipping': {
          target: 'http://localhost:8084',
          changeOrigin: true,
          secure: false,
        },
        '/api': {
          target: API_BASE,
          changeOrigin: true,
          secure: false,
          configure: (proxy, _options) => {
            proxy.on('error', (err, _req, _res) => {
              console.log('proxy error', err);
            });
            proxy.on('proxyReq', (proxyReq, req, _res) => {
              console.log('Sending Request to the Target:', req.method, req.url);
              const auth = req.headers['authorization'];
              if (auth) {
                proxyReq.setHeader('authorization', auth);
                proxyReq.setHeader('Authorization', auth);
              }
            });
            proxy.on('proxyRes', (proxyRes, req, _res) => {
              console.log('Received Response from the Target:', proxyRes.statusCode, req.url);
            });
          },
        },
        // Proxy for static uploads (avatars, images, etc.)
        '/uploads': {
          target: API_BASE,
          changeOrigin: true,
          secure: false,
        },
        '/actuator': {
          target: API_BASE,
          changeOrigin: true,
          secure: false,
        },
        '/ws': {
          target: API_BASE,
          changeOrigin: true,
          ws: true,
          secure: false
        }
      }
    },
    build: {
      outDir: 'dist',
      sourcemap: isDev,
      chunkSizeWarningLimit: 500,
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: isProd,
          drop_debugger: true,
        },
      },
      rollupOptions: {
        output: {
          manualChunks: (id) => {
            // Vendor chunks - split by package type for better caching
            if (typeof id === 'string' && id.includes('node_modules')) {
              if (id.includes('react') || id.includes('react-dom') || id.includes('react-router')) {
                return 'react-vendor';
              }
              if (id.includes('@mui') || id.includes('@emotion')) {
                return 'mui-vendor';
              }
              if (id.includes('i18next') || id.includes('react-i18next')) {
                return 'i18n-vendor';
              }
              if (id.includes('axios') || id.includes('date-fns')) {
                return 'utils-vendor';
              }
              if (id.includes('chart') || id.includes('recharts')) {
                return 'charts-vendor';
              }
              if (id.includes('@tanstack')) {
                return 'query-vendor';
              }
              return 'vendor';
            }
            // Feature-based chunks for better code splitting
            if (typeof id === 'string') {
              if (id.includes('/admin/')) {
                return 'admin';
              }
              if (id.includes('/pages/')) {
                return 'pages';
              }
              if (id.includes('/components/')) {
                return 'components';
              }
              if (id.includes('/services/')) {
                return 'services';
              }
              if (id.includes('/hooks/')) {
                return 'hooks';
              }
            }
            return 'main';
          },
          chunkFileNames: (chunkInfo) => {
            const facadeModuleId = chunkInfo.facadeModuleId
              ? chunkInfo.facadeModuleId.split('/').pop()?.replace('.tsx', '').replace('.ts', '')
              : 'chunk';
            return `js/${facadeModuleId}-[hash].js`;
          },
          entryFileNames: 'js/[name]-[hash].js',
          assetFileNames: (assetInfo) => {
            const info = assetInfo.name?.split('.') || []
            const ext = info[info.length - 1]
            if (assetInfo.name && /\.(css)$/.test(assetInfo.name)) {
              return `css/[name]-[hash].${ext}`
            }
            if (assetInfo.name && /\.(png|jpe?g|svg|gif|tiff|bmp|ico|webp)$/i.test(assetInfo.name)) {
              return `images/[name]-[hash].${ext}`
            }
            return `assets/[name]-[hash].${ext}`
          }
        },
        external: (id) => {
          // Don't bundle these as they're provided by the environment
          return typeof id === 'string' && ['fs', 'path', 'crypto'].includes(id);
        }
      }
    },
    optimizeDeps: {
      // Pre-bundle these dependencies for faster dev server startup
      include: [
        'react',
        'react-dom',
        'react-router-dom',
        '@mui/material',
        '@mui/icons-material',
        'axios',
        '@tanstack/react-query',
        'i18next',
        'react-i18next'
      ],
      exclude: ['@vite/client', '@vite/env']
    },
    esbuild: {
      // Remove console.log in production
      drop: isProd ? ['console', 'debugger'] : [],
      legalComments: 'none',
    }
  }
}) 
