import React from 'react';
import { Snackbar, Alert, AlertColor } from '@mui/material';

interface ToastNotificationProps {
  type: AlertColor;
  message: string;
  open: boolean;
  onClose: () => void;
}

const ToastNotification: React.FC<ToastNotificationProps> = ({ type, message, open, onClose }) => {
  return (
    <Snackbar
      open={open}
      autoHideDuration={4000}
      onClose={onClose}
      anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
    >
      <Alert onClose={onClose} severity={type} sx={{ width: '100%' }}>
        {message}
      </Alert>
    </Snackbar>
  );
};

export default ToastNotification;
