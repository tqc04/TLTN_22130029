import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Container, Paper, Typography, TextField, Button, Snackbar, Alert, Box } from '@mui/material';
import { apiService } from '../services/api';

const ResetPasswordPage: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [token, setToken] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [loading, setLoading] = useState(false);
  const [snackbarOpen, setSnackbarOpen] = useState(false);
  const [message, setMessage] = useState('');
  const [isError, setIsError] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const t = params.get('token') || '';
    setToken(t);
  }, [location.search]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) {
      setIsError(true);
      setMessage('Invalid or missing token.');
      setSnackbarOpen(true);
      return;
    }
    if (password.length < 6) {
      setIsError(true);
      setMessage('Password must be at least 6 characters.');
      setSnackbarOpen(true);
      return;
    }
    if (password !== confirm) {
      setIsError(true);
      setMessage('Password confirmation does not match.');
      setSnackbarOpen(true);
      return;
    }
    try {
      setLoading(true);
      const res = await apiService.resetPassword(token, password);
      if (res.success) {
        setIsError(false);
        setMessage(res.message || 'Password reset successfully. Redirecting to login...');
        setSnackbarOpen(true);
        setTimeout(() => navigate('/login'), 1500);
      } else {
        setIsError(true);
        setMessage(res.message || 'Password reset failed.');
        setSnackbarOpen(true);
      }
    } catch (err: any) {
      setIsError(true);
      setMessage(err?.response?.data?.message || 'Password reset failed.');
      setSnackbarOpen(true);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="sm" sx={{ mt: 8 }}>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h5" sx={{ mb: 2 }}>Reset Password</Typography>
        <Box component="form" onSubmit={handleSubmit}>
          <TextField
            fullWidth
            type="password"
            label="New Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            margin="normal"
            required
          />
          <TextField
            fullWidth
            type="password"
            label="Confirm New Password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            margin="normal"
            required
          />
          <Button type="submit" variant="contained" disabled={loading} sx={{ mt: 2 }}>
            {loading ? 'Submitting...' : 'Reset Password'}
          </Button>
        </Box>
      </Paper>
      <Snackbar
        open={snackbarOpen}
        autoHideDuration={4000}
        onClose={() => setSnackbarOpen(false)}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert onClose={() => setSnackbarOpen(false)} severity={isError ? 'error' : 'success'} sx={{ width: '100%' }}>
          {message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default ResetPasswordPage;


