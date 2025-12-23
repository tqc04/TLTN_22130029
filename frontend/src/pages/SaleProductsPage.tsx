import React, { useEffect, useState } from 'react'
import { Container, Grid, Typography, Box, Card, CardContent, Button } from '@mui/material'
import { apiService } from '../services/api'
import { Link as RouterLink } from 'react-router-dom'

const SaleProductsPage: React.FC = () => {
  type SaleProduct = { id: number; name: string; price: number; description?: string }
  const [products, setProducts] = useState<SaleProduct[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let mounted = true
    setLoading(true)
    apiService.getSaleProducts(24)
      .then(res => { if (mounted && res.success) setProducts(res.data as any) })
      .finally(() => { if (mounted) setLoading(false) })
    return () => { mounted = false }
  }, [])

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 6 }}>
      <Box mb={3}>
        <Typography variant="h4" component="h1">Sản phẩm giảm giá</Typography>
        <Typography color="text.secondary">Ưu đãi hấp dẫn, số lượng có hạn</Typography>
      </Box>
      {loading ? (
        <div>Loading...</div>
      ) : (
        <Grid container spacing={2}>
          {products.map(p => (
            <Grid item xs={12} sm={6} md={4} lg={3} key={p.id}>
              <Card>
                <CardContent>
                  <Typography variant="subtitle1" fontWeight={700}>{p.name}</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }} noWrap>
                    {p.description || ''}
                  </Typography>
                  <Typography variant="h6" color="primary">${p.price}</Typography>
                  <Button component={RouterLink} to={`/products/${p.id}`} size="small" sx={{ mt: 1 }}>
                    Xem chi tiết
                  </Button>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}
    </Container>
  )
}

export default SaleProductsPage


