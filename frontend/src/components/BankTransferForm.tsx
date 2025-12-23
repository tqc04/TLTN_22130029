import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Alert,
  Button,
  Divider,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Chip,
  CircularProgress,
} from '@mui/material';
import {
  AccountBalance,
  QrCode,
  ContentCopy,
  CheckCircle,
  Schedule,
  Payment,
} from '@mui/icons-material';
import { apiService } from '../services/api';
import notificationService from '../services/notificationService';

interface BankTransferFormProps {
  orderNumber: string;
  amount: number;
  onTransferComplete?: () => void;
}

interface BankInfo {
  bankName: string;
  accountNumber: string;
  accountName: string;
  branch: string;
  amount: number;
  transferContent: string;
  currency: string;
  expiryTime: string;
}

const BankTransferForm: React.FC<BankTransferFormProps> = ({
  orderNumber,
  onTransferComplete,
}) => {
  const [bankInfo, setBankInfo] = useState<BankInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [transferStatus, setTransferStatus] = useState<string>('PENDING');
  const [copied, setCopied] = useState<string>('');

  useEffect(() => {
    loadBankTransferInfo();
  }, [orderNumber]);

  const loadBankTransferInfo = async () => {
    try {
      setLoading(true);
      const response = await apiService.createPayment({
        paymentMethod: 'BANK_TRANSFER',
        orderNumber: orderNumber,
        orderId: parseInt(orderNumber), // Keep for backward compatibility
      });

      if (response.success) {
        setBankInfo(response.data as BankInfo);
      } else {
        notificationService.error(response.message || 'Failed to load bank transfer information');
      }
    } catch (error) {
      console.error('Error loading bank transfer info:', error);
      notificationService.error('Failed to load bank transfer information');
    } finally {
      setLoading(false);
    }
  };

  const checkTransferStatus = async () => {
    try {
      const response = await apiService.getBankTransferStatus(orderNumber);
      if (response.success) {
        const responseData = response.data as { status: string };
        setTransferStatus(responseData.status);
        if (responseData.status === 'COMPLETED') {
          notificationService.success('Payment completed successfully!');
          onTransferComplete?.();
        }
      } else {
        notificationService.error(response.message || 'Failed to check transfer status');
      }
    } catch (error) {
      console.error('Error checking transfer status:', error);
      notificationService.error('Failed to check transfer status');
    }
  };

  const copyToClipboard = async (text: string, field: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(field);
      notificationService.success(`${field} copied to clipboard`);
      setTimeout(() => setCopied(''), 2000);
    } catch (error) {
      notificationService.error('Failed to copy to clipboard');
    }
  };

  const formatVND = (amount: number) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(amount);
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!bankInfo) {
    return (
      <Alert severity="error">
        Failed to load bank transfer information. Please try again.
      </Alert>
    );
  }

  return (
    <Card sx={{ maxWidth: 800, mx: 'auto', mt: 2 }}>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
          <AccountBalance sx={{ mr: 1, color: 'primary.main' }} />
          <Typography variant="h6" component="h2">
            Bank Transfer Information
          </Typography>
        </Box>

        <Alert severity="info" sx={{ mb: 3 }}>
          <Typography variant="body2">
            Please transfer the exact amount to the account below. The transfer content is important for order identification.
          </Typography>
        </Alert>

        <Grid container spacing={3}>
          {/* Bank Information */}
          <Grid item xs={12} md={6}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Bank Account Details
                </Typography>
                
                <List dense>
                  <ListItem>
                    <ListItemIcon>
                      <AccountBalance color="primary" />
                    </ListItemIcon>
                    <ListItemText
                      primary="Bank Name"
                      secondary={bankInfo.bankName}
                    />
                  </ListItem>
                  
                  <ListItem>
                    <ListItemIcon>
                      <Payment color="primary" />
                    </ListItemIcon>
                    <ListItemText
                      primary="Account Number"
                      secondary={
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                          <Typography variant="body2" sx={{ fontFamily: 'monospace', mr: 1 }}>
                            {bankInfo.accountNumber}
                          </Typography>
                          <Button
                            size="small"
                            startIcon={copied === 'accountNumber' ? <CheckCircle /> : <ContentCopy />}
                            onClick={() => copyToClipboard(bankInfo.accountNumber, 'accountNumber')}
                          >
                            Copy
                          </Button>
                        </Box>
                      }
                    />
                  </ListItem>
                  
                  <ListItem>
                    <ListItemIcon>
                      <AccountBalance color="primary" />
                    </ListItemIcon>
                    <ListItemText
                      primary="Account Name"
                      secondary={bankInfo.accountName}
                    />
                  </ListItem>
                  
                  <ListItem>
                    <ListItemIcon>
                      <AccountBalance color="primary" />
                    </ListItemIcon>
                    <ListItemText
                      primary="Branch"
                      secondary={bankInfo.branch}
                    />
                  </ListItem>
                </List>
              </CardContent>
            </Card>
          </Grid>

          {/* Transfer Details */}
          <Grid item xs={12} md={6}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Transfer Details
                </Typography>
                
                <List dense>
                  <ListItem>
                    <ListItemIcon>
                      <Payment color="primary" />
                    </ListItemIcon>
                    <ListItemText
                      primary="Amount"
                      secondary={
                        <Typography variant="h6" color="primary" fontWeight="bold">
                          {formatVND(bankInfo.amount)}
                        </Typography>
                      }
                    />
                  </ListItem>
                  
                  <ListItem>
                    <ListItemIcon>
                      <Payment color="primary" />
                    </ListItemIcon>
                    <ListItemText
                      primary="Transfer Content"
                      secondary={
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                          <Typography variant="body2" sx={{ fontFamily: 'monospace', mr: 1 }}>
                            {bankInfo.transferContent}
                          </Typography>
                          <Button
                            size="small"
                            startIcon={copied === 'transferContent' ? <CheckCircle /> : <ContentCopy />}
                            onClick={() => copyToClipboard(bankInfo.transferContent, 'transferContent')}
                          >
                            Copy
                          </Button>
                        </Box>
                      }
                    />
                  </ListItem>
                  
                  <ListItem>
                    <ListItemIcon>
                      <Schedule color="warning" />
                    </ListItemIcon>
                    <ListItemText
                      primary="Expiry Time"
                      secondary={bankInfo.expiryTime}
                    />
                  </ListItem>
                </List>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        <Divider sx={{ my: 3 }} />

        {/* QR Code Section */}
        <Box sx={{ textAlign: 'center', mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            Quick Transfer with QR Code
          </Typography>
          <Button
            variant="outlined"
            startIcon={<QrCode />}
            onClick={() => window.open(`/api/payments/qr/${orderNumber}`, '_blank')}
          >
            Generate QR Code
          </Button>
        </Box>

        {/* Status and Actions */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 3 }}>
          <Box>
            <Chip
              label={transferStatus}
              color={
                transferStatus === 'COMPLETED' ? 'success' :
                transferStatus === 'FAILED' ? 'error' : 'warning'
              }
              icon={transferStatus === 'COMPLETED' ? <CheckCircle /> : <Schedule />}
            />
          </Box>
          
          <Button
            variant="contained"
            onClick={checkTransferStatus}
            disabled={transferStatus === 'COMPLETED'}
          >
            Check Payment Status
          </Button>
        </Box>

        {/* Instructions */}
        <Alert severity="warning" sx={{ mt: 3 }}>
          <Typography variant="body2">
            <strong>Important:</strong>
            <br />
            • Transfer the exact amount: {formatVND(bankInfo.amount)}
            <br />
            • Include the transfer content: {bankInfo.transferContent}
            <br />
            • Payment will be confirmed within 24 hours
            <br />
            • Contact support if you have any issues
          </Typography>
        </Alert>
      </CardContent>
    </Card>
  );
};

export default BankTransferForm; 