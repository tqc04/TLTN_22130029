import React, { useEffect, useState, useCallback } from 'react'
import { 
  Box, 
  Button, 
  Card, 
  CardContent, 
  Dialog, 
  DialogActions, 
  DialogContent, 
  DialogTitle, 
  MenuItem, 
  Stack, 
  TextField, 
  Typography,
  Chip,
  IconButton,
  Grid,
  Paper,
  Alert,
  Snackbar,
  FormControl,
  InputLabel,
  Select,
  Rating,
  Avatar,
  Divider
} from '@mui/material'
import { 
  Reviews, 
  CheckCircle, 
  Cancel, 
  Visibility, 
  Delete,
  Star,
  Person,
  ShoppingBag,
  TrendingUp,
  Warning,
  ThumbUp,
  ThumbDown
} from '@mui/icons-material'
import { DataGrid, GridColDef } from '@mui/x-data-grid'
import { apiService } from '../services/api'

interface Review {
  id: number
  productId: string
  userId: string
  userName?: string
  userAvatar?: string
  rating: number
  title: string
  content: string
  isApproved: boolean
  isSpam: boolean
  verifiedPurchase: boolean
  helpfulVotes: number
  totalVotes: number
  createdAt: string
  updatedAt?: string
  sentimentLabel?: string
  sentimentScore?: number
  spamScore?: number
}

const ReviewsAdminPage: React.FC = () => {
  const [rows, setRows] = useState<Review[]>([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [rowCount, setRowCount] = useState(0)
  const [loading, setLoading] = useState(false)
  
  // Filters
  const [statusFilter, setStatusFilter] = useState<string>('all') // all, approved, pending
  const [productIdFilter, setProductIdFilter] = useState<string>('')
  const [ratingFilter, setRatingFilter] = useState<string>('')
  
  // Stats
  const [stats, setStats] = useState<any>(null)
  
  // Detail dialog
  const [selectedReview, setSelectedReview] = useState<Review | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'warning' | 'info'
  })

  const loadReviews = useCallback(async () => {
    setLoading(true)
    try {
      const isApproved = statusFilter === 'approved' ? true : statusFilter === 'pending' ? false : undefined
      const rating = ratingFilter ? parseInt(ratingFilter) : undefined
      
      const response = await apiService.adminGetReviews({
        page,
        size: pageSize,
        productId: productIdFilter || undefined,
        isApproved,
        rating
      })
      
      if (response.success && response.data) {
        const reviews = (response.data as any).content || []
        const transformedReviews: Review[] = reviews.map((r: any) => ({
          id: r.id,
          productId: r.productId,
          userId: r.userId,
          userName: r.userName || r.user?.firstName + ' ' + r.user?.lastName || 'User',
          userAvatar: r.userAvatar || r.user?.avatarUrl,
          rating: r.rating || 0,
          title: r.title || '',
          content: r.content || '',
          isApproved: r.isApproved || false,
          isSpam: r.isSpam || false,
          verifiedPurchase: r.verifiedPurchase || r.verifiedPurchase || false,
          helpfulVotes: r.helpfulVotes || 0,
          totalVotes: r.totalVotes || 0,
          createdAt: r.createdAt,
          updatedAt: r.updatedAt,
          sentimentLabel: r.sentimentLabel,
          sentimentScore: r.sentimentScore,
          spamScore: r.spamScore
        }))
        setRows(transformedReviews)
        setRowCount((response.data as any).totalElements || 0)
      }
    } catch (error: any) {
      console.error('Error loading reviews:', error)
      showSnackbar('Lỗi khi tải danh sách đánh giá', 'error')
    } finally {
      setLoading(false)
    }
  }, [page, pageSize, statusFilter, productIdFilter, ratingFilter])

  const loadStats = useCallback(async () => {
    try {
      const response = await apiService.adminGetReviewStats()
      if (response.success && response.data) {
        setStats(response.data)
      }
    } catch (error) {
      console.error('Error loading stats:', error)
    }
  }, [])

  useEffect(() => {
    loadReviews()
    loadStats()
  }, [loadReviews, loadStats])

  const showSnackbar = (message: string, severity: 'success' | 'error' | 'warning' | 'info') => {
    setSnackbar({ open: true, message, severity })
  }

  const handleApprove = async (reviewId: number) => {
    try {
      const response = await apiService.approveReview(reviewId)
      if (response.success) {
        showSnackbar('Đã duyệt đánh giá thành công', 'success')
        loadReviews()
        loadStats()
      } else {
        showSnackbar('Lỗi khi duyệt đánh giá', 'error')
      }
    } catch (error) {
      showSnackbar('Lỗi khi duyệt đánh giá', 'error')
    }
  }

  const handleReject = async (reviewId: number) => {
    if (!window.confirm('Bạn có chắc chắn muốn từ chối đánh giá này?')) {
      return
    }
    try {
      const response = await apiService.rejectReview(reviewId)
      if (response.success) {
        showSnackbar('Đã từ chối đánh giá thành công', 'success')
        loadReviews()
        loadStats()
      } else {
        showSnackbar('Lỗi khi từ chối đánh giá', 'error')
      }
    } catch (error) {
      showSnackbar('Lỗi khi từ chối đánh giá', 'error')
    }
  }

  const handleViewDetail = (review: Review) => {
    setSelectedReview(review)
    setDetailOpen(true)
  }

  const columns: GridColDef[] = [
    {
      field: 'id',
      headerName: 'ID',
      width: 80,
    },
    {
      field: 'user',
      headerName: 'Người dùng',
      width: 200,
      renderCell: (params) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Avatar src={params.row.userAvatar} sx={{ width: 32, height: 32 }}>
            {params.row.userName?.charAt(0) || 'U'}
          </Avatar>
          <Typography variant="body2">{params.row.userName || 'User'}</Typography>
        </Box>
      ),
    },
    {
      field: 'productId',
      headerName: 'Mã sản phẩm',
      width: 150,
    },
    {
      field: 'rating',
      headerName: 'Đánh giá',
      width: 150,
      renderCell: (params) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Rating value={params.row.rating} readOnly size="small" />
          <Typography variant="body2">{params.row.rating}/5</Typography>
        </Box>
      ),
    },
    {
      field: 'title',
      headerName: 'Tiêu đề',
      width: 200,
    },
    {
      field: 'content',
      headerName: 'Nội dung',
      width: 300,
      renderCell: (params) => (
        <Typography variant="body2" sx={{ 
          overflow: 'hidden', 
          textOverflow: 'ellipsis', 
          whiteSpace: 'nowrap',
          maxWidth: 300
        }}>
          {params.row.content}
        </Typography>
      ),
    },
    {
      field: 'status',
      headerName: 'Trạng thái',
      width: 150,
      renderCell: (params) => (
        <Box sx={{ display: 'flex', gap: 1 }}>
          {params.row.isApproved ? (
            <Chip label="Đã duyệt" color="success" size="small" icon={<CheckCircle />} />
          ) : (
            <Chip label="Chờ duyệt" color="warning" size="small" icon={<Warning />} />
          )}
          {params.row.isSpam && (
            <Chip label="Spam" color="error" size="small" />
          )}
        </Box>
      ),
    },
    {
      field: 'verifiedPurchase',
      headerName: 'Xác minh',
      width: 120,
      renderCell: (params) => (
        params.row.verifiedPurchase ? (
          <Chip label="Đã mua" color="info" size="small" />
        ) : (
          <Chip label="Chưa mua" size="small" variant="outlined" />
        )
      ),
    },
    {
      field: 'helpfulVotes',
      headerName: 'Hữu ích',
      width: 100,
      renderCell: (params) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
          <ThumbUp fontSize="small" />
          <Typography variant="body2">{params.row.helpfulVotes}/{params.row.totalVotes}</Typography>
        </Box>
      ),
    },
    {
      field: 'createdAt',
      headerName: 'Ngày tạo',
      width: 150,
      renderCell: (params) => (
        <Typography variant="body2">
          {new Date(params.row.createdAt).toLocaleDateString('vi-VN')}
        </Typography>
      ),
    },
    {
      field: 'actions',
      headerName: 'Thao tác',
      width: 200,
      sortable: false,
      renderCell: (params) => (
        <Box sx={{ display: 'flex', gap: 1 }}>
          <IconButton 
            size="small" 
            color="primary" 
            onClick={() => handleViewDetail(params.row)}
          >
            <Visibility />
          </IconButton>
          {!params.row.isApproved && (
            <IconButton 
              size="small" 
              color="success" 
              onClick={() => handleApprove(params.row.id)}
            >
              <CheckCircle />
            </IconButton>
          )}
          {params.row.isApproved && (
            <IconButton 
              size="small" 
              color="error" 
              onClick={() => handleReject(params.row.id)}
            >
              <Cancel />
            </IconButton>
          )}
        </Box>
      ),
    },
  ]

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 3, fontWeight: 700, color: '#1e293b' }}>
        <Reviews sx={{ mr: 2, verticalAlign: 'middle', fontSize: 32 }} />
        Quản lý Đánh giá Sản phẩm
      </Typography>

      {/* Stats Cards */}
      {stats && (
        <Grid container spacing={0} sx={{ mb: 3, ml: 0 }}>
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', color: 'white' }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Box>
                    <Typography variant="h4" sx={{ fontWeight: 700 }}>
                      {stats.totalReviews || 0}
                    </Typography>
                    <Typography variant="body2" sx={{ opacity: 0.9 }}>
                      Tổng đánh giá
                    </Typography>
                  </Box>
                  <Reviews sx={{ fontSize: 48, opacity: 0.3 }} />
                </Box>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)', color: 'white' }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Box>
                    <Typography variant="h4" sx={{ fontWeight: 700 }}>
                      {stats.approvedReviews || 0}
                    </Typography>
                    <Typography variant="body2" sx={{ opacity: 0.9 }}>
                      Đã duyệt
                    </Typography>
                  </Box>
                  <CheckCircle sx={{ fontSize: 48, opacity: 0.3 }} />
                </Box>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ background: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)', color: 'white' }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Box>
                    <Typography variant="h4" sx={{ fontWeight: 700 }}>
                      {stats.pendingReviews || 0}
                    </Typography>
                    <Typography variant="body2" sx={{ opacity: 0.9 }}>
                      Chờ duyệt
                    </Typography>
                  </Box>
                  <Warning sx={{ fontSize: 48, opacity: 0.3 }} />
                </Box>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid item xs={12} sm={6} md={3}>
            <Card sx={{ background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)', color: 'white' }}>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Box>
                    <Typography variant="h4" sx={{ fontWeight: 700 }}>
                      {stats.averageRating ? stats.averageRating.toFixed(1) : '0.0'}
                    </Typography>
                    <Typography variant="body2" sx={{ opacity: 0.9 }}>
                      Điểm trung bình
                    </Typography>
                  </Box>
                  <Star sx={{ fontSize: 48, opacity: 0.3 }} />
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* Filters */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Stack direction="row" spacing={2} alignItems="center">
          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel>Trạng thái</InputLabel>
            <Select
              value={statusFilter}
              label="Trạng thái"
              onChange={(e) => {
                setStatusFilter(e.target.value)
                setPage(0)
              }}
            >
              <MenuItem value="all">Tất cả</MenuItem>
              <MenuItem value="approved">Đã duyệt</MenuItem>
              <MenuItem value="pending">Chờ duyệt</MenuItem>
            </Select>
          </FormControl>
          
          <TextField
            size="small"
            label="Mã sản phẩm"
            value={productIdFilter}
            onChange={(e) => {
              setProductIdFilter(e.target.value)
              setPage(0)
            }}
            sx={{ minWidth: 200 }}
          />
          
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>Đánh giá</InputLabel>
            <Select
              value={ratingFilter}
              label="Đánh giá"
              onChange={(e) => {
                setRatingFilter(e.target.value)
                setPage(0)
              }}
            >
              <MenuItem value="">Tất cả</MenuItem>
              <MenuItem value="5">5 sao</MenuItem>
              <MenuItem value="4">4 sao</MenuItem>
              <MenuItem value="3">3 sao</MenuItem>
              <MenuItem value="2">2 sao</MenuItem>
              <MenuItem value="1">1 sao</MenuItem>
            </Select>
          </FormControl>
          
          <Button 
            variant="outlined" 
            onClick={() => {
              setStatusFilter('all')
              setProductIdFilter('')
              setRatingFilter('')
              setPage(0)
            }}
          >
            Xóa bộ lọc
          </Button>
        </Stack>
      </Paper>

      {/* Data Grid */}
      <Paper sx={{ height: 600, width: '100%' }}>
        <DataGrid
          rows={rows}
          columns={columns}
          page={page}
          pageSize={pageSize}
          onPageChange={setPage}
          onPageSizeChange={setPageSize}
          rowCount={rowCount}
          loading={loading}
          paginationMode="server"
          disableSelectionOnClick
          sx={{
            '& .MuiDataGrid-cell': {
              borderBottom: '1px solid #e2e8f0',
            },
          }}
        />
      </Paper>

      {/* Detail Dialog */}
      <Dialog open={detailOpen} onClose={() => setDetailOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Typography variant="h6">Chi tiết Đánh giá</Typography>
            <IconButton onClick={() => setDetailOpen(false)}>
              <Cancel />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          {selectedReview && (
            <Box>
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                    <Avatar src={selectedReview.userAvatar} sx={{ width: 56, height: 56 }}>
                      {selectedReview.userName?.charAt(0) || 'U'}
                    </Avatar>
                    <Box>
                      <Typography variant="h6">{selectedReview.userName || 'User'}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        Mã sản phẩm: {selectedReview.productId}
                      </Typography>
                    </Box>
                  </Box>
                </Grid>
                
                <Grid item xs={12}>
                  <Divider />
                </Grid>
                
                <Grid item xs={12}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <Rating value={selectedReview.rating} readOnly />
                    <Typography variant="body1" sx={{ fontWeight: 600 }}>
                      {selectedReview.rating}/5
                    </Typography>
                  </Box>
                </Grid>
                
                <Grid item xs={12}>
                  <Typography variant="h6" sx={{ mb: 1 }}>
                    {selectedReview.title}
                  </Typography>
                </Grid>
                
                <Grid item xs={12}>
                  <Typography variant="body1" sx={{ mb: 2, whiteSpace: 'pre-wrap' }}>
                    {selectedReview.content}
                  </Typography>
                </Grid>
                
                <Grid item xs={12}>
                  <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                    {selectedReview.isApproved ? (
                      <Chip label="Đã duyệt" color="success" icon={<CheckCircle />} />
                    ) : (
                      <Chip label="Chờ duyệt" color="warning" icon={<Warning />} />
                    )}
                    {selectedReview.verifiedPurchase && (
                      <Chip label="Đã mua hàng" color="info" />
                    )}
                    {selectedReview.isSpam && (
                      <Chip label="Spam" color="error" />
                    )}
                  </Box>
                </Grid>
                
                {selectedReview.sentimentLabel && (
                  <Grid item xs={12}>
                    <Typography variant="body2" color="text.secondary">
                      Phân tích cảm xúc: {selectedReview.sentimentLabel} 
                      {selectedReview.sentimentScore && ` (${(selectedReview.sentimentScore * 100).toFixed(1)}%)`}
                    </Typography>
                  </Grid>
                )}
                
                <Grid item xs={12}>
                  <Typography variant="body2" color="text.secondary">
                    Ngày tạo: {new Date(selectedReview.createdAt).toLocaleString('vi-VN')}
                  </Typography>
                  {selectedReview.updatedAt && (
                    <Typography variant="body2" color="text.secondary">
                      Cập nhật: {new Date(selectedReview.updatedAt).toLocaleString('vi-VN')}
                    </Typography>
                  )}
                </Grid>
              </Grid>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          {selectedReview && !selectedReview.isApproved && (
            <Button 
              variant="contained" 
              color="success" 
              startIcon={<CheckCircle />}
              onClick={() => {
                handleApprove(selectedReview.id)
                setDetailOpen(false)
              }}
            >
              Duyệt đánh giá
            </Button>
          )}
          {selectedReview && selectedReview.isApproved && (
            <Button 
              variant="contained" 
              color="error" 
              startIcon={<Cancel />}
              onClick={() => {
                handleReject(selectedReview.id)
                setDetailOpen(false)
              }}
            >
              Từ chối đánh giá
            </Button>
          )}
          <Button onClick={() => setDetailOpen(false)}>Đóng</Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  )
}

export default ReviewsAdminPage

