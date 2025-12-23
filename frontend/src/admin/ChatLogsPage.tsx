import React, { useState, useEffect } from 'react'
import {
  Container,
  Card,
  CardContent,
  Typography,
  Box,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  TextField,
  InputAdornment,
  Pagination,
  Grid,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  CircularProgress,
  Alert,
  Stack,
  Divider,
  Tooltip,
} from '@mui/material'
import {
  Search,
  Visibility,
  Chat,
  Storage,
  SmartToy,
  Cancel,
  CheckCircle,
  Refresh,
  Person,
  ShoppingCart,
} from '@mui/icons-material'
import { apiService } from '../services/api'

interface ChatLog {
  id: number
  userId: string
  sessionId: string
  userMessage: string
  aiResponse: string
  isProductRelated: boolean
  foundProducts: boolean
  usedAI: boolean
  responseSource: string
  productIds?: string
  productNames?: string
  feedback?: string
  rating?: number
  createdAt: string
  updatedAt: string
}

interface ChatLogStatistics {
  totalLogs: number
  productQueriesWithResults: number
  productQueriesWithoutResults: number
  aiQueries: number
  responseSourceBreakdown: Record<string, number>
  averageRating: number
  mostAskedProductQueries: Array<{ query: string; count: number }>
}

const ChatLogsPage: React.FC = () => {
  const [logs, setLogs] = useState<ChatLog[]>([])
  const [statistics, setStatistics] = useState<ChatLogStatistics | null>(null)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [size] = useState(20)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [searchTerm, setSearchTerm] = useState('')
  const [filterUserId, setFilterUserId] = useState('')
  const [filterProductRelated, setFilterProductRelated] = useState<boolean | null>(null)
  const [selectedLog, setSelectedLog] = useState<ChatLog | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [feedbackOpen, setFeedbackOpen] = useState(false)
  const [feedbackText, setFeedbackText] = useState('')
  const [feedbackRating, setFeedbackRating] = useState<number>(0)

  useEffect(() => {
    fetchLogs()
    fetchStatistics()
  }, [page, size, filterUserId, filterProductRelated])

  const fetchLogs = async () => {
    setLoading(true)
    try {
      const result = await apiService.getChatLogs(
        page,
        size,
        filterUserId || undefined,
        filterProductRelated !== null ? filterProductRelated : undefined
      )

      if (result.success && result.data) {
        setLogs(result.data.data || [])
        setTotalPages(result.data.totalPages || 0)
        setTotalElements(result.data.totalElements || 0)
      }
    } catch (error) {
      console.error('Error fetching chat logs:', error)
    } finally {
      setLoading(false)
    }
  }

  const fetchStatistics = async () => {
    try {
      const result = await apiService.getChatLogStatistics()

      if (result.success && result.data) {
        setStatistics(result.data.statistics)
      }
    } catch (error) {
      console.error('Error fetching statistics:', error)
    }
  }

  const handleViewDetail = (log: ChatLog) => {
    setSelectedLog(log)
    setDetailOpen(true)
  }

  const handleSubmitFeedback = async () => {
    if (!selectedLog) return

    try {
      const result = await apiService.submitChatLogFeedback(
        selectedLog.id,
        feedbackText || undefined,
        feedbackRating > 0 ? feedbackRating : undefined
      )

      if (result.success) {
        setFeedbackOpen(false)
        setFeedbackText('')
        setFeedbackRating(0)
        fetchLogs()
        fetchStatistics()
      }
    } catch (error) {
      console.error('Error submitting feedback:', error)
    }
  }

  const getResponseSourceColor = (source: string) => {
    switch (source) {
      case 'DATABASE':
        return 'success'
      case 'AI':
        return 'primary'
      case 'NO_PRODUCTS':
        return 'warning'
      case 'FALLBACK':
        return 'error'
      default:
        return 'default'
    }
  }

  const getResponseSourceIcon = (source: string) => {
    switch (source) {
      case 'DATABASE':
        return <Storage fontSize="small" />
      case 'AI':
        return <SmartToy fontSize="small" />
      case 'NO_PRODUCTS':
        return <Cancel fontSize="small" />
      default:
        return <Chat fontSize="small" />
    }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  const filteredLogs = logs.filter(log => {
    if (searchTerm) {
      const searchLower = searchTerm.toLowerCase()
      return (
        log.userMessage.toLowerCase().includes(searchLower) ||
        log.aiResponse.toLowerCase().includes(searchLower) ||
        log.userId.toLowerCase().includes(searchLower)
      )
    }
    return true
  })

  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 1 }}>
          Chat Logs Management
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Xem và phân tích các câu hỏi từ chatbot để cải thiện sản phẩm
        </Typography>
      </Box>

      {/* Statistics Cards */}
      {statistics && (
        <Grid container spacing={0} sx={{ mb: 4, ml: 0 }}>
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      Tổng số logs
                    </Typography>
                    <Typography variant="h5" sx={{ fontWeight: 'bold', mt: 1 }}>
                      {statistics.totalLogs.toLocaleString()}
                    </Typography>
                  </Box>
                  <Chat sx={{ fontSize: 40, color: 'primary.main', opacity: 0.3 }} />
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      Tìm thấy sản phẩm
                    </Typography>
                    <Typography variant="h5" sx={{ fontWeight: 'bold', mt: 1, color: 'success.main' }}>
                      {statistics.productQueriesWithResults.toLocaleString()}
                    </Typography>
                  </Box>
                  <CheckCircle sx={{ fontSize: 40, color: 'success.main', opacity: 0.3 }} />
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      Không tìm thấy
                    </Typography>
                    <Typography variant="h5" sx={{ fontWeight: 'bold', mt: 1, color: 'warning.main' }}>
                      {statistics.productQueriesWithoutResults.toLocaleString()}
                    </Typography>
                  </Box>
                  <Cancel sx={{ fontSize: 40, color: 'warning.main', opacity: 0.3 }} />
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      Câu hỏi AI
                    </Typography>
                    <Typography variant="h5" sx={{ fontWeight: 'bold', mt: 1, color: 'primary.main' }}>
                      {statistics.aiQueries.toLocaleString()}
                    </Typography>
                  </Box>
                  <SmartToy sx={{ fontSize: 40, color: 'primary.main', opacity: 0.3 }} />
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* Filters */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                size="small"
                placeholder="Tìm kiếm..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Search />
                    </InputAdornment>
                  ),
                }}
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <TextField
                fullWidth
                size="small"
                placeholder="User ID"
                value={filterUserId}
                onChange={(e) => setFilterUserId(e.target.value)}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Person fontSize="small" />
                    </InputAdornment>
                  ),
                }}
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <FormControl fullWidth size="small">
                <InputLabel>Loại câu hỏi</InputLabel>
                <Select
                  value={filterProductRelated === null ? 'all' : filterProductRelated.toString()}
                  label="Loại câu hỏi"
                  onChange={(e) => {
                    const value = e.target.value
                    setFilterProductRelated(value === 'all' ? null : value === 'true')
                  }}
                >
                  <MenuItem value="all">Tất cả</MenuItem>
                  <MenuItem value="true">Liên quan sản phẩm</MenuItem>
                  <MenuItem value="false">Câu hỏi chung</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={2}>
              <Button
                fullWidth
                variant="outlined"
                startIcon={<Refresh />}
                onClick={() => {
                  setSearchTerm('')
                  setFilterUserId('')
                  setFilterProductRelated(null)
                  fetchLogs()
                  fetchStatistics()
                }}
              >
                Làm mới
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Table */}
      <Card>
        <CardContent>
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : filteredLogs.length === 0 ? (
            <Alert severity="info">Không có dữ liệu</Alert>
          ) : (
            <>
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Thời gian</TableCell>
                      <TableCell>User ID</TableCell>
                      <TableCell>Câu hỏi</TableCell>
                      <TableCell>Nguồn</TableCell>
                      <TableCell>Trạng thái</TableCell>
                      <TableCell>Đánh giá</TableCell>
                      <TableCell>Thao tác</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {filteredLogs.map((log) => (
                      <TableRow key={log.id} hover>
                        <TableCell>{formatDate(log.createdAt)}</TableCell>
                        <TableCell>
                          <Chip
                            label={log.userId === 'anonymous' ? 'Ẩn danh' : log.userId}
                            size="small"
                            color={log.userId === 'anonymous' ? 'default' : 'primary'}
                          />
                        </TableCell>
                        <TableCell>
                          <Tooltip title={log.userMessage}>
                            <Typography
                              variant="body2"
                              sx={{
                                maxWidth: 300,
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                              }}
                            >
                              {log.userMessage}
                            </Typography>
                          </Tooltip>
                        </TableCell>
                        <TableCell>
                          <Chip
                            icon={getResponseSourceIcon(log.responseSource)}
                            label={log.responseSource}
                            size="small"
                            color={getResponseSourceColor(log.responseSource) as any}
                          />
                        </TableCell>
                        <TableCell>
                          <Stack direction="row" spacing={1}>
                            {log.isProductRelated && (
                              <Chip
                                icon={<ShoppingCart />}
                                label="Sản phẩm"
                                size="small"
                                color="info"
                              />
                            )}
                            {log.foundProducts && (
                              <Chip label="Có kết quả" size="small" color="success" />
                            )}
                            {log.usedAI && (
                              <Chip icon={<SmartToy />} label="AI" size="small" color="primary" />
                            )}
                          </Stack>
                        </TableCell>
                        <TableCell>
                          {log.rating ? (
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                              {Array.from({ length: 5 }).map((_, i) => (
                                <Typography
                                  key={i}
                                  sx={{ color: i < log.rating! ? 'warning.main' : 'grey.300' }}
                                >
                                  ★
                                </Typography>
                              ))}
                            </Box>
                          ) : (
                            <Typography variant="body2" color="text.secondary">
                              Chưa đánh giá
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell>
                          <IconButton
                            size="small"
                            onClick={() => handleViewDetail(log)}
                            color="primary"
                          >
                            <Visibility />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>

              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 3 }}>
                <Typography variant="body2" color="text.secondary">
                  Tổng: {totalElements} logs
                </Typography>
                <Pagination
                  count={totalPages}
                  page={page + 1}
                  onChange={(_, newPage) => setPage(newPage - 1)}
                  color="primary"
                />
              </Box>
            </>
          )}
        </CardContent>
      </Card>

      {/* Detail Dialog */}
      <Dialog open={detailOpen} onClose={() => setDetailOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">Chi tiết Chat Log</Typography>
            <IconButton onClick={() => setDetailOpen(false)} size="small">
              <Cancel />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          {selectedLog && (
            <Stack spacing={3}>
              <Box>
                <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                  Thời gian
                </Typography>
                <Typography variant="body1">{formatDate(selectedLog.createdAt)}</Typography>
              </Box>

              <Divider />

              <Box>
                <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                  User ID
                </Typography>
                <Chip
                  label={selectedLog.userId === 'anonymous' ? 'Ẩn danh' : selectedLog.userId}
                  color={selectedLog.userId === 'anonymous' ? 'default' : 'primary'}
                />
              </Box>

              <Box>
                <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                  Câu hỏi của user
                </Typography>
                <Paper sx={{ p: 2, bgcolor: 'grey.100' }}>
                  <Typography variant="body1">{selectedLog.userMessage}</Typography>
                </Paper>
              </Box>

              <Box>
                <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                  Phản hồi
                </Typography>
                <Paper sx={{ p: 2, bgcolor: (theme) => theme.palette.primary.light, color: (theme) => theme.palette.primary.contrastText }}>
                  <Typography variant="body1">{selectedLog.aiResponse}</Typography>
                </Paper>
              </Box>

              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                    Nguồn phản hồi
                  </Typography>
                  <Chip
                    icon={getResponseSourceIcon(selectedLog.responseSource)}
                    label={selectedLog.responseSource}
                    color={getResponseSourceColor(selectedLog.responseSource) as any}
                  />
                </Grid>
                <Grid item xs={6}>
                  <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                    Loại câu hỏi
                  </Typography>
                  <Chip
                    label={selectedLog.isProductRelated ? 'Liên quan sản phẩm' : 'Câu hỏi chung'}
                    color={selectedLog.isProductRelated ? 'info' : 'default'}
                  />
                </Grid>
              </Grid>

              {selectedLog.productNames && (
                <Box>
                  <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                    Sản phẩm được tìm thấy
                  </Typography>
                  <Paper sx={{ p: 2, bgcolor: (theme) => theme.palette.success.light, color: (theme) => theme.palette.success.contrastText }}>
                    <Typography variant="body2">
                      {(() => {
                        try {
                          const names = JSON.parse(selectedLog.productNames || '[]')
                          return Array.isArray(names) ? names.join(', ') : selectedLog.productNames
                        } catch {
                          return selectedLog.productNames
                        }
                      })()}
                    </Typography>
                  </Paper>
                </Box>
              )}

              {selectedLog.feedback && (
                <Box>
                  <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                    Feedback
                  </Typography>
                  <Paper sx={{ p: 2, bgcolor: (theme) => theme.palette.warning.light, color: (theme) => theme.palette.warning.contrastText }}>
                    <Typography variant="body2">{selectedLog.feedback}</Typography>
                  </Paper>
                </Box>
              )}
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailOpen(false)}>Đóng</Button>
          <Button
            variant="contained"
            onClick={() => {
              setFeedbackOpen(true)
            }}
          >
            Thêm Feedback
          </Button>
        </DialogActions>
      </Dialog>

      {/* Feedback Dialog */}
      <Dialog open={feedbackOpen} onClose={() => setFeedbackOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Thêm Feedback</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              fullWidth
              label="Feedback"
              multiline
              rows={4}
              value={feedbackText}
              onChange={(e) => setFeedbackText(e.target.value)}
              placeholder="Nhập feedback về câu trả lời này..."
            />
            <FormControl fullWidth>
              <InputLabel>Đánh giá (1-5 sao)</InputLabel>
              <Select
                value={feedbackRating}
                label="Đánh giá (1-5 sao)"
                onChange={(e) => setFeedbackRating(Number(e.target.value))}
              >
                <MenuItem value={0}>Chưa đánh giá</MenuItem>
                <MenuItem value={1}>1 sao</MenuItem>
                <MenuItem value={2}>2 sao</MenuItem>
                <MenuItem value={3}>3 sao</MenuItem>
                <MenuItem value={4}>4 sao</MenuItem>
                <MenuItem value={5}>5 sao</MenuItem>
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFeedbackOpen(false)}>Hủy</Button>
          <Button variant="contained" onClick={handleSubmitFeedback}>
            Lưu
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  )
}

export default ChatLogsPage

