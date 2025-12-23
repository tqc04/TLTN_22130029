import React, { useState, useEffect } from 'react'
import {
  Container,
  Paper,
  Typography,
  Box,
  Grid,
  TextField,
  Button,
  Avatar,
  Alert,
  Tab,
  Tabs,
  Switch,
  FormControlLabel,
  InputAdornment,
  IconButton,
  Chip
} from '@mui/material'
import {
  Person,
  Email,
  Phone,
  LocationOn,
  Edit,
  Save,
  Cancel,
  Lock,
  Visibility,
  VisibilityOff,
  PhotoCamera,
  Settings,
  Security,
  Notifications
} from '@mui/icons-material'
import { useAuth } from '../contexts/AuthContext'
import { apiService } from '../services/api'

// Helper function to format avatar URL
const formatAvatarUrl = (url: string | undefined | null): string => {
  if (!url) return ''
  // If URL is already absolute (starts with http:// or https://), return as is
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  // If URL starts with /uploads, it's a relative path - use it directly (Vite proxy will handle it)
  if (url.startsWith('/uploads')) {
    return url
  }
  // Otherwise, prepend /uploads if it doesn't start with /
  return url.startsWith('/') ? url : `/uploads/${url}`
}

interface TabPanelProps {
  children?: React.ReactNode
  index: number
  value: number
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`profile-tabpanel-${index}`}
      aria-labelledby={`profile-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  )
}

interface ProfileData {
  firstName: string
  lastName: string
  email: string
  phoneNumber: string
  address: string
  dateOfBirth: string
  avatarUrl: string
}

interface PasswordData {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

interface PreferencesData {
  personalizationEnabled: boolean
  chatbotEnabled: boolean
  recommendationEnabled: boolean
  emailNotifications: boolean
  pushNotifications: boolean
}

const ProfilePage: React.FC = () => {
  const { user, isAuthenticated, refreshUser } = useAuth()
  const [tabValue, setTabValue] = useState(0)
  const [isEditing, setIsEditing] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null)
  
  const [profileData, setProfileData] = useState<ProfileData>({
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    address: '',
    dateOfBirth: '',
    avatarUrl: ''
  })

  const [passwordData, setPasswordData] = useState<PasswordData>({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  })

  const [preferences, setPreferences] = useState<PreferencesData>({
    personalizationEnabled: true,
    chatbotEnabled: true,
    recommendationEnabled: true,
    emailNotifications: true,
    pushNotifications: false
  })

  const [showPasswords, setShowPasswords] = useState({
    current: false,
    new: false,
    confirm: false
  })

  const [uploadingAvatar, setUploadingAvatar] = useState(false)
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null)
  const [selectedAvatarFile, setSelectedAvatarFile] = useState<File | null>(null)
  const fileInputRef = React.useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (user) {
      setProfileData(prev => {
        const newAvatarUrl = user.avatarUrl || user.profileImageUrl || prev.avatarUrl || ''
        console.log('useEffect - Updating profileData with user:', {
          userAvatarUrl: user.avatarUrl,
          userProfileImageUrl: user.profileImageUrl,
          prevAvatarUrl: prev.avatarUrl,
          finalAvatarUrl: newAvatarUrl
        })
        
        return {
          firstName: user.firstName || '',
          lastName: user.lastName || '',
          email: user.email || '',
          phoneNumber: user.phoneNumber || '',
          address: user.address || '',
          dateOfBirth: user.dateOfBirth || '',
          // Always use the latest avatar URL from user, or keep existing if user doesn't have one
          avatarUrl: newAvatarUrl
        }
      })
      
      setPreferences({
        personalizationEnabled: user.personalizationEnabled ?? true,
        chatbotEnabled: user.chatbotEnabled ?? true,
        recommendationEnabled: user.recommendationEnabled ?? true,
        emailNotifications: true,
        pushNotifications: false
      })
    }
  }, [user])

  if (!isAuthenticated) {
    return (
      <Container maxWidth="sm" sx={{ mt: 4 }}>
        <Alert severity="warning">
          Please log in to view your profile.
        </Alert>
      </Container>
    )
  }

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue)
    setMessage(null)
  }

  const handleProfileSave = async () => {
    setIsLoading(true)
    setMessage(null)

    try {
      // Convert profileData to match backend expectations
      // Don't include email in profile update to avoid duplicate email issues
      const updateData = {
        firstName: profileData.firstName,
        lastName: profileData.lastName,
        phoneNumber: profileData.phoneNumber,
        address: profileData.address,
        dateOfBirth: profileData.dateOfBirth
      }
      
      const response = await apiService.updateUserProfile(updateData)
      if (response.success) {
        setMessage({ type: 'success', text: 'Profile updated successfully!' })
        setIsEditing(false)
        
        // Refresh user data in AuthContext to sync changes everywhere (navbar, etc.)
        try {
          await refreshUser()
        } catch (error) {
          console.error('Failed to refresh user after profile update:', error)
        }
      }
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to update profile';
      setMessage({
        type: 'error',
        text: errorMessage
      })
    } finally {
      setIsLoading(false)
    }
  }

  const handlePasswordChange = async () => {
    if (!passwordData.currentPassword || !passwordData.newPassword) {
      setMessage({ type: 'error', text: 'Please fill in all password fields' })
      return
    }

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setMessage({ type: 'error', text: 'New passwords do not match' })
      return
    }

    if (passwordData.newPassword.length < 6) {
      setMessage({ type: 'error', text: 'New password must be at least 6 characters' })
      return
    }

    setIsLoading(true)
    setMessage(null)

    try {
      const response = await apiService.changePassword(
        passwordData.currentPassword,
        passwordData.newPassword
      )
      
      if (response.success) {
        setMessage({ type: 'success', text: 'Password changed successfully!' })
        setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' })
      }
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to change password';
      setMessage({
        type: 'error',
        text: errorMessage
      })
    } finally {
      setIsLoading(false)
    }
  }

  const handlePreferencesSave = async () => {
    setIsLoading(true)
    setMessage(null)

    try {
      const response = await apiService.updatePreferences({
        personalizationEnabled: preferences.personalizationEnabled,
        chatbotEnabled: preferences.chatbotEnabled,
        recommendationEnabled: preferences.recommendationEnabled
      })
      if (response.success) {
        setMessage({ type: 'success', text: 'Preferences updated successfully!' })
        
        // Refresh user data in AuthContext to sync changes everywhere
        try {
          await refreshUser()
        } catch (error) {
          console.error('Failed to refresh user after preferences update:', error)
        }
      }
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to update preferences';
      setMessage({
        type: 'error',
        text: errorMessage
      })
    } finally {
      setIsLoading(false)
    }
  }

  const handleNotificationSettingsSave = async () => {
    setIsLoading(true)
    setMessage(null)

    try {
      const response = await apiService.updateNotificationSettings({
        emailNotifications: preferences.emailNotifications,
        pushNotifications: preferences.pushNotifications
      })
      if (response.success) {
        setMessage({ type: 'success', text: 'Notification settings updated successfully!' })
        
        // Refresh user data in AuthContext to sync changes everywhere
        try {
          await refreshUser()
        } catch (error) {
          console.error('Failed to refresh user after notification settings update:', error)
        }
      }
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to update notification settings';
      setMessage({
        type: 'error',
        text: errorMessage
      })
    } finally {
      setIsLoading(false)
    }
  }

  const handleAvatarClick = () => {
    fileInputRef.current?.click()
  }

  const handleAvatarChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    // Validate file type
    if (!file.type.startsWith('image/')) {
      setMessage({ type: 'error', text: 'Please select an image file' })
      return
    }

    // Validate file size (5MB)
    if (file.size > 5 * 1024 * 1024) {
      setMessage({ type: 'error', text: 'File size must be less than 5MB' })
      return
    }

    // Create preview URL
    const reader = new FileReader()
    reader.onloadend = () => {
      setAvatarPreview(reader.result as string)
      setSelectedAvatarFile(file)
    }
    reader.readAsDataURL(file)
  }

  const handleSaveAvatar = async () => {
    if (!selectedAvatarFile || !user?.id) {
      setMessage({ type: 'error', text: 'Please select an image first' })
      return
    }

    setUploadingAvatar(true)
    setMessage(null)

    try {
      const response = await apiService.uploadAvatar(user.id, selectedAvatarFile)
      console.log('Upload avatar response:', response)
      
      if (response.success && response.data) {
        const newAvatarUrl = response.data.avatarUrl
        console.log('New avatar URL:', newAvatarUrl)
        
        // Clear preview and selected file first
        setAvatarPreview(null)
        setSelectedAvatarFile(null)
        
        // Update profile data with new avatar URL immediately
        setProfileData(prev => {
          const updated = {
            ...prev,
            avatarUrl: newAvatarUrl
          }
          console.log('Updated profileData:', updated)
          return updated
        })
        
        // Refresh user data in AuthContext to sync avatar everywhere (navbar, etc.)
        try {
          await refreshUser()
          
          // Wait a moment for AuthContext to update, then get the latest user
          setTimeout(() => {
            const refreshedUser = JSON.parse(localStorage.getItem('auth_user') || '{}')
            console.log('Refreshed user from localStorage:', refreshedUser)
            
            const finalAvatarUrl = refreshedUser.avatarUrl || refreshedUser.profileImageUrl || newAvatarUrl
            console.log('Final avatar URL:', finalAvatarUrl)
            
            // Update profileData with the refreshed avatar URL
            setProfileData(prev => ({
              ...prev,
              avatarUrl: finalAvatarUrl
            }))
          }, 200)
        } catch (error) {
          console.error('Failed to refresh user after avatar upload:', error)
          // Fallback: manually update localStorage
          const rawUser = localStorage.getItem('auth_user')
          if (rawUser) {
            const userObj = JSON.parse(rawUser)
            userObj.avatarUrl = newAvatarUrl
            userObj.profileImageUrl = newAvatarUrl
            localStorage.setItem('auth_user', JSON.stringify(userObj))
            
            // Update profileData with the new URL
            setProfileData(prev => ({
              ...prev,
              avatarUrl: newAvatarUrl
            }))
          }
        }
        
        setMessage({ type: 'success', text: 'Avatar uploaded successfully!' })
      } else {
        setMessage({ type: 'error', text: 'Failed to upload avatar' })
      }
    } catch (error: unknown) {
      console.error('Avatar upload error:', error)
      const errorMessage = error instanceof Error ? error.message : 'Failed to upload avatar'
      setMessage({
        type: 'error',
        text: errorMessage
      })
    } finally {
      setUploadingAvatar(false)
      // Reset file input
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    }
  }

  const handleCancelAvatar = () => {
    setAvatarPreview(null)
    setSelectedAvatarFile(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4, pt: '40px' }}>
      {/* Profile Header */}
      <Paper elevation={3} sx={{ p: 3, mb: 3 }}>
        <Grid container spacing={3} alignItems="flex-start">
          <Grid item>
            <Box 
              sx={{ 
                display: 'flex', 
                flexDirection: 'column', 
                alignItems: 'center',
                gap: 2
              }}
            >
              <Box position="relative">
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleAvatarChange}
                  accept="image/*"
                  style={{ display: 'none' }}
                />
                <Avatar
                  src={avatarPreview || formatAvatarUrl(profileData.avatarUrl)}
                  sx={{ 
                    width: 100, 
                    height: 100,
                    cursor: 'pointer',
                    opacity: uploadingAvatar ? 0.6 : 1,
                    border: avatarPreview ? '3px solid' : 'none',
                    borderColor: avatarPreview ? 'primary.main' : 'transparent',
                    transition: 'all 0.3s ease'
                  }}
                  onClick={handleAvatarClick}
                >
                  {user?.firstName?.[0]}{user?.lastName?.[0]}
                </Avatar>
                <IconButton
                  sx={{
                    position: 'absolute',
                    bottom: 0,
                    right: 0,
                    backgroundColor: 'primary.main',
                    color: 'white',
                    '&:hover': { backgroundColor: 'primary.dark' },
                    cursor: 'pointer',
                    boxShadow: '0 2px 8px rgba(0,0,0,0.2)'
                  }}
                  size="small"
                  onClick={handleAvatarClick}
                  disabled={uploadingAvatar || !!avatarPreview}
                >
                  <PhotoCamera fontSize="small" />
                </IconButton>
                {uploadingAvatar && (
                  <Box
                    sx={{
                      position: 'absolute',
                      top: '50%',
                      left: '50%',
                      transform: 'translate(-50%, -50%)',
                      color: 'primary.main',
                      backgroundColor: 'rgba(255,255,255,0.9)',
                      padding: '4px 8px',
                      borderRadius: 1
                    }}
                  >
                    <Typography variant="caption" fontWeight={600}>Uploading...</Typography>
                  </Box>
                )}
              </Box>
              
              {avatarPreview && !uploadingAvatar && (
                <Box
                  sx={{
                    display: 'flex',
                    gap: 1.5,
                    width: '100%',
                    justifyContent: 'center'
                  }}
                >
                  <Button
                    variant="contained"
                    color="primary"
                    size="medium"
                    onClick={handleSaveAvatar}
                    startIcon={<Save />}
                    sx={{
                      borderRadius: 2,
                      textTransform: 'none',
                      fontWeight: 600,
                      px: 3,
                      py: 1,
                      boxShadow: '0 2px 8px rgba(102, 126, 234, 0.3)',
                      '&:hover': {
                        boxShadow: '0 4px 12px rgba(102, 126, 234, 0.4)',
                        transform: 'translateY(-1px)'
                      },
                      transition: 'all 0.2s ease-in-out'
                    }}
                  >
                    Lưu
                  </Button>
                  <Button
                    variant="outlined"
                    color="error"
                    size="medium"
                    onClick={handleCancelAvatar}
                    startIcon={<Cancel />}
                    sx={{
                      borderRadius: 2,
                      textTransform: 'none',
                      fontWeight: 600,
                      px: 3,
                      py: 1,
                      borderWidth: 2,
                      '&:hover': {
                        borderWidth: 2,
                        backgroundColor: 'error.light',
                        color: 'error.contrastText',
                        transform: 'translateY(-1px)'
                      },
                      transition: 'all 0.2s ease-in-out'
                    }}
                  >
                    Hủy
                  </Button>
                </Box>
              )}
            </Box>
          </Grid>
          <Grid item xs>
            <Typography variant="h4" gutterBottom>
              {user?.firstName} {user?.lastName}
            </Typography>
            <Typography variant="body1" color="text.secondary" gutterBottom>
              {user?.email}
            </Typography>
            <Box display="flex" gap={1}>
              <Chip
                label={user?.role}
                color={user?.role === 'ADMIN' ? 'primary' : 'default'}
                size="small"
              />
              <Chip
                label={user?.isEmailVerified ? 'Verified' : 'Unverified'}
                color={user?.isEmailVerified ? 'success' : 'warning'}
                size="small"
              />
            </Box>
          </Grid>
        </Grid>
      </Paper>

      {/* Tabs */}
      <Paper elevation={3}>
        <Tabs
          value={tabValue}
          onChange={handleTabChange}
          aria-label="profile tabs"
          sx={{ borderBottom: 1, borderColor: 'divider' }}
        >
          <Tab icon={<Person />} label="Profile" />
          <Tab icon={<Security />} label="Security" />
          <Tab icon={<Settings />} label="Preferences" />
          <Tab icon={<Notifications />} label="Notifications" />
        </Tabs>

        {message && (
          <Box sx={{ p: 2 }}>
            <Alert severity={message.type}>{message.text}</Alert>
          </Box>
        )}

        {/* Profile Tab */}
        <TabPanel value={tabValue} index={0}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="First Name"
                value={profileData.firstName}
                onChange={(e) => setProfileData(prev => ({ ...prev, firstName: e.target.value }))}
                disabled={!isEditing}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Person />
                    </InputAdornment>
                  ),
                }}
                sx={{ mb: 2 }}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Last Name"
                value={profileData.lastName}
                onChange={(e) => setProfileData(prev => ({ ...prev, lastName: e.target.value }))}
                disabled={!isEditing}
                sx={{ mb: 2 }}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Email"
                value={profileData.email}
                disabled={true} // Email cannot be edited
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Email />
                    </InputAdornment>
                  ),
                }}
                sx={{ mb: 2 }}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Phone Number"
                value={profileData.phoneNumber}
                onChange={(e) => setProfileData(prev => ({ ...prev, phoneNumber: e.target.value }))}
                disabled={!isEditing}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Phone />
                    </InputAdornment>
                  ),
                }}
                sx={{ mb: 2 }}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Address"
                value={profileData.address}
                onChange={(e) => setProfileData(prev => ({ ...prev, address: e.target.value }))}
                disabled={!isEditing}
                multiline
                rows={2}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <LocationOn />
                    </InputAdornment>
                  ),
                }}
                sx={{ mb: 2 }}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Date of Birth"
                type="date"
                value={profileData.dateOfBirth}
                onChange={(e) => setProfileData(prev => ({ ...prev, dateOfBirth: e.target.value }))}
                disabled={!isEditing}
                InputLabelProps={{ shrink: true }}
                sx={{ mb: 2 }}
              />
            </Grid>
          </Grid>

          <Box sx={{ mt: 3, display: 'flex', gap: 2 }}>
            {!isEditing ? (
              <Button
                variant="contained"
                startIcon={<Edit />}
                onClick={() => setIsEditing(true)}
              >
                Edit Profile
              </Button>
            ) : (
              <>
                <Button
                  variant="contained"
                  startIcon={<Save />}
                  onClick={handleProfileSave}
                  disabled={isLoading}
                >
                  Save Changes
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<Cancel />}
                  onClick={() => setIsEditing(false)}
                  disabled={isLoading}
                >
                  Cancel
                </Button>
              </>
            )}
          </Box>
        </TabPanel>

        {/* Security Tab */}
        <TabPanel value={tabValue} index={1}>
          <Typography variant="h6" gutterBottom>
            Change Password
          </Typography>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Current Password"
                type={showPasswords.current ? 'text' : 'password'}
                value={passwordData.currentPassword}
                onChange={(e) => setPasswordData(prev => ({ ...prev, currentPassword: e.target.value }))}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Lock />
                    </InputAdornment>
                  ),
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        onClick={() => setShowPasswords(prev => ({ ...prev, current: !prev.current }))}
                        edge="end"
                      >
                        {showPasswords.current ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
                sx={{ mb: 2 }}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="New Password"
                type={showPasswords.new ? 'text' : 'password'}
                value={passwordData.newPassword}
                onChange={(e) => setPasswordData(prev => ({ ...prev, newPassword: e.target.value }))}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Lock />
                    </InputAdornment>
                  ),
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        onClick={() => setShowPasswords(prev => ({ ...prev, new: !prev.new }))}
                        edge="end"
                      >
                        {showPasswords.new ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
                sx={{ mb: 2 }}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Confirm New Password"
                type={showPasswords.confirm ? 'text' : 'password'}
                value={passwordData.confirmPassword}
                onChange={(e) => setPasswordData(prev => ({ ...prev, confirmPassword: e.target.value }))}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Lock />
                    </InputAdornment>
                  ),
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        onClick={() => setShowPasswords(prev => ({ ...prev, confirm: !prev.confirm }))}
                        edge="end"
                      >
                        {showPasswords.confirm ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
                sx={{ mb: 2 }}
              />
            </Grid>
          </Grid>

          <Button
            variant="contained"
            onClick={handlePasswordChange}
            disabled={isLoading}
            sx={{ mt: 2 }}
          >
            Change Password
          </Button>
        </TabPanel>

        {/* Preferences Tab */}
        <TabPanel value={tabValue} index={2}>
          <Typography variant="h6" gutterBottom>
            Account Preferences
          </Typography>
          <Box sx={{ mb: 3 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={preferences.personalizationEnabled}
                  onChange={(e) => setPreferences(prev => ({ ...prev, personalizationEnabled: e.target.checked }))}
                />
              }
              label="Enable Personalization"
            />
            <Typography variant="body2" color="text.secondary" sx={{ ml: 4 }}>
              Allow the system to personalize your experience based on your behavior
            </Typography>
          </Box>

          <Box sx={{ mb: 3 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={preferences.chatbotEnabled}
                  onChange={(e) => setPreferences(prev => ({ ...prev, chatbotEnabled: e.target.checked }))}
                />
              }
              label="Enable AI Assistant"
            />
            <Typography variant="body2" color="text.secondary" sx={{ ml: 4 }}>
              Allow access to the AI-powered chatbot assistant
            </Typography>
          </Box>

          <Box sx={{ mb: 3 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={preferences.recommendationEnabled}
                  onChange={(e) => setPreferences(prev => ({ ...prev, recommendationEnabled: e.target.checked }))}
                />
              }
              label="Enable Recommendations"
            />
            <Typography variant="body2" color="text.secondary" sx={{ ml: 4 }}>
              Receive personalized product recommendations
            </Typography>
          </Box>

          <Button
            variant="contained"
            onClick={handlePreferencesSave}
            disabled={isLoading}
          >
            Save Preferences
          </Button>
        </TabPanel>

        {/* Notifications Tab */}
        <TabPanel value={tabValue} index={3}>
          <Typography variant="h6" gutterBottom>
            Notification Settings
          </Typography>
          <Box sx={{ mb: 3 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={preferences.emailNotifications}
                  onChange={(e) => setPreferences(prev => ({ ...prev, emailNotifications: e.target.checked }))}
                />
              }
              label="Email Notifications"
            />
            <Typography variant="body2" color="text.secondary" sx={{ ml: 4 }}>
              Receive order updates and promotional emails
            </Typography>
          </Box>

          <Box sx={{ mb: 3 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={preferences.pushNotifications}
                  onChange={(e) => setPreferences(prev => ({ ...prev, pushNotifications: e.target.checked }))}
                />
              }
              label="Push Notifications"
            />
            <Typography variant="body2" color="text.secondary" sx={{ ml: 4 }}>
              Receive browser push notifications for important updates
            </Typography>
          </Box>

          <Button
            variant="contained"
            onClick={handleNotificationSettingsSave}
            disabled={isLoading}
          >
            Save Settings
          </Button>
        </TabPanel>
      </Paper>
    </Container>
  )
}

export default ProfilePage 