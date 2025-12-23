import React from 'react'
import { Container, Box, Typography, Paper } from '@mui/material'
import ChatWidget from '../components/ChatWidget'

const ChatbotPage: React.FC = () => {
  return (
    <Container maxWidth="sm" sx={{ mt: 6, mb: 6 }}>
      <Paper sx={{ p: 2, mb: 2, borderRadius: 3, textAlign: 'center' }} elevation={3}>
        <Typography variant="h5" fontWeight={700} gutterBottom>
          AI Tư Vấn Điện Tử
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Hỏi đáp, tư vấn mua sắm điện thoại, laptop, tai nghe, thiết bị gia dụng... Hãy cho biết nhu cầu và ngân sách của bạn.
        </Typography>
      </Paper>

      <Box display="flex" justifyContent="center">
        <ChatWidget
          embedded
          defaultOpen
          title="AI Tư Vấn Điện Tử"
          welcomeMessage="Xin chào, tôi là AI tư vấn đồ điện tử. Bạn đang cần điện thoại, laptop, tai nghe hay thiết bị gia dụng? Hãy cho tôi biết nhu cầu, ngân sách và thương hiệu bạn thích."
        />
      </Box>
    </Container>
  )
}

export default ChatbotPage