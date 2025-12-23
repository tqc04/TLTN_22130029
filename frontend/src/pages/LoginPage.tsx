import React, { useState } from 'react';
import {
  Container,
  Paper,
  TextField,
  Button,
  Typography,
  Box,
  CircularProgress,
  Divider,
  IconButton,
  Grid,
  Link
} from '@mui/material';
import {
  Google as GoogleIcon,
  Facebook as FacebookIcon,
  Visibility,
  VisibilityOff
} from '@mui/icons-material';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { useNotification } from '../contexts/NotificationContext';

const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const { login, isLoading } = useAuth();
  const navigate = useNavigate();
  const { notify } = useNotification();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!username || !password) {
      notify('Please enter both username and password', 'warning');
      return;
    }

    const success = await login(username, password);
    if (success) {
      notify('Login successful!', 'success');
      navigate('/');
    } else {
      notify('Invalid username or password', 'error');
    }
  };

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2
      }}
    >
      <Container maxWidth="sm">
        <Paper 
          elevation={24}
          sx={{ 
            p: 4,
            borderRadius: 3,
            background: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(10px)',
            border: '1px solid rgba(255, 255, 255, 0.2)'
          }}
        >
          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            {/* Logo and Header */}
            <Box sx={{ mb: 3, textAlign: 'center' }}>
              <Typography 
                variant="h3" 
                component="h1" 
                sx={{ 
                  fontWeight: 'bold',
                  background: 'linear-gradient(45deg, #667eea, #764ba2)',
                  backgroundClip: 'text',
                  WebkitBackgroundClip: 'text',
                  color: 'transparent',
                  mb: 1
                }}
              >
                TechHub
              </Typography>
              <Typography variant="h5" color="text.primary" sx={{ fontWeight: 500 }}>
                Welcome back
              </Typography>
              <Typography variant="body1" color="text.secondary" sx={{ mt: 1 }}>
                Sign in to your account to continue
              </Typography>
            </Box>

            {/* Social Login Buttons */}
            <Box sx={{ width: '100%', mb: 3 }}>
              <Typography variant="body2" color="text.secondary" textAlign="center" sx={{ mb: 2 }}>
                Sign in with
              </Typography>
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Button
                    fullWidth
                    variant="outlined"
                    startIcon={<GoogleIcon />}
                    component="a"
                    href="http://localhost:8080/oauth2/authorization/google"
                    sx={{
                      py: 1.5,
                      borderRadius: 2,
                      textTransform: 'none',
                      borderColor: '#db4437',
                      color: '#db4437',
                      '&:hover': {
                        borderColor: '#db4437',
                        backgroundColor: 'rgba(219, 68, 55, 0.04)'
                      }
                    }}
                  >
                    Google
                  </Button>
                </Grid>
                <Grid item xs={6}>
                  <Button
                    fullWidth
                    variant="outlined"
                    startIcon={<FacebookIcon />}
                    component="a"
                    href="http://localhost:8080/oauth2/authorization/facebook"
                    sx={{
                      py: 1.5,
                      borderRadius: 2,
                      textTransform: 'none',
                      borderColor: '#4267B2',
                      color: '#4267B2',
                      '&:hover': {
                        borderColor: '#4267B2',
                        backgroundColor: 'rgba(66, 103, 178, 0.04)'
                      }
                    }}
                  >
                    Facebook
                  </Button>
                </Grid>
              </Grid>
            </Box>

            <Divider sx={{ width: '100%', my: 3 }}>
              <Typography variant="body2" color="text.secondary">
                or continue with email
              </Typography>
            </Divider>

            {/* Login Form */}
            <Box component="form" onSubmit={handleSubmit} sx={{ width: '100%' }}>
              <TextField
                fullWidth
                label="Username or Email"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                margin="normal"
                required
                autoComplete="username"
                autoFocus
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2
                  }
                }}
              />
              
              <TextField
                fullWidth
                label="Password"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                margin="normal"
                required
                autoComplete="current-password"
                InputProps={{
                  endAdornment: (
                    <IconButton
                      aria-label="toggle password visibility"
                      onClick={togglePasswordVisibility}
                      edge="end"
                    >
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  ),
                }}
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2
                  }
                }}
              />

              <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 1, mb: 2 }}>
                <Link
                  component="button"
                  type="button"
                  variant="body2"
                  onClick={() => navigate('/forgot-password')}
                  sx={{
                    textDecoration: 'none',
                    color: 'primary.main',
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    '&:hover': {
                      textDecoration: 'underline'
                    }
                  }}
                >
                  Forgot password?
                </Link>
              </Box>

              <Button
                type="submit"
                fullWidth
                variant="contained"
                disabled={isLoading}
                sx={{
                  mt: 2, 
                  mb: 3, 
                  py: 1.5,
                  borderRadius: 2,
                  fontSize: '1rem',
                  fontWeight: 600,
                  textTransform: 'none',
                  background: 'linear-gradient(45deg, #667eea, #764ba2)',
                  '&:hover': {
                    background: 'linear-gradient(45deg, #5a6fd8, #6a3f8a)',
                  }
                }}
                size="large"
                startIcon={isLoading ? <CircularProgress size={20} color="inherit" /> : null}
              >
                {isLoading ? 'Signing In...' : 'Sign In'}
              </Button>
            </Box>

            {/* Sign Up Link */}
            <Typography variant="body2" color="text.secondary" textAlign="center">
              Don't have an account?{' '}
              <Link
                component="button"
                type="button"
                onClick={() => navigate('/signup')}
                sx={{
                  color: 'primary.main',
                  textDecoration: 'none',
                  fontWeight: 500,
                  background: 'none',
                  border: 'none',
                  cursor: 'pointer',
                  '&:hover': {
                    textDecoration: 'underline'
                  }
                }}
              >
                Sign up for free
              </Link>
            </Typography>
          </Box>
        </Paper>
      </Container>
    </Box>
  );
};

export default LoginPage; 