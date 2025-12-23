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
  IconButton,
  FormControlLabel,
  Checkbox,
  Alert
} from '@mui/material'
import { 
  Visibility, 
  VisibilityOff, 
  Person, 
  Email, 
  Lock, 
  Phone 
} from '@mui/icons-material'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import { apiService } from '../services/api'
import { useNotification } from '../contexts/NotificationContext';

interface SignUpFormData {
  username: string
  email: string
  password: string
  confirmPassword: string
  firstName: string
  lastName: string
  phoneNumber: string
  agreeToTerms: boolean
}

const SignUpPage: React.FC = () => {
  const navigate = useNavigate()
  const [formData, setFormData] = useState<SignUpFormData>({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    firstName: '',
    lastName: '',
    phoneNumber: '',
    agreeToTerms: false
  })
  
  const [errors, setErrors] = useState<Partial<Record<keyof SignUpFormData, string>>>({})
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null)
  const { notify } = useNotification();

  const validateForm = (): boolean => {
    const newErrors: Partial<Record<keyof SignUpFormData, string>> = {}

    // Username validation
    if (!formData.username.trim()) {
      newErrors.username = 'Username is required'
    } else if (formData.username.length < 3) {
      newErrors.username = 'Username must be at least 3 characters'
    }

    // Email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!formData.email.trim()) {
      newErrors.email = 'Email is required'
    } else if (!emailRegex.test(formData.email)) {
      newErrors.email = 'Please enter a valid email'
    }

    // Password validation
    if (!formData.password) {
      newErrors.password = 'Password is required'
    } else if (formData.password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters'
    }

    // Confirm password validation
    if (!formData.confirmPassword) {
      newErrors.confirmPassword = 'Please confirm your password'
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match'
    }

    // Name validation
    if (!formData.firstName.trim()) {
      newErrors.firstName = 'First name is required'
    }
    if (!formData.lastName.trim()) {
      newErrors.lastName = 'Last name is required'
    }

    // Phone validation (optional but if provided, should be valid)
    if (formData.phoneNumber && !/^\+?[\d\s-()]+$/.test(formData.phoneNumber)) {
      newErrors.phoneNumber = 'Please enter a valid phone number'
    }

    // Terms agreement
    if (!formData.agreeToTerms) {
      newErrors.agreeToTerms = 'You must agree to the terms and conditions'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleInputChange = (field: keyof SignUpFormData) => (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const value = field === 'agreeToTerms' ? event.target.checked : event.target.value
    setFormData(prev => ({ ...prev, [field]: value }))
    
    // Clear error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: undefined }))
    }
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    
    if (!validateForm()) {
      return
    }

    setIsLoading(true)
    setMessage(null)

    try {
      // Chỉ gửi các trường cần thiết, không gửi undefined/null
      const payload: any = {
        username: formData.username,
        email: formData.email,
        password: formData.password,
        firstName: formData.firstName,
        lastName: formData.lastName
      };
      if (formData.phoneNumber) payload.phoneNumber = formData.phoneNumber;

      const response = await apiService.register(payload)

      if (response.success) {
        setMessage({
          type: 'success',
          text: 'Registration successful! Please check your email for verification.'
        })
        notify('Registration successful! Please check your email for verification.', 'success');
        // Redirect to login after 3 seconds
        setTimeout(() => {
          navigate('/login')
        }, 3000)
      } else {
        setMessage({
          type: 'error',
          text: response.message || 'Registration failed'
        })
        notify(response.message || 'Registration failed', 'error');
      }
    } catch (error: unknown) {
      // Hiển thị lỗi rõ ràng nếu có message từ backend
      const errorMessage = error instanceof Error ? error.message : 'Registration failed. Please try again.';
      setMessage({
        type: 'error',
        text: errorMessage
      })
      notify(errorMessage, 'error');
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Container maxWidth="sm" sx={{ mt: 4, mb: 4 }}>
      <Paper elevation={3} sx={{ p: 4 }}>
        <Box textAlign="center" mb={3}>
          <Typography variant="h4" component="h1" gutterBottom>
            Create Account
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Join our e-commerce platform
          </Typography>
          <Box mt={1}>
            <Button component={RouterLink} to="/signup-phone" size="small">
              Hoặc đăng ký bằng số điện thoại
            </Button>
          </Box>
        </Box>

        {message && (
          <Alert severity={message.type} sx={{ mb: 3 }}>
            {message.text}
          </Alert>
        )}

        <Box component="form" onSubmit={handleSubmit}>
          <Box display="flex" gap={2} mb={2}>
            <TextField
              fullWidth
              label="First Name"
              value={formData.firstName}
              onChange={handleInputChange('firstName')}
              error={!!errors.firstName}
              helperText={errors.firstName}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Person />
                  </InputAdornment>
                ),
              }}
            />
            <TextField
              fullWidth
              label="Last Name"
              value={formData.lastName}
              onChange={handleInputChange('lastName')}
              error={!!errors.lastName}
              helperText={errors.lastName}
            />
          </Box>

          <TextField
            fullWidth
            label="Username"
            value={formData.username}
            onChange={handleInputChange('username')}
            error={!!errors.username}
            helperText={errors.username}
            sx={{ mb: 2 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Person />
                </InputAdornment>
              ),
            }}
          />

          <TextField
            fullWidth
            label="Email Address"
            type="email"
            value={formData.email}
            onChange={handleInputChange('email')}
            error={!!errors.email}
            helperText={errors.email}
            sx={{ mb: 2 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Email />
                </InputAdornment>
              ),
            }}
          />

          <TextField
            fullWidth
            label="Phone Number (Optional)"
            value={formData.phoneNumber}
            onChange={handleInputChange('phoneNumber')}
            error={!!errors.phoneNumber}
            helperText={errors.phoneNumber || 'Optional field'}
            sx={{ mb: 2 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Phone />
                </InputAdornment>
              ),
            }}
          />

          <TextField
            fullWidth
            label="Password"
            type={showPassword ? 'text' : 'password'}
            value={formData.password}
            onChange={handleInputChange('password')}
            error={!!errors.password}
            helperText={errors.password}
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
          />

          <TextField
            fullWidth
            label="Confirm Password"
            type={showConfirmPassword ? 'text' : 'password'}
            value={formData.confirmPassword}
            onChange={handleInputChange('confirmPassword')}
            error={!!errors.confirmPassword}
            helperText={errors.confirmPassword}
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
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    edge="end"
                  >
                    {showConfirmPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />

          <FormControlLabel
            control={
              <Checkbox
                checked={formData.agreeToTerms}
                onChange={handleInputChange('agreeToTerms')}
                color="primary"
              />
            }
            label={
              <Typography variant="body2">
                I agree to the{' '}
                <Link component={RouterLink} to="/terms" color="primary">
                  Terms and Conditions
                </Link>{' '}
                and{' '}
                <Link component={RouterLink} to="/privacy" color="primary">
                  Privacy Policy
                </Link>
              </Typography>
            }
            sx={{ mb: 2 }}
          />
          {typeof errors.agreeToTerms === 'string' && (
            <Typography variant="caption" color="error" display="block" sx={{ mt: -1, mb: 2 }}>
              {errors.agreeToTerms}
            </Typography>
          )}

          <Button
            type="submit"
            fullWidth
            variant="contained"
            size="large"
            disabled={isLoading}
            sx={{ mb: 2 }}
          >
            {isLoading ? 'Creating Account...' : 'Create Account'}
          </Button>

          <Box textAlign="center">
            <Typography variant="body2">
              Already have an account?{' '}
              <Link component={RouterLink} to="/login" color="primary">
                Sign In
              </Link>
            </Typography>
          </Box>
        </Box>
      </Paper>
    </Container>
  )
}

export default SignUpPage 