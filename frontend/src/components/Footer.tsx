import React, { useState } from 'react'
import {
  Box,
  Container,
  Grid,
  Typography,
  Link,
  TextField,
  Button,
  IconButton,
} from '@mui/material'
import {
  Facebook,
  Twitter,
  Instagram,
  LinkedIn,
  YouTube,
  Send,
  Security,
  LocalShipping,
  SupportAgent,
  VerifiedUser,
  PaymentOutlined,
  LocationOn,
} from '@mui/icons-material'

const Footer: React.FC = () => {
  const [email, setEmail] = useState('')

  const handleNewsletterSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (email) {
      // Here you would typically send to your newsletter service
      console.log('Newsletter subscription:', email)
      setEmail('')
    }
  }

  const quickLinks = [
    { label: 'Home', href: '/', title: 'Trang chủ TechHub' },
    { label: 'Products', href: '/products', title: 'Danh sách sản phẩm' },
    { label: 'About Us', href: '/about', title: 'Về chúng tôi' },
    { label: 'Contact', href: '/contact', title: 'Liên hệ' },
    { label: 'Blog', href: '/blog', title: 'Blog công nghệ' },
    { label: 'Careers', href: '/careers', title: 'Tuyển dụng' },
  ]

  const categories = [
    { label: 'Electronics', href: '/products?category=electronics', title: 'Sản phẩm điện tử' },
    { label: 'Gaming', href: '/products?category=gaming', title: 'Sản phẩm gaming' },
    { label: 'Computers', href: '/products?category=computers', title: 'Máy tính và laptop' },
    { label: 'Mobile Devices', href: '/products?category=mobile', title: 'Thiết bị di động' },
    { label: 'Audio & Headphones', href: '/products?category=audio', title: 'Tai nghe và âm thanh' },
    { label: 'Accessories', href: '/products?category=accessories', title: 'Phụ kiện công nghệ' },
  ]

  const socialLinks = [
    { icon: <Facebook />, href: 'https://facebook.com', label: 'Facebook', color: '#1877f2', ariaLabel: 'Theo dõi TechHub trên Facebook' },
    { icon: <Twitter />, href: 'https://twitter.com', label: 'Twitter', color: '#1da1f2', ariaLabel: 'Theo dõi TechHub trên Twitter' },
    { icon: <Instagram />, href: 'https://instagram.com', label: 'Instagram', color: '#e4405f', ariaLabel: 'Theo dõi TechHub trên Instagram' },
    { icon: <LinkedIn />, href: 'https://linkedin.com', label: 'LinkedIn', color: '#0077b5', ariaLabel: 'Theo dõi TechHub trên LinkedIn' },
    { icon: <YouTube />, href: 'https://youtube.com', label: 'YouTube', color: '#ff0000', ariaLabel: 'Theo dõi TechHub trên YouTube' },
  ]

  const features = [
    { 
      icon: <LocalShipping />, 
      text: 'Free Shipping', 
      subtext: 'Miễn phí vận chuyển',
      ariaLabel: 'Miễn phí vận chuyển cho đơn hàng trên 50$'
    },
    { 
      icon: <Security />, 
      text: 'Secure Payments', 
      subtext: 'Thanh toán an toàn 100%',
      ariaLabel: 'Thanh toán an toàn 100%'
    },
    { 
      icon: <SupportAgent />, 
      text: '24/7 Support', 
      subtext: 'Hỗ trợ 24/7',
      ariaLabel: 'Đội ngũ hỗ trợ chuyên nghiệp 24/7'
    },
    { 
      icon: <VerifiedUser />, 
      text: 'Money Back', 
      subtext: 'Bảo đảm hoàn tiền',
      ariaLabel: 'Bảo đảm hoàn tiền trong 30 ngày'
    },
  ]

  return (
    <Box 
      component="footer" 
      role="contentinfo"
      sx={{ bgcolor: '#181818', color: 'white', mt: 'auto' }}
    >
      {/* Features Banner - Service Guarantees */}
      <Box 
        component="section"
        aria-label="Cam kết dịch vụ"
        sx={{ bgcolor: '#232323', py: 3 }}
      >
        <Container maxWidth="lg">
          <Grid container spacing={3} justifyContent="center">
            {features.map((feature, index) => (
              <Grid 
                item 
                xs={6} 
                md={3} 
                key={index} 
                sx={{ textAlign: 'center' }}
              >
                <Box 
                  sx={{ 
                    color: '#8b5cf6', 
                    mb: 1,
                    fontSize: '2.5rem',
                    display: 'flex',
                    justifyContent: 'center'
                  }}
                  aria-hidden="true"
                >
                  {feature.icon}
                </Box>
                <Typography 
                  component="h3"
                  variant="body1" 
                  fontWeight="bold"
                  sx={{ mb: 0.5 }}
                >
                  {feature.text}
                </Typography>
                <Typography 
                  variant="caption" 
                  sx={{ opacity: 0.8, display: 'block' }}
                  aria-label={feature.ariaLabel}
                >
                  {feature.subtext}
                </Typography>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* Main Footer Content */}
      <Container maxWidth="lg" sx={{ py: 5 }}>
        <Grid container spacing={4} alignItems="flex-start">
          {/* Company Info + Social */}
          <Grid item xs={12} md={4}>
            <Typography 
              component="h2"
              variant="h5" 
              fontWeight="bold" 
              sx={{ color: '#8b5cf6', mb: 2 }}
            >
              TechHub
            </Typography>
            <Typography 
              component="p"
              variant="body2" 
              sx={{ mb: 3, opacity: 0.9, lineHeight: 1.7 }}
            >
              Your trusted e-commerce destination for the latest tech products.
            </Typography>
            <Box 
              component="nav"
              aria-label="Social media links"
              sx={{ display: 'flex', gap: 1.5, mt: 2 }}
            >
              {socialLinks.map((social, idx) => (
                <IconButton 
                  key={idx} 
                  href={social.href}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={social.ariaLabel}
                  sx={{ 
                    color: 'white', 
                    bgcolor: 'rgba(255,255,255,0.1)',
                    '&:hover': { 
                      color: social.color,
                      bgcolor: 'rgba(255,255,255,0.2)',
                      transform: 'translateY(-2px)'
                    },
                    transition: 'all 0.3s ease'
                  }}
                >
                  {social.icon}
                </IconButton>
              ))}
            </Box>
          </Grid>

          {/* Quick Links */}
          <Grid item xs={6} sm={4} md={2}>
            <Typography 
              component="h3"
              variant="subtitle1" 
              fontWeight="bold" 
              sx={{ mb: 2 }}
            >
              Quick Links
            </Typography>
            <Box component="nav" aria-label="Quick navigation links">
              {quickLinks.map((link, idx) => (
                <Link 
                  key={idx} 
                  href={link.href} 
                  color="inherit" 
                  underline="hover" 
                  title={link.title}
                  sx={{ 
                    display: 'block', 
                    mb: 1.5, 
                    opacity: 0.8, 
                    fontSize: '0.9rem',
                    '&:hover': { 
                      color: '#8b5cf6', 
                      opacity: 1,
                      transform: 'translateX(4px)'
                    },
                    transition: 'all 0.2s ease'
                  }}
                >
                  {link.label}
                </Link>
              ))}
            </Box>
          </Grid>

          {/* Categories */}
          <Grid item xs={6} sm={4} md={2}>
            <Typography 
              component="h3"
              variant="subtitle1" 
              fontWeight="bold" 
              sx={{ mb: 2 }}
            >
              Categories
            </Typography>
            <Box component="nav" aria-label="Product categories">
              {categories.map((cat, idx) => (
                <Link 
                  key={idx} 
                  href={cat.href} 
                  color="inherit" 
                  underline="hover" 
                  title={cat.title}
                  sx={{ 
                    display: 'block', 
                    mb: 1.5, 
                    opacity: 0.8,
                    fontSize: '0.9rem',
                    '&:hover': { 
                      color: '#8b5cf6', 
                      opacity: 1,
                      transform: 'translateX(4px)'
                    },
                    transition: 'all 0.2s ease'
                  }}
                >
                  {cat.label}
                </Link>
              ))}
            </Box>
          </Grid>

          {/* Newsletter */}
          <Grid item xs={12} md={4}>
            <Typography 
              component="h3"
              variant="subtitle1" 
              fontWeight="bold" 
              sx={{ mb: 2 }}
            >
              Stay Updated
            </Typography>
            <Box 
              component="form" 
              onSubmit={handleNewsletterSubmit}
              aria-label="Newsletter subscription form"
              sx={{ display: 'flex', gap: 1, mb: 1.5 }}
            >
              <TextField
                type="email"
                size="small"
                placeholder="Your email address"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
                aria-label="Email address for newsletter"
                sx={{ 
                  bgcolor: 'white', 
                  borderRadius: 1, 
                  flex: 1,
                  '& .MuiOutlinedInput-root': {
                    '&:hover fieldset': {
                      borderColor: '#8b5cf6',
                    },
                  }
                }}
                InputProps={{ sx: { fontSize: 14 } }}
              />
              <Button 
                type="submit" 
                variant="contained" 
                sx={{ 
                  borderRadius: 1, 
                  px: 2, 
                  minWidth: 48,
                  bgcolor: '#8b5cf6',
                  '&:hover': {
                    bgcolor: '#7c3aed',
                    transform: 'scale(1.05)'
                  },
                  transition: 'all 0.2s ease'
                }}
                aria-label="Subscribe to newsletter"
              >
                <Send fontSize="small" />
              </Button>
            </Box>
            <Typography 
              component="p"
              variant="caption" 
              sx={{ opacity: 0.7, lineHeight: 1.6 }}
            >
              Get special offers, free giveaways, and exclusive deals.
            </Typography>
          </Grid>
        </Grid>
      </Container>

      {/* Bottom Bar - Copyright */}
      <Box 
        component="section"
        aria-label="Copyright information"
        sx={{ bgcolor: '#111', py: 2.5, borderTop: '1px solid rgba(255,255,255,0.1)' }}
      >
        <Container maxWidth="lg">
          <Box sx={{ 
            display: 'flex', 
            flexDirection: { xs: 'column', md: 'row' }, 
            justifyContent: 'space-between', 
            alignItems: 'center', 
            gap: 2 
          }}>
            <Typography 
              component="p"
              variant="body2" 
              sx={{ opacity: 0.8, textAlign: { xs: 'center', md: 'left' } }}
            >
              © 2025 TechHub. All rights reserved. | Made with{' '}
              <Box component="span" sx={{ color: '#e25555' }} aria-label="love">❤️</Box>
              {' '}for shopping experiences
            </Typography>
            <Box 
              component="div"
              sx={{ 
                display: 'flex', 
                gap: 2, 
                alignItems: 'center' 
              }}
              aria-label="Payment and location information"
            >
              <PaymentOutlined 
                fontSize="small" 
                sx={{ opacity: 0.7 }}
                aria-label="Accepted payment methods"
              />
              <LocationOn 
                fontSize="small" 
                sx={{ opacity: 0.7 }}
                aria-label="Store locations"
              />
            </Box>
          </Box>
        </Container>
      </Box>
    </Box>
  )
}

export default Footer
