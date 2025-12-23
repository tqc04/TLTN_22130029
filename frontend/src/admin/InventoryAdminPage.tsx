import React, { useEffect, useState } from 'react'
import { 
  Box,
  Card, 
  CardContent, 
  Typography,
  Alert,
  CircularProgress,
  Chip,
  Stack,
  TextField,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Snackbar
} from '@mui/material'
import { DataGrid, GridColDef } from '@mui/x-data-grid'
import { Inventory as InventoryIcon, Add, Warning } from '@mui/icons-material'
import { apiService } from '../services/api'

interface InventoryItem {
  id: number
  productId: number
  productName: string
  productSku: string
  warehouseId: number
  warehouseName: string
  quantity: number
  reservedQuantity: number
  availableQuantity: number
  reorderLevel: number
  reorderQuantity: number
  lastRestockedAt: string
  updatedAt: string
}

const InventoryAdminPage: React.FC = () => {
  const [rows, setRows] = useState<InventoryItem[]>([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [rowCount, setRowCount] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Restock dialog
  const [restockOpen, setRestockOpen] = useState(false)
  const [selectedItem, setSelectedItem] = useState<InventoryItem | null>(null)
  const [restockQuantity, setRestockQuantity] = useState<number>(0)
  const [restocking, setRestocking] = useState(false)

  // Snackbar
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error'
  })

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await apiService.getInventory(page, pageSize)
      
      if (response.success && response.data) {
        const inventoryData = response.data as any
        const items: InventoryItem[] = inventoryData.content || []
        const total = inventoryData.totalElements || 0
        
        setRows(items)
        setRowCount(total)
      } else {
        throw new Error('Failed to load inventory')
      }
    } catch (err: any) {
      console.error('Failed to load inventory:', err)
      setError(err?.response?.data?.message || err?.message || 'Failed to load inventory')
      setRows([])
      setRowCount(0)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [page, pageSize])

  const handleRestockClick = (item: InventoryItem) => {
    setSelectedItem(item)
    setRestockQuantity(item.reorderQuantity || 10)
    setRestockOpen(true)
  }

  const handleRestockConfirm = async () => {
    if (!selectedItem || restockQuantity <= 0) return

    setRestocking(true)
    try {
      const response = await apiService.adminRestockProduct(
        selectedItem.productId,
        restockQuantity,
        selectedItem.warehouseId
      )

      if (response.success) {
        setSnackbar({
          open: true,
          message: `Successfully restocked ${restockQuantity} units of ${selectedItem.productName}`,
          severity: 'success'
        })
        setRestockOpen(false)
        load() // Reload data
      } else {
        throw new Error('Restock failed')
      }
    } catch (err: any) {
      setSnackbar({
        open: true,
        message: err?.response?.data?.message || err?.message || 'Failed to restock',
        severity: 'error'
      })
    } finally {
      setRestocking(false)
    }
  }

  const columns: GridColDef[] = [
    {
      field: 'productSku',
      headerName: 'SKU',
      width: 130,
      renderCell: (params) => (
        <Box sx={{ 
          bgcolor: 'grey.100', 
          px: 1.5, 
          py: 0.5, 
          borderRadius: 1,
          fontFamily: 'monospace',
          fontSize: '0.875rem',
          fontWeight: 600,
          color: 'text.primary'
        }}>
          {params.value}
        </Box>
      )
    },
    {
      field: 'productName',
      headerName: 'Product',
      flex: 1,
      minWidth: 250,
      renderCell: (params) => (
        <Stack spacing={0.5}>
          <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>
            {params.value}
          </Typography>
          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
            ID: {params.row.productId}
          </Typography>
        </Stack>
      )
    },
    {
      field: 'warehouseName',
      headerName: 'Warehouse',
      width: 160,
      renderCell: (params) => (
        <Chip 
          label={params.value}
          size="small"
          variant="outlined"
          sx={{ 
            borderColor: 'primary.main',
            color: 'primary.main',
            fontWeight: 500
          }}
        />
      )
    },
    {
      field: 'quantity',
      headerName: 'Stock',
      width: 110,
      type: 'number',
      renderCell: (params) => {
        const isLowStock = params.value <= params.row.reorderLevel
        return (
          <Chip 
            label={params.value}
            size="small"
            sx={{
              bgcolor: isLowStock ? 'warning.light' : 'success.light',
              color: isLowStock ? 'warning.dark' : 'success.dark',
              fontWeight: 700,
              minWidth: 50
            }}
          />
        )
      }
    },
    {
      field: 'availableQuantity',
      headerName: 'Available',
      width: 110,
      type: 'number',
      renderCell: (params) => (
        <Chip 
          label={params.value}
          size="small"
          sx={{
            bgcolor: params.value > 0 ? 'info.light' : 'error.light',
            color: params.value > 0 ? 'info.dark' : 'error.dark',
            fontWeight: 700,
            minWidth: 50
          }}
        />
      )
    },
    {
      field: 'reservedQuantity',
      headerName: 'Reserved',
      width: 110,
      type: 'number',
      align: 'center',
      headerAlign: 'center',
      renderCell: (params) => (
        <Box sx={{
          bgcolor: params.value > 0 ? 'warning.lighter' : 'grey.100',
          px: 1.5,
          py: 0.5,
          borderRadius: 1,
          fontWeight: 600,
          color: params.value > 0 ? 'warning.dark' : 'text.secondary',
          minWidth: 40,
          textAlign: 'center'
        }}>
          {params.value}
        </Box>
      )
    },
    {
      field: 'reorderLevel',
      headerName: 'Reorder At',
      width: 130,
      type: 'number',
      renderCell: (params) => {
        const isLowStock = params.row.quantity <= params.value
        return (
          <Stack direction="row" spacing={1} alignItems="center">
            {isLowStock && (
              <Warning 
                sx={{ 
                  color: 'warning.main',
                  fontSize: 20,
                  animation: 'pulse 2s ease-in-out infinite',
                  '@keyframes pulse': {
                    '0%, 100%': { opacity: 1 },
                    '50%': { opacity: 0.5 }
                  }
                }} 
              />
            )}
            <Typography 
              variant="body2" 
              sx={{ 
                fontWeight: 600,
                color: isLowStock ? 'warning.dark' : 'text.secondary'
              }}
            >
              {params.value}
            </Typography>
          </Stack>
        )
      }
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 140,
      sortable: false,
      align: 'center',
      headerAlign: 'center',
      renderCell: (params) => (
        <Button
          size="small"
          variant="contained"
          startIcon={<Add />}
          onClick={() => handleRestockClick(params.row)}
          sx={{
            bgcolor: 'primary.main',
            '&:hover': {
              bgcolor: 'primary.dark',
              transform: 'translateY(-2px)',
              boxShadow: 2
            },
            transition: 'all 0.2s',
            textTransform: 'none',
            fontWeight: 600,
            px: 2
          }}
        >
          Restock
        </Button>
      )
    }
  ]

  return (
    <Box>
      <Card sx={{ 
        borderRadius: 3,
        boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
        overflow: 'hidden'
      }}>
        <CardContent sx={{ p: 3 }}>
          {/* Header */}
          <Box sx={{ 
            mb: 3,
            pb: 2,
            borderBottom: '2px solid',
            borderColor: 'divider'
          }}>
            <Stack direction="row" spacing={2} alignItems="center">
              <Box sx={{
                bgcolor: 'primary.light',
                p: 1.5,
                borderRadius: 2,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <InventoryIcon sx={{ fontSize: 32, color: 'primary.main' }} />
              </Box>
              <Box>
                <Typography variant="h5" sx={{ fontWeight: 700, color: 'text.primary' }}>
                  Inventory Management
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
                  Monitor and manage stock levels across warehouses
                </Typography>
              </Box>
            </Stack>
          </Box>

          {error && (
            <Alert 
              severity="error" 
              sx={{ 
                mb: 3,
                borderRadius: 2,
                '& .MuiAlert-icon': {
                  fontSize: 24
                }
              }}
            >
              {error}
            </Alert>
          )}

          {/* Stats Summary */}
          {rows.length > 0 && (
            <Stack 
              direction="row" 
              spacing={2} 
              sx={{ mb: 3 }}
            >
              <Box sx={{
                flex: 1,
                bgcolor: 'success.lighter',
                p: 2,
                borderRadius: 2,
                border: '1px solid',
                borderColor: 'success.light'
              }}>
                <Typography variant="caption" sx={{ color: 'success.dark', fontWeight: 600 }}>
                  Total Items
                </Typography>
                <Typography variant="h4" sx={{ color: 'success.dark', fontWeight: 700 }}>
                  {rowCount}
                </Typography>
              </Box>
              <Box sx={{
                flex: 1,
                bgcolor: 'warning.lighter',
                p: 2,
                borderRadius: 2,
                border: '1px solid',
                borderColor: 'warning.light'
              }}>
                <Typography variant="caption" sx={{ color: 'warning.dark', fontWeight: 600 }}>
                  Low Stock
                </Typography>
                <Typography variant="h4" sx={{ color: 'warning.dark', fontWeight: 700 }}>
                  {rows.filter(r => r.quantity <= r.reorderLevel).length}
                </Typography>
              </Box>
              <Box sx={{
                flex: 1,
                bgcolor: 'info.lighter',
                p: 2,
                borderRadius: 2,
                border: '1px solid',
                borderColor: 'info.light'
              }}>
                <Typography variant="caption" sx={{ color: 'info.dark', fontWeight: 600 }}>
                  Total Stock
                </Typography>
                <Typography variant="h4" sx={{ color: 'info.dark', fontWeight: 700 }}>
                  {rows.reduce((sum, r) => sum + r.quantity, 0)}
                </Typography>
              </Box>
            </Stack>
          )}

          <DataGrid
            rows={rows}
            columns={columns}
            loading={loading}
            pagination
            paginationMode="server"
            paginationModel={{ page, pageSize }}
            onPaginationModelChange={(model) => {
              setPage(model.page)
              setPageSize(model.pageSize)
            }}
            rowCount={rowCount}
            pageSizeOptions={[10, 25, 50]}
            autoHeight
            disableRowSelectionOnClick
            sx={{
              border: 'none',
              '& .MuiDataGrid-columnHeaders': {
                bgcolor: 'grey.50',
                borderRadius: 1,
                borderBottom: '2px solid',
                borderColor: 'divider',
                '& .MuiDataGrid-columnHeaderTitle': {
                  fontWeight: 700,
                  fontSize: '0.875rem',
                  color: 'text.primary'
                }
              },
              '& .MuiDataGrid-cell': {
                borderBottom: '1px solid',
                borderColor: 'grey.100',
                py: 2
              },
              '& .MuiDataGrid-cell:focus': {
                outline: 'none'
              },
              '& .MuiDataGrid-row': {
                '&:hover': {
                  bgcolor: 'action.hover',
                  cursor: 'pointer'
                },
                '&.Mui-selected': {
                  bgcolor: 'primary.lighter',
                  '&:hover': {
                    bgcolor: 'primary.light'
                  }
                }
              },
              '& .MuiDataGrid-footerContainer': {
                borderTop: '2px solid',
                borderColor: 'divider',
                bgcolor: 'grey.50',
                mt: 2
              }
            }}
          />
        </CardContent>
      </Card>

      {/* Restock Dialog */}
      <Dialog 
        open={restockOpen} 
        onClose={() => setRestockOpen(false)} 
        maxWidth="sm" 
        fullWidth
        PaperProps={{
          sx: {
            borderRadius: 3,
            boxShadow: '0 8px 32px rgba(0,0,0,0.12)'
          }
        }}
      >
        <DialogTitle sx={{ 
          bgcolor: 'primary.main', 
          color: 'white',
          fontWeight: 700,
          fontSize: '1.25rem'
        }}>
          <Stack direction="row" spacing={1} alignItems="center">
            <Add />
            <span>Restock Product</span>
          </Stack>
        </DialogTitle>
        <DialogContent sx={{ mt: 3 }}>
          {selectedItem && (
            <Stack spacing={3}>
              {/* Product Info Card */}
              <Box sx={{
                bgcolor: 'grey.50',
                p: 2,
                borderRadius: 2,
                border: '1px solid',
                borderColor: 'grey.200'
              }}>
                <Stack spacing={1.5}>
                  <Box>
                    <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>
                      Product Name
                    </Typography>
                    <Typography variant="body1" sx={{ fontWeight: 600, color: 'text.primary' }}>
                      {selectedItem.productName}
                    </Typography>
                  </Box>
                  <Box>
                    <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>
                      SKU
                    </Typography>
                    <Typography variant="body2" sx={{ fontFamily: 'monospace', color: 'text.primary' }}>
                      {selectedItem.productSku}
                    </Typography>
                  </Box>
                  <Stack direction="row" spacing={3}>
                    <Box>
                      <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>
                        Current Stock
                      </Typography>
                      <Typography variant="h6" sx={{ fontWeight: 700, color: 'primary.main' }}>
                        {selectedItem.quantity}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>
                        Available
                      </Typography>
                      <Typography variant="h6" sx={{ fontWeight: 700, color: 'success.main' }}>
                        {selectedItem.availableQuantity}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" sx={{ color: 'text.secondary', fontWeight: 600 }}>
                        Warehouse
                      </Typography>
                      <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>
                        {selectedItem.warehouseName}
                      </Typography>
                    </Box>
                  </Stack>
                </Stack>
              </Box>

              {/* Restock Input */}
              <TextField
                label="Restock Quantity"
                type="number"
                value={restockQuantity}
                onChange={(e) => setRestockQuantity(parseInt(e.target.value) || 0)}
                fullWidth
                inputProps={{ min: 1 }}
                helperText={`New total will be: ${selectedItem.quantity + restockQuantity}`}
                sx={{
                  '& .MuiOutlinedInput-root': {
                    '& fieldset': {
                      borderWidth: 2
                    },
                    '&:hover fieldset': {
                      borderColor: 'primary.main'
                    },
                    '&.Mui-focused fieldset': {
                      borderColor: 'primary.main'
                    }
                  }
                }}
              />
            </Stack>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 2.5, gap: 1 }}>
          <Button 
            onClick={() => setRestockOpen(false)} 
            disabled={restocking}
            sx={{
              textTransform: 'none',
              fontWeight: 600,
              px: 3
            }}
          >
            Cancel
          </Button>
          <Button 
            onClick={handleRestockConfirm} 
            variant="contained"
            disabled={restocking || restockQuantity <= 0}
            sx={{
              textTransform: 'none',
              fontWeight: 600,
              px: 3,
              bgcolor: 'success.main',
              '&:hover': {
                bgcolor: 'success.dark'
              }
            }}
          >
            {restocking ? (
              <Stack direction="row" spacing={1} alignItems="center">
                <CircularProgress size={20} color="inherit" />
                <span>Processing...</span>
              </Stack>
            ) : (
              'Confirm Restock'
            )}
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

export default InventoryAdminPage


