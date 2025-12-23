import React, { useCallback, useEffect, useMemo, useState } from 'react'
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
  Alert,
  Snackbar,
  Grid,
  Paper,
  Autocomplete,
  CircularProgress
} from '@mui/material'
import { FormControlLabel, Switch } from '@mui/material'
import { 
  Add, 
  Edit, 
  Delete, 
  Visibility, 
  Upload, 
  Save, 
  Cancel,
  Inventory,
  AttachMoney,
  Store,
  Category as CategoryIcon
} from '@mui/icons-material'
import { DataGrid, GridColDef } from '@mui/x-data-grid'
import { apiService, Brand, Category, Product, ProductVariant } from '../services/api'
import { formatPrice } from '../utils/priceUtils'

const ProductsAdminPage: React.FC = () => {
  const [rows, setRows] = useState<Product[]>([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [rowCount, setRowCount] = useState(0)
  const [loading, setLoading] = useState(false)
  
  // Variants management state
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null)
  const [variants, setVariants] = useState<ProductVariant[]>([])
  const [openVariantsDialog, setOpenVariantsDialog] = useState(false)
  const [openVariantDialog, setOpenVariantDialog] = useState(false)
  const [editingVariant, setEditingVariant] = useState<ProductVariant | null>(null)
  const [variantImageFile, setVariantImageFile] = useState<File | null>(null)
  const [variantForm, setVariantForm] = useState<Partial<ProductVariant>>({
    variantName: '',
    sku: '',
    color: '',
    size: '',
    price: 0,
    stockQuantity: 0,
    isActive: true,
    isDefault: false
  })
  
  // Component mount effect
  useEffect(() => {
    // Component mounted - no logging needed in production
  }, [])

  const [selected, setSelected] = useState<Product | null>(null)
  const [editOpen, setEditOpen] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)
  const [form, setForm] = useState<ProductForm>({})
  type ProductImage = { id: string; imageUrl: string; filename?: string }
  type ProductForm = Partial<Product & { saleDiscountPercent?: number }>
  const [images, setImages] = useState<ProductImage[]>([])
  const [createFiles, setCreateFiles] = useState<File[]>([])
  const [editFiles, setEditFiles] = useState<File[]>([])
  const [restockQuantity, setRestockQuantity] = useState<number>(0)
  const [mainImageFile, setMainImageFile] = useState<File | null>(null)
  const [editImageFile, setEditImageFile] = useState<File | null>(null)
  const [openVariantsAfterCreate, setOpenVariantsAfterCreate] = useState<boolean>(true)

  // Brand/Category dropdown options (use existing data instead of forcing numeric IDs)
  const [brands, setBrands] = useState<Brand[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [loadingMeta, setLoadingMeta] = useState(false)

  const [snackbar, setSnackbar] = useState({
    open: false,
    message: '',
    severity: 'success' as 'success' | 'error' | 'warning' | 'info'
  })

  // Load brands & categories once for admin forms
  useEffect(() => {
    let mounted = true
    const loadMeta = async () => {
      setLoadingMeta(true)
      try {
        // Use product-service meta endpoints (SQL-backed) so brand/category match products.brandId/categoryId
        const [b, c] = await Promise.all([apiService.getProductMetaBrands(), apiService.getProductMetaCategories()])
        if (!mounted) return
        setBrands(Array.isArray(b.data) ? b.data : [])
        setCategories(Array.isArray(c.data) ? c.data : [])
      } catch (_e) {
        // Ignore; dropdowns will be empty if services are down
      } finally {
        if (mounted) setLoadingMeta(false)
      }
    }
    loadMeta()
    return () => { mounted = false }
  }, [])

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await apiService.adminGetProducts(page, pageSize)

      const content: Product[] = res.data?.content || []
      const total: number = (res.data?.totalElements ?? content.length) as number
      
      if (content.length === 0 && page === 0) {
        // Fallback demo data to avoid empty feeling
        const demo: Product[] = [
          { id: '1', name: 'iPhone 15 Pro', description: 'Latest iPhone with advanced features', price: 999.99, imageUrl: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400', category: 'Phones', brand: 'Apple', stockQuantity: 12, rating: 5, createdAt: '2024-01-01', updatedAt: '2024-01-01' },
          { id: '2', name: 'Samsung Galaxy S24', description: 'Premium Android smartphone', price: 899.99, imageUrl: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400', category: 'Phones', brand: 'Samsung', stockQuantity: 8, rating: 4.8, createdAt: '2024-01-01', updatedAt: '2024-01-01' },
        ]
        setRows(demo)
        setRowCount(demo.length)
      } else {
        setRows(content)
        setRowCount(total)
      }
    } catch (error) {
      console.error('Error loading admin products:', error)
      showSnackbar('Error loading products', 'error')
    } finally {
      setLoading(false)
    }
  }, [page, pageSize])

  useEffect(() => {
    load()
  }, [load, page, pageSize])

  const loadImages = async (productId?: string) => {
    if (!productId) return setImages([])
    try {
      const res = await apiService.adminListProductImages(productId)
      const data = res.data as any
      // Backend may return { success: true, images: [...] } or just array
      const rawImages = Array.isArray(data) ? data : (data?.images || [])
      
      const imgs = rawImages.map((img: any) => ({
        id: img.id || img.filename || String(Math.random()), // Use filename as ID if available (from FileUploadController)
        imageUrl: img.url || img.imageUrl,
        filename: img.filename
      }))
      setImages(imgs)
    } catch (error) {
      console.error('Error loading images:', error)
    }
  }

  const showSnackbar = (message: string, severity: 'success' | 'error' | 'warning' | 'info') => {
    setSnackbar({ open: true, message, severity })
  }

  const handleView = useCallback((row: Product) => {
    setSelected(row)
    setForm(row)
    setEditOpen(true)
    loadImages(row.id)
  }, [])

  const handleEdit = useCallback((row: Product) => {
    setSelected(row)
    setForm({
      name: row.name,
      price: row.price,
      stockQuantity: row.stockQuantity,
      imageUrl: row.imageUrl,
      description: row.description,
      categoryId: row.categoryId,
      brandId: row.brandId,
      isOnSale: row.isOnSale || false,
      salePrice: row.salePrice,
      saleStartAt: row.saleStartAt,
      saleEndAt: row.saleEndAt
    })
    setRestockQuantity(0)
    setEditImageFile(null)
    setEditFiles([])
    setEditOpen(true)
    // Chỉ load images, không load variants ngay - để user click vào tab variants mới load
    loadImages(row.id)
  }, [])

  // Variants management functions
  const handleManageVariants = useCallback(async (product: Product) => {
    setSelectedProduct(product)
    try {
      // Use admin endpoint to include inactive variants; fallback to public endpoint if needed
      try {
        const adminRes = await apiService.adminGetProductVariants(product.id)
        setVariants(adminRes.data || [])
      } catch (_e) {
        const response = await apiService.getProductVariants(product.id)
        setVariants(response.data || [])
      }
      setOpenVariantsDialog(true)
    } catch (error) {
      console.error('Error loading variants:', error)
      showSnackbar('Error loading variants', 'error')
    }
  }, [])

  const handleDelete = useCallback((row: Product) => {
    setSelected(row)
    setConfirmOpen(true)
  }, [])

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
      field: 'name', 
      headerName: 'Product Name', 
      flex: 1.2, 
      minWidth: 200,
      renderCell: (params) => (
        <Box>
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            {params.value}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {params.row.brand}
          </Typography>
        </Box>
      )
    },
    { 
      field: 'category', 
      headerName: 'Category', 
      width: 120,
      renderCell: (params) => (
        <Chip label={params.value} size="small" color="secondary" />
      )
    },
    { 
      field: 'isOnSale', 
      headerName: 'Sale', 
      width: 90,
      renderCell: (params) => (
        <Chip label={params.value ? 'SALE' : '—'} size="small" color={params.value ? 'error' : 'default'} variant={params.value ? 'filled' : 'outlined'} />
      )
    },
    { 
      field: 'price', 
      headerName: 'Price', 
      width: 120, 
      valueFormatter: (p) => formatPrice(p.value),
      renderCell: (params) => (
        <Typography variant="body2" sx={{ fontWeight: 700, color: 'success.main' }}>
          {formatPrice(params.value)}
        </Typography>
      )
    },
    { 
      field: 'stockQuantity', 
      headerName: 'Stock', 
      width: 100,
      renderCell: (params) => (
        <Chip 
          label={params.value} 
          size="small" 
          color={params.value < 10 ? 'error' : params.value < 50 ? 'warning' : 'success'}
          variant={params.value < 10 ? 'filled' : 'outlined'}
        />
      )
    },
    { 
      field: 'isActive', 
      headerName: 'Status', 
      width: 100, 
      valueFormatter: (p) => p.value ? 'Active' : 'Inactive',
      renderCell: (params) => (
        <Chip 
          label={params.value ? 'Active' : 'Inactive'} 
          size="small" 
          color={params.value ? 'success' : 'default'}
          variant="outlined"
        />
      )
    },
    {
      field: 'actions', 
      headerName: 'Actions', 
      width: 280, 
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
            color="secondary" 
            onClick={() => handleManageVariants(params.row)}
            sx={{ bgcolor: 'secondary.50', '&:hover': { bgcolor: 'secondary.100' } }}
            title="Manage Variants"
          >
            <Inventory fontSize="small" />
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
  ]), [handleView, handleEdit, handleManageVariants, handleDelete])

  const handleAddVariant = () => {
    setEditingVariant(null)
    setVariantImageFile(null)
    setVariantForm({
      variantName: '',
      sku: '',
      color: '',
      size: '',
      price: selectedProduct?.price || 0,
      stockQuantity: 0,
      isActive: true,
      isDefault: false
    })
    setOpenVariantDialog(true)
  }

  const handleEditVariant = (variant: ProductVariant) => {
    setEditingVariant(variant)
    setVariantImageFile(null)
    setVariantForm(variant)
    setOpenVariantDialog(true)
  }

  const handleSaveVariant = async () => {
    try {
      // Upload variant image if user selected a file
      let imageUrl = variantForm.imageUrl || ''
      if (variantImageFile) {
        const uploadResponse = await apiService.adminUploadImage(variantImageFile)
        imageUrl = uploadResponse.data.url
      }

      const payload = {
        ...variantForm,
        imageUrl
      }

      if (editingVariant) {
        await apiService.updateVariant(editingVariant.id, payload)
        showSnackbar('Variant updated successfully', 'success')
      } else {
        await apiService.createVariant(selectedProduct!.id, payload)
        showSnackbar('Variant created successfully', 'success')
      }
      
      // Reload variants
      try {
        const adminRes = await apiService.adminGetProductVariants(selectedProduct!.id)
        setVariants(adminRes.data || [])
      } catch (_e) {
        const response = await apiService.getProductVariants(selectedProduct!.id)
        setVariants(response.data || [])
      }
      setOpenVariantDialog(false)
      setVariantImageFile(null)
    } catch (error) {
      console.error('Error saving variant:', error)
      showSnackbar('Error saving variant', 'error')
    }
  }

  const handleDeleteVariant = async (variantId: number) => {
    if (window.confirm('Are you sure you want to delete this variant?')) {
      try {
        await apiService.deleteVariant(variantId)
        showSnackbar('Variant deleted successfully', 'success')
        
        // Reload variants
        try {
          const adminRes = await apiService.adminGetProductVariants(selectedProduct!.id)
          setVariants(adminRes.data || [])
        } catch (_e) {
          const response = await apiService.getProductVariants(selectedProduct!.id)
          setVariants(response.data || [])
        }
      } catch (error) {
        console.error('Error deleting variant:', error)
        showSnackbar('Error deleting variant', 'error')
      }
    }
  }

  const save = async () => {
    if (!selected) return
    try {
      // Upload main image first if provided
      let mainImageUrl = form.imageUrl || ''
      if (editImageFile) {
        const uploadResponse = await apiService.adminUploadImage(editImageFile)
        mainImageUrl = uploadResponse.data.url
      }
      
      // Update product info with new image URL
      const updateData = {
        ...form,
        imageUrl: mainImageUrl,
        categoryId: form.categoryId ? Number(form.categoryId) : undefined,
        brandId: form.brandId ? Number(form.brandId) : undefined
      }
      await apiService.adminUpdateProduct(selected.id, updateData)
      
      // Upload gallery images if any
      if (editFiles.length > 0) {
        await apiService.adminUploadProductImages(selected.id, editFiles)
      }
      
      // Handle restock if quantity is provided
      if (restockQuantity > 0) {
        await apiService.adminRestockProduct(selected.id, restockQuantity)
        showSnackbar(`Product updated and restocked with ${restockQuantity} units`, 'success')
      } else {
        showSnackbar('Product updated successfully', 'success')
      }
      
      setEditOpen(false)
      setRestockQuantity(0) // Reset restock quantity
      setEditImageFile(null) // Reset edit image file
      setEditFiles([]) // Reset edit files
      await load()
      await loadImages(selected.id) // Reload images to show newly uploaded ones
    } catch (error) {
      console.error('Error saving product:', error)
      showSnackbar('Error updating product', 'error')
    }
  }

  const create = async () => {
    try {
      // Upload main image first if provided
      let mainImageUrl = ''
      if (mainImageFile) {
        const uploadResponse = await apiService.adminUploadImage(mainImageFile)
        mainImageUrl = uploadResponse.data.url
      }
      
      // Create product with main image URL
      const productData = {
        ...form,
        imageUrl: mainImageUrl,
        categoryId: form.categoryId ? Number(form.categoryId) : undefined,
        brandId: form.brandId ? Number(form.brandId) : undefined
      }
      
      const response = await apiService.adminCreateProduct(productData)
      const raw = response.data as any
      const newProduct: any = raw?.data || raw
      const createdProductId: string | undefined = raw?.id || newProduct?.id
      
      // Upload gallery images if any
      if (createFiles.length > 0 && createdProductId) {
        await apiService.adminUploadProductImages(createdProductId, createFiles)
      }
      
      setCreateOpen(false)
      resetForm()
      await load()
      showSnackbar('Product created successfully', 'success')

      // Optionally open variants right after product is created (variants require productId)
      if (openVariantsAfterCreate && createdProductId) {
        const createdProduct: Product = {
          ...(newProduct || {}),
          id: createdProductId,
          name: newProduct?.name || String(form.name || ''),
          description: newProduct?.description || String(form.description || ''),
          price: Number(newProduct?.price ?? form.price ?? 0),
          imageUrl: newProduct?.imageUrl || undefined,
          category: newProduct?.category || String(form.category || ''),
          brand: newProduct?.brand || String(form.brand || ''),
          stockQuantity: Number(newProduct?.stockQuantity ?? form.stockQuantity ?? 0),
          createdAt: newProduct?.createdAt || new Date().toISOString(),
          updatedAt: newProduct?.updatedAt || new Date().toISOString()
        }
        await handleManageVariants(createdProduct)
      }
    } catch (error) {
      console.error('Error creating product:', error)
      showSnackbar('Error creating product', 'error')
    }
  }

  const confirmDelete = async () => {
    if (!selected) return
    try {
      await apiService.adminDeleteProduct(selected.id)
      setConfirmOpen(false)
      await load()
      showSnackbar('Product deleted successfully', 'success')
    } catch (error) {
      showSnackbar('Error deleting product', 'error')
    }
  }

  const resetForm = () => {
    setForm({})
    setImages([])
    setCreateFiles([])
    setMainImageFile(null)
    setRestockQuantity(0)
  }

  const handleFixStock = async () => {
    try {
      const response = await apiService.adminFixAllProductStock()
      const result = response.data as { updatedProducts: number; totalProducts: number }
      showSnackbar(`Stock fixed! Updated ${result.updatedProducts} out of ${result.totalProducts} products`, 'success')
      await load() // Reload the products to see updated stock
    } catch (error) {
      console.error('Error fixing stock:', error)
      showSnackbar('Error fixing product stock', 'error')
    }
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" sx={{ fontWeight: 900, mb: 1, background: 'linear-gradient(45deg, #10b981 0%, #059669 100%)', backgroundClip: 'text', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
          Product Management
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Manage your product catalog, inventory, and pricing
        </Typography>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={0} sx={{ mb: 4, ml: 0 }}>
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
                    {rowCount}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    Total Products
                  </Typography>
                </Box>
                <Inventory sx={{ fontSize: 40, opacity: 0.8 }} />
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
                    {rows.filter(r => r.stockQuantity < 10).length}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    Low Stock
                  </Typography>
                </Box>
                <AttachMoney sx={{ fontSize: 40, opacity: 0.8 }} />
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
                    {rows.filter(r => r.isActive).length}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    Active Products
                  </Typography>
                </Box>
                <CategoryIcon sx={{ fontSize: 40, opacity: 0.8 }} />
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
                    {new Set(rows.map(r => r.brand)).size}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.9 }}>
                    Brands
                  </Typography>
                </Box>
                                 <Store sx={{ fontSize: 40, opacity: 0.8 }} />
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
              Product List
            </Typography>
            <Stack direction="row" spacing={2}>
              <Button
                variant="contained"
                startIcon={<Add />}
                onClick={() => {
                  resetForm()
                  setCreateOpen(true)
                }}
                sx={{
                  bgcolor: 'success.main',
                  '&:hover': { bgcolor: 'success.dark' },
                  borderRadius: 2,
                  px: 3,
                  py: 1
                }}
              >
                Add Product
              </Button>
              <Button
                variant="outlined"
                startIcon={<Inventory />}
                onClick={handleFixStock}
                sx={{
                  borderColor: 'warning.main',
                  color: 'warning.main',
                  '&:hover': { 
                    borderColor: 'warning.dark',
                    color: 'warning.dark',
                    bgcolor: 'warning.50'
                  },
                  borderRadius: 2,
                  px: 3,
                  py: 1
                }}
              >
                Fix All Stock
              </Button>
            </Stack>
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

      {/* Edit Dialog */}
      <Dialog 
        open={editOpen} 
        onClose={() => {
          setEditOpen(false)
          setEditImageFile(null)
          setEditFiles([])
        }} 
        maxWidth="md" 
        fullWidth
        keepMounted={false}
        TransitionProps={{ unmountOnExit: true }}
      >
        <DialogTitle sx={{ 
          bgcolor: 'primary.main', 
          color: 'white',
          display: 'flex',
          alignItems: 'center',
          gap: 1
        }}>
          <Edit />
          Edit Product
        </DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField 
                fullWidth 
                label="Product Name" 
                value={form.name || ''} 
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                sx={{ mb: 2 }}
              />
              <Autocomplete
                options={brands}
                loading={loadingMeta}
                getOptionLabel={(o) => o?.name || ''}
                value={brands.find(b => b.id === Number(form.brandId)) || null}
                onChange={(_, v) => setForm({ ...form, brandId: v?.id, brand: v?.name })}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Brand"
                    sx={{ mb: 2 }}
                    InputProps={{
                      ...params.InputProps,
                      endAdornment: (
                        <>
                          {loadingMeta ? <CircularProgress color="inherit" size={16} /> : null}
                          {params.InputProps.endAdornment}
                        </>
                      )
                    }}
                  />
                )}
              />
              <Autocomplete
                options={categories}
                loading={loadingMeta}
                getOptionLabel={(o) => o?.name || ''}
                value={categories.find(c => c.id === Number(form.categoryId)) || null}
                onChange={(_, v) => setForm({ ...form, categoryId: v?.id, category: v?.name })}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Category"
                    sx={{ mb: 2 }}
                    InputProps={{
                      ...params.InputProps,
                      endAdornment: (
                        <>
                          {loadingMeta ? <CircularProgress color="inherit" size={16} /> : null}
                          {params.InputProps.endAdornment}
                        </>
                      )
                    }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField 
                fullWidth 
                label="Price" 
                type="number" 
                value={form.price || ''} 
                onChange={(e) => setForm({ ...form, price: Number(e.target.value) })}
                sx={{ mb: 2 }}
              />
              <FormControlLabel 
                control={<Switch checked={Boolean(form.isOnSale)} onChange={(e) => setForm({ ...form, isOnSale: e.target.checked })} />} 
                label="On Sale"
              />
              <TextField 
                fullWidth 
                label="Discount (%)" 
                type="number" 
                value={form.saleDiscountPercent || ''} 
                onChange={(e) => {
                  const pct = Math.max(0, Math.min(100, Number(e.target.value)))
                  const salePrice = form.price ? Number((Number(form.price) * (1 - pct / 100)).toFixed(2)) : undefined
                  setForm({ ...form, salePrice, ...(isNaN(pct) ? {} : { saleDiscountPercent: pct }) })
                }}
                sx={{ mb: 1, mt: 1 }}
                disabled={!form.isOnSale}
                helperText={form.price != null && form.saleDiscountPercent != null ? `Giá sau giảm: ${formatPrice(Number(form.price) * (1 - (Number(form.saleDiscountPercent) || 0)/100))}` : ''}
              />
              <TextField
                fullWidth
                label="Sale Start (datetime)"
                type="datetime-local"
                value={form.saleStartAt || ''}
                onChange={(e) => setForm({ ...form, saleStartAt: e.target.value })}
                sx={{ mb: 2 }}
                disabled={!form.isOnSale}
                InputLabelProps={{ shrink: true }}
              />
              <TextField
                fullWidth
                label="Sale End (datetime)"
                type="datetime-local"
                value={form.saleEndAt || ''}
                onChange={(e) => setForm({ ...form, saleEndAt: e.target.value })}
                sx={{ mb: 2 }}
                disabled={!form.isOnSale}
                InputLabelProps={{ shrink: true }}
              />
              <TextField 
                fullWidth 
                label="Stock Quantity" 
                type="number" 
                value={form.stockQuantity || ''} 
                onChange={(e) => setForm({ ...form, stockQuantity: Number(e.target.value) })}
                sx={{ mb: 2 }}
              />
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" sx={{ mb: 1, fontWeight: 500 }}>
                  Ảnh Chính (Main Image)
                </Typography>
                <Button 
                  variant="outlined" 
                  component="label" 
                  startIcon={<Upload />}
                  sx={{ mb: 1 }}
                  fullWidth
                >
                  Chọn Ảnh Từ PC
                  <input 
                    hidden 
                    type="file" 
                    accept="image/*" 
                    onChange={(e) => {
                      const file = e.target.files?.[0]
                      if (file) {
                        setEditImageFile(file)
                      }
                    }} 
                  />
                </Button>
                <Typography variant="caption" display="block" color="text.secondary" sx={{ mb: 1 }}>
                  Hoặc nhập URL ảnh (nếu không chọn file)
                </Typography>
                <TextField 
                  fullWidth 
                  size="small"
                  label="Hoặc nhập URL ảnh" 
                  value={form.imageUrl || ''} 
                  onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
                  disabled={!!editImageFile}
                  helperText={editImageFile ? "Đã chọn file, URL sẽ bị bỏ qua" : "URL ảnh hiển thị trên danh sách sản phẩm"}
                />
                {editImageFile && (
                  <Box sx={{ mt: 1 }}>
                    <Paper sx={{ p: 1, display: 'inline-block', position: 'relative' }}>
                      <img 
                        src={URL.createObjectURL(editImageFile)} 
                        alt="edit-preview" 
                        style={{ 
                          width: 100, 
                          height: 100, 
                          objectFit: 'cover', 
                          borderRadius: 8 
                        }} 
                      />
                      <IconButton
                        size="small"
                        color="error"
                        sx={{ 
                          position: 'absolute', 
                          top: -8, 
                          right: -8,
                          bgcolor: 'error.main',
                          color: 'white',
                          '&:hover': { bgcolor: 'error.dark' }
                        }}
                        onClick={() => setEditImageFile(null)}
                      >
                        <Delete fontSize="small" />
                      </IconButton>
                    </Paper>
                  </Box>
                )}
                {!editImageFile && form.imageUrl && (
                  <Box sx={{ mt: 1 }}>
                    <Paper sx={{ p: 1, display: 'inline-block' }}>
                      <img 
                        src={form.imageUrl} 
                        alt="current-preview" 
                        style={{ 
                          width: 100, 
                          height: 100, 
                          objectFit: 'cover', 
                          borderRadius: 8 
                        }} 
                      />
                    </Paper>
                  </Box>
                )}
              </Box>
              {/* Restock Field - Only for edit mode */}
              <TextField 
                fullWidth 
                label="Nhập Thêm Hàng" 
                type="number"
                value={restockQuantity} 
                onChange={(e) => setRestockQuantity(Number(e.target.value))}
                sx={{ mb: 2 }}
                helperText={`Hiện tại: ${form.stockQuantity || 0} sản phẩm. Nhập số lượng muốn thêm vào kho.`}
                InputProps={{
                  startAdornment: (
                    <Box sx={{ mr: 1, display: 'flex', alignItems: 'center' }}>
                      <Inventory fontSize="small" color="primary" />
                    </Box>
                  )
                }}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField 
                fullWidth 
                label="Description" 
                value={form.description || ''} 
                multiline 
                rows={3} 
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                sx={{ mb: 2 }}
              />
            </Grid>
          </Grid>

          {selected && (
            <Box sx={{ mt: 3 }}>
              <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 600 }}>
                Ảnh Chi Tiết Sản Phẩm (Gallery)
              </Typography>
              <Button variant="outlined" component="label" startIcon={<Upload />} sx={{ mb: 2 }}>
                Upload Images
                <input hidden multiple type="file" accept="image/*" onChange={(e) => {
                  const files = Array.from(e.target.files || []) as File[]
                  setEditFiles(files)
                }} />
              </Button>
              
              {/* Preview new files to be uploaded */}
              {editFiles.length > 0 && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    Ảnh mới sẽ được upload khi lưu:
                  </Typography>
                  <Grid container spacing={2}>
                    {editFiles.map((f, idx) => (
                      <Grid item key={idx}>
                        <Paper sx={{ p: 1, position: 'relative' }}>
                          <img 
                            src={URL.createObjectURL(f)} 
                            alt={`new-${idx}`} 
                            style={{ 
                              width: 80, 
                              height: 80, 
                              objectFit: 'cover', 
                              borderRadius: 8 
                            }} 
                          />
                          <IconButton
                            size="small"
                            color="error"
                            sx={{ 
                              position: 'absolute', 
                              top: -8, 
                              right: -8,
                              bgcolor: 'error.main',
                              color: 'white',
                              '&:hover': { bgcolor: 'error.dark' }
                            }}
                            onClick={() => setEditFiles(editFiles.filter((_, i) => i !== idx))}
                          >
                            <Delete fontSize="small" />
                          </IconButton>
                        </Paper>
                      </Grid>
                    ))}
                  </Grid>
                </Box>
              )}
              
              {/* Existing images from database */}
              {images.length > 0 && (
                <Box>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    Ảnh hiện có:
                  </Typography>
                  <Grid container spacing={2}>
                    {images.map((img) => (
                      <Grid item key={img.id}>
                        <Paper sx={{ p: 1, position: 'relative' }}>
                          <img 
                            src={img.imageUrl} 
                            alt="product" 
                            style={{ 
                              width: 80, 
                              height: 80, 
                              objectFit: 'cover', 
                              borderRadius: 8 
                            }} 
                          />
                          <IconButton
                            size="small"
                            color="error"
                            sx={{ 
                              position: 'absolute', 
                              top: -8, 
                              right: -8,
                              bgcolor: 'error.main',
                              color: 'white',
                              '&:hover': { bgcolor: 'error.dark' }
                            }}
                            onClick={async () => { 
                              try {
                                await apiService.adminDeleteProductImage(selected.id, img.id)
                                await loadImages(selected.id)
                                showSnackbar('Image deleted successfully', 'success')
                              } catch (error) {
                                showSnackbar('Error deleting image', 'error')
                              }
                            }}
                          >
                            <Delete fontSize="small" />
                          </IconButton>
                        </Paper>
                      </Grid>
                    ))}
                  </Grid>
                </Box>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 2 }}>
          <Button onClick={() => setEditOpen(false)} startIcon={<Cancel />}>
            Cancel
          </Button>
          {selected && (
            <Button
              variant="outlined"
              color="secondary"
              startIcon={<Inventory />}
              onClick={() => handleManageVariants(selected)}
            >
              Manage Variants
            </Button>
          )}
          <Button variant="contained" onClick={save} startIcon={<Save />}>
            Save Changes
          </Button>
        </DialogActions>
      </Dialog>

      {/* Create Dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="md" fullWidth keepMounted={false}>
        <DialogTitle sx={{ 
          bgcolor: 'success.main', 
          color: 'white',
          display: 'flex',
          alignItems: 'center',
          gap: 1
        }}>
          <Add />
          Create New Product
        </DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField 
                fullWidth 
                label="Product Name" 
                value={form.name || ''} 
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                sx={{ mb: 2 }}
              />
              <Autocomplete
                options={brands}
                loading={loadingMeta}
                getOptionLabel={(o) => o?.name || ''}
                value={brands.find(b => b.id === Number(form.brandId)) || null}
                onChange={(_, v) => setForm({ ...form, brandId: v?.id, brand: v?.name })}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Brand"
                    sx={{ mb: 2 }}
                    InputProps={{
                      ...params.InputProps,
                      endAdornment: (
                        <>
                          {loadingMeta ? <CircularProgress color="inherit" size={16} /> : null}
                          {params.InputProps.endAdornment}
                        </>
                      )
                    }}
                  />
                )}
              />
              <Autocomplete
                options={categories}
                loading={loadingMeta}
                getOptionLabel={(o) => o?.name || ''}
                value={categories.find(c => c.id === Number(form.categoryId)) || null}
                onChange={(_, v) => setForm({ ...form, categoryId: v?.id, category: v?.name })}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Category"
                    sx={{ mb: 2 }}
                    InputProps={{
                      ...params.InputProps,
                      endAdornment: (
                        <>
                          {loadingMeta ? <CircularProgress color="inherit" size={16} /> : null}
                          {params.InputProps.endAdornment}
                        </>
                      )
                    }}
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField 
                fullWidth 
                label="Price" 
                type="number" 
                value={form.price || ''} 
                onChange={(e) => setForm({ ...form, price: Number(e.target.value) })}
                sx={{ mb: 2 }}
              />
              <FormControlLabel 
                control={<Switch checked={Boolean(form.isOnSale)} onChange={(e) => setForm({ ...form, isOnSale: e.target.checked })} />} 
                label="On Sale"
              />
              <TextField 
                fullWidth 
                label="Discount (%)" 
                type="number" 
                value={form.saleDiscountPercent || ''} 
                onChange={(e) => {
                  const pct = Math.max(0, Math.min(100, Number(e.target.value)))
                  const salePrice = form.price ? Number((Number(form.price) * (1 - pct / 100)).toFixed(2)) : undefined
                  setForm({ ...form, salePrice, ...(isNaN(pct) ? {} : { saleDiscountPercent: pct }) })
                }}
                sx={{ mb: 1, mt: 1 }}
                disabled={!form.isOnSale}
                helperText={form.price != null && form.saleDiscountPercent != null ? `Giá sau giảm: ${formatPrice(Number(form.price) * (1 - (Number(form.saleDiscountPercent) || 0)/100))}` : ''}
              />
              <TextField
                fullWidth
                label="Sale Start (datetime)"
                type="datetime-local"
                value={form.saleStartAt || ''}
                onChange={(e) => setForm({ ...form, saleStartAt: e.target.value })}
                sx={{ mb: 2 }}
                disabled={!form.isOnSale}
                InputLabelProps={{ shrink: true }}
              />
              <TextField
                fullWidth
                label="Sale End (datetime)"
                type="datetime-local"
                value={form.saleEndAt || ''}
                onChange={(e) => setForm({ ...form, saleEndAt: e.target.value })}
                sx={{ mb: 2 }}
                disabled={!form.isOnSale}
                InputLabelProps={{ shrink: true }}
              />
              <TextField 
                fullWidth 
                label="Stock Quantity" 
                type="number" 
                value={form.stockQuantity || ''} 
                onChange={(e) => setForm({ ...form, stockQuantity: Number(e.target.value) })}
                sx={{ mb: 2 }}
              />
              {/* Ảnh chính - File Upload */}
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" sx={{ mb: 1, fontWeight: 600 }}>
                  Ảnh Chính (Main Image)
                </Typography>
                <Button 
                  variant="outlined" 
                  component="label" 
                  startIcon={<Upload />}
                  sx={{ mb: 1 }}
                >
                  Chọn Ảnh Chính
                  <input 
                    hidden 
                    type="file" 
                    accept="image/*" 
                    onChange={(e) => {
                      const file = e.target.files?.[0]
                      if (file) {
                        setMainImageFile(file)
                      }
                    }} 
                  />
                </Button>
                <Typography variant="caption" display="block" color="text.secondary">
                  Ảnh hiển thị trên danh sách sản phẩm (chỉ chọn 1 ảnh)
                </Typography>
                {mainImageFile && (
                  <Box sx={{ mt: 1 }}>
                    <Paper sx={{ p: 1, display: 'inline-block', position: 'relative' }}>
                      <img 
                        src={URL.createObjectURL(mainImageFile)} 
                        alt="main-preview" 
                        style={{ 
                          width: 100, 
                          height: 100, 
                          objectFit: 'cover', 
                          borderRadius: 8 
                        }} 
                      />
                      <IconButton
                        size="small"
                        color="error"
                        sx={{ 
                          position: 'absolute', 
                          top: -8, 
                          right: -8,
                          bgcolor: 'error.main',
                          color: 'white',
                          '&:hover': { bgcolor: 'error.dark' }
                        }}
                        onClick={() => setMainImageFile(null)}
                      >
                        <Delete fontSize="small" />
                      </IconButton>
                    </Paper>
                  </Box>
                )}
              </Box>
            </Grid>
            <Grid item xs={12}>
              <TextField 
                fullWidth 
                label="Description" 
                value={form.description || ''} 
                multiline 
                rows={3} 
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                sx={{ mb: 2 }}
              />
            </Grid>
          </Grid>

          <Box sx={{ mt: 1, mb: 2 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={openVariantsAfterCreate}
                  onChange={(e) => setOpenVariantsAfterCreate(e.target.checked)}
                />
              }
              label="Sau khi tạo sản phẩm, mở ngay phần tạo phiên bản (variants: màu sắc/bộ nhớ/giá/tồn kho)"
            />
          </Box>

          <Box sx={{ mt: 3 }}>
            <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 600 }}>
              Ảnh Chi Tiết Sản Phẩm (Gallery)
            </Typography>
            <Button variant="outlined" component="label" startIcon={<Upload />}>
              Upload Images
              <input hidden multiple type="file" accept="image/*" onChange={(e) => {
                const files = Array.from(e.target.files || []) as File[]
                setCreateFiles(files)
              }} />
            </Button>
            {createFiles.length > 0 && (
              <Grid container spacing={2} sx={{ mt: 2 }}>
                {createFiles.map((f, idx) => (
                  <Grid item key={idx}>
                    <Paper sx={{ p: 1, position: 'relative' }}>
                      <img 
                        src={URL.createObjectURL(f)} 
                        alt={`new-${idx}`} 
                        style={{ 
                          width: 80, 
                          height: 80, 
                          objectFit: 'cover', 
                          borderRadius: 8 
                        }} 
                      />
                      <IconButton
                        size="small"
                        color="error"
                        sx={{ 
                          position: 'absolute', 
                          top: -8, 
                          right: -8,
                          bgcolor: 'error.main',
                          color: 'white',
                          '&:hover': { bgcolor: 'error.dark' }
                        }}
                        onClick={() => setCreateFiles(createFiles.filter((_, i) => i !== idx))}
                      >
                        <Delete fontSize="small" />
                      </IconButton>
                    </Paper>
                  </Grid>
                ))}
              </Grid>
            )}
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 3, pt: 2 }}>
          <Button onClick={() => setCreateOpen(false)} startIcon={<Cancel />}>
            Cancel
          </Button>
          <Button variant="contained" onClick={create} startIcon={<Save />}>
            Create Product
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete Confirmation */}
      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)}>
        <DialogTitle sx={{ color: 'error.main' }}>
          Delete Product?
        </DialogTitle>
        <DialogContent>
          <Typography>
            Are you sure you want to delete "{selected?.name}"? This action cannot be undone.
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

      {/* Variants Management Dialog */}
      <Dialog open={openVariantsDialog} onClose={() => setOpenVariantsDialog(false)} maxWidth="lg" fullWidth keepMounted={false}>
        <DialogTitle>
          <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
            Manage Variants - {selectedProduct?.name}
          </Typography>
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="subtitle1">
              Product Variants ({variants.length})
            </Typography>
            <Button
              variant="contained"
              startIcon={<Add />}
              onClick={handleAddVariant}
              size="small"
            >
              Add Variant
            </Button>
          </Box>
          
          <DataGrid
            rows={variants}
            columns={[
              { 
                field: 'imageUrl',
                headerName: 'Image',
                width: 90,
                sortable: false,
                renderCell: (params) => {
                  const url = params.value as string | undefined
                  if (!url) return <Typography variant="caption" color="text.secondary">—</Typography>
                  return (
                    <img
                      src={url}
                      alt="variant"
                      style={{ width: 48, height: 48, objectFit: 'cover', borderRadius: 8 }}
                    />
                  )
                }
              },
              { field: 'variantName', headerName: 'Name', width: 150 },
              { field: 'sku', headerName: 'SKU', width: 120 },
              { field: 'color', headerName: 'Color', width: 100 },
              { field: 'size', headerName: 'Size', width: 100 },
              { field: 'price', headerName: 'Price', width: 100, type: 'number' },
              { field: 'stockQuantity', headerName: 'Stock', width: 100, type: 'number' },
              {
                field: 'isActive',
                headerName: 'Active',
                width: 90,
                renderCell: (params) => (
                  <Chip
                    label={params.value ? 'Yes' : 'No'}
                    color={params.value ? 'success' : 'default'}
                    size="small"
                    variant={params.value ? 'filled' : 'outlined'}
                  />
                )
              },
              { 
                field: 'isDefault', 
                headerName: 'Default', 
                width: 100,
                renderCell: (params) => (
                  <Chip 
                    label={params.value ? 'Yes' : 'No'} 
                    color={params.value ? 'primary' : 'default'} 
                    size="small" 
                  />
                )
              },
              {
                field: 'actions',
                headerName: 'Actions',
                width: 120,
                renderCell: (params) => (
                  <Stack direction="row" spacing={1}>
                    <IconButton 
                      size="small" 
                      color="primary" 
                      onClick={() => handleEditVariant(params.row)}
                    >
                      <Edit fontSize="small" />
                    </IconButton>
                    <IconButton 
                      size="small" 
                      color="error" 
                      onClick={() => handleDeleteVariant(params.row.id)}
                    >
                      <Delete fontSize="small" />
                    </IconButton>
                  </Stack>
                )
              }
            ]}
            pageSizeOptions={[5, 10, 25]}
            initialState={{
              pagination: { paginationModel: { pageSize: 10 } }
            }}
            disableRowSelectionOnClick
            autoHeight
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenVariantsDialog(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Variant Form Dialog */}
      <Dialog open={openVariantDialog} onClose={() => setOpenVariantDialog(false)} maxWidth="md" fullWidth keepMounted={false}>
        <DialogTitle>
          <Typography variant="h6" sx={{ fontWeight: 'bold' }}>
            {editingVariant ? 'Edit Variant' : 'Add New Variant'}
          </Typography>
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Variant Name"
                value={variantForm.variantName || ''}
                onChange={(e) => setVariantForm(prev => ({ ...prev, variantName: e.target.value }))}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="SKU"
                value={variantForm.sku || ''}
                onChange={(e) => setVariantForm(prev => ({ ...prev, sku: e.target.value }))}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Color"
                value={variantForm.color || ''}
                onChange={(e) => setVariantForm(prev => ({ ...prev, color: e.target.value }))}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Size"
                value={variantForm.size || ''}
                onChange={(e) => setVariantForm(prev => ({ ...prev, size: e.target.value }))}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Price"
                type="number"
                value={variantForm.price || 0}
                onChange={(e) => setVariantForm(prev => ({ ...prev, price: Number(e.target.value) }))}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Stock Quantity"
                type="number"
                value={variantForm.stockQuantity || 0}
                onChange={(e) => setVariantForm(prev => ({ ...prev, stockQuantity: Number(e.target.value) }))}
                required
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <Box>
                <Typography variant="body2" sx={{ mb: 1, fontWeight: 600 }}>
                  Variant Image
                </Typography>
                <Button
                  variant="outlined"
                  component="label"
                  startIcon={<Upload />}
                  fullWidth
                  sx={{ mb: 1 }}
                >
                  Chọn ảnh cho phiên bản
                  <input
                    hidden
                    type="file"
                    accept="image/*"
                    onChange={(e) => {
                      const file = e.target.files?.[0]
                      if (file) setVariantImageFile(file)
                    }}
                  />
                </Button>
                <Typography variant="caption" display="block" color="text.secondary" sx={{ mb: 1 }}>
                  Hoặc nhập URL ảnh (nếu không chọn file)
                </Typography>
                <TextField
                  fullWidth
                  size="small"
                  label="Image URL"
                  value={variantForm.imageUrl || ''}
                  onChange={(e) => setVariantForm(prev => ({ ...prev, imageUrl: e.target.value }))}
                  disabled={!!variantImageFile}
                  helperText={variantImageFile ? 'Đã chọn file, URL sẽ bị bỏ qua' : ''}
                />
                {(variantImageFile || variantForm.imageUrl) && (
                  <Box sx={{ mt: 1 }}>
                    <Paper sx={{ p: 1, display: 'inline-block', position: 'relative' }}>
                      <img
                        src={variantImageFile ? URL.createObjectURL(variantImageFile) : (variantForm.imageUrl as string)}
                        alt="variant-preview"
                        style={{ width: 100, height: 100, objectFit: 'cover', borderRadius: 8 }}
                      />
                      <IconButton
                        size="small"
                        color="error"
                        sx={{
                          position: 'absolute',
                          top: -8,
                          right: -8,
                          bgcolor: 'error.main',
                          color: 'white',
                          '&:hover': { bgcolor: 'error.dark' }
                        }}
                        onClick={() => setVariantImageFile(null)}
                      >
                        <Delete fontSize="small" />
                      </IconButton>
                    </Paper>
                  </Box>
                )}
              </Box>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Barcode"
                value={variantForm.barcode || ''}
                onChange={(e) => setVariantForm(prev => ({ ...prev, barcode: e.target.value }))}
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={variantForm.isActive || false}
                    onChange={(e) => setVariantForm(prev => ({ ...prev, isActive: e.target.checked }))}
                  />
                }
                label="Active"
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={variantForm.isDefault || false}
                    onChange={(e) => setVariantForm(prev => ({ ...prev, isDefault: e.target.checked }))}
                  />
                }
                label="Default Variant"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenVariantDialog(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSaveVariant}>
            {editingVariant ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default ProductsAdminPage


