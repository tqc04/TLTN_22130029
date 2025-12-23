import { createTheme } from '@mui/material/styles'

const theme = createTheme({
  palette: {
    primary: {
      main: '#6366f1', // Indigo premium
      light: '#8b5cf6',
      dark: '#4f46e5',
    },
    secondary: {
      main: '#f59e0b', // Amber premium
      light: '#fbbf24',
      dark: '#d97706',
    },
    background: {
      default: '#ffffff', // White background
      paper: '#ffffff', // White paper
    },
    text: {
      primary: '#1a202c',
      secondary: '#4a5568',
    },
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h1: {
      fontSize: '2.5rem',
      fontWeight: 600,
      lineHeight: 1.2,
      marginBottom: '1rem',
    },
    h2: {
      fontSize: '2rem',
      fontWeight: 600,
      lineHeight: 1.3,
      marginBottom: '0.75rem',
    },
    h3: {
      fontSize: '1.75rem',
      fontWeight: 500,
      lineHeight: 1.4,
      marginBottom: '0.5rem',
    },
    h4: {
      fontSize: '1.5rem',
      fontWeight: 500,
      lineHeight: 1.4,
      marginBottom: '0.5rem',
    },
    h5: {
      fontSize: '1.25rem',
      fontWeight: 500,
      lineHeight: 1.4,
      marginBottom: '0.25rem',
    },
    h6: {
      fontSize: '1rem',
      fontWeight: 500,
      lineHeight: 1.4,
      marginBottom: '0.25rem',
    },
  },
  components: {
    MuiContainer: {
      styleOverrides: {
        root: {
          paddingLeft: '24px',
          paddingRight: '24px',
          '@media (min-width: 600px)': {
            paddingLeft: '32px',
            paddingRight: '32px',
          },
          '@media (min-width: 960px)': {
            paddingLeft: '48px',
            paddingRight: '48px',
          },
          '@media (min-width: 1280px)': {
            paddingLeft: '64px',
            paddingRight: '64px',
          },
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: 12,
          fontWeight: 600,
          padding: '12px 32px',
          minHeight: '48px',
          fontSize: '1rem',
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        },
        contained: {
          boxShadow: '0 4px 12px rgba(99,102,241,0.3)',
          '&:hover': {
            boxShadow: '0 8px 24px rgba(99,102,241,0.4)',
            transform: 'translateY(-2px)',
          },
        },
        outlined: {
          borderWidth: '2px',
          '&:hover': {
            borderWidth: '2px',
            transform: 'translateY(-2px)',
            boxShadow: '0 8px 24px rgba(99,102,241,0.2)',
          },
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 16,
          background: '#ffffff',
          border: '1px solid #e2e8f0',
          boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
          '&:hover': {
            transform: 'translateY(-4px)',
            boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
            border: '1px solid #cbd5e1',
          },
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          borderRadius: 0,
          boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
        },
      },
    },
    MuiCardContent: {
      styleOverrides: {
        root: {
          padding: '24px',
          '&:last-child': {
            paddingBottom: '24px',
          },
        },
      },
    },
    MuiGrid: {
      styleOverrides: {
        root: {
          '&.MuiGrid-container': {
            margin: 0,
          },
        },
      },
    },
  },
})

export default theme 