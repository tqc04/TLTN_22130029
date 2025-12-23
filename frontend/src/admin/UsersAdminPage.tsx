import React, { useEffect, useMemo, useState } from 'react'
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
  Avatar,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormControlLabel,
  Switch
} from '@mui/material'
import {
  People,
  Edit,
  Delete,
  Visibility,
  Save,
  Cancel,
  PersonAdd,
  VerifiedUser,
  Block,
  AdminPanelSettings,
  Person,
  Build
} from '@mui/icons-material'
import { DataGrid, GridColDef } from '@mui/x-data-grid'
import { apiService, User } from '../services/api'
import notificationService from '../services/notificationService'

const UsersAdminPage: React.FC = () => {
  const [rows, setRows] = useState<User[]>([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [rowCount, setRowCount] = useState(0)
  const [loading, setLoading] = useState(false)

  const [selected, setSelected] = useState<User | null>(null)
  const [editOpen, setEditOpen] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [form, setForm] = useState<Partial<User> & { password?: string }>({})
  const [reason, setReason] = useState<string>('')

  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'warning' | 'info'
  })

  const load = async () => {
    setLoading(true)
    try {
      console.log('Loading users - page:', page, 'pageSize:', pageSize)
      
      // Call API with pagination
      const response = await apiService.getUsers(page, pageSize)
      console.log('Users API response:', response)
      
      if (response.success && response.data) {
        // Backend returns Page<User> with content array
        const users: User[] = response.data.content || []
        const total = response.data.totalElements || 0
        
        console.log('Loaded users:', users.length, 'Total:', total)
        
        setRows(users)
        setRowCount(total)
        showSnackbar(`Loaded ${users.length} users successfully`, 'success')
      } else {
        throw new Error('Failed to load users')
      }
    } catch (error: any) {
      console.error('UsersAdminPage - Load error:', error)
      const errorMsg = error?.response?.data?.error || error?.message || 'Unknown error'
      showSnackbar(`Error: ${errorMsg}`, 'error')
      setRows([])
      setRowCount(0)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [page, pageSize])
  
  // Debug effect to monitor rows state
  useEffect(() => {
    console.log('UsersAdminPage - Rows state updated:', rows)
    console.log('UsersAdminPage - Rows length:', rows.length)
  }, [rows])

  const showSnackbar = (message: string, severity: 'success' | 'error' | 'warning' | 'info') => {
    setSnackbar({ open: true, message, severity })
  }

  const getRoleColor = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'error'
      case 'MODERATOR':
        return 'warning'
      case 'REPAIR_TECHNICIAN':
        return 'info'
      default:
        return 'default'
    }
  }

  const getRoleIcon = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return <AdminPanelSettings fontSize="small" />
      case 'MODERATOR':
        return <VerifiedUser fontSize="small" />
      case 'REPAIR_TECHNICIAN':
        return <Build fontSize="small" />
      default:
        return <Person fontSize="small" />
    }
  }

  const columns: GridColDef[] = useMemo(() => ([
    { 
      field: 'id', 
      headerName: 'ID', 
      width: 80,
      renderCell: (params) => (
        <Chip label={params.value} size="small" color="primary" variant="outlined" />
      )
    },
    { 
      field: 'avatar', 
      headerName: 'Avatar', 
      width: 80,
      renderCell: (params) => (
        <Avatar 
          src={params.value} 
          sx={{ width: 40, height: 40 }}
        >
          {params.row.firstName?.charAt(0) || params.row.username?.charAt(0) || 'U'}
        </Avatar>
      )
    },
    { 
      field: 'username', 
      headerName: 'Username', 
      flex: 1, 
      minWidth: 150,
      renderCell: (params) => (
        <Box>
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            {params.value}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            ID: {params.row.id}
          </Typography>
        </Box>
      )
    },
    { 
      field: 'email', 
      headerName: 'Email', 
      flex: 1.2, 
      minWidth: 200,
      renderCell: (params) => (
        <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
          {params.value}
        </Typography>
      )
    },
    { 
      field: 'fullName', 
      headerName: 'Full Name', 
      width: 160,
      valueGetter: (params) => `${params.row.firstName || ''} ${params.row.lastName || ''}`.trim() || 'N/A',
      renderCell: (params) => (
        <Typography variant="body2" sx={{ fontWeight: 500 }}>
          {params.value}
        </Typography>
      )
    },
    { 
      field: 'role', 
      headerName: 'Role', 
      width: 120,
      renderCell: (params) => (
        <Chip 
          label={params.value} 
          size="small" 
          color={getRoleColor(params.value) as any}
          icon={getRoleIcon(params.value)}
          variant="outlined"
        />
      )
    },
    { 
      field: 'isEmailVerified', 
      headerName: 'Verified', 
      width: 110,
      renderCell: (params) => (
        <Chip 
          label={params.value ? 'Yes' : 'No'} 
          size="small" 
          color={params.value ? 'success' : 'default'}
          icon={params.value ? <VerifiedUser /> : <Block />}
          variant="outlined"
        />
      )
    },
    {
      field: 'actions', 
      headerName: 'Actions', 
      width: 260, 
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
            onClick={() => handleEdit(params.row)}
            sx={{ bgcolor: 'primary.50', '&:hover': { bgcolor: 'primary.100' } }}
          >
            <Edit fontSize="small" />
          </IconButton>
          <IconButton 
            size="small" 
            color="error" 
            onClick={() => handleDelete(params.row)}
            sx={{ bgcolor: 'error.50', '&:hover': { bgcolor: 'error.100' } }}
          >
            <Delete fontSize="small" />
          </IconButton>
        </Stack>
      )
    }
  ]), [])

  const handleView = (row: User) => {
    setSelected(row)
    setForm(row)
    setEditOpen(true)
  }

  const handleEdit = (row: User) => {
    setSelected(row)
    setForm({ 
      firstName: row.firstName, 
      lastName: row.lastName, 
      email: row.email, 
      phoneNumber: row.phoneNumber, 
      role: row.role, 
      isEmailVerified: row.isEmailVerified 
    })
    setEditOpen(true)
  }

  const handleDelete = (row: User) => {
    setSelected(row)
    setConfirmOpen(true)
  }

  const save = async () => {
    try {
      if (selected) {
        const updateData: any = { ...form };
        if (reason.trim()) {
          updateData.reason = reason.trim();
        }
        
        // Optimistic update - Update UI immediately
        const updatedUser = { ...selected, ...updateData };
        setRows(prevRows => 
          prevRows.map(row => 
            row.id === selected.id ? updatedUser : row
          )
        );
        
        // Call API
        const response = await apiService.adminUpdateUser(selected.id, updateData)
        console.log('User update response:', response)
        
        showSnackbar('User updated successfully', 'success')
        notificationService.success('Cập nhật người dùng thành công')
      } else {
        const createPayload: any = { ...form };
        if (!createPayload.username || !createPayload.password || !form.email) {
          showSnackbar('Username, password and email are required', 'warning')
          notificationService.warning('Thiếu Username/Password/Email')
          return
        }
        
        // Call API
        const response = await apiService.adminCreateUser(createPayload)
        console.log('User create response:', response)
        
        showSnackbar('User created successfully', 'success')
        notificationService.success('Tạo người dùng mới thành công')
      }
      
      setEditOpen(false)
      setReason('')
      
      // Refresh data to ensure consistency
      await load()
    } catch (error) {
      console.error('Error saving user:', error)
      showSnackbar('Error saving user', 'error')
      notificationService.error('Lưu người dùng thất bại')
      
      // Revert optimistic update on error
      if (selected) {
        await load()
      }
    }
  }

  const confirmDelete = async () => {
    if (!selected) return
    try {
      await apiService.adminDeleteUser(selected.id)
      setConfirmOpen(false)
      await load()
      showSnackbar('User deleted successfully', 'success')
      notificationService.success('Xóa người dùng thành công')
    } catch (error) {
      showSnackbar('Error deleting user', 'error')
      notificationService.error('Xóa người dùng thất bại')
    }
  }

  const resetForm = () => {
    setForm({})
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 900, mb: 1, background: 'linear-gradient(45deg, #3b82f6 0%, #2563eb 100%)', backgroundClip: 'text', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
          User Management
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Manage user accounts, roles, and permissions
        </Typography>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={0} sx={{ mb: 4, ml: 0 }}>
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
                    {rowCount}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    Total Users
                  </Typography>
                </Box>
                <People sx={{ fontSize: 40, opacity: 0.8 }} />
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
                    {rows.filter(r => r.isEmailVerified).length}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    Verified Users
                  </Typography>
                </Box>
                <VerifiedUser sx={{ fontSize: 40, opacity: 0.8 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>
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
                    {rows.filter(r => r.role === 'ADMIN').length}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    Admin Users
                  </Typography>
                </Box>
                <AdminPanelSettings sx={{ fontSize: 40, opacity: 0.8 }} />
              </Box>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{
            height: '100%',
            background: 'linear-gradient(135deg, #06b6d4 0%, #0891b2 100%)',
            color: 'white'
          }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" sx={{ fontWeight: 900, mb: 1 }}>
                    {rows.filter(r => r.role === 'REPAIR_TECHNICIAN').length}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    Repair Technicians
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
            background: 'linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)',
            color: 'white'
          }}>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box>
                  <Typography variant="h4" sx={{ fontWeight: 900, mb: 1 }}>
                    {rows.filter(r => !r.isEmailVerified).length}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    Unverified Users
                  </Typography>
                </Box>
                <Block sx={{ fontSize: 40, opacity: 0.8 }} />
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
              User List
            </Typography>
            <Button
              variant="contained"
              startIcon={<PersonAdd />}
              onClick={() => {
                resetForm()
                setSelected(null)
                setEditOpen(true)
              }}
              sx={{
                bgcolor: 'primary.main',
                '&:hover': { bgcolor: 'primary.dark' },
                borderRadius: 2,
                px: 3,
                py: 1
              }}
            >
              Add User
            </Button>
          </Box>

          <Box sx={{ height: 600, width: '100%' }}>
            <DataGrid
              rows={rows}
              columns={columns}
              pagination
              paginationMode="client"
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

      {/* Edit/Create Dialog */}
      <Dialog open={editOpen} onClose={() => setEditOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle sx={{ 
          bgcolor: selected ? 'primary.main' : 'success.main', 
          color: 'white',
          display: 'flex',
          alignItems: 'center',
          gap: 1
        }}>
          {selected ? <Edit /> : <PersonAdd />}
          {selected ? 'Edit User' : 'Create New User'}
        </DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField 
                fullWidth 
                label="First Name" 
                value={form.firstName || ''} 
                onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                sx={{ mb: 2 }}
              />
              <TextField 
                fullWidth 
                label="Last Name" 
                value={form.lastName || ''} 
                onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                sx={{ mb: 2 }}
              />
              <TextField 
                fullWidth 
                label="Email" 
                type="email"
                value={form.email || ''} 
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                sx={{ mb: 2 }}
              />
              {!selected && (
                <>
                  <TextField
                    fullWidth
                    label="Username"
                    value={(form as any).username || ''}
                    onChange={(e) => setForm({ ...form, username: e.target.value as any })}
                    sx={{ mb: 2 }}
                  />
                  <TextField
                    fullWidth
                    label="Password"
                    type="password"
                    value={form.password || ''}
                    onChange={(e) => setForm({ ...form, password: e.target.value })}
                    sx={{ mb: 2 }}
                  />
                </>
              )}
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField 
                fullWidth 
                label="Phone Number" 
                value={form.phoneNumber || ''} 
                onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })}
                sx={{ mb: 2 }}
              />
              <FormControl fullWidth sx={{ mb: 2 }}>
                <InputLabel>Role</InputLabel>
                <Select
                  value={form.role || 'USER'}
                  label="Role"
                  onChange={(e) => setForm({ ...form, role: e.target.value })}
                >
                  <MenuItem value="USER">User</MenuItem>
                  <MenuItem value="MODERATOR">Moderator</MenuItem>
                  <MenuItem value="PRODUCT_MANAGER">Product Manager</MenuItem>
                  <MenuItem value="USER_MANAGER">User Manager</MenuItem>
                  <MenuItem value="SUPPORT">Support</MenuItem>
                  <MenuItem value="REPAIR_TECHNICIAN">Repair Technician</MenuItem>
                  <MenuItem value="ADMIN">Admin</MenuItem>
                </Select>
              </FormControl>
              <FormControlLabel
                control={
                  <Switch
                    checked={form.isEmailVerified || false}
                    onChange={(e) => setForm({ ...form, isEmailVerified: e.target.checked })}
                  />
                }
                label="Email Verified"
              />
              {selected && (
                <TextField
                  fullWidth
                  label="New Password (optional)"
                  type="password"
                  value={form.password || ''}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  sx={{ mt: 2 }}
                />
              )}
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Lý do thay đổi quyền hạn (nếu có)"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="Nhập lý do thay đổi quyền hạn..."
                multiline
                rows={2}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 2 }}>
          <Button onClick={() => setEditOpen(false)} startIcon={<Cancel />}>
            Cancel
          </Button>
          <Button variant="contained" onClick={save} startIcon={<Save />}>
            {selected ? 'Save Changes' : 'Create User'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete Confirmation */}
      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)}>
        <DialogTitle sx={{ color: 'error.main' }}>
          Delete User?
        </DialogTitle>
        <DialogContent>
          <Typography>
            Are you sure you want to delete user "{selected?.username}"? This action cannot be undone.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmOpen(false)}>
            Cancel
          </Button>
          <Button color="error" variant="contained" onClick={confirmDelete}>
            Delete
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

export default UsersAdminPage


