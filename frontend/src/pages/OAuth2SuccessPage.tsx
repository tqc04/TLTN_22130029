import { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { apiService } from '../services/api';

const OAuth2SuccessPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [status, setStatus] = useState<string>('Đang xử lý đăng nhập...');

  useEffect(() => {
    const handleOAuth2Success = async () => {
      const params = new URLSearchParams(location.search);
      const token = params.get('token');
      const error = params.get('error');
      const provider = params.get('provider');

      // Handle error case
      if (error) {
        setStatus(`Lỗi đăng nhập: ${decodeURIComponent(error)}`);
        setTimeout(() => {
          navigate('/login');
        }, 3000);
        return;
      }

      // Handle missing token
      if (!token) {
        setStatus('Không tìm thấy token. Đang chuyển hướng...');
        setTimeout(() => {
          navigate('/login');
        }, 2000);
        return;
      }

      try {
        // Save token first
        localStorage.setItem('jwt', token);
        localStorage.setItem('auth_token', token);

        // Decode JWT to get email/username for getUserProfile
        // JWT format: header.payload.signature
        const payload = JSON.parse(atob(token.split('.')[1]));
        const identifier = payload.sub || payload.email || payload.username;

        if (!identifier) {
          throw new Error('Không thể lấy thông tin người dùng từ token');
        }

        setStatus('Đang tải thông tin người dùng...');

        // Fetch user profile
        const response = await apiService.getUserProfile();
        
        if (response.success && response.data) {
          const userWithRoles = {
            ...response.data,
            roles: response.data.role ? [response.data.role] : [],
            isActive: true,
          };

          // Save user to localStorage
          localStorage.setItem('auth_user', JSON.stringify(userWithRoles));

          setStatus('Đăng nhập thành công! Đang chuyển hướng...');

          // Reload page to ensure AuthContext re-initializes with the new user
          // This is necessary because initializeAuth() only runs once on mount
          setTimeout(() => {
            window.location.href = '/';
          }, 500);
        } else {
          throw new Error('Không thể lấy thông tin người dùng');
        }
      } catch (error) {
        console.error('OAuth2 login error:', error);
        setStatus(`Lỗi: ${error instanceof Error ? error.message : 'Đăng nhập thất bại'}`);
        
        // Clear invalid token
        localStorage.removeItem('auth_token');
        localStorage.removeItem('jwt');
        localStorage.removeItem('auth_user');

        setTimeout(() => {
          navigate('/login');
        }, 3000);
      }
    };

    handleOAuth2Success();
  }, [location, navigate]);

  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      minHeight: '100vh',
      flexDirection: 'column',
      gap: '20px'
    }}>
      <div style={{ fontSize: '18px', color: '#666' }}>{status}</div>
    </div>
  );
};

export default OAuth2SuccessPage; 