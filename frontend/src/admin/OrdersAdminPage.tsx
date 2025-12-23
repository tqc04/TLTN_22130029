import React, { useEffect, useMemo, useState, useCallback } from 'react'
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
  Select
} from '@mui/material'
import { 
  ShoppingCart, 
  Edit, 
  Visibility, 
  Cancel, 
  Save, 
  Close,
  AttachMoney,
  LocalShipping,
  CheckCircle,
  Warning,
  Error
} from '@mui/icons-material'
import { DataGrid, GridColDef } from '@mui/x-data-grid'
import { apiService, Order } from '../services/api'
import { formatPrice } from '../utils/priceUtils'

const OrdersAdminPage: React.FC = () => {
  const [rows, setRows] = useState<Order[]>([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [rowCount, setRowCount] = useState(0)
  const [loading, setLoading] = useState(false)
  const [status, setStatus] = useState<string>('')

  const [selected, setSelected] = useState<Order | null>(null)
  const [viewOpen, setViewOpen] = useState(false)
  const [viewLoading, setViewLoading] = useState(false)
  const [orderDetails, setOrderDetails] = useState<Order | null>(null)
  const [statusOpen, setStatusOpen] = useState(false)
  const [newStatus, setNewStatus] = useState<string>('PENDING')
  const [reason, setReason] = useState<string>('')

  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'warning' | 'info'
  })

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await apiService.adminGetOrders(page, pageSize, status || undefined)
      setRows(res.data.content || [])
      setRowCount(res.data.totalElements || 0)
    } catch (error: unknown) {
      console.error('Error loading orders:', error)
      const message = (error as any)?.response?.data?.message || (error as Error)?.message || 'Error loading orders'
      showSnackbar(`Error: ${message}`, 'error')
    } finally {
      setLoading(false)
    }
  }, [page, pageSize, status])

  useEffect(() => {
    load()
  }, [load])

  const showSnackbar = (message: string, severity: 'success' | 'error' | 'warning' | 'info') => {
    setSnackbar({ open: true, message, severity })
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'DELIVERED':
      case 'COMPLETED':
        return 'success'
      case 'SHIPPED':
      case 'PROCESSING':
        return 'info'
      case 'PENDING':
      case 'PENDING_APPROVAL':
        return 'warning'
      case 'CANCELLED':
      case 'REFUNDED':
        return 'error'
      default:
        return 'default'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'DELIVERED':
      case 'COMPLETED':
        return <CheckCircle fontSize="small" />
      case 'SHIPPED':
        return <LocalShipping fontSize="small" />
      case 'PROCESSING':
        return <Edit fontSize="small" />
      case 'PENDING':
      case 'PENDING_APPROVAL':
        return <Warning fontSize="small" />
      case 'CANCELLED':
      case 'REFUNDED':
        return <Error fontSize="small" />
      default:
        return <Warning fontSize="small" />
    }
  }

  const handleView = useCallback(async (row: Order) => {
    setViewOpen(true)
    setViewLoading(true)
    setOrderDetails(null)
    try {
      const res = await apiService.adminGetOrder(row.id)
      setOrderDetails(res.data)
    } catch (error: unknown) {
      console.error('Error loading order details:', error)
      const message = (error as any)?.response?.data?.message || (error as Error)?.message || 'Error loading order details'
      showSnackbar(`Error: ${message}`, 'error')
      // Fallback to row data if API fails
      setOrderDetails(row)
    } finally {
      setViewLoading(false)
    }
  }, [])

  const openStatus = useCallback((row: Order) => {
    setSelected(row)
    setNewStatus(row.status)
    setReason('')
    setStatusOpen(true)
  }, [])

  const handleCancel = useCallback(async (row: Order) => {
    try {
      await apiService.cancelOrder(row.id, 'Cancelled by admin')
      await load()
      showSnackbar('Order cancelled successfully', 'success')
    } catch (error) {
      showSnackbar('Error cancelling order', 'error')
    }
  }, [load])

  const columns: GridColDef[] = useMemo(() => ([
    { 
      field: 'id', 
      headerName: 'Order ID', 
      width: 100,
      renderCell: (params) => (
        <Chip label={`#${params.value}`} size="small" color="primary" variant="outlined" />
      )
    },
    { 
      field: 'orderNumber', 
      headerName: 'Order #', 
      width: 160,
      renderCell: (params) => (
        <Typography variant="body2" sx={{ fontWeight: 600, fontFamily: 'monospace' }}>
          {params.value}
        </Typography>
      )
    },
    { 
      field: 'userFullName', 
      headerName: 'Customer', 
      flex: 1, 
      minWidth: 160,
      renderCell: (params) => (
        <Box>
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            {params.value}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Order #{params.row.orderNumber}
          </Typography>
        </Box>
      )
    },
    { 
      field: 'status', 
      headerName: 'Status', 
      width: 150,
      renderCell: (params) => (
        <Chip 
          label={params.value} 
          size="small" 
          color={getStatusColor(params.value)}
          icon={getStatusIcon(params.value)}
          variant="outlined"
        />
      )
    },
    { 
      field: 'totalAmount', 
      headerName: 'Total', 
      width: 120,
      renderCell: (params) => (
        <Typography variant="body2" sx={{ fontWeight: 700, color: 'success.main' }}>
          {formatPrice(params.value)}
        </Typography>
      )
    },
    { 
      field: 'createdAt', 
      headerName: 'Created', 
      width: 180,
      valueFormatter: (params) => new Date(params.value).toLocaleDateString('vi-VN')
    },
    {
      field: 'actions', 
      headerName: 'Actions', 
      width: 320, 
      sortable: false, 
      filterable: false,
      renderCell: (params) => (
        <Stack direction="row" spacing={1}>
          <IconButton 
            size="small" 
            color="info" 
            onClick={() => handleView(params.row)}
            sx={{ bgcolor: 'info.50', '&:hover': { bgcolor: 'info.100' } }}
          >
            <Visibility fontSize="small" />
          </IconButton>
          <IconButton 
            size="small" 
            color="primary" 
            onClick={() => openStatus(params.row)}
            sx={{ bgcolor: 'primary.50', '&:hover': { bgcolor: 'primary.100' } }}
          >
            <Edit fontSize="small" />
          </IconButton>
          <IconButton 
            size="small" 
            color="error" 
            onClick={() => handleCancel(params.row)}
            sx={{ bgcolor: 'error.50', '&:hover': { bgcolor: 'error.100' } }}
          >
            <Cancel fontSize="small" />
          </IconButton>
        </Stack>
      )
    }
  ]), [handleView, openStatus, handleCancel])

  const saveStatus = async () => {
    if (!selected) return
    try {
      await apiService.adminUpdateOrderStatus(selected.id, newStatus, reason)
      setStatusOpen(false)
      await load()
      showSnackbar('Order status updated successfully', 'success')
    } catch (error) {
      showSnackbar('Error updating order status', 'error')
    }
  }

  const getStatusOptions = () => [
    { value: 'PENDING', label: 'Pending' },
    { value: 'PENDING_APPROVAL', label: 'Pending Approval' },
    { value: 'APPROVED', label: 'Approved' },
    { value: 'CONFIRMED', label: 'Confirmed' },
    { value: 'PROCESSING', label: 'Processing' },
    { value: 'SHIPPED', label: 'Shipped' },
    { value: 'DELIVERED', label: 'Delivered' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'CANCELLED', label: 'Cancelled' },
    { value: 'REFUNDED', label: 'Refunded' }
  ]

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 900, mb: 1, background: 'linear-gradient(45deg, #f59e0b 0%, #d97706 100%)', backgroundClip: 'text', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
          Order Management
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Manage customer orders, track shipments, and update order statuses
        </Typography>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={0} sx={{ mb: 4, ml: 0 }}>
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
                    {rowCount}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    Total Orders
                  </Typography>
                </Box>
                <ShoppingCart sx={{ fontSize: 40, opacity: 0.8 }} />
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
                    {rows.filter(r => r.status === 'PENDING' || r.status === 'PENDING_APPROVAL').length}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    Pending Orders
                  </Typography>
                </Box>
                <Warning sx={{ fontSize: 40, opacity: 0.8 }} />
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
                    {rows.filter(r => r.status === 'SHIPPED' || r.status === 'DELIVERED').length}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    Shipped Orders
                  </Typography>
                </Box>
                <LocalShipping sx={{ fontSize: 40, opacity: 0.8 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ 
            height: '100%', 
            background: 'linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)',
            color: 'white'
          }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" sx={{ fontWeight: 900, mb: 1 }}>
                    {formatPrice(rows.reduce((sum, r) => sum + r.totalAmount, 0))}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    Total Revenue
                  </Typography>
                </Box>
                <AttachMoney sx={{ fontSize: 40, opacity: 0.8 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Main Content */}
      <Card sx={{ borderRadius: 3, boxShadow: '0 8px 32px rgba(0,0,0,0.08)' }}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
            <Typography variant="h6" sx={{ fontWeight: 700 }}>
              Order List
            </Typography>
            <FormControl size="small" sx={{ width: 220 }}>
              <InputLabel>Filter by Status</InputLabel>
              <Select
                value={status}
                label="Filter by Status"
                onChange={(e) => setStatus(e.target.value)}
              >
                <MenuItem value="">All Orders</MenuItem>
                {getStatusOptions().map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>

          <Box sx={{ height: 600, width: '100%' }}>
            <DataGrid
              rows={rows}
              columns={columns}
              pagination
              paginationMode="server"
              rowCount={rowCount}
              pageSizeOptions={[10, 25, 50]}
              paginationModel={{ page, pageSize }}
              onPaginationModelChange={(m) => { setPage(m.page); setPageSize(m.pageSize) }}
              loading={loading}
              getRowId={(r) => r.id}
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
            />
          </Box>
        </CardContent>
      </Card>

      {/* View Order Details Dialog */}
      <Dialog open={viewOpen} onClose={() => setViewOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle sx={{ 
          bgcolor: 'info.main', 
          color: 'white',
          display: 'flex',
          alignItems: 'center',
          gap: 1
        }}>
          <Visibility />
          Order Details
        </DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          {viewLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 200 }}>
              <Typography>Loading order details...</Typography>
            </Box>
          ) : orderDetails ? (
            <Stack spacing={3}>
              {/* Order Info */}
              <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
                <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>
                  Order Information
                </Typography>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Order Number</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 600, fontFamily: 'monospace' }}>
                      {orderDetails.orderNumber}
                    </Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Status</Typography>
                    <Box sx={{ mt: 0.5 }}>
                      <Chip 
                        label={orderDetails.status} 
                        size="small" 
                        color={getStatusColor(orderDetails.status)}
                        icon={getStatusIcon(orderDetails.status)}
                        variant="outlined"
                      />
                    </Box>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Customer</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 600 }}>
                      {orderDetails.userFullName || 'N/A'}
                    </Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Total Amount</Typography>
                    <Typography variant="body1" sx={{ fontWeight: 700, color: 'success.main' }}>
                      ${Number(orderDetails.totalAmount).toFixed(2)}
                    </Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Payment Method</Typography>
                    <Typography variant="body1">
                      {orderDetails.paymentMethod || 'N/A'}
                    </Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Payment Status</Typography>
                    <Typography variant="body1">
                      {orderDetails.paymentStatus || 'N/A'}
                    </Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="caption" color="text.secondary">Created Date</Typography>
                    <Typography variant="body1">
                      {new Date(orderDetails.createdAt).toLocaleString('vi-VN')}
                    </Typography>
                  </Grid>
                  {orderDetails.updatedAt && (
                    <Grid item xs={12} sm={6}>
                      <Typography variant="caption" color="text.secondary">Last Updated</Typography>
                      <Typography variant="body1">
                        {new Date(orderDetails.updatedAt).toLocaleString('vi-VN')}
                      </Typography>
                    </Grid>
                  )}
                </Grid>
              </Paper>

              {/* Order Items */}
              {orderDetails.orderItems && orderDetails.orderItems.length > 0 && (
                <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
                  <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>
                    Order Items ({orderDetails.orderItems.length})
                  </Typography>
                  <Stack spacing={2}>
                    {orderDetails.orderItems.map((item: any, index: number) => (
                      <Box key={index} sx={{ p: 2, bgcolor: 'white', borderRadius: 1, border: '1px solid #e0e0e0' }}>
                        <Grid container spacing={2} alignItems="center">
                          <Grid item xs={12} sm={6}>
                            <Typography variant="body2" sx={{ fontWeight: 600 }}>
                              {item.productName || `Product ${item.productId}`}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              Product ID: {item.productId}
                            </Typography>
                          </Grid>
                          <Grid item xs={6} sm={2}>
                            <Typography variant="caption" color="text.secondary">Quantity</Typography>
                            <Typography variant="body2" sx={{ fontWeight: 600 }}>
                              {item.quantity}
                            </Typography>
                          </Grid>
                          <Grid item xs={6} sm={2}>
                            <Typography variant="caption" color="text.secondary">Unit Price</Typography>
                            <Typography variant="body2">
                              {formatPrice(item.unitPrice || item.price || 0)}
                            </Typography>
                          </Grid>
                          <Grid item xs={12} sm={2}>
                            <Typography variant="caption" color="text.secondary">Subtotal</Typography>
                            <Typography variant="body2" sx={{ fontWeight: 700, color: 'success.main' }}>
                              {formatPrice(item.totalPrice || item.price || (item.unitPrice || 0) * (item.quantity || 0))}
                            </Typography>
                          </Grid>
                        </Grid>
                      </Box>
                    ))}
                  </Stack>
                </Paper>
              )}

              {/* Shipping Info */}
              {(orderDetails.shippingAddress || orderDetails.billingAddress) && (
                <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
                  <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>
                    Shipping & Billing Information
                  </Typography>
                  <Grid container spacing={2}>
                    {orderDetails.shippingAddress && (
                      <Grid item xs={12}>
                        <Typography variant="caption" color="text.secondary">Shipping Address</Typography>
                        <Typography variant="body2">
                          {orderDetails.shippingAddress}
                        </Typography>
                      </Grid>
                    )}
                    {orderDetails.billingAddress && (
                      <Grid item xs={12}>
                        <Typography variant="caption" color="text.secondary">Billing Address</Typography>
                        <Typography variant="body2">
                          {orderDetails.billingAddress}
                        </Typography>
                      </Grid>
                    )}
                    {orderDetails.shippingFee !== undefined && (
                      <Grid item xs={12} sm={6}>
                        <Typography variant="caption" color="text.secondary">Shipping Fee</Typography>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {formatPrice(orderDetails.shippingFee)}
                        </Typography>
                      </Grid>
                    )}
                  </Grid>
                </Paper>
              )}

              {/* Order Summary */}
              <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
                <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>
                  Order Summary
                </Typography>
                <Grid container spacing={2}>
                  {orderDetails.subtotal !== undefined && (
                    <Grid item xs={12} sm={6}>
                      <Typography variant="caption" color="text.secondary">Subtotal</Typography>
                      <Typography variant="body2">
                        {formatPrice(orderDetails.subtotal)}
                      </Typography>
                    </Grid>
                  )}
                  {orderDetails.taxAmount !== undefined && orderDetails.taxAmount > 0 && (
                    <Grid item xs={12} sm={6}>
                      <Typography variant="caption" color="text.secondary">Tax</Typography>
                      <Typography variant="body2">
                        {formatPrice(orderDetails.taxAmount)}
                      </Typography>
                    </Grid>
                  )}
                  {orderDetails.shippingFee !== undefined && orderDetails.shippingFee > 0 && (
                    <Grid item xs={12} sm={6}>
                      <Typography variant="caption" color="text.secondary">Shipping Fee</Typography>
                      <Typography variant="body2">
                        {formatPrice(orderDetails.shippingFee)}
                      </Typography>
                    </Grid>
                  )}
                  {orderDetails.discountAmount !== undefined && orderDetails.discountAmount > 0 && (
                    <Grid item xs={12} sm={6}>
                      <Typography variant="caption" color="text.secondary">Discount</Typography>
                      <Typography variant="body2" sx={{ color: 'success.main' }}>
                        -{formatPrice(orderDetails.discountAmount)}
                      </Typography>
                    </Grid>
                  )}
                  <Grid item xs={12}>
                    <Box sx={{ pt: 1, borderTop: '2px solid #e0e0e0' }}>
                      <Typography variant="caption" color="text.secondary">Total Amount</Typography>
                  <Typography variant="h6" sx={{ fontWeight: 700, color: 'success.main' }}>
                    {formatPrice(orderDetails.totalAmount)}
                  </Typography>
                    </Box>
                  </Grid>
                </Grid>
              </Paper>

              {/* Additional Notes */}
              {orderDetails.notes && (
                <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
                  <Typography variant="h6" sx={{ mb: 1, fontWeight: 700 }}>
                    Notes
                  </Typography>
                  <Typography variant="body2">
                    {orderDetails.notes}
                  </Typography>
                </Paper>
              )}
            </Stack>
          ) : (
            <Alert severity="error">Failed to load order details</Alert>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 2 }}>
          <Button onClick={() => setViewOpen(false)} startIcon={<Close />}>
            Close
          </Button>
          {orderDetails && (
            <Button 
              variant="contained" 
              onClick={() => {
                setViewOpen(false)
                openStatus(orderDetails)
              }} 
              startIcon={<Edit />}
            >
              Edit Status
            </Button>
          )}
        </DialogActions>
      </Dialog>

      {/* Status Update Dialog */}
      <Dialog open={statusOpen} onClose={() => setStatusOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ 
          bgcolor: 'primary.main', 
          color: 'white',
          display: 'flex',
          alignItems: 'center',
          gap: 1
        }}>
          <Edit />
          Update Order Status
        </DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <Stack spacing={3}>
            <Box>
              <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
                Order Details
              </Typography>
              <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  Order #{selected?.orderNumber} - {selected?.userFullName}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Current Status: {selected?.status}
                </Typography>
              </Paper>
            </Box>

            <FormControl fullWidth>
              <InputLabel>New Status</InputLabel>
              <Select
                value={newStatus}
                label="New Status"
                onChange={(e) => setNewStatus(e.target.value)}
              >
                {getStatusOptions().map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              fullWidth
              label="Reason for Status Change (Optional)"
              multiline
              rows={3}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Enter reason for status change..."
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 2 }}>
          <Button onClick={() => setStatusOpen(false)} startIcon={<Close />}>
            Cancel
          </Button>
          <Button variant="contained" onClick={saveStatus} startIcon={<Save />}>
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

export default OrdersAdminPage


