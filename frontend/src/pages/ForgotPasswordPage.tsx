import React, { useState } from 'react'
import { 
  Container, 
  Paper, 
  TextField, 
  Button, 
  Typography, 
  Box, 
  Link,
  InputAdornment,
  Stepper,
  Step,
  StepLabel,
  IconButton
} from '@mui/material'
import { 
  Email, 
  Lock, 
  ArrowBack,
  Visibility,
  VisibilityOff 
} from '@mui/icons-material'
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router-dom'
import { apiService } from '../services/api'
import { useNotification } from '../contexts/NotificationContext';

type Step = 'email' | 'reset'

const ForgotPasswordPage: React.FC = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const resetToken = searchParams.get('token')
  const { notify } = useNotification();
  
  const [currentStep] = useState<Step>(resetToken ? 'reset' : 'email')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)

  const validateEmail = (email: string): boolean => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return emailRegex.test(email)
  }

  const validatePassword = (password: string): boolean => {
    return password.length >= 6
  }

  const handleEmailSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    
    if (!email.trim()) {
      notify('Email is required', 'error')
      return
    }
    
    if (!validateEmail(email)) {
      notify('Please enter a valid email address', 'error')
      return
    }

    setIsLoading(true)

    try {
      const response = await apiService.forgotPassword(email)
      if (response.success) {
        notify('Password reset instructions have been sent to your email. Please check your inbox.', 'success')
      } else {
        notify(response.message || 'Failed to send reset email', 'error')
      }
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to send reset email. Please try again.';
      notify(errorMessage, 'error')
    } finally {
      setIsLoading(false)
    }
  }

  const handlePasswordReset = async (event: React.FormEvent) => {
    event.preventDefault()
    
    if (!password.trim()) {
      notify('Password is required', 'error')
      return
    }
    
    if (!validatePassword(password)) {
      notify('Password must be at least 6 characters long', 'error')
      return
    }
    
    if (password !== confirmPassword) {
      notify('Passwords do not match', 'error')
      return
    }

    if (!resetToken) {
      notify('Invalid reset token', 'error')
      return
    }

    setIsLoading(true)

    try {
      const response = await apiService.resetPassword(resetToken, password)
      if (response.success) {
        notify('Password reset successfully! You can now login with your new password.', 'success')
        setTimeout(() => {
          navigate('/login')
        }, 2000)
      } else {
        notify(response.message || 'Failed to reset password', 'error')
      }
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to reset password. Please try again.';
      notify(errorMessage, 'error')
    } finally {
      setIsLoading(false)
    }
  }

  const steps = ['Enter Email', 'Reset Password']
  const activeStep = currentStep === 'email' ? 0 : 1

  return (
    <Container maxWidth="sm" sx={{ mt: 4, mb: 4 }}>
      <Paper elevation={3} sx={{ p: 4 }}>
        <Box textAlign="center" mb={3}>
          <Typography variant="h4" component="h1" gutterBottom>
            {currentStep === 'email' ? 'Forgot Password' : 'Reset Password'}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {currentStep === 'email' 
              ? 'Enter your email address and we\'ll send you instructions to reset your password'
              : 'Enter your new password'
            }
          </Typography>
        </Box>

        <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
          {steps.map((label) => (
            <Step key={label}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>

        {currentStep === 'email' ? (
          <Box component="form" onSubmit={handleEmailSubmit}>
            <TextField
              fullWidth
              label="Email Address"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              sx={{ mb: 3 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Email />
                  </InputAdornment>
                ),
              }}
              placeholder="Enter your registered email address"
            />

            <Button
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              disabled={isLoading}
              sx={{ mb: 3 }}
            >
              {isLoading ? 'Sending...' : 'Send Reset Instructions'}
            </Button>

            <Box textAlign="center">
              <Link component={RouterLink} to="/login" color="primary">
                <Box display="flex" alignItems="center" justifyContent="center">
                  <ArrowBack sx={{ mr: 1, fontSize: 16 }} />
                  Back to Login
                </Box>
              </Link>
            </Box>
          </Box>
        ) : (
          <Box component="form" onSubmit={handlePasswordReset}>
            <TextField
              fullWidth
              label="New Password"
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              sx={{ mb: 2 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Lock />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={() => setShowPassword(!showPassword)}
                      edge="end"
                    >
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
              placeholder="Enter your new password"
            />

            <TextField
              fullWidth
              label="Confirm New Password"
              type={showConfirmPassword ? 'text' : 'password'}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              sx={{ mb: 3 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Lock />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      edge="end"
                    >
                      {showConfirmPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
              placeholder="Confirm your new password"
            />

            <Button
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              disabled={isLoading}
              sx={{ mb: 3 }}
            >
              {isLoading ? 'Resetting...' : 'Reset Password'}
            </Button>

            <Box textAlign="center">
              <Link component={RouterLink} to="/login" color="primary">
                <Box display="flex" alignItems="center" justifyContent="center">
                  <ArrowBack sx={{ mr: 1, fontSize: 16 }} />
                  Back to Login
                </Box>
              </Link>
            </Box>
          </Box>
        )}
      </Paper>
    </Container>
  )
}

export default ForgotPasswordPage 