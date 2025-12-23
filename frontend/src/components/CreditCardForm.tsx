import React, { useState } from 'react';
import {
  Box,
  TextField,
  Grid,
  Typography,
  Alert,
  Card,
  CardContent,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { CreditCard, Security } from '@mui/icons-material';

interface CreditCardFormProps {
  onCardDataChange: (cardData: CardData) => void;
  errors?: { [key: string]: string };
}

export interface CardData {
  cardNumber: string;
  cardHolderName: string;
  expMonth: string;
  expYear: string;
  cvc: string;
}

const CreditCardForm: React.FC<CreditCardFormProps> = ({ onCardDataChange, errors = {} }) => {
  const [cardData, setCardData] = useState<CardData>({
    cardNumber: '',
    cardHolderName: '',
    expMonth: '',
    expYear: '',
    cvc: '',
  });

  const handleInputChange = (field: keyof CardData, value: string) => {
    const newCardData = { ...cardData, [field]: value };
    setCardData(newCardData);
    onCardDataChange(newCardData);
  };

  const formatCardNumber = (value: string) => {
    const v = value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
    const matches = v.match(/\d{4,16}/g);
    const match = (matches && matches[0]) || '';
    const parts = [];
    for (let i = 0, len = match.length; i < len; i += 4) {
      parts.push(match.substring(i, i + 4));
    }
    if (parts.length) {
      return parts.join(' ');
    } else {
      return v;
    }
  };

  const getCardType = (cardNumber: string) => {
    const number = cardNumber.replace(/\s/g, '');
    if (number.startsWith('4')) return 'Visa';
    if (number.startsWith('5')) return 'Mastercard';
    if (number.startsWith('34') || number.startsWith('37')) return 'American Express';
    if (number.startsWith('6')) return 'Discover';
    return '';
  };

  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: 10 }, (_, i) => currentYear + i);
  const months = Array.from({ length: 12 }, (_, i) => String(i + 1).padStart(2, '0'));

  return (
    <Card sx={{ maxWidth: 600, mx: 'auto', mt: 2 }}>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
          <CreditCard sx={{ mr: 1, color: 'primary.main' }} />
          <Typography variant="h6" component="h2">
            Credit Card Information
          </Typography>
        </Box>

        <Alert severity="info" sx={{ mb: 3 }}>
          <Security sx={{ mr: 1 }} />
          Your payment information is secure and encrypted
        </Alert>

        <Grid container spacing={3}>
          {/* Card Number */}
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Card Number"
              value={cardData.cardNumber}
              onChange={(e) => handleInputChange('cardNumber', formatCardNumber(e.target.value))}
              placeholder="1234 5678 9012 3456"
              error={!!errors.cardNumber}
              helperText={errors.cardNumber || getCardType(cardData.cardNumber)}
              inputProps={{ maxLength: 19 }}
            />
          </Grid>

          {/* Card Holder Name */}
          <Grid item xs={12}>
            <TextField
              fullWidth
              label="Cardholder Name"
              value={cardData.cardHolderName}
              onChange={(e) => handleInputChange('cardHolderName', e.target.value.toUpperCase())}
              placeholder="JOHN DOE"
              error={!!errors.cardHolderName}
              helperText={errors.cardHolderName}
            />
          </Grid>

          {/* Expiry Date and CVC */}
          <Grid item xs={6}>
            <FormControl fullWidth error={!!errors.expMonth}>
              <InputLabel>Expiry Month</InputLabel>
              <Select
                value={cardData.expMonth}
                label="Expiry Month"
                onChange={(e) => handleInputChange('expMonth', e.target.value)}
              >
                {months.map((month) => (
                  <MenuItem key={month} value={month}>
                    {month}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>

          <Grid item xs={6}>
            <FormControl fullWidth error={!!errors.expYear}>
              <InputLabel>Expiry Year</InputLabel>
              <Select
                value={cardData.expYear}
                label="Expiry Year"
                onChange={(e) => handleInputChange('expYear', e.target.value)}
              >
                {years.map((year) => (
                  <MenuItem key={year} value={String(year)}>
                    {year}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>

          {/* CVC */}
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              label="CVC/CVV"
              value={cardData.cvc}
              onChange={(e) => handleInputChange('cvc', e.target.value.replace(/\D/g, ''))}
              placeholder="123"
              error={!!errors.cvc}
              helperText={errors.cvc || '3 or 4 digit security code'}
              inputProps={{ maxLength: 4 }}
            />
          </Grid>
        </Grid>

        {/* Test Card Information */}
        <Alert severity="warning" sx={{ mt: 3 }}>
          <Typography variant="body2">
            <strong>Test Cards:</strong>
            <br />
            • Visa: 4242 4242 4242 4242
            <br />
            • Mastercard: 5555 5555 5555 4444
            <br />
            • Any future date, any 3-digit CVC
          </Typography>
        </Alert>
      </CardContent>
    </Card>
  );
};

export default CreditCardForm; 