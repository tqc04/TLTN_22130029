import React, { useEffect, useState } from 'react'
import {
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Typography,
  Chip,
  IconButton,
  Grid,
  Alert,
  Snackbar,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  SelectChangeEvent
} from '@mui/material'
import {
  Edit,
  Visibility,
  Save,
  Cancel,
  Build,
  CheckCircle,
  Error,
  Schedule,
  Pending,
  Refresh,
  FilterList,
  Search
} from '@mui/icons-material'
import { DataGrid, GridColDef } from '@mui/x-data-grid'
import { apiService } from '../services/api'
import notificationService from '../services/notificationService'

interface WarrantyRequest {
  id: number
  requestNumber: string
  userId: number
  customerName: string
  customerEmail: string
  customerPhone?: string
  orderId: number
  orderNumber: string
  productId: number
  productName: string
  productSku?: string
  issueDescription: string
  status: string
  priority: string
  createdAt: string
  updatedAt: string
  rejectionReason?: string
  resolutionNotes?: string
  actualCompletionDate?: string
}

interface WarrantyStats {
  PENDING: number
  APPROVED: number
  RECEIVED: number
  IN_PROGRESS: number
  COMPLETED: number
  REJECTED: number
  CANCELLED: number
}

const WarrantyAdminPage: React.FC = () => {
  const [requests, setRequests] = useState<WarrantyRequest[]>([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)

  // Filters
  const [statusFilter, setStatusFilter] = useState('')
  const [userIdFilter, setUserIdFilter] = useState('')
  const [searchTerm, setSearchTerm] = useState('')

  // Dialog states
  const [selectedRequest, setSelectedRequest] = useState<WarrantyRequest | null>(null)
  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [viewDialogOpen, setViewDialogOpen] = useState(false)
  const [newStatus, setNewStatus] = useState('')
  const [notes, setNotes] = useState('')
  const [stats, setStats] = useState<WarrantyStats | null>(null)

  // Snackbar
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'warning' | 'info'
  })

  const loadRequests = async () => {
    setLoading(true)
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: pageSize.toString()
      })

      if (statusFilter) params.append('status', statusFilter)
      if (userIdFilter) params.append('userId', userIdFilter)

      const response = await apiService.get(`/api/admin/warranty/requests?${params}`)
      if (response.success) {
        setRequests(response.data as WarrantyRequest[])
        setTotalElements((response as any).totalElements || 0)
      }
      notificationService.info('Tải danh sách yêu cầu bảo hành thành công')
    } catch (error) {
      console.error('Error loading warranty requests:', error)
      showSnackbar('Không thể tải danh sách yêu cầu bảo hành', 'error')
      notificationService.error('Không thể tải danh sách yêu cầu bảo hành')
    } finally {
      setLoading(false)
    }
  }

  const loadStats = async () => {
    try {
      const response = await apiService.get('/api/admin/warranty/stats')
      if (response.success) {
        setStats(response.data as WarrantyStats)
      }
    } catch (error) {
      console.error('Error loading stats:', error)
    }
  }

  useEffect(() => {
    loadRequests()
    loadStats()
  }, [page, pageSize, statusFilter, userIdFilter])

  const showSnackbar = (message: string, severity: 'success' | 'error' | 'warning' | 'info') => {
    setSnackbar({ open: true, message, severity })
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'warning'
      case 'APPROVED':
        return 'info'
      case 'RECEIVED':
        return 'primary'
      case 'IN_PROGRESS':
        return 'secondary'
      case 'COMPLETED':
        return 'success'
      case 'REJECTED':
        return 'error'
      case 'CANCELLED':
        return 'default'
      default:
        return 'default'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <Pending fontSize="small" />
      case 'APPROVED':
        return <CheckCircle fontSize="small" />
      case 'RECEIVED':
        return <Schedule fontSize="small" />
      case 'IN_PROGRESS':
        return <Build fontSize="small" />
      case 'COMPLETED':
        return <CheckCircle fontSize="small" />
      case 'REJECTED':
        return <Error fontSize="small" />
      default:
        return <Schedule fontSize="small" />
    }
  }

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'HIGH':
        return 'error'
      case 'NORMAL':
        return 'primary'
      case 'LOW':
        return 'success'
      default:
        return 'default'
    }
  }

  const handleViewRequest = (request: WarrantyRequest) => {
    setSelectedRequest(request)
    setViewDialogOpen(true)
  }

  const handleEditRequest = (request: WarrantyRequest) => {
    setSelectedRequest(request)
    setNewStatus(request.status)
    setNotes('')
    setEditDialogOpen(true)
  }

  const handleUpdateStatus = async () => {
    if (!selectedRequest || !newStatus) return

    try {
      const response = await apiService.put(`/admin/warranty/requests/${selectedRequest.id}/status`, {
        status: newStatus,
        notes: notes || undefined
      })

      if (response.success) {
        showSnackbar('Cập nhật trạng thái thành công', 'success')
        notificationService.success('Cập nhật trạng thái thành công')
        setEditDialogOpen(false)
        loadRequests()
        loadStats()
      }
    } catch (error) {
      console.error('Error updating status:', error)
      showSnackbar('Không thể cập nhật trạng thái', 'error')
      notificationService.error('Không thể cập nhật trạng thái')
    }
  }

  const handleFilterChange = (filterType: string, value: string) => {
    setPage(0) // Reset to first page when filtering
    switch (filterType) {
      case 'status':
        setStatusFilter(value)
        break
      case 'userId':
        setUserIdFilter(value)
        break
    }
  }

  const handleClearFilters = () => {
    setStatusFilter('')
    setUserIdFilter('')
    setSearchTerm('')
    setPage(0)
  }

  const filteredRequests = requests.filter(request =>
    searchTerm === '' ||
    request.requestNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
    request.customerName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    request.productName.toLowerCase().includes(searchTerm.toLowerCase())
  )

  const columns: GridColDef[] = [
    {
      field: 'requestNumber',
      headerName: 'Request #',
      width: 120,
      renderCell: (params) => (
        <Chip
          label={params.value}
          size="small"
          color="primary"
          variant="outlined"
        />
      )
    },
    {
      field: 'customerName',
      headerName: 'Customer',
      width: 150,
      renderCell: (params) => (
        <Box>
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            {params.value}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            ID: {params.row.userId}
          </Typography>
        </Box>
      )
    },
    {
      field: 'productName',
      headerName: 'Product',
      width: 180,
      renderCell: (params) => (
        <Typography variant="body2">
          {params.value}
        </Typography>
      )
    },
    {
      field: 'issueDescription',
      headerName: 'Issue',
      width: 200,
      renderCell: (params) => (
        <Typography variant="body2" noWrap>
          {params.value}
        </Typography>
      )
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 120,
      renderCell: (params) => (
        <Chip
          label={params.value}
          size="small"
          color={getStatusColor(params.value) as any}
          icon={getStatusIcon(params.value)}
          variant="outlined"
        />
      )
    },
    {
      field: 'priority',
      headerName: 'Priority',
      width: 100,
      renderCell: (params) => (
        <Chip
          label={params.value}
          size="small"
          color={getPriorityColor(params.value) as any}
          variant="outlined"
        />
      )
    },
    {
      field: 'createdAt',
      headerName: 'Created',
      width: 120,
      renderCell: (params) => (
        <Typography variant="caption">
          {new Date(params.value).toLocaleDateString()}
        </Typography>
      )
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 150,
      sortable: false,
      filterable: false,
      renderCell: (params) => (
        <Stack direction="row" spacing={1}>
          <IconButton
            size="small"
            color="info"
            onClick={() => handleViewRequest(params.row)}
            sx={{ bgcolor: 'info.50', '&:hover': { bgcolor: 'info.100' } }}
          >
            <Visibility fontSize="small" />
          </IconButton>
          <IconButton
            size="small"
            color="primary"
            onClick={() => handleEditRequest(params.row)}
            sx={{ bgcolor: 'primary.50', '&:hover': { bgcolor: 'primary.100' } }}
          >
            <Edit fontSize="small" />
          </IconButton>
        </Stack>
      )
    }
  ]

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 900, mb: 1, background: 'linear-gradient(45deg, #3b82f6 0%, #2563eb 100%)', backgroundClip: 'text', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
          Warranty Management
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Manage warranty requests and repair technician assignments
        </Typography>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={0} sx={{ mb: 4, ml: 0 }}>
        {stats && (
          <>
            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{
                height: '100%',
                background: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
                color: 'white'
              }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Box>
                      <Typography variant="h4" sx={{ fontWeight: 900, mb: 1 }}>
                        {stats.PENDING}
                      </Typography>
                      <Typography variant="body2" sx={{ opacity: 0.9 }}>
                        Pending
                      </Typography>
                    </Box>
                    <Pending sx={{ fontSize: 40, opacity: 0.8 }} />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{
                height: '100%',
                background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
                color: 'white'
              }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Box>
                      <Typography variant="h4" sx={{ fontWeight: 900, mb: 1 }}>
                        {stats.IN_PROGRESS}
                      </Typography>
                      <Typography variant="body2" sx={{ opacity: 0.9 }}>
                        In Progress
                      </Typography>
                    </Box>
                    <Build sx={{ fontSize: 40, opacity: 0.8 }} />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{
                height: '100%',
                background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)',
                color: 'white'
              }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Box>
                      <Typography variant="h4" sx={{ fontWeight: 900, mb: 1 }}>
                        {stats.COMPLETED}
                      </Typography>
                      <Typography variant="body2" sx={{ opacity: 0.9 }}>
                        Completed
                      </Typography>
                    </Box>
                    <CheckCircle sx={{ fontSize: 40, opacity: 0.8 }} />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{
                height: '100%',
                background: 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)',
                color: 'white'
              }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Box>
                      <Typography variant="h4" sx={{ fontWeight: 900, mb: 1 }}>
                        {stats.REJECTED}
                      </Typography>
                      <Typography variant="body2" sx={{ opacity: 0.9 }}>
                        Rejected
                      </Typography>
                    </Box>
                    <Error sx={{ fontSize: 40, opacity: 0.8 }} />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          </>
        )}
      </Grid>

      {/* Filters */}
      <Card sx={{ mb: 3, borderRadius: 3, boxShadow: '0 8px 32px rgba(0,0,0,0.08)' }}>
        <CardContent>
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
            <TextField
              label="Search"
              variant="outlined"
              size="small"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              sx={{ minWidth: 200 }}
              InputProps={{
                startAdornment: <Search sx={{ mr: 1, color: 'text.secondary' }} />
              }}
            />
            <FormControl size="small" sx={{ minWidth: 150 }}>
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                label="Status"
                onChange={(e: SelectChangeEvent) => handleFilterChange('status', e.target.value)}
              >
                <MenuItem value="">All Status</MenuItem>
                <MenuItem value="PENDING">Pending</MenuItem>
                <MenuItem value="APPROVED">Approved</MenuItem>
                <MenuItem value="RECEIVED">Received</MenuItem>
                <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
                <MenuItem value="COMPLETED">Completed</MenuItem>
                <MenuItem value="REJECTED">Rejected</MenuItem>
                <MenuItem value="CANCELLED">Cancelled</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="User ID"
              variant="outlined"
              size="small"
              value={userIdFilter}
              onChange={(e) => handleFilterChange('userId', e.target.value)}
              sx={{ minWidth: 120 }}
            />
            <Button
              variant="outlined"
              startIcon={<Refresh />}
              onClick={loadRequests}
              disabled={loading}
            >
              Refresh
            </Button>
            <Button
              variant="outlined"
              startIcon={<FilterList />}
              onClick={handleClearFilters}
            >
              Clear Filters
            </Button>
          </Box>
        </CardContent>
      </Card>

      {/* Main Content */}
      <Card sx={{ borderRadius: 3, boxShadow: '0 8px 32px rgba(0,0,0,0.08)' }}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
            <Typography variant="h6" sx={{ fontWeight: 700 }}>
              Warranty Requests ({totalElements})
            </Typography>
          </Box>

          <Box sx={{ height: 600, width: '100%' }}>
            <DataGrid
              rows={filteredRequests}
              columns={columns}
              pagination
              paginationMode="server"
              rowCount={totalElements}
              loading={loading}
              getRowId={(row) => row.id}
              sx={{
                border: 'none',
                '& .MuiDataGrid-cell': {
                  borderBottom: '1px solid #f0f0f0',
                },
                '& .MuiDataGrid-columnHeaders': {
                  bgcolor: '#f8fafc',
                  borderBottom: '2px solid #e2e8f0',
                },
                '& .MuiDataGrid-row:hover': {
                  bgcolor: '#f8fafc',
                },
              }}
              paginationModel={{ page, pageSize }}
              onPaginationModelChange={(model) => {
                setPage(model.page)
                setPageSize(model.pageSize)
              }}
              pageSizeOptions={[10, 25, 50]}
            />
          </Box>
        </CardContent>
      </Card>

      {/* View Dialog */}
      <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle sx={{
          bgcolor: 'info.main',
          color: 'white',
          display: 'flex',
          alignItems: 'center',
          gap: 1
        }}>
          <Visibility />
          View Warranty Request - {selectedRequest?.requestNumber}
        </DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          {selectedRequest && (
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>Customer Information</Typography>
                <TextField
                  fullWidth
                  label="Customer Name"
                  value={selectedRequest.customerName}
                  InputProps={{ readOnly: true }}
                  sx={{ mb: 2 }}
                />
                <TextField
                  fullWidth
                  label="Email"
                  value={selectedRequest.customerEmail}
                  InputProps={{ readOnly: true }}
                  sx={{ mb: 2 }}
                />
                <TextField
                  fullWidth
                  label="Phone"
                  value={selectedRequest.customerPhone || 'N/A'}
                  InputProps={{ readOnly: true }}
                  sx={{ mb: 2 }}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>Product Information</Typography>
                <TextField
                  fullWidth
                  label="Product Name"
                  value={selectedRequest.productName}
                  InputProps={{ readOnly: true }}
                  sx={{ mb: 2 }}
                />
                <TextField
                  fullWidth
                  label="Product SKU"
                  value={selectedRequest.productSku || 'N/A'}
                  InputProps={{ readOnly: true }}
                  sx={{ mb: 2 }}
                />
                <TextField
                  fullWidth
                  label="Order Number"
                  value={selectedRequest.orderNumber}
                  InputProps={{ readOnly: true }}
                  sx={{ mb: 2 }}
                />
              </Grid>
              <Grid item xs={12}>
                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>Issue Description</Typography>
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  value={selectedRequest.issueDescription}
                  InputProps={{ readOnly: true }}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>Request Details</Typography>
                <TextField
                  fullWidth
                  label="Status"
                  value={selectedRequest.status}
                  InputProps={{ readOnly: true }}
                  sx={{ mb: 2 }}
                />
                <TextField
                  fullWidth
                  label="Priority"
                  value={selectedRequest.priority}
                  InputProps={{ readOnly: true }}
                  sx={{ mb: 2 }}
                />
                <TextField
                  fullWidth
                  label="Created Date"
                  value={new Date(selectedRequest.createdAt).toLocaleString()}
                  InputProps={{ readOnly: true }}
                />
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>Additional Information</Typography>
                {selectedRequest.rejectionReason && (
                  <TextField
                    fullWidth
                    label="Rejection Reason"
                    multiline
                    rows={2}
                    value={selectedRequest.rejectionReason}
                    InputProps={{ readOnly: true }}
                    sx={{ mb: 2 }}
                  />
                )}
                {selectedRequest.resolutionNotes && (
                  <TextField
                    fullWidth
                    label="Resolution Notes"
                    multiline
                    rows={2}
                    value={selectedRequest.resolutionNotes}
                    InputProps={{ readOnly: true }}
                    sx={{ mb: 2 }}
                  />
                )}
                {selectedRequest.actualCompletionDate && (
                  <TextField
                    fullWidth
                    label="Completion Date"
                    value={new Date(selectedRequest.actualCompletionDate).toLocaleString()}
                    InputProps={{ readOnly: true }}
                  />
                )}
              </Grid>
            </Grid>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 2 }}>
          <Button onClick={() => setViewDialogOpen(false)} startIcon={<Cancel />}>
            Close
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle sx={{
          bgcolor: 'primary.main',
          color: 'white',
          display: 'flex',
          alignItems: 'center',
          gap: 1
        }}>
          <Edit />
          Update Warranty Status - {selectedRequest?.requestNumber}
        </DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <FormControl fullWidth sx={{ mb: 2 }}>
                <InputLabel>New Status</InputLabel>
                <Select
                  value={newStatus}
                  label="New Status"
                  onChange={(e) => setNewStatus(e.target.value)}
                >
                  <MenuItem value="PENDING">Pending</MenuItem>
                  <MenuItem value="APPROVED">Approved</MenuItem>
                  <MenuItem value="RECEIVED">Received</MenuItem>
                  <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
                  <MenuItem value="COMPLETED">Completed</MenuItem>
                  <MenuItem value="REJECTED">Rejected</MenuItem>
                  <MenuItem value="CANCELLED">Cancelled</MenuItem>
                </Select>
              </FormControl>
              <TextField
                fullWidth
                label="Notes (optional)"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Add notes about this status change..."
                multiline
                rows={3}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 2 }}>
          <Button onClick={() => setEditDialogOpen(false)} startIcon={<Cancel />}>
            Cancel
          </Button>
          <Button variant="contained" onClick={handleUpdateStatus} startIcon={<Save />}>
            Update Status
          </Button>
        </DialogActions>
      </Dialog>

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert
          onClose={() => setSnackbar({ ...snackbar, open: false })}
          severity={snackbar.severity}
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  )
}

export default WarrantyAdminPage
