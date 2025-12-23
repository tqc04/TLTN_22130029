import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Box,
  Button,
  Card,
  CardContent,
  Grid,
  Chip,
  Alert,
  CircularProgress,
  TextField,
  Paper
} from '@mui/material';
import { apiService } from '../services/api';
import { Product } from '../services/api';

const MLTestPage: React.FC = () => {
  const [mlStatus, setMlStatus] = useState<{modelLoaded: boolean, modelInfo: string} | null>(null);
  const [recommendations, setRecommendations] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [userId, setUserId] = useState<string>('1');

  useEffect(() => {
    checkMLStatus();
  }, []);

  const checkMLStatus = async () => {
    try {
      const response = await apiService.getMLStatus();
      if (response.success) {
        setMlStatus(response.data);
      }
    } catch (err) {
      console.error('Failed to check ML status:', err);
    }
  };

  const getMLRecommendations = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiService.getMLRecommendations(userId, 10);
      if (response.success) {
        setRecommendations(response.data || []);
      } else {
        setError(response.message || 'Failed to get recommendations');
      }
    } catch (err) {
      setError('Failed to get ML recommendations');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        🤖 ML Recommendation Test
      </Typography>

      {/* ML Status */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          ML Model Status
        </Typography>
        {mlStatus ? (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Chip 
              label={mlStatus.modelLoaded ? 'Loaded' : 'Not Loaded'} 
              color={mlStatus.modelLoaded ? 'success' : 'error'}
            />
            <Typography variant="body2" color="text.secondary">
              {mlStatus.modelInfo}
            </Typography>
          </Box>
        ) : (
          <CircularProgress size={24} />
        )}
      </Paper>

      {/* Test Controls */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          Test ML Recommendations
        </Typography>
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', mb: 2 }}>
          <TextField
            label="User ID"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            size="small"
            type="number"
          />
          <Button
            variant="contained"
            onClick={getMLRecommendations}
            disabled={loading}
            startIcon={loading ? <CircularProgress size={20} /> : null}
          >
            Get ML Recommendations
          </Button>
        </Box>
        {error && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {error}
          </Alert>
        )}
      </Paper>

      {/* Recommendations */}
      {recommendations.length > 0 && (
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            ML Recommendations for User {userId}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Found {recommendations.length} recommendations
          </Typography>
          <Grid container spacing={2}>
            {recommendations.map((product, index) => (
              <Grid item xs={12} sm={6} md={4} key={product.id || index}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" noWrap>
                      {product.name}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                      {product.brand}
                    </Typography>
                    <Typography variant="h6" color="primary">
                      ${product.price}
                    </Typography>
                    {product.salePrice && (
                      <Typography variant="body2" color="error">
                        Sale: ${product.salePrice}
                      </Typography>
                    )}
                    <Box sx={{ mt: 1 }}>
                      <Chip 
                        label={`Score: ${product.rating?.toFixed(2) || 'N/A'}`} 
                        size="small" 
                        color="primary" 
                        variant="outlined"
                      />
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Paper>
      )}

      {recommendations.length === 0 && !loading && (
        <Paper sx={{ p: 3, textAlign: 'center' }}>
          <Typography variant="body1" color="text.secondary">
            No recommendations yet. Click "Get ML Recommendations" to test.
          </Typography>
        </Paper>
      )}
    </Container>
  );
};

export default MLTestPage;
