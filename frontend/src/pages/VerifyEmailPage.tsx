import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { apiService } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { useNotification } from '../contexts/NotificationContext';
import { Container, Paper, Typography, Button, CircularProgress, Snackbar, Alert } from '@mui/material';

const VerifyEmailPage: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { notify } = useNotification();
  const [status, setStatus] = useState<'pending' | 'success' | 'error'>('pending');
  const [message, setMessage] = useState('');
  const [snackbarOpen, setSnackbarOpen] = useState(false);

  useEffect(() => {
    // Always logout current user to avoid confusion
    logout();

    const params = new URLSearchParams(location.search);
    const token = params.get('token');
    if (!token) {
      setStatus('error');
      setMessage('Invalid verification link.');
      setSnackbarOpen(true);
      notify('Invalid verification link.', 'error');
      return;
    }

    apiService.verifyEmail(token)
      .then(res => {
        if (res.success) {
          setStatus('success');
          setMessage('Email verified successfully! Redirecting to login...');
          notify('Email verified successfully! Redirecting to login...', 'success');
          setTimeout(() => navigate('/login'), 1500);
        } else {
          setStatus('error');
          setMessage(res.message || 'Verification failed.');
          notify(res.message || 'Verification failed.', 'error');
        }
        setSnackbarOpen(true);
      })
      .catch((err) => {
        const backendMsg = (err?.response?.data?.message) || (err?.response?.data?.error) || (err?.response?.data?.body);
        const msg = backendMsg || 'Verification failed. Please try again.';
        setStatus('error');
        setMessage(msg);
        setSnackbarOpen(true);
        notify(msg, 'error');
      });
  }, [location.search, logout, notify]);

  const handleSnackbarClose = (_event?: React.SyntheticEvent | Event, reason?: string) => {
    if (reason === 'clickaway') return;
    setSnackbarOpen(false);
  };

  return (
    <Container maxWidth="sm" sx={{ mt: 8 }}>
      <Paper sx={{ p: 4, textAlign: 'center' }}>
        {status === 'pending' && <CircularProgress />}
        <Typography variant="h5" sx={{ mb: 2 }}>
          {status === 'pending' ? 'Verifying your email...' : message}
        </Typography>
        {status === 'success' && (
          <Button variant="contained" onClick={() => navigate('/login')}>
            Go to Login
          </Button>
        )}
        {status === 'error' && (
          <Button variant="outlined" onClick={() => navigate('/')}>Back to Home</Button>
        )}
      </Paper>
      <Snackbar
        open={snackbarOpen}
        autoHideDuration={4000}
        onClose={handleSnackbarClose}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert onClose={handleSnackbarClose} severity={status === 'success' ? 'success' : 'error'} sx={{ width: '100%' }}>
          {message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default VerifyEmailPage; 