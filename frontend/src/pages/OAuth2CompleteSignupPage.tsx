import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Box, Button, Container, Paper, TextField, Typography, Alert } from '@mui/material';
import axios from 'axios';

const OAuth2CompleteSignupPage: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const params = new URLSearchParams(location.search);
  const email = params.get('email') || '';

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    if (!username || !password || !confirmPassword) {
      setError('Vui lòng nhập đầy đủ thông tin.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.');
      return;
    }
    setLoading(true);
    try {
      const res = await axios.post('http://localhost:8080/api/auth/oauth2-signup', {
        email,
        username,
        password
      });
      if (res.data && res.data.token) {
        localStorage.setItem('jwt', res.data.token);
        localStorage.setItem('auth_token', res.data.token);
        localStorage.setItem('auth_user', JSON.stringify(res.data.user));
        setSuccess('Đăng ký thành công! Đang chuyển hướng...');
        setTimeout(() => navigate('/'), 1000);
      } else {
        setError(res.data.message || 'Đăng ký thất bại.');
      }
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Đăng ký thất bại.';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="sm" sx={{ mt: 8 }}>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h5" sx={{ mb: 2 }}>Hoàn thiện đăng ký tài khoản</Typography>
        <Typography variant="body2" sx={{ mb: 2 }}>Email: <b>{email}</b></Typography>
        <form onSubmit={handleSubmit}>
          <TextField
            label="Tên đăng nhập"
            fullWidth
            margin="normal"
            value={username}
            onChange={e => setUsername(e.target.value)}
            required
          />
          <TextField
            label="Mật khẩu"
            type="password"
            fullWidth
            margin="normal"
            value={password}
            onChange={e => setPassword(e.target.value)}
            required
          />
          <TextField
            label="Xác nhận mật khẩu"
            type="password"
            fullWidth
            margin="normal"
            value={confirmPassword}
            onChange={e => setConfirmPassword(e.target.value)}
            required
          />
          {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
          {success && <Alert severity="success" sx={{ mt: 2 }}>{success}</Alert>}
          <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
            <Button type="submit" variant="contained" disabled={loading}>Đăng ký & Đăng nhập</Button>
          </Box>
        </form>
      </Paper>
    </Container>
  );
};

export default OAuth2CompleteSignupPage; 