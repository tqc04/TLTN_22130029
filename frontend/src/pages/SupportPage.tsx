import React, { useState } from 'react';
import { apiService } from '../services/api';
import {
  Container,
  Grid,
  Card,
  CardContent,
  Typography,
  Button,
  Box,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Alert,
  Chip,
  Avatar,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Paper,
  Snackbar,
  Tabs,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  LinearProgress,
} from '@mui/material';
import { ExpandMore, Send, ContactSupport, Schedule, Security, Payment, LocalShipping, Refresh, ShoppingCart, Email, Phone, Chat, LocationOn, Help, Build, CheckCircle, Cancel, Pending, Add, Visibility } from '@mui/icons-material';

const SupportPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0);

  // Contact form state
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    subject: '',
    category: '',
    message: '',
  });

  // Warranty form state
  const [warrantyFormData, setWarrantyFormData] = useState({
    customerName: '',
    customerEmail: '',
    customerPhone: '',
    orderNumber: '',
    productName: '',
    productSku: '',
    issueDescription: '',
    priority: 'NORMAL',
  });

  const [loading, setLoading] = useState(false);
  const [warrantyLoading, setWarrantyLoading] = useState(false);
  const [warrantyRequests, setWarrantyRequests] = useState<any[]>([]);
  const [warrantyRequestsLoading, setWarrantyRequestsLoading] = useState(false);

  type Severity = 'success' | 'error' | 'warning' | 'info'
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: Severity }>({ open: false, message: '', severity: 'success' });

  const categories = [
    { value: 'general', label: 'Câu hỏi Tổng quát' },
    { value: 'technical', label: 'Hỗ trợ Kỹ thuật' },
    { value: 'billing', label: 'Thanh toán & Hóa đơn' },
    { value: 'shipping', label: 'Vận chuyển & Giao hàng' },
    { value: 'returns', label: 'Đổi trả & Hoàn tiền' },
    { value: 'warranty', label: 'Bảo hành Sản phẩm' },
    { value: 'product', label: 'Thông tin Sản phẩm' },
    { value: 'account', label: 'Vấn đề Tài khoản' },
    { value: 'other', label: 'Khác' },
  ];

  const warrantyPriorities = [
    { value: 'LOW', label: 'Thấp' },
    { value: 'NORMAL', label: 'Bình thường' },
    { value: 'HIGH', label: 'Cao' },
    { value: 'URGENT', label: 'Khẩn cấp' },
  ];

  const warrantyStatuses = {
    'PENDING': { label: 'Đang chờ', color: 'warning', icon: <Pending /> },
    'APPROVED': { label: 'Đã duyệt', color: 'info', icon: <CheckCircle /> },
    'RECEIVED': { label: 'Đã nhận', color: 'primary', icon: <CheckCircle /> },
    'IN_PROGRESS': { label: 'Đang xử lý', color: 'secondary', icon: <Build /> },
    'COMPLETED': { label: 'Hoàn thành', color: 'success', icon: <CheckCircle /> },
    'REJECTED': { label: 'Từ chối', color: 'error', icon: <Cancel /> },
    'CANCELLED': { label: 'Đã hủy', color: 'default', icon: <Cancel /> },
  };

  const faqs = [
    {
      question: 'Làm thế nào để theo dõi đơn hàng?',
      answer: 'Bạn có thể theo dõi đơn hàng bằng cách đăng nhập vào tài khoản và truy cập phần "Đơn hàng của tôi". Bạn sẽ nhận được thông tin theo dõi qua email khi đơn hàng được giao.',
      category: 'shipping',
    },
    {
      question: 'Chính sách đổi trả của bạn là gì?',
      answer: 'Chúng tôi cung cấp chính sách đổi trả trong 30 ngày cho hầu hết các sản phẩm. Sản phẩm phải trong tình trạng nguyên vẹn với đầy đủ bao bì. Một số sản phẩm có thể có chính sách đổi trả khác.',
      category: 'returns',
    },
    {
      question: 'Thời gian giao hàng mất bao lâu?',
      answer: 'Giao hàng tiêu chuẩn mất 3-5 ngày làm việc. Giao hàng nhanh (1-2 ngày làm việc) có sẵn với phí bổ sung. Thời gian giao hàng quốc tế thay đổi theo địa điểm.',
      category: 'shipping',
    },
    {
      question: 'Bạn có giao hàng quốc tế không?',
      answer: 'Có, chúng tôi giao hàng đến hầu hết các quốc gia trên thế giới. Chi phí và thời gian giao hàng thay đổi theo địa điểm. Bạn có thể kiểm tra tính khả dụng trong quá trình thanh toán.',
      category: 'shipping',
    },
    {
      question: 'Bạn chấp nhận phương thức thanh toán nào?',
      answer: 'Chúng tôi chấp nhận tất cả các loại thẻ tín dụng chính (Visa, MasterCard, American Express), PayPal, Apple Pay, Google Pay và chuyển khoản ngân hàng cho tài khoản doanh nghiệp.',
      category: 'billing',
    },
    {
      question: 'Làm thế nào để đặt lại mật khẩu?',
      answer: 'Nhấp vào "Quên mật khẩu" trên trang đăng nhập. Nhập địa chỉ email của bạn và chúng tôi sẽ gửi cho bạn một liên kết để đặt lại mật khẩu.',
      category: 'account',
    },
    {
      question: 'Thông tin thanh toán của tôi có an toàn không?',
      answer: 'Có, chúng tôi sử dụng mã hóa SSL theo tiêu chuẩn ngành để bảo vệ thông tin thanh toán của bạn. Chúng tôi không bao giờ lưu trữ chi tiết thẻ tín dụng trên máy chủ của chúng tôi.',
      category: 'security',
    },
    {
      question: 'Tôi có thể hủy đơn hàng không?',
      answer: 'Đơn hàng có thể được hủy trong vòng 1 giờ sau khi đặt nếu chúng chưa được xử lý để giao hàng. Liên hệ ngay với đội ngũ hỗ trợ của chúng tôi.',
      category: 'general',
    },
    {
      question: 'Làm thế nào để yêu cầu bảo hành?',
      answer: 'Truy cập tab "Bảo hành Sản phẩm" trong trang hỗ trợ. Điền đầy đủ thông tin đơn hàng, sản phẩm và mô tả vấn đề. Chúng tôi sẽ xử lý yêu cầu của bạn trong thời gian sớm nhất.',
      category: 'general',
    },
    {
      question: 'Thời gian xử lý bảo hành là bao lâu?',
      answer: 'Thời gian xử lý bảo hành thường từ 3-7 ngày làm việc tùy thuộc vào mức độ phức tạp của vấn đề. Bạn sẽ nhận được thông báo qua email về tiến độ xử lý.',
      category: 'general',
    },
  ];

  const supportChannels = [
    {
      icon: <Email />,
      title: 'Hỗ trợ qua Email',
      description: 'Nhận hỗ trợ qua email',
      contact: 'support@shoppro.com',
      responseTime: 'Trong vòng 24 giờ',
      color: 'primary.main',
    },
    {
      icon: <Phone />,
      title: 'Hỗ trợ qua Điện thoại',
      description: 'Nói chuyện với đội ngũ của chúng tôi',
      contact: '+84 1900 123 456',
      responseTime: 'T2-T6, 9:00-18:00',
      color: 'success.main',
    },
    {
      icon: <Chat />,
      title: 'Chat Trực tuyến',
      description: 'Hỗ trợ tức thời',
      contact: 'Có sẵn 24/7',
      responseTime: 'Phản hồi ngay lập tức',
      color: 'warning.main',
    },
  ];

  const handleInputChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleWarrantyInputChange = (field: string, value: string) => {
    setWarrantyFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
    if (newValue === 1) {
      loadWarrantyRequests();
    }
  };

  // Load warranty requests
  const loadWarrantyRequests = async () => {
    setWarrantyRequestsLoading(true);
    try {
      const response = await apiService.getMyWarrantyRequests();
      if (response.success && response.data) {
        setWarrantyRequests((response.data as any).content || []);
      } else {
        setWarrantyRequests([]);
      }
    } catch (error) {
      console.error('Error loading warranty requests:', error);
      setWarrantyRequests([]);
    } finally {
      setWarrantyRequestsLoading(false);
    }
  };

  const handleWarrantySubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setWarrantyLoading(true);

    try {
      const response = await apiService.createWarrantyRequest(warrantyFormData);
      if (response.success) {
        setSnackbar({
          open: true,
          message: 'Yêu cầu bảo hành đã được tạo thành công! Bạn sẽ nhận được email xác nhận.',
          severity: 'success',
        });
        setWarrantyFormData({
          customerName: '',
          customerEmail: '',
          customerPhone: '',
          orderNumber: '',
          productName: '',
          productSku: '',
          issueDescription: '',
          priority: 'NORMAL',
        });
        loadWarrantyRequests();
      } else {
        throw new Error(response.message || 'Failed to create warranty request');
      }

    } catch (error) {
      setSnackbar({
        open: true,
        message: 'Có lỗi xảy ra khi tạo yêu cầu bảo hành. Vui lòng thử lại.',
        severity: 'error',
      });
      setWarrantyLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await apiService.sendSupportMessage(formData);
      if (response.success) {
      setSnackbar({
        open: true,
          message: 'Tin nhắn của bạn đã được gửi thành công! Chúng tôi sẽ phản hồi sớm nhất có thể.',
        severity: 'success',
      });
      setFormData({
        name: '',
        email: '',
        subject: '',
        category: '',
        message: '',
      });
      } else {
        throw new Error(response.message || 'Failed to send message');
      }
    } catch (error) {
      setSnackbar({
        open: true,
        message: 'Có lỗi xảy ra khi gửi tin nhắn. Vui lòng thử lại.',
        severity: 'error',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSnackbarClose = () => {
    setSnackbar({ ...snackbar, open: false });
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4, pt: '40px' }}>
      {/* Header */}
      <Box sx={{ textAlign: 'center', mb: 6 }}>
        <Typography variant="h4" component="h1" sx={{ fontWeight: 700, mb: 2, color: 'text.primary' }}>
          Trung tâm Hỗ trợ
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 4, maxWidth: 600, mx: 'auto' }}>
          Chúng tôi luôn sẵn sàng hỗ trợ! Tìm câu trả lời cho câu hỏi của bạn hoặc liên hệ với đội ngũ hỗ trợ
        </Typography>
        
        {/* Support Stats */}
        <Box sx={{ 
          display: 'flex', 
          justifyContent: 'center', 
          gap: { xs: 3, md: 6 }, 
          flexWrap: 'wrap',
          mb: 4
        }}>
          <Paper sx={{ 
            p: 3, 
            textAlign: 'center', 
            minWidth: 120,
            borderRadius: 2,
            boxShadow: '0 2px 8px rgba(0,0,0,0.08)'
          }}>
            <Typography variant="h4" sx={{ fontWeight: 700, color: 'primary.main', mb: 1 }}>
              24/7
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Hỗ trợ Có sẵn
            </Typography>
          </Paper>
          <Paper sx={{ 
            p: 3, 
            textAlign: 'center', 
            minWidth: 120,
            borderRadius: 2,
            boxShadow: '0 2px 8px rgba(0,0,0,0.08)'
          }}>
            <Typography variant="h4" sx={{ fontWeight: 700, color: 'success.main', mb: 1 }}>
              &lt; 2h
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Thời gian Phản hồi TB
            </Typography>
          </Paper>
          <Paper sx={{ 
            p: 3, 
            textAlign: 'center', 
            minWidth: 120,
            borderRadius: 2,
            boxShadow: '0 2px 8px rgba(0,0,0,0.08)'
          }}>
            <Typography variant="h4" sx={{ fontWeight: 700, color: 'warning.main', mb: 1 }}>
              98%
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Tỷ lệ Hài lòng
            </Typography>
          </Paper>
        </Box>
      </Box>

      {/* Tabs Section */}
      <Box sx={{ mb: 4 }}>
        <Tabs
          value={activeTab}
          onChange={handleTabChange}
          centered
          sx={{
            '& .MuiTab-root': {
              fontWeight: 600,
              fontSize: '1rem',
              textTransform: 'none',
              minHeight: 64,
            },
            '& .MuiTabs-indicator': {
              height: 3,
              borderRadius: '3px 3px 0 0',
            }
          }}
        >
          <Tab
            icon={<ContactSupport />}
            label="Liên hệ Hỗ trợ"
            sx={{ minHeight: 64, gap: 1 }}
          />
          <Tab
            icon={<Build />}
            label="Bảo hành Sản phẩm"
            sx={{ minHeight: 64, gap: 1 }}
          />
        </Tabs>
      </Box>

      {/* Tab Content */}
      {activeTab === 0 && (
        <Grid container spacing={4}>
        {/* Contact Form */}
        <Grid item xs={12} lg={6}>
          <Card sx={{ borderRadius: 3, height: 'fit-content' }}>
            <CardContent sx={{ p: 4 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                <Avatar sx={{ bgcolor: 'primary.main', mr: 2 }}>
                  <ContactSupport />
                </Avatar>
                <Typography variant="h5" sx={{ fontWeight: 'bold' }}>
                  Liên hệ với Chúng tôi
                </Typography>
              </Box>

              <Box component="form" onSubmit={handleSubmit}>
                <Grid container spacing={3}>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      label="Họ và tên"
                      value={formData.name}
                      onChange={(e) => handleInputChange('name', e.target.value)}
                      required
                      sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      fullWidth
                      label="Địa chỉ Email"
                      type="email"
                      value={formData.email}
                      onChange={(e) => handleInputChange('email', e.target.value)}
                      required
                      sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <TextField
                      fullWidth
                      label="Tiêu đề"
                      value={formData.subject}
                      onChange={(e) => handleInputChange('subject', e.target.value)}
                      required
                      sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <FormControl fullWidth>
                      <InputLabel>Danh mục</InputLabel>
                      <Select
                        value={formData.category}
                        label="Danh mục"
                        onChange={(e) => handleInputChange('category', e.target.value)}
                        sx={{ borderRadius: 2 }}
                      >
                        {categories.map((category) => (
                          <MenuItem key={category.value} value={category.value}>
                            {category.label}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  </Grid>
                  <Grid item xs={12}>
                    <TextField
                      fullWidth
                      label="Tin nhắn"
                      multiline
                      rows={4}
                      value={formData.message}
                      onChange={(e) => handleInputChange('message', e.target.value)}
                      required
                      placeholder="Vui lòng mô tả chi tiết vấn đề hoặc câu hỏi của bạn..."
                      sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <Button
                      type="submit"
                      variant="contained"
                      fullWidth
                      size="large"
                      startIcon={loading ? <Refresh /> : <Send />}
                      disabled={loading}
                      sx={{
                        borderRadius: 2,
                        py: 1.5,
                        fontSize: '1.1rem',
                        fontWeight: 'bold',
                        textTransform: 'none',
                      }}
                    >
                      {loading ? 'Đang gửi...' : 'Gửi Tin nhắn'}
                    </Button>
                  </Grid>
                </Grid>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Support Channels */}
        <Grid item xs={12} lg={6}>
          <Card sx={{ borderRadius: 3, mb: 4 }}>
            <CardContent sx={{ p: 4 }}>
              <Typography variant="h5" sx={{ fontWeight: 'bold', mb: 3 }}>
                Nhận Hỗ trợ Nhanh hơn
              </Typography>

              <Grid container spacing={3}>
                {supportChannels.map((channel, index) => (
                  <Grid item xs={12} key={index}>
                    <Paper
                      sx={{
                        p: 3,
                        borderRadius: 2,
                        border: '1px solid',
                        borderColor: 'divider',
                        '&:hover': {
                          borderColor: channel.color,
                          boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
                        },
                        transition: 'all 0.2s ease',
                      }}
                    >
                      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                        <Avatar sx={{ bgcolor: channel.color, mr: 2 }}>
                          {channel.icon}
                        </Avatar>
                        <Box>
                          <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
                            {channel.title}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            {channel.description}
                          </Typography>
                        </Box>
                      </Box>

                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Typography variant="body1" sx={{ fontWeight: 'bold' }}>
                          {channel.contact}
                        </Typography>
                        <Chip
                          label={channel.responseTime}
                          size="small"
                          color="primary"
                          variant="outlined"
                        />
                      </Box>
                    </Paper>
                  </Grid>
                ))}
              </Grid>
            </CardContent>
          </Card>

          {/* Office Information */}
          <Card sx={{ borderRadius: 3 }}>
            <CardContent sx={{ p: 4 }}>
              <Typography variant="h5" sx={{ fontWeight: 'bold', mb: 3 }}>
                Thông tin Văn phòng
              </Typography>

              <List>
                <ListItem>
                  <ListItemIcon>
                    <LocationOn color="primary" />
                  </ListItemIcon>
                  <ListItemText
                    primary="Trụ sở Chính"
                    secondary="123 Đường Công nghệ, Thành phố Số, TP.HCM, Việt Nam"
                  />
                </ListItem>
                <ListItem>
                  <ListItemIcon>
                    <Schedule color="primary" />
                  </ListItemIcon>
                  <ListItemText
                    primary="Giờ Làm việc"
                    secondary="Thứ Hai - Thứ Sáu: 9:00 - 18:00"
                  />
                </ListItem>
                <ListItem>
                  <ListItemIcon>
                    <Security color="primary" />
                  </ListItemIcon>
                  <ListItemText
                    primary="Bảo mật"
                    secondary="Tất cả thông tin liên lạc đều được mã hóa và bảo mật"
                  />
                </ListItem>
              </List>
            </CardContent>
          </Card>
        </Grid>
        </Grid>
      )}

      {activeTab === 1 && (
        <Grid container spacing={4}>
          {/* Create Warranty Request */}
          <Grid item xs={12} lg={6}>
            <Card sx={{ borderRadius: 3, height: 'fit-content' }}>
              <CardContent sx={{ p: 4 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                  <Avatar sx={{ bgcolor: 'primary.main', mr: 2 }}>
                    <Add />
                  </Avatar>
                  <Typography variant="h5" sx={{ fontWeight: 'bold' }}>
                    Tạo Yêu cầu Bảo hành
                  </Typography>
                </Box>

                <Box component="form" onSubmit={handleWarrantySubmit}>
                  <Grid container spacing={3}>
                    <Grid item xs={12} sm={6}>
                      <TextField
                        fullWidth
                        label="Họ và tên"
                        value={warrantyFormData.customerName}
                        onChange={(e) => handleWarrantyInputChange('customerName', e.target.value)}
                        required
                        sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
                      />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField
                        fullWidth
                        label="Email"
                        type="email"
                        value={warrantyFormData.customerEmail}
                        onChange={(e) => handleWarrantyInputChange('customerEmail', e.target.value)}
                        required
                        sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
                      />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField
                        fullWidth
                        label="Số điện thoại"
                        value={warrantyFormData.customerPhone}
                        onChange={(e) => handleWarrantyInputChange('customerPhone', e.target.value)}
                        sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
                      />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <FormControl fullWidth>
                        <InputLabel>Độ ưu tiên</InputLabel>
                        <Select
                          value={warrantyFormData.priority}
                          label="Độ ưu tiên"
                          onChange={(e) => handleWarrantyInputChange('priority', e.target.value)}
                          sx={{ borderRadius: 2 }}
                        >
                          {warrantyPriorities.map((priority) => (
                            <MenuItem key={priority.value} value={priority.value}>
                              {priority.label}
                            </MenuItem>
                          ))}
                        </Select>
                      </FormControl>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField
                        fullWidth
                        label="Mã đơn hàng"
                        value={warrantyFormData.orderNumber}
                        onChange={(e) => handleWarrantyInputChange('orderNumber', e.target.value)}
                        required
                        sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
                      />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField
                        fullWidth
                        label="Tên sản phẩm"
                        value={warrantyFormData.productName}
                        onChange={(e) => handleWarrantyInputChange('productName', e.target.value)}
                        required
                        sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        fullWidth
                        label="Mã sản phẩm (SKU)"
                        value={warrantyFormData.productSku}
                        onChange={(e) => handleWarrantyInputChange('productSku', e.target.value)}
                        sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        fullWidth
                        label="Mô tả vấn đề"
                        multiline
                        rows={4}
                        value={warrantyFormData.issueDescription}
                        onChange={(e) => handleWarrantyInputChange('issueDescription', e.target.value)}
                        required
                        placeholder="Vui lòng mô tả chi tiết vấn đề bạn đang gặp phải với sản phẩm..."
                        sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <Button
                        type="submit"
                        variant="contained"
                        fullWidth
                        size="large"
                        startIcon={warrantyLoading ? <Refresh /> : <Send />}
                        disabled={warrantyLoading}
                        sx={{
                          borderRadius: 2,
                          py: 1.5,
                          fontSize: '1.1rem',
                          fontWeight: 'bold',
                          textTransform: 'none',
                        }}
                      >
                        {warrantyLoading ? 'Đang gửi...' : 'Gửi Yêu cầu Bảo hành'}
                      </Button>
                    </Grid>
                  </Grid>
                </Box>
              </CardContent>
            </Card>
      </Grid>

          {/* Warranty Requests List */}
          <Grid item xs={12} lg={6}>
            <Card sx={{ borderRadius: 3 }}>
              <CardContent sx={{ p: 4 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
                  <Typography variant="h5" sx={{ fontWeight: 'bold' }}>
                    Yêu cầu Bảo hành của Tôi
                  </Typography>
                  <Button
                    variant="outlined"
                    startIcon={<Refresh />}
                    onClick={loadWarrantyRequests}
                    disabled={warrantyRequestsLoading}
                    sx={{ borderRadius: 2 }}
                  >
                    Làm mới
                  </Button>
                </Box>

                {warrantyRequestsLoading ? (
                  <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                    <LinearProgress sx={{ width: '100%', maxWidth: 200 }} />
                  </Box>
                ) : warrantyRequests.length === 0 ? (
                  <Box sx={{ textAlign: 'center', p: 4 }}>
                    <Build sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
                    <Typography variant="h6" color="text.secondary" sx={{ mb: 1 }}>
                      Chưa có yêu cầu bảo hành nào
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Tạo yêu cầu bảo hành đầu tiên của bạn
                    </Typography>
                  </Box>
                ) : (
                  <TableContainer>
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableCell sx={{ fontWeight: 'bold' }}>Mã yêu cầu</TableCell>
                          <TableCell sx={{ fontWeight: 'bold' }}>Sản phẩm</TableCell>
                          <TableCell sx={{ fontWeight: 'bold' }}>Trạng thái</TableCell>
                          <TableCell sx={{ fontWeight: 'bold' }}>Thời gian</TableCell>
                          <TableCell sx={{ fontWeight: 'bold' }}>Hành động</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {warrantyRequests.map((request) => (
                          <TableRow key={request.id} hover>
                            <TableCell>
                              <Typography variant="body2" sx={{ fontFamily: 'monospace', fontWeight: 600 }}>
                                {request.requestNumber}
                              </Typography>
                            </TableCell>
                            <TableCell>
                              <Box>
                                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                                  {request.productName}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                  {request.issueDescription}
                                </Typography>
                              </Box>
                            </TableCell>
                            <TableCell>
                              <Chip
                                icon={warrantyStatuses[request.status as keyof typeof warrantyStatuses]?.icon}
                                label={warrantyStatuses[request.status as keyof typeof warrantyStatuses]?.label}
                                color={warrantyStatuses[request.status as keyof typeof warrantyStatuses]?.color as any}
                                size="small"
                                variant="outlined"
                              />
                            </TableCell>
                            <TableCell>
                              <Typography variant="body2">
                                {new Date(request.createdAt).toLocaleDateString('vi-VN')}
                              </Typography>
                            </TableCell>
                            <TableCell>
                              <Button
                                size="small"
                                variant="outlined"
                                startIcon={<Visibility />}
                                sx={{ borderRadius: 2, textTransform: 'none' }}
                              >
                                Chi tiết
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* FAQ Section */}
      <Box sx={{ mt: 8 }}>
        <Typography variant="h4" component="h2" sx={{ fontWeight: 'bold', textAlign: 'center', mb: 1 }}>
          Câu hỏi thường gặp
        </Typography>
        <Typography variant="h6" color="text.secondary" sx={{ textAlign: 'center', mb: 4 }}>
          Tìm câu trả lời nhanh cho các câu hỏi phổ biến
        </Typography>

        <Grid container spacing={2}>
          {faqs.map((faq, index) => (
            <Grid item xs={12} md={6} key={index}>
              <Accordion sx={{ borderRadius: 2, mb: 1 }}>
                <AccordionSummary expandIcon={<ExpandMore />}>
                  <Typography sx={{ fontWeight: 'bold' }}>
                    {faq.question}
                  </Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Typography color="text.secondary" sx={{ lineHeight: 1.6 }}>
                    {faq.answer}
                  </Typography>
                </AccordionDetails>
              </Accordion>
            </Grid>
          ))}
        </Grid>
      </Box>

      {/* Help Categories */}
      <Box sx={{ mt: 8 }}>
        <Typography variant="h4" component="h2" sx={{ fontWeight: 'bold', textAlign: 'center', mb: 1 }}>
          Danh mục Hỗ trợ
        </Typography>
        <Typography variant="h6" color="text.secondary" sx={{ textAlign: 'center', mb: 4 }}>
          Duyệt các bài viết hỗ trợ theo danh mục
        </Typography>

        <Grid container spacing={3}>
          {[
            { icon: <ShoppingCart />, title: 'Đơn hàng & Vận chuyển', count: 15, color: 'primary.main' },
            { icon: <Payment />, title: 'Thanh toán & Hóa đơn', count: 12, color: 'success.main' },
            { icon: <Refresh />, title: 'Đổi trả & Hoàn tiền', count: 8, color: 'warning.main' },
            { icon: <Build />, title: 'Bảo hành & Sửa chữa', count: 10, color: 'error.main' },
            { icon: <Security />, title: 'Tài khoản & Bảo mật', count: 10, color: 'info.main' },
            { icon: <LocalShipping />, title: 'Thông tin Vận chuyển', count: 6, color: 'secondary.main' },
            { icon: <Help />, title: 'Hỗ trợ Tổng quát', count: 20, color: 'primary.dark' },
          ].map((category, index) => (
            <Grid item xs={12} sm={6} md={4} key={index}>
              <Card
                sx={{
                  borderRadius: 3,
                  p: 3,
                  textAlign: 'center',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  '&:hover': {
                    transform: 'translateY(-4px)',
                    boxShadow: '0 8px 25px rgba(0,0,0,0.15)',
                  },
                }}
              >
                <Avatar sx={{ bgcolor: category.color, width: 64, height: 64, mx: 'auto', mb: 2 }}>
                  {category.icon}
                </Avatar>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1 }}>
                  {category.title}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {category.count} articles
                </Typography>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Box>

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleSnackbarClose}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Alert onClose={handleSnackbarClose} severity={snackbar.severity}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default SupportPage; 