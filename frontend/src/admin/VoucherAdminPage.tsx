import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Box,
  Button,
  Card,
  CardContent,
  Grid,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Alert,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  Add,
  Edit,
  LocalOffer,
  TrendingUp,
  People,
  CheckCircle,
  Cancel,
} from '@mui/icons-material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { vi } from 'date-fns/locale';
import { apiService } from '../services/api';

interface Voucher {
  id: number;
  code: string;
  name: string;
  description: string;
  type: 'PERCENTAGE' | 'FIXED_AMOUNT';
  value: number;
  minOrderAmount: number;
  maxDiscountAmount: number;
  startDate: string;
  endDate: string;
  usageLimit: number;
  usageCount: number;
  usageLimitPerUser: number;
  isActive: boolean;
  isPublic: boolean;
  applicableTo: string;
  createdAt: string;
  updatedAt: string;
}

interface VoucherFormData {
  code: string;
  name: string;
  description: string;
  type: 'PERCENTAGE' | 'FIXED_AMOUNT';
  value: number;
  minOrderAmount: number;
  maxDiscountAmount: number;
  startDate: Date | null;
  endDate: Date | null;
  usageLimit: number;
  usageLimitPerUser: number;
  isActive: boolean;
  isPublic: boolean;
  applicableTo: string;
}

const VoucherAdminPage: React.FC = () => {
  const [vouchers, setVouchers] = useState<Voucher[]>([]);
  const [loading, setLoading] = useState(false);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingVoucher, setEditingVoucher] = useState<Voucher | null>(null);
  const [formData, setFormData] = useState<VoucherFormData>({
    code: '',
    name: '',
    description: '',
    type: 'PERCENTAGE',
    value: 0,
    minOrderAmount: 0,
    maxDiscountAmount: 0,
    startDate: null,
    endDate: null,
    usageLimit: 0,
    usageLimitPerUser: 1,
    isActive: true,
    isPublic: true,
    applicableTo: 'ALL',
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    loadVouchers();
  }, []);

  const loadVouchers = async () => {
    try {
      setLoading(true);
      setError('');
      const response = await apiService.get('/api/vouchers/admin/all');
      if (response.success && response.data) {
        // Backend đang trả về các field boolean là `active` và `public`
        // trong khi frontend dùng `isActive` và `isPublic`.
        // Chuẩn hóa lại để UI hiển thị đúng trạng thái.
        const rawVouchers = response.data as any[];
        const normalizedVouchers: Voucher[] = rawVouchers.map((v) => ({
          ...v,
          isActive: v.isActive ?? v.active ?? false,
          isPublic: v.isPublic ?? v.public ?? false,
        }));

        setVouchers(normalizedVouchers);
      } else {
        setError(response.message || 'Không thể tải danh sách voucher');
      }
    } catch (error: any) {
      console.error('Error loading vouchers:', error);
      setError(error.response?.data?.message || error.message || 'Không thể tải danh sách voucher');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (voucher?: Voucher) => {
    if (voucher) {
      setEditingVoucher(voucher);
      setFormData({
        code: voucher.code,
        name: voucher.name,
        description: voucher.description || '',
        type: voucher.type,
        value: voucher.value,
        minOrderAmount: voucher.minOrderAmount || 0,
        maxDiscountAmount: voucher.maxDiscountAmount || 0,
        startDate: voucher.startDate ? new Date(voucher.startDate) : null,
        endDate: voucher.endDate ? new Date(voucher.endDate) : null,
        usageLimit: voucher.usageLimit || 0,
        usageLimitPerUser: voucher.usageLimitPerUser || 1,
        isActive: voucher.isActive,
        isPublic: voucher.isPublic,
        applicableTo: voucher.applicableTo || 'ALL',
      });
    } else {
      setEditingVoucher(null);
      setFormData({
        code: '',
        name: '',
        description: '',
        type: 'PERCENTAGE',
        value: 0,
        minOrderAmount: 0,
        maxDiscountAmount: 0,
        startDate: null,
        endDate: null,
        usageLimit: 0,
        usageLimitPerUser: 1,
        isActive: true,
        isPublic: true,
        applicableTo: 'ALL',
      });
    }
    setOpenDialog(true);
    setError('');
    setSuccess('');
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingVoucher(null);
  };

  const handleSubmit = async () => {
    // Validation
    if (!formData.code || !formData.code.trim()) {
      setError('Vui lòng nhập mã voucher');
      return;
    }
    if (!formData.name || !formData.name.trim()) {
      setError('Vui lòng nhập tên voucher');
      return;
    }
    if (formData.value <= 0) {
      setError('Giá trị voucher phải lớn hơn 0');
      return;
    }
    if (formData.type === 'PERCENTAGE' && formData.value > 100) {
      setError('Giá trị phần trăm không được vượt quá 100%');
      return;
    }
    if (formData.startDate && formData.endDate && formData.startDate > formData.endDate) {
      setError('Ngày kết thúc phải sau ngày bắt đầu');
      return;
    }

    try {
      setLoading(true);
      setError('');
      setSuccess('');

      // Prepare voucher data with proper format
      const voucherData: any = {
        code: formData.code.trim().toUpperCase(),
        name: formData.name.trim(),
        description: formData.description || '',
        type: formData.type,
        value: formData.value,
        minOrderAmount: formData.minOrderAmount || 0,
        maxDiscountAmount: formData.maxDiscountAmount || 0,
        usageLimit: formData.usageLimit || 0,
        usageLimitPerUser: formData.usageLimitPerUser || 1,
        isActive: formData.isActive,
        isPublic: formData.isPublic,
        applicableTo: formData.applicableTo || 'ALL',
        freeShipping: false, // Default to false
      };

      // Add dates if provided
      if (formData.startDate) {
        voucherData.startDate = formData.startDate.toISOString();
      }
      if (formData.endDate) {
        voucherData.endDate = formData.endDate.toISOString();
      }

      let response;
      if (editingVoucher) {
        response = await apiService.put(`/api/vouchers/${editingVoucher.id}`, voucherData);
      } else {
        response = await apiService.post('/api/vouchers', voucherData);
      }

      if (response.success) {
        setSuccess(editingVoucher ? 'Cập nhật voucher thành công!' : 'Tạo voucher thành công!');
        setTimeout(() => {
          handleCloseDialog();
          loadVouchers();
        }, 1000);
      } else {
        const errorMsg = response.message || 'Có lỗi xảy ra';
        setError(errorMsg);
      }
    } catch (error: any) {
      console.error('Error submitting voucher:', error);
      const errorMsg = error.response?.data?.message || error.message || 'Có lỗi xảy ra khi tạo voucher';
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleStatus = async (voucher: Voucher) => {
    try {
      const response = await apiService.put(`/api/vouchers/${voucher.id}`, {
        ...voucher,
        isActive: !voucher.isActive,
      });

      if (response.success) {
        setSuccess(`Voucher ${voucher.isActive ? 'đã bị vô hiệu hóa' : 'đã được kích hoạt'}!`);
        loadVouchers();
      }
    } catch (error: any) {
      setError('Không thể thay đổi trạng thái voucher');
    }
  };

  const getVoucherTypeLabel = (type: string) => {
    return type === 'PERCENTAGE' ? 'Phần trăm' : 'Số tiền cố định';
  };

  const getVoucherTypeColor = (type: string) => {
    return type === 'PERCENTAGE' ? 'primary' : 'secondary';
  };

  const formatCurrency = (amount: number) => {
    return amount.toLocaleString('vi-VN') + '₫';
  };

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns} adapterLocale={vi}>
      <Container maxWidth="lg" sx={{ py: 4 }}>
        {/* Header */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
          <Box>
            <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 1 }}>
              Quản lý Voucher
            </Typography>
            <Typography variant="body1" color="text.secondary">
              Tạo và quản lý mã giảm giá cho hệ thống
            </Typography>
          </Box>
          <Button
            variant="contained"
            startIcon={<Add />}
            onClick={() => handleOpenDialog()}
            sx={{ borderRadius: 2 }}
          >
            Tạo Voucher
          </Button>
        </Box>

        {/* Alerts */}
        {error && (
          <Alert severity="error" sx={{ mb: 3, borderRadius: 2 }}>
            {error}
          </Alert>
        )}

        {success && (
          <Alert severity="success" sx={{ mb: 3, borderRadius: 2 }}>
            {success}
          </Alert>
        )}

        {/* Stats Cards */}
        <Grid container spacing={0} sx={{ mb: 4, ml: 0 }}>
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ borderRadius: 3 }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <LocalOffer color="primary" />
                  <Box>
                    <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                      {vouchers.length}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Tổng voucher
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ borderRadius: 3 }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <CheckCircle color="success" />
                  <Box>
                    <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                      {vouchers.filter(v => v.isActive).length}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Đang hoạt động
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ borderRadius: 3 }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <People color="info" />
                  <Box>
                    <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                      {vouchers.reduce((sum, v) => sum + v.usageCount, 0)}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Lượt sử dụng
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ borderRadius: 3 }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <TrendingUp color="warning" />
                  <Box>
                    <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                      {vouchers.filter(v => v.isPublic).length}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Công khai
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        {/* Vouchers Table */}
        <Card sx={{ borderRadius: 3 }}>
          <CardContent sx={{ p: 0 }}>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow sx={{ bgcolor: 'grey.50' }}>
                    <TableCell sx={{ fontWeight: 'bold' }}>Mã Voucher</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>Loại</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>Giá trị</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>ĐK tối thiểu</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>Giới hạn</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>Đã dùng</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>Trạng thái</TableCell>
                    <TableCell sx={{ fontWeight: 'bold' }}>Thao tác</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {vouchers.map((voucher) => (
                    <TableRow key={voucher.id} hover>
                      <TableCell>
                        <Box>
                          <Typography variant="subtitle2" sx={{ fontWeight: 'bold' }}>
                            {voucher.code}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            {voucher.name}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={getVoucherTypeLabel(voucher.type)}
                          color={getVoucherTypeColor(voucher.type)}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {voucher.type === 'PERCENTAGE'
                            ? `${voucher.value}%`
                            : formatCurrency(voucher.value)
                          }
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {voucher.minOrderAmount > 0
                            ? formatCurrency(voucher.minOrderAmount)
                            : 'Không có'
                          }
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {voucher.usageLimit > 0
                            ? `${voucher.usageLimit}`
                            : 'Không giới hạn'
                          }
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {voucher.usageCount}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                          <Chip
                            label={voucher.isActive ? 'Hoạt động' : 'Vô hiệu'}
                            color={voucher.isActive ? 'success' : 'error'}
                            size="small"
                          />
                          <Chip
                            label={voucher.isPublic ? 'Công khai' : 'Riêng tư'}
                            color={voucher.isPublic ? 'primary' : 'default'}
                            size="small"
                            variant="outlined"
                          />
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', gap: 1 }}>
                          <Tooltip title="Chỉnh sửa">
                            <IconButton
                              size="small"
                              onClick={() => handleOpenDialog(voucher)}
                            >
                              <Edit />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title={voucher.isActive ? 'Vô hiệu hóa' : 'Kích hoạt'}>
                            <IconButton
                              size="small"
                              onClick={() => handleToggleStatus(voucher)}
                            >
                              {voucher.isActive ? <Cancel /> : <CheckCircle />}
                            </IconButton>
                          </Tooltip>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>

        {/* Create/Edit Dialog */}
        <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
          <DialogTitle>
            <Typography variant="h5" sx={{ fontWeight: 'bold' }}>
              {editingVoucher ? 'Chỉnh sửa Voucher' : 'Tạo Voucher Mới'}
            </Typography>
          </DialogTitle>
          <DialogContent>
            <Grid container spacing={3} sx={{ mt: 1 }}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Mã Voucher"
                  value={formData.code}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
                  required
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControl fullWidth>
                  <InputLabel>Loại Voucher</InputLabel>
                  <Select
                    value={formData.type}
                    label="Loại Voucher"
                    onChange={(e) => setFormData({ ...formData, type: e.target.value as 'PERCENTAGE' | 'FIXED_AMOUNT' })}
                  >
                    <MenuItem value="PERCENTAGE">Phần trăm (%)</MenuItem>
                    <MenuItem value="FIXED_AMOUNT">Số tiền cố định</MenuItem>
                  </Select>
                </FormControl>
              </Grid>

              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Tên Voucher"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                />
              </Grid>

              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Mô tả"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  multiline
                  rows={2}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  type="number"
                  label={`Giá trị ${formData.type === 'PERCENTAGE' ? '(%)' : '(₫)'}`}
                  value={formData.value}
                  onChange={(e) => setFormData({ ...formData, value: Number(e.target.value) })}
                  required
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  type="number"
                  label="Giá trị đơn hàng tối thiểu (₫)"
                  value={formData.minOrderAmount}
                  onChange={(e) => setFormData({ ...formData, minOrderAmount: Number(e.target.value) })}
                  helperText="Đơn hàng tối thiểu để áp dụng voucher"
                />
              </Grid>

              {formData.type === 'PERCENTAGE' && (
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Giảm tối đa (₫)"
                    value={formData.maxDiscountAmount}
                    onChange={(e) => setFormData({ ...formData, maxDiscountAmount: Number(e.target.value) })}
                    helperText="Số tiền giảm tối đa (chỉ áp dụng cho voucher phần trăm)"
                  />
                </Grid>
              )}

              <Grid item xs={12} sm={6}>
                <DatePicker
                  label="Ngày bắt đầu"
                  value={formData.startDate}
                  onChange={(date: Date | null) => setFormData({ ...formData, startDate: date })}
                  slotProps={{ textField: { fullWidth: true } }}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <DatePicker
                  label="Ngày kết thúc"
                  value={formData.endDate}
                  onChange={(date: Date | null) => setFormData({ ...formData, endDate: date })}
                  slotProps={{ textField: { fullWidth: true } }}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  type="number"
                  label="Giới hạn sử dụng"
                  value={formData.usageLimit}
                  onChange={(e) => setFormData({ ...formData, usageLimit: Number(e.target.value) })}
                  helperText="0 = không giới hạn"
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  type="number"
                  label="Giới hạn mỗi người dùng"
                  value={formData.usageLimitPerUser}
                  onChange={(e) => setFormData({ ...formData, usageLimitPerUser: Number(e.target.value) })}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={formData.isActive}
                      onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                    />
                  }
                  label="Kích hoạt"
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={formData.isPublic}
                      onChange={(e) => setFormData({ ...formData, isPublic: e.target.checked })}
                    />
                  }
                  label="Công khai"
                />
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleCloseDialog}>Hủy</Button>
            <Button
              variant="contained"
              onClick={handleSubmit}
              disabled={loading}
            >
              {loading ? 'Đang lưu...' : (editingVoucher ? 'Cập nhật' : 'Tạo')}
            </Button>
          </DialogActions>
        </Dialog>
      </Container>
    </LocalizationProvider>
  );
};

export default VoucherAdminPage;
