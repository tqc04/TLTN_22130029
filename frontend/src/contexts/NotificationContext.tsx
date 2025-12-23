import React, { createContext, useContext, useState, ReactNode } from 'react';
import { Snackbar, Alert, Button } from '@mui/material';

export type NotificationType = 'success' | 'error' | 'warning' | 'info';

interface NotificationContextType {
  notify: (message: string, type?: NotificationType, durationMs?: number) => void;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export const useNotification = () => {
  const ctx = useContext(NotificationContext);
  if (!ctx) throw new Error('useNotification must be used within NotificationProvider');
  return ctx;
};

interface NotificationProviderProps {
  children: ReactNode;
}

export const NotificationProvider: React.FC<NotificationProviderProps> = ({ children }) => {
  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState('');
  const [type, setType] = useState<NotificationType>('info');
  const [duration, setDuration] = useState<number>(3000);

  const notify = (msg: string, t: NotificationType = 'info', durationMs?: number) => {
    setMessage(msg);
    setType(t);
    setDuration(durationMs ?? 3000);
    setOpen(true);
  };

  const handleClose = (_event?: React.SyntheticEvent | Event, reason?: string) => {
    if (reason === 'clickaway') return;
    setOpen(false);
  };

  return (
    <NotificationContext.Provider value={{ notify }}>
      {children}
      <Snackbar
        open={open}
        autoHideDuration={duration}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
        sx={{ zIndex: 9999 }}
      >
        <Alert 
          onClose={handleClose} 
          severity={type} 
          sx={{ 
            width: '100%',
            '& .MuiAlert-message': {
              width: '100%'
            }
          }}
          action={
            <Button 
              color="inherit" 
              size="small" 
              onClick={handleClose}
              sx={{ 
                minWidth: 'auto', 
                p: 0.5,
                fontSize: '1.2rem',
                fontWeight: 'bold',
                '&:hover': {
                  backgroundColor: 'rgba(255,255,255,0.1)'
                }
              }}
            >
              ✕
            </Button>
          }
        >
          {message}
        </Alert>
      </Snackbar>
    </NotificationContext.Provider>
  );
}; 