import React, { useState } from 'react'
import { Container, Paper, TextField, Button, Typography, Box, Alert } from '@mui/material'
import { useNavigate, Link as RouterLink } from 'react-router-dom'
import { apiService } from '../services/api'

const PhoneSignUpPage: React.FC = () => {
  const navigate = useNavigate()
  const [step, setStep] = useState<'request' | 'verify'>('request')
  const [phone, setPhone] = useState('')
  const [otp, setOtp] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const [loading, setLoading] = useState(false)

  const sendOtp = async () => {
    setMessage(null)
    if (!phone.trim()) {
      setMessage({ type: 'error', text: 'Vui lòng nhập số điện thoại' })
      return
    }
    setLoading(true)
    try {
      const res = await apiService.sendPhoneOtp(phone)
      if (res.success) {
        setMessage({ type: 'success', text: 'Đã gửi OTP. Vui lòng kiểm tra SMS.' })
        setStep('verify')
      } else {
        setMessage({ type: 'error', text: res.message || 'Gửi OTP thất bại' })
      }
    } catch (e) {
      setMessage({ type: 'error', text: 'Gửi OTP thất bại' })
    } finally {
      setLoading(false)
    }
  }

  const verifyOtp = async () => {
    setMessage(null)
    if (!otp.trim()) {
      setMessage({ type: 'error', text: 'Vui lòng nhập OTP' })
      return
    }
    setLoading(true)
    try {
      const res = await apiService.verifyPhoneOtp({ phone, otp, username, password, firstName, lastName })
      if (res.success) {
        // Lưu token + user
        localStorage.setItem('auth_token', res.data.token as unknown as string)
        localStorage.setItem('auth_user', JSON.stringify(res.data.user))
        setMessage({ type: 'success', text: 'Đăng ký/Đăng nhập bằng số điện thoại thành công' })
        setTimeout(() => navigate('/'), 1000)
      } else {
        setMessage({ type: 'error', text: res.message || 'Xác minh thất bại' })
      }
    } catch (e) {
      setMessage({ type: 'error', text: 'Xác minh thất bại' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <Container maxWidth="sm" sx={{ mt: 4, mb: 4 }}>
      <Paper elevation={3} sx={{ p: 4 }}>
        <Typography variant="h5" gutterBottom>
          Đăng ký/Đăng nhập bằng số điện thoại
        </Typography>
        {message && (
          <Alert severity={message.type} sx={{ mb: 2 }}>
            {message.text}
          </Alert>
        )}
        {step === 'request' ? (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              label="Số điện thoại"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              fullWidth
            />
            <Button variant="contained" onClick={sendOtp} disabled={loading}>
              Gửi OTP
            </Button>
            <Button component={RouterLink} to="/signup">Quay về đăng ký bằng email</Button>
          </Box>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField label="OTP" value={otp} onChange={(e) => setOtp(e.target.value)} fullWidth />
            <Typography variant="subtitle1">Thông tin tài khoản mới (nếu chưa có tài khoản):</Typography>
            <TextField label="Username" value={username} onChange={(e) => setUsername(e.target.value)} fullWidth />
            <TextField label="Mật khẩu" type="password" value={password} onChange={(e) => setPassword(e.target.value)} fullWidth />
            <TextField label="Họ" value={lastName} onChange={(e) => setLastName(e.target.value)} fullWidth />
            <TextField label="Tên" value={firstName} onChange={(e) => setFirstName(e.target.value)} fullWidth />
            <Box sx={{ display: 'flex', gap: 2 }}>
              <Button variant="contained" onClick={verifyOtp} disabled={loading}>Xác minh</Button>
              <Button variant="text" onClick={() => setStep('request')}>Nhập lại số điện thoại</Button>
            </Box>
          </Box>
        )}
      </Paper>
    </Container>
  )
}

export default PhoneSignUpPage


