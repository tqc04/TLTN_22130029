import React, { useEffect, useState } from 'react'
import {
  Card,
  CardContent,
  Typography,
  Grid,
  Box,
  CircularProgress,
  Paper,
  Tab,
  Tabs,
  Select,
  MenuItem,
  FormControl,
  InputLabel
} from '@mui/material'
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from 'recharts'
import {
  TrendingUp,
  TrendingDown,
  AttachMoney,
  ShoppingCart,
  People,
  Inventory
} from '@mui/icons-material'
import { apiService } from '../services/api'

interface AnalyticsData {
  revenue: {
    total: number
    change: number
    chartData: { name: string; value: number }[]
  }
  orders: {
    total: number
    change: number
    chartData: { name: string; value: number }[]
  }
  customers: {
    total: number
    change: number
    new: number
  }
  products: {
    total: number
    lowStock: number
  }
  categoryDistribution: { name: string; value: number }[]
  topProducts: { name: string; sales: number; revenue: number }[]
  salesByDay: { day: string; sales: number; orders: number }[]
}

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16']

const AnalyticsAdminPage: React.FC = () => {
  const [loading, setLoading] = useState(true)
  const [tabValue, setTabValue] = useState(0)
  const [timeRange, setTimeRange] = useState<'7days' | '30days' | '90days' | 'year'>('30days')
  const [analyticsData, setAnalyticsData] = useState<AnalyticsData | null>(null)

  useEffect(() => {
    fetchAnalyticsData()
  }, [timeRange])

  const fetchAnalyticsData = async () => {
    setLoading(true)
    try {
      // Fetch real data from backend API
      const [overviewRes, salesRes, salesByPeriodRes, usersRes, productsRes, categoryRes, topProductsRes] = await Promise.all([
        apiService.getAnalyticsOverview(),
        apiService.getAnalyticsSales(timeRange),
        apiService.getAnalyticsSalesByPeriod('day'),
        apiService.getAnalyticsUsers(),
        apiService.getAnalyticsProducts(),
        apiService.getCategoryDistribution(),
        apiService.getTopProducts(5)
      ])

      const overview = overviewRes.data || {
        totalSales: 0,
        totalOrders: 0,
        activeUsers: 0,
        todaySales: 0,
        todayOrders: 0
      }

      const sales = salesRes.data || { labels: [], values: [] }
      const salesByPeriod = salesByPeriodRes.data?.data || []
      const users = usersRes.data || {
        newUsers: 0,
        totalUsers: 0,
        retainedUsers: 0
      }
      const products = productsRes.data || { total: 0, lowStock: 0 }
      const categories = categoryRes.data || []
      const topProducts = topProductsRes.data || []

      // Calculate change percentage (simplified - compare with previous period)
      const previousPeriodSales = sales.values.length > 0 
        ? sales.values.slice(0, Math.floor(sales.values.length / 2)).reduce((a, b) => a + b, 0)
        : 0
      const currentPeriodSales = sales.values.length > 0
        ? sales.values.slice(Math.floor(sales.values.length / 2)).reduce((a, b) => a + b, 0)
        : 0
      const revenueChange = previousPeriodSales > 0 
        ? ((currentPeriodSales - previousPeriodSales) / previousPeriodSales) * 100 
        : 0

      // Transform sales by period data for charts
      const salesByDay = salesByPeriod.map((item: any) => ({
        day: item.label || item.date,
        sales: item.sales || 0,
        orders: item.orders || 0
      }))

      // Transform sales data for revenue chart
      const revenueChartData = sales.labels.map((label: string, index: number) => ({
        name: label,
        value: sales.values[index] || 0
      }))

      const analyticsData: AnalyticsData = {
        revenue: {
          total: overview.totalSales || 0,
          change: revenueChange,
          chartData: revenueChartData
        },
        orders: {
          total: overview.totalOrders || 0,
          change: revenueChange, // Use same change for now
          chartData: revenueChartData.map(item => ({ ...item, value: Math.floor(item.value / 1000) }))
        },
        customers: {
          total: users.totalUsers || 0,
          change: users.totalUsers > 0 ? ((users.newUsers || 0) / users.totalUsers) * 100 : 0,
          new: users.newUsers || 0
        },
        products: {
          total: products.total || 0,
          lowStock: products.lowStock || 0
        },
        categoryDistribution: categories,
        topProducts: topProducts.map((p: any) => ({
          name: p.productName || p.name || 'Unknown Product',
          sales: p.totalQuantity || 0,
          revenue: p.totalRevenue || 0
        })),
        salesByDay
      }

      setAnalyticsData(analyticsData)
    } catch (error) {
      console.error('Failed to fetch analytics:', error)
      // Fallback to empty data on error
      setAnalyticsData({
        revenue: { total: 0, change: 0, chartData: [] },
        orders: { total: 0, change: 0, chartData: [] },
        customers: { total: 0, change: 0, new: 0 },
        products: { total: 0, lowStock: 0 },
        categoryDistribution: [],
        topProducts: [],
        salesByDay: []
      })
    } finally {
      setLoading(false)
    }
  }

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
  }

  const StatCard = ({ title, value, change, icon, color }: any) => (
    <Card sx={{ 
      height: '100%', 
      background: `linear-gradient(135deg, ${color}15 0%, ${color}05 100%)`,
      border: `1px solid ${color}30`,
      position: 'relative',
      overflow: 'hidden'
    }}>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <Box>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              {title}
            </Typography>
            <Typography variant="h4" sx={{ fontWeight: 'bold', color, mb: 1 }}>
              {value}
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              {change >= 0 ? (
                <TrendingUp sx={{ fontSize: 16, color: '#10b981' }} />
              ) : (
                <TrendingDown sx={{ fontSize: 16, color: '#ef4444' }} />
              )}
              <Typography variant="caption" sx={{ color: change >= 0 ? '#10b981' : '#ef4444' }}>
                {Math.abs(change)}% vs last period
              </Typography>
            </Box>
          </Box>
          <Box sx={{ 
            width: 56, 
            height: 56, 
            borderRadius: '50%', 
            backgroundColor: `${color}20`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            {icon}
          </Box>
        </Box>
      </CardContent>
    </Card>
  )

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '400px' }}>
        <CircularProgress />
      </Box>
    )
  }

  if (!analyticsData) {
    return (
      <Card sx={{ borderRadius: 3 }}>
        <CardContent>
          <Typography>Failed to load analytics data</Typography>
        </CardContent>
      </Card>
    )
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
          Analytics Dashboard
        </Typography>
        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>Time Range</InputLabel>
          <Select
            value={timeRange}
            label="Time Range"
            onChange={(e) => setTimeRange(e.target.value as any)}
          >
            <MenuItem value="7days">Last 7 Days</MenuItem>
            <MenuItem value="30days">Last 30 Days</MenuItem>
            <MenuItem value="90days">Last 90 Days</MenuItem>
            <MenuItem value="year">This Year</MenuItem>
          </Select>
        </FormControl>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={0} sx={{ mb: 3, ml: 0 }}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Revenue"
            value={formatCurrency(analyticsData.revenue.total)}
            change={analyticsData.revenue.change}
            icon={<AttachMoney sx={{ fontSize: 32, color: '#3b82f6' }} />}
            color="#3b82f6"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Orders"
            value={analyticsData.orders.total}
            change={analyticsData.orders.change}
            icon={<ShoppingCart sx={{ fontSize: 32, color: '#10b981' }} />}
            color="#10b981"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Customers"
            value={analyticsData.customers.total}
            change={analyticsData.customers.change}
            icon={<People sx={{ fontSize: 32, color: '#f59e0b' }} />}
            color="#f59e0b"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Products"
            value={`${analyticsData.products.total} (${analyticsData.products.lowStock} low)`}
            change={0}
            icon={<Inventory sx={{ fontSize: 32, color: '#8b5cf6' }} />}
            color="#8b5cf6"
          />
        </Grid>
      </Grid>

      {/* Tabs */}
      <Paper sx={{ borderRadius: 3, mb: 3 }}>
        <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)} sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tab label="Sales Overview" />
          <Tab label="Products" />
          <Tab label="Customers" />
        </Tabs>
      </Paper>

      {/* Tab Content */}
      {tabValue === 0 && (
        <Grid container spacing={3}>
          {/* Revenue Trend */}
          <Grid item xs={12} lg={8}>
            <Card sx={{ borderRadius: 3 }}>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 3 }}>
                  Revenue Trend
                </Typography>
                <ResponsiveContainer width="100%" height={300}>
                  <AreaChart data={analyticsData.salesByDay}>
                    <defs>
                      <linearGradient id="colorSales" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.8}/>
                        <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="day" />
                    <YAxis />
                    <Tooltip formatter={(value: any) => formatCurrency(value)} />
                    <Legend />
                    <Area type="monotone" dataKey="sales" stroke="#3b82f6" fillOpacity={1} fill="url(#colorSales)" name="Sales (VND)" />
                  </AreaChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </Grid>

          {/* Category Distribution */}
          <Grid item xs={12} lg={4}>
            <Card sx={{ borderRadius: 3, height: '100%' }}>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 3 }}>
                  Sales by Category
                </Typography>
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie
                      data={analyticsData.categoryDistribution}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, percent }) => `${name} ${((percent || 0) * 100).toFixed(0)}%`}
                      outerRadius={80}
                      fill="#8884d8"
                      dataKey="value"
                    >
                      {analyticsData.categoryDistribution.map((_, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </Grid>

          {/* Daily Orders */}
          <Grid item xs={12}>
            <Card sx={{ borderRadius: 3 }}>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 3 }}>
                  Daily Orders
                </Typography>
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={analyticsData.salesByDay}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="day" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="orders" fill="#10b981" name="Orders" />
                  </BarChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {tabValue === 1 && (
        <Grid container spacing={3}>
          {/* Top Products */}
          <Grid item xs={12}>
            <Card sx={{ borderRadius: 3 }}>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 3 }}>
                  Top Selling Products
                </Typography>
                <ResponsiveContainer width="100%" height={400}>
                  <BarChart data={analyticsData.topProducts} layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis type="number" />
                    <YAxis dataKey="name" type="category" width={150} />
                    <Tooltip formatter={(value: any) => formatCurrency(value)} />
                    <Legend />
                    <Bar dataKey="revenue" fill="#3b82f6" name="Revenue (VND)" />
                    <Bar dataKey="sales" fill="#10b981" name="Units Sold" />
                  </BarChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {tabValue === 2 && (
        <Grid container spacing={3}>
          {/* Customer Stats */}
          <Grid item xs={12} md={6}>
            <Card sx={{ borderRadius: 3 }}>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 3 }}>
                  Customer Growth
                </Typography>
                <ResponsiveContainer width="100%" height={300}>
                  <LineChart data={analyticsData.revenue.chartData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Line type="monotone" dataKey="value" stroke="#8b5cf6" strokeWidth={2} name="Customers" />
                  </LineChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </Grid>

          {/* New Customers */}
          <Grid item xs={12} md={6}>
            <Card sx={{ borderRadius: 3 }}>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>
                  Customer Summary
                </Typography>
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <Paper sx={{ p: 2, textAlign: 'center', bgcolor: '#3b82f615' }}>
                      <Typography variant="h3" sx={{ fontWeight: 'bold', color: '#3b82f6' }}>
                        {analyticsData.customers.total}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        Total Customers
                      </Typography>
                    </Paper>
                  </Grid>
                  <Grid item xs={6}>
                    <Paper sx={{ p: 2, textAlign: 'center', bgcolor: '#10b98115' }}>
                      <Typography variant="h3" sx={{ fontWeight: 'bold', color: '#10b981' }}>
                        {analyticsData.customers.new}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        New This Period
                      </Typography>
                    </Paper>
                  </Grid>
                  <Grid item xs={12}>
                    <Paper sx={{ p: 2, textAlign: 'center', bgcolor: '#f59e0b15' }}>
                      <Typography variant="h3" sx={{ fontWeight: 'bold', color: '#f59e0b' }}>
                        +{analyticsData.customers.change}%
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        Growth Rate
                      </Typography>
                    </Paper>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}
    </Box>
  )
}

export default AnalyticsAdminPage
