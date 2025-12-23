import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { CircularProgress, Box, Typography, Alert, AlertTitle } from '@mui/material';
import { Security, VerifiedUser } from '@mui/icons-material';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requireAuth?: boolean;
  requireAdmin?: boolean;
  requireRole?: string | string[]; // Specific role(s) required
  redirectTo?: string;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requireAuth = true,
  requireAdmin = false,
  requireRole,
  redirectTo = '/login'
}) => {
  const { isAuthenticated, isAdmin, isLoading, user } = useAuth();
  const location = useLocation();
  
  // Debug logging (only in development)
  if (import.meta.env.DEV) {
    console.log('ProtectedRoute - Path:', location.pathname);
    console.log('ProtectedRoute - Auth Status:', { isAuthenticated, isAdmin, isLoading });
    console.log('ProtectedRoute - User:', user?.username, user?.role);
  }

  // Show loading while checking authentication
  if (isLoading) {
    return (
      <Box
        display="flex"
        flexDirection="column"
        justifyContent="center"
        alignItems="center"
        minHeight="60vh"
        gap={2}
      >
        <CircularProgress size={48} />
        <Typography variant="body1" color="text.secondary">
          Verifying access permissions...
        </Typography>
      </Box>
    );
  }

  // Check if authentication is required
  if (requireAuth && !isAuthenticated) {
    return <Navigate to={redirectTo} state={{ from: location }} replace />;
  }

  // Check if admin access is required
  if (requireAdmin && !isAdmin) {
    return (
      <Box
        display="flex"
        justifyContent="center"
        alignItems="center"
        minHeight="60vh"
        sx={{ p: 3 }}
      >
        <Alert 
          severity="error" 
          sx={{ 
            maxWidth: 500, 
            '& .MuiAlert-icon': { fontSize: '2rem' }
          }}
        >
          <AlertTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Security />
            Administrator Access Required
          </AlertTitle>
          <Typography variant="body2" sx={{ mb: 1 }}>
            This page requires administrator privileges. Please contact your system administrator if you believe you should have access.
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Current role: {user?.role || 'No role assigned'}
          </Typography>
        </Alert>
      </Box>
    );
  }

  // Check if specific role is required
  if (requireRole) {
    const hasRequiredRole = Array.isArray(requireRole) 
      ? requireRole.includes(user?.role || '')
      : user?.role === requireRole;
    
    if (!hasRequiredRole) {
      return (
        <Box
          display="flex"
          justifyContent="center"
          alignItems="center"
          minHeight="60vh"
          sx={{ p: 3 }}
        >
          <Alert 
            severity="warning" 
            sx={{ 
              maxWidth: 500,
              '& .MuiAlert-icon': { fontSize: '2rem' }
            }}
          >
            <AlertTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <VerifiedUser />
              Insufficient Privileges
            </AlertTitle>
            <Typography variant="body2" sx={{ mb: 1 }}>
              This page requires specific role permissions that you don't currently have.
            </Typography>
            <Typography variant="body2" sx={{ mb: 1 }}>
              <strong>Required:</strong> {Array.isArray(requireRole) ? requireRole.join(' or ') : requireRole}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Your role: {user?.role || 'No role assigned'}
            </Typography>
          </Alert>
        </Box>
      );
    }
  }

  return <>{children}</>;
};

export default ProtectedRoute; 