import React, { useState, useEffect, useRef } from 'react'
import {
  Container,
  Typography,
  Box,
  Paper,
  Grid,
  TextField,
  Button,
  Divider,
  Stepper,
  Step,
  StepLabel,
  Alert,
  CircularProgress,
  Autocomplete,
  Chip,
} from '@mui/material'
import { CheckCircle } from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { useCart } from '../contexts/CartContext'
import { useAuth } from '../contexts/AuthContext'
import { useNotification } from '../contexts/NotificationContext'
import { apiService } from '../services/api'
import PaymentMethodSelector from '../components/PaymentMethodSelector'
import VoucherSelector from '../components/VoucherSelector'
import axios from 'axios';
import { calculateTotalAmount, formatPrice } from '../utils/priceUtils'

const steps = ['Shipping Address', 'Payment & Voucher']

// Removed unused Province/Commune interfaces

const CheckoutPage: React.FC = () => {
  const navigate = useNavigate()
  const { cart, cartLoading } = useCart()
  const { user, isAuthenticated } = useAuth()
  const { notify } = useNotification()
  
  const [activeStep, setActiveStep] = useState(0)
  const [loading, setLoading] = useState(false)
  const [isProcessingOrder, setIsProcessingOrder] = useState(false)
  const processingRef = useRef(false)
  const navigatingToResultRef = useRef(false)
  const PENDING_REDIRECT_KEY = 'pending_payment_result_redirect'
  
  // Voucher states
  const [voucherCode, setVoucherCode] = useState('')
  const [appliedVoucher, setAppliedVoucher] = useState<any>(null)
  const [voucherLoading, setVoucherLoading] = useState(false)
  const [voucherSelectorOpen, setVoucherSelectorOpen] = useState(false)
  
  // Shipping data
  type GHNProvince = { ProvinceID: number; ProvinceName: string }
  type GHNDistrict = { DistrictID: number; DistrictName: string }
  type GHNWard = { WardCode: string; WardName: string }
  const [provinces, setProvinces] = useState<GHNProvince[]>([]);
  const [districts, setDistricts] = useState<GHNDistrict[]>([]);
  const [wards, setWards] = useState<GHNWard[]>([]);
  const [selectedProvince, setSelectedProvince] = useState<GHNProvince | null>(null);
  const [selectedDistrict, setSelectedDistrict] = useState<GHNDistrict | null>(null);
  const [selectedWard, setSelectedWard] = useState<GHNWard | null>(null);
  
  // Saved addresses (local)
  type SavedAddress = {
    id: string;
    label: string;
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    address: string;
    provinceId: string | null;
    communeCode: string | null;
    districtId: number | null;
    provinceName: string;
    communeName: string;
    districtName: string;
    note: string;
  };

  const [savedAddresses, setSavedAddresses] = useState<SavedAddress[]>(() => {
    try {
      const raw = localStorage.getItem('saved_addresses');
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  });
  const [selectedAddressId, setSelectedAddressId] = useState<string | null>(null);
  const persistAddresses = (list: SavedAddress[]) => {
    setSavedAddresses(list);
    try { localStorage.setItem('saved_addresses', JSON.stringify(list)); } catch {}
  };

  // Form states
  // Sửa state shippingAddress để lưu districtId và wardCode
  const [shippingAddress, setShippingAddress] = useState<{
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    address: string;
    provinceId: string | null;
    communeCode: string | null;
    districtId: number | null;
    provinceName: string;
    communeName: string;
    districtName: string;
    note: string;
  }>({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    address: '',
    provinceId: null,
    communeCode: null,
    districtId: null,
    provinceName: '',
    communeName: '',
    districtName: '',
    note: ''
  });
  
  const [paymentMethod, setPaymentMethod] = useState('credit_card')
  const [creditCardGateway, setCreditCardGateway] = useState('vnpay')
  const [selectedBank, setSelectedBank] = useState('vietcombank')
  const [orderNumber, setOrderNumber] = useState<string | null>(null)
  const [shippingFee, setShippingFee] = useState<number | null>(null);

  // Load provinces on component mount
  useEffect(() => {
    axios.get('/api/shipping/ghn/provinces')
      .then(res => {
        const list = Array.isArray(res?.data?.data) ? (res.data.data as GHNProvince[]) : []
        setProvinces(list)
      })
      .catch(() => setProvinces([]));
  }, []);

  // If there is a pending redirect (e.g., after order creation), immediately continue navigation
  useEffect(() => {
    const pending = localStorage.getItem(PENDING_REDIRECT_KEY)
    if (pending) {
      navigatingToResultRef.current = true
      window.location.href = pending
    }
  }, [])

  // Redirect if cart is empty (avoid redirect while processing order or when an ongoing order exists)
  useEffect(() => {
    if (isProcessingOrder || navigatingToResultRef.current) {
      return
    }
    const ongoingOrder = localStorage.getItem('ongoingOrder')
    if (ongoingOrder) {
      return
    }
    if (!cartLoading && (!cart || cart.items.length === 0)) {
      navigate('/cart')
    }
  }, [cart, cartLoading, navigate, isProcessingOrder])

  // Redirect to login if not authenticated
  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login')
    }
  }, [isAuthenticated, navigate])

  // Cleanup effect to reset processing state when component unmounts
  useEffect(() => {
    return () => {
      setIsProcessingOrder(false);
      setLoading(false);
      processingRef.current = false; // Reset ref flag
    };
  }, []);

  // Check if there's an ongoing order in localStorage
  useEffect(() => {
    const ongoingOrder = localStorage.getItem('ongoingOrder');
    if (ongoingOrder) {
      try {
        const orderData = JSON.parse(ongoingOrder);
        const orderTime = new Date(orderData.timestamp);
        const now = new Date();
        const timeDiff = now.getTime() - orderTime.getTime();
        
        // If order was created less than 5 minutes ago, show warning
        if (timeDiff < 5 * 60 * 1000) {
          // Silent check - no notification
        } else {
          // Clear old order data
          localStorage.removeItem('ongoingOrder');
        }
      } catch (error) {
        localStorage.removeItem('ongoingOrder');
      }
    }
  }, []);

  // Handle page unload to reset processing state
  useEffect(() => {
    const handleBeforeUnload = () => {
      if (processingRef.current) {
        // Don't allow page unload if order is being processed
        return 'Order is being processed. Are you sure you want to leave?';
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, []);

  // Check URL parameters for order status
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const orderStatus = urlParams.get('orderStatus');
    const orderNumber = urlParams.get('orderNumber');
    
    if (orderStatus === 'success' && orderNumber) {
      setOrderNumber(orderNumber);
      setActiveStep(steps.length);
      localStorage.removeItem('ongoingOrder'); // Clear ongoing order data
    } else if (orderStatus === 'failed') {
      localStorage.removeItem('ongoingOrder'); // Clear ongoing order data
    }
  }, []);

  // Update form when user data is loaded
  useEffect(() => {
    if (user) {
      // Prefill from last used saved address if available
      const last = savedAddresses[0];
      if (last) {
        setSelectedAddressId(last.id);
        setShippingAddress({
          firstName: last.firstName,
          lastName: last.lastName,
          email: last.email,
          phone: last.phone,
          address: last.address,
          provinceId: last.provinceId,
          communeCode: last.communeCode,
          districtId: last.districtId,
          provinceName: last.provinceName,
          communeName: last.communeName,
          districtName: last.districtName,
          note: last.note,
        });
      } else {
        setShippingAddress(prev => ({
          ...prev,
          firstName: user.firstName || '',
          lastName: user.lastName || '',
          email: user.email || ''
        }));
      }
    }
  }, [user, savedAddresses])

  // Sửa lại các hàm lấy tỉnh, huyện, xã/phường để gọi API GHN qua backend proxy
  // Ví dụ: /api/shipping/ghn/provinces, /api/shipping/ghn/districts?province_id=, /api/shipping/ghn/wards?district_id=

  // Load provinces on component mount
  useEffect(() => {
    axios.get('/api/shipping/ghn/provinces')
      .then(res => {
        const list = Array.isArray(res?.data?.data) ? res.data.data : []
        setProvinces(list)
      })
      .catch(() => setProvinces([]));
  }, []);

  const handleProvinceChange = (province: GHNProvince | null) => {
    setSelectedProvince(province);
    setSelectedDistrict(null);
    setSelectedWard(null);
    setDistricts([]);
    setWards([]);
    setShippingAddress(prev => ({
      ...prev,
      provinceId: province ? String(province.ProvinceID) : null,
      provinceName: province?.ProvinceName || '',
      districtId: null,
      districtName: '',
      communeCode: null,
      communeName: ''
    }));
    if (province) {
      axios.get(`/api/shipping/ghn/districts?province_id=${province.ProvinceID}`)
        .then(res => {
          const list = Array.isArray(res?.data?.data) ? (res.data.data as GHNDistrict[]) : []
          setDistricts(list)
        })
        .catch(() => setDistricts([]));
    }
  };

  const handleDistrictChange = (district: GHNDistrict | null) => {
    setSelectedDistrict(district);
    setSelectedWard(null);
    setWards([]);
    setShippingAddress(prev => ({
      ...prev,
      districtId: district?.DistrictID || null,
      districtName: district?.DistrictName || '',
      communeCode: null,
      communeName: ''
    }));
    if (district) {
      axios.get(`/api/shipping/ghn/wards?district_id=${district.DistrictID}`)
        .then(res => {
          const list = Array.isArray(res?.data?.data) ? (res.data.data as GHNWard[]) : []
          setWards(list)
        })
        .catch(() => setWards([]));
    }
  };

  const handleWardChange = (ward: GHNWard | null) => {
    setSelectedWard(ward);
    setShippingAddress(prev => ({
      ...prev,
      communeCode: ward?.WardCode || null,
      communeName: ward?.WardName || ''
    }));
  };

  // Saved address helpers
  const saveCurrentAddress = async (label?: string) => {
    const id = Date.now().toString();
    const entry: SavedAddress = {
      id,
      label: label || `${shippingAddress.address} - ${shippingAddress.communeName}`,
      firstName: shippingAddress.firstName,
      lastName: shippingAddress.lastName,
      email: shippingAddress.email,
      phone: shippingAddress.phone,
      address: shippingAddress.address,
      provinceId: shippingAddress.provinceId,
      communeCode: shippingAddress.communeCode,
      districtId: shippingAddress.districtId,
      provinceName: shippingAddress.provinceName,
      communeName: shippingAddress.communeName,
      districtName: shippingAddress.districtName,
      note: shippingAddress.note,
    };
    const next = [entry, ...savedAddresses.filter(a => a.id !== entry.id)].slice(0, 5);
    persistAddresses(next);
    setSelectedAddressId(entry.id);
    // Also sync selectors so user can Next ngay
    try { await syncSelectorsWithSaved(entry); } catch {}
  };

  const applySavedAddress = async (addr: SavedAddress) => {
    setSelectedAddressId(addr.id);
    setShippingAddress({
      firstName: addr.firstName,
      lastName: addr.lastName,
      email: addr.email,
      phone: addr.phone,
      address: addr.address,
      provinceId: addr.provinceId,
      communeCode: addr.communeCode,
      districtId: addr.districtId,
      provinceName: addr.provinceName,
      communeName: addr.communeName,
      districtName: addr.districtName,
      note: addr.note,
    });
    // Đồng bộ dropdowns tỉnh/huyện/xã theo địa chỉ đã lưu
    try { await syncSelectorsWithSaved(addr); } catch {}
  };

  const removeSavedAddress = (id: string) => {
    const next = savedAddresses.filter(a => a.id !== id);
    persistAddresses(next);
    if (selectedAddressId === id) setSelectedAddressId(next[0]?.id || null);
  };

  // Đồng bộ selectedProvince/selectedDistrict/selectedWard dựa trên địa chỉ đã lưu
  const syncSelectorsWithSaved = async (addr: SavedAddress) => {
    // Tìm province từ danh sách đã load
    const province = provinces.find((p: GHNProvince) => String(p.ProvinceID) === String(addr.provinceId) || p.ProvinceName === addr.provinceName);
    if (province) {
      handleProvinceChange(province);
      // Load districts for province
      try {
        const dRes = await axios.get(`/api/shipping/ghn/districts?province_id=${province.ProvinceID}`);
        const ds: GHNDistrict[] = Array.isArray(dRes?.data?.data) ? (dRes.data.data as GHNDistrict[]) : [];
        setDistricts(ds);
        const district = ds.find((d: GHNDistrict) => String(d.DistrictID) === String(addr.districtId) || d.DistrictName === addr.districtName);
        if (district) {
          handleDistrictChange(district);
          const wRes = await axios.get(`/api/shipping/ghn/wards?district_id=${district.DistrictID}`);
          const ws: GHNWard[] = Array.isArray(wRes?.data?.data) ? (wRes.data.data as GHNWard[]) : [];
          setWards(ws);
          const ward = ws.find((w: GHNWard) => String(w.WardCode) === String(addr.communeCode) || w.WardName === addr.communeName);
          if (ward) {
            handleWardChange(ward);
          }
        }
      } catch {}
    }
    
  };

  // Lấy thông tin warehouse để tính phí ship
  const fetchWarehouseForFirstProduct = async (productId: string) => {
    try {
      const res = await fetch(`http://localhost:8080/api/inventory/status/${productId}`);
      if (res.ok) {
        const data = await res.json();
        if (data && data.warehouse) {
          return {
            provinceCode: data.warehouse.provinceCode ? String(data.warehouse.provinceCode) : (data.warehouse.province ? String(data.warehouse.province) : ''),
            districtCode: data.warehouse.districtCode ? String(data.warehouse.districtCode) : (data.warehouse.districtId ? String(data.warehouse.districtId) : ''),
            wardCode: data.warehouse.wardCode ? String(data.warehouse.wardCode) : '',
          };
        }
      }
    } catch (e) {
      console.error('Error fetching warehouse info:', e);
    }
    // fallback HCM Q1
    return { provinceCode: '79', districtCode: '1454', wardCode: '20109' };
  };

  // Sửa calculateShippingFee để lấy districtCode từ selectedWard.parent_code nếu có
  const calculateShippingFee = async () => {
    // Lấy mã từ selectedProvince, selectedDistrict, selectedWard (GHN)
    const toProvinceId = selectedProvince?.ProvinceID;
    const toDistrictId = selectedDistrict?.DistrictID;
    const toWardCode = selectedWard?.WardCode;
    if (!toProvinceId || !toDistrictId || !toWardCode || !cart || !cart.items.length) {
      setShippingFee(null);
      return;
    }
    // Lấy thông tin kho gửi cho sản phẩm đầu tiên (địa chỉ kho gửi phải lấy từ DB, không lấy từ địa chỉ nhận)
    const firstProductId = cart.items[0].productId;
    const warehouse = await fetchWarehouseForFirstProduct(firstProductId);
    const fromProvinceId = warehouse.provinceCode;
    const fromDistrictId = warehouse.districtCode;
    const fromWardCode = warehouse.wardCode;
    try {
      const requestBody = {
        fromProvinceId,
        fromDistrictId,
        fromWardCode,
        toProvinceId,
        toDistrictId,
        toWardCode,
        weight: 1000
      };
      const response = await fetch('http://localhost:8080/api/shipping/calculate-fee-v2', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
      });
      if (response.ok) {
        const data = await response.json();
        if (data.fee !== undefined) {
          setShippingFee(data.fee > 0 ? data.fee : 30000); // Fallback to 30k if 0
        } else if (data.shippingFee !== undefined) {
          setShippingFee(data.shippingFee > 0 ? data.shippingFee : 30000); // Fallback to 30k if 0
        } else {
          setShippingFee(30000); // Default shipping fee
        }
      } else {
        setShippingFee(30000); // Default shipping fee on API error
      }
    } catch (error) {
      setShippingFee(30000); // Default shipping fee on error
    }
  };

  // Calculate total using centralized function
  const priceBreakdown = calculateTotalAmount(
    cart?.subtotal || 0,
    shippingFee || 0,
    appliedVoucher?.discountAmount || 0
  )

  // Voucher functions
  const handleApplyVoucher = async () => {
    if (!voucherCode.trim()) {
      notify('Vui lòng nhập mã voucher', 'warning')
      return
    }

    if (!user?.id || !cart) {
      notify('Vui lòng đăng nhập và có sản phẩm trong giỏ hàng', 'warning')
      return
    }

    try {
      setVoucherLoading(true)
      
      // Build validation request
      const validationRequest = {
        voucherCode: voucherCode.trim(),
        userId: String(user.id),
        orderAmount: cart.subtotal,
        items: cart.items.map(item => ({
          productId: item.productId,
          productName: item.productName,
          categoryId: 1, // TODO: Get from product service
          brandId: 1,     // TODO: Get from product service
          price: item.productPrice,
          quantity: item.quantity,
        }))
      }

      const response = await apiService.validateVoucher(validationRequest)
      
      if (response.success && (response.data as any)?.valid) {
        const data = response.data as any
        const discountAmount = typeof data.discountAmount === 'number' 
          ? data.discountAmount 
          : parseFloat(data.discountAmount || 0)
        
        setAppliedVoucher({
          code: data.voucherCode,
          id: data.voucherId,
          discountAmount: discountAmount,
          message: data.message || 'Voucher áp dụng thành công!'
        })
        notify(data.message || 'Voucher áp dụng thành công!', 'success')
        setVoucherCode('')
      } else {
        const errorMsg = (response.data as any)?.message || response.message || 'Mã voucher không hợp lệ'
        notify(errorMsg, 'error')
      }
    } catch (error: any) {
      console.error('Error applying voucher:', error)
      const errorMsg = error.response?.data?.message || error.message || 'Không thể áp dụng mã voucher'
      notify(errorMsg, 'error')
    } finally {
      setVoucherLoading(false)
    }
  }

  const handleRemoveVoucher = () => {
    setAppliedVoucher(null)
    setVoucherCode('')
    notify('Đã xóa voucher', 'info')
  }

  const handleSelectVoucherFromList = (selectedVoucherCode: string) => {
    setVoucherCode(selectedVoucherCode)
    // Auto apply the selected voucher
    setTimeout(() => {
      handleApplyVoucher()
    }, 100)
  }

  // Khi chọn tỉnh/thành phố, load xã/phường theo provinceCode


  // Tự động cập nhật phí shipping khi thay đổi địa chỉ giao hàng
  useEffect(() => {
    if (selectedProvince && selectedDistrict && selectedWard) {
      calculateShippingFee();
    } else {
      setShippingFee(null);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedProvince, selectedDistrict, selectedWard]);

  // Khi bấm Next ở bước Shipping Address, nếu shippingFee chưa xác định thì gọi lại calculateShippingFee trước khi sang bước tiếp theo
  const handleNext = async () => {
    if (activeStep === 1) {
      // Đảm bảo đã có phí ship trước khi sang bước tiếp
      if (shippingFee === null) {
        await calculateShippingFee();
        if (shippingFee === null) {
          return;
        }
      }
    }
    if (activeStep === steps.length - 1) {
      handlePlaceOrder();
    } else {
      setActiveStep((prevActiveStep) => prevActiveStep + 1);
    }
  };

  const handleBack = () => {
    // Prevent going back if order is being processed or has been completed
    if (processingRef.current || activeStep === steps.length) {
      return;
    }
    setActiveStep((prevActiveStep) => prevActiveStep - 1)
  }

  // Handle payment method change
  const handlePaymentMethodChange = (method: string) => {
    setPaymentMethod(method);
  };

  const handleGatewayChange = (gateway: string) => {
    setCreditCardGateway(gateway);
  };

  const handleBankChange = (bank: string) => {
    setSelectedBank(bank);
  };

  // Handle payment processing
  const handlePayment = async () => {
    if (!cart || !user) {
      return;
    }

    // Check authentication first
    if (!isAuthenticated) {
      notify('Please login to continue with payment', 'error');
      navigate('/login');
      return;
    }

    if (processingRef.current) {
      return;
    }

    try {
      processingRef.current = true; // Set ref flag
      setLoading(true);
      setIsProcessingOrder(true); // Set flag để prevent duplicate clicks

      // Add a small delay to prevent rapid clicks
      await new Promise(resolve => setTimeout(resolve, 200));

      // Double-check if still processing (in case of rapid clicks)
      if (!processingRef.current) {
        return;
      }

      // Set timeout for the entire operation
      const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error('Request timeout - please try again')), 30000); // 30 seconds timeout
      });

      // Create the main operation promise
      const operationPromise = (async () => {
        // Determine actual payment method
        let actualPaymentMethod = paymentMethod;

        if (paymentMethod === 'credit_card') {
          // Chỉ sử dụng VNPay cho thanh toán online
          actualPaymentMethod = 'VNPAY';
        } else {
          // Normalize other methods like 'cod' -> 'COD'
          actualPaymentMethod = (paymentMethod || '').toUpperCase();
        }
        
        // Payment method mapping for debugging

        // For VNPay, validate configuration
        if (actualPaymentMethod === 'VNPAY') {
          try {
            // Validate VNPay configuration
            const validationResponse = await apiService.get('/api/payments/vnpay/validate');

            if (!validationResponse.success) {
              console.error('VNPay validation failed:', validationResponse);
              notify('VNPay payment configuration: ' + (validationResponse.message || 'Unknown error'), 'info');
              // Don't return - allow payment to proceed in development mode
            }

            // VNPay validation successful or in development mode
          } catch (error: any) {
            console.error('VNPay configuration error:', error);
            notify('VNPay payment validation failed: ' + (error.message || 'Unknown error'), 'error');
            return;
          }
        }

        // Create order first
        const fullShippingAddress = [
          shippingAddress.address,
          shippingAddress.communeName,
          shippingAddress.districtName,
          shippingAddress.provinceName
            ].filter(Boolean).join(', ');

        const orderData = {
          // userId removed - will be obtained from JWT on backend
          orderItems: cart?.items?.map(item => ({
            productId: item.productId,
            productName: item.productName,
            productImage: item.productImage,
            productSku: item.productSku || '',
            quantity: item.quantity,
            unitPrice: item.productPrice
          })),
          shippingAddress: fullShippingAddress,
          paymentMethod: actualPaymentMethod,
          shippingFee: shippingFee,
          subtotal: cart?.subtotal || 0,
          taxAmount: priceBreakdown.taxAmount,
          voucherCode: appliedVoucher?.code || null,
          discountAmount: appliedVoucher?.discountAmount || 0,
          totalAmount: priceBreakdown.totalAmount,
          notes: shippingAddress.note
        };
        
        // Order data prepared for API
        console.log('Order data being sent:', orderData);
        console.log('Calculated total amount:', orderData.totalAmount);
        console.log('Applied voucher:', appliedVoucher);

        const orderResponse = await apiService.createOrder(orderData);

        // Order creation response received

        if (!orderResponse.success) {
          console.error('Order creation failed:', orderResponse);
          notify('Failed to create order: ' + (orderResponse.message || 'Unknown error'), 'error');
          return;
        }

        const orderId = orderResponse.data.id;
        // Order created successfully

        // Save order data to localStorage for recovery
        localStorage.setItem('ongoingOrder', JSON.stringify({
          orderId: orderId,
          orderNumber: orderResponse.data.orderNumber,
          timestamp: new Date().toISOString(),
          paymentMethod: actualPaymentMethod
        }));

        // Don't clear cart yet - wait for payment success
        // Cart will be cleared only after successful payment or order confirmation

                 // Process payment based on method
        
        if (actualPaymentMethod === 'VNPAY') {
          // Starting VNPay payment process
          // Call VNPay specific endpoint and then route to PaymentResult with redirectUrl
          const paymentResponse = await apiService.createVNPayPayment(orderId);
          
          if (paymentResponse.success) {
            const redirectUrl = (paymentResponse.data as { paymentUrl?: string }).paymentUrl;
            if (!redirectUrl) {
              notify('VNPay payment creation failed: missing redirect URL', 'error');
              localStorage.removeItem('ongoingOrder');
              return;
            }
            // VNPay redirect URL ready
            localStorage.removeItem('ongoingOrder');
            // DON'T clear cart here - only clear after successful payment confirmation
            // Cart will be cleared in PaymentResultPage after payment success
            navigatingToResultRef.current = true;
            window.location.href = redirectUrl;
          } else {
            console.error('VNPay payment creation failed:', paymentResponse);
            localStorage.removeItem('ongoingOrder'); // Clear ongoing order data
            notify('VNPay payment creation failed: ' + (paymentResponse.message || 'Unknown error'), 'error');
            // Payment failed - order will be rolled back by backend transaction
          }
        } else if (actualPaymentMethod === 'COD') {
          // COD - no payment needed, but wait for backend confirmation
          localStorage.removeItem('ongoingOrder'); // Clear ongoing order data

          // COD payment should be processed by backend, wait for response
          const target = `/payment-result?orderNumber=${orderResponse.data.orderNumber}&success=true&status=SUCCESS&message=Đặt hàng thành công&paymentMethod=COD`
          navigatingToResultRef.current = true;
          localStorage.setItem(PENDING_REDIRECT_KEY, target)

          try {
            window.location.href = target;
          } catch (error) {
            console.error('Redirect failed:', error);
            notify('Redirect failed, please check the order manually', 'warning');
          }
        }
      })();

      // Race between timeout and operation
      await Promise.race([operationPromise, timeoutPromise]);

    } catch (error) {
      console.error('Payment processing failed:', error);
      localStorage.removeItem('ongoingOrder');
    } finally {
      setLoading(false);
      setIsProcessingOrder(false); // Reset flag
      processingRef.current = false; // Reset ref flag
    }
  };

  // Sửa handlePlaceOrder để gửi shipping_address và billing_address là chuỗi địa chỉ đầy đủ
  const handlePlaceOrder = async () => {
    // This is now handled by handlePayment
    await handlePayment();
  }

  const isStepValid = (step: number) => {
    switch (step) {
      case 0:
        // Shipping Address validation
        const shippingValid = shippingAddress.firstName && shippingAddress.lastName && 
               shippingAddress.email && shippingAddress.address && 
               shippingAddress.provinceId && shippingAddress.provinceId !== '' &&
               shippingAddress.communeCode && shippingAddress.communeCode !== '';
        return shippingValid;
      case 1:
        // Payment Method validation
        const paymentValid = paymentMethod !== '';
        return paymentValid;
      default:
        return true
    }
  }

  // Show loading state
  if (cartLoading || !cart) {
    return (
      <Container maxWidth="lg" sx={{ py: 4, pt: '40px', textAlign: 'center' }}>
        <CircularProgress size={60} />
        <Typography variant="h6" sx={{ mt: 2 }}>
          Loading checkout...
        </Typography>
        <Box sx={{ mt: 2 }}>
          <Typography variant="body2" color="text.secondary">
            Debug Info: Cart Loading: {cartLoading ? 'Yes' : 'No'} | 
            Cart: {cart ? 'Loaded' : 'Not Loaded'} | 
            User: {user ? 'Authenticated' : 'Not Authenticated'}
          </Typography>
          <Button 
            variant="outlined" 
            size="small" 
            onClick={() => window.location.reload()}
            sx={{ mt: 1 }}
          >
            Force Refresh
          </Button>
        </Box>
      </Container>
    )
  }

  // Check if cart is empty
  if (!cart || cart.items.length === 0) {
    // If we're in the middle of redirecting to result page, show a lightweight spinner instead of empty-cart UI
    if (navigatingToResultRef.current || localStorage.getItem(PENDING_REDIRECT_KEY)) {
      return (
        <Container maxWidth="lg" sx={{ py: 4, pt: '40px', textAlign: 'center' }}>
          <CircularProgress size={60} />
          <Typography variant="h6" sx={{ mt: 2 }}>
            Đang chuyển hướng...
          </Typography>
        </Container>
      )
    }
    return (
      <Container maxWidth="lg" sx={{ py: 4, pt: '40px', textAlign: 'center' }}>
        <Typography variant="h4" gutterBottom>
          Your cart is empty
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
          Please add some items to your cart before proceeding to checkout.
        </Typography>
        <Button 
          variant="contained" 
          onClick={() => navigate('/products')}
          sx={{ mr: 2 }}
        >
          Continue Shopping
        </Button>
        <Button 
          variant="outlined" 
          onClick={() => navigate('/cart')}
        >
          View Cart
        </Button>
      </Container>
    )
  }

  // Order confirmation step
  if (activeStep === steps.length) {
    return (
      <Container maxWidth="md" sx={{ py: 4, pt: '40px' }}>
        <Paper sx={{ p: 4, textAlign: 'center', borderRadius: 3 }}>
          <CheckCircle sx={{ fontSize: 80, color: 'success.main', mb: 2 }} />
          <Typography variant="h4" gutterBottom fontWeight="bold">
            Order Confirmed!
          </Typography>
          <Typography variant="h6" color="text.secondary" sx={{ mb: 3 }}>
            Order Number: {orderNumber}
          </Typography>
          <Typography variant="body1" sx={{ mb: 3 }}>
            Thank you for your purchase! We'll send you an email confirmation with tracking details.
          </Typography>
          <Button
            variant="contained"
            size="large"
            onClick={() => navigate('/')}
            sx={{ mr: 2 }}
          >
            Continue Shopping
          </Button>
          <Button
            variant="outlined"
            size="large"
            onClick={() => navigate('/orders')}
          >
            View Orders
          </Button>
        </Paper>
      </Container>
    )
  }

  const renderStepContent = (step: number) => {
    switch (step) {
      case 0:
        return (
          <Box>
            <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              📍 Địa chỉ giao hàng
            </Typography>
            
            {savedAddresses.length > 0 && (
              <Box sx={{ mb: 3 }}>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1, fontWeight: 'bold' }}>
                  Địa chỉ đã lưu
                </Typography>
                <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                  {savedAddresses.map(a => (
                    <Chip 
                      key={a.id} 
                      label={a.label} 
                      color={selectedAddressId === a.id ? 'primary' : 'default'} 
                      onClick={() => applySavedAddress(a)} 
                      onDelete={() => removeSavedAddress(a.id)}
                      sx={{ mb: 1 }}
                    />
                  ))}
                  <Chip 
                    label="Thêm mới" 
                    variant="outlined" 
                    onClick={() => setSelectedAddressId(null)}
                    sx={{ mb: 1 }}
                  />
                </Box>
              </Box>
            )}
            
            <Box sx={{ 
              p: 3, 
              borderRadius: 2, 
              bgcolor: 'grey.50', 
              border: '1px solid',
              borderColor: 'grey.300'
            }}>
              <Grid container spacing={3}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                    label="Họ"
                  value={shippingAddress.firstName}
                  onChange={(e) => setShippingAddress(prev => ({ ...prev, firstName: e.target.value }))}
                  required
                    variant="outlined"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                    label="Tên"
                  value={shippingAddress.lastName}
                  onChange={(e) => setShippingAddress(prev => ({ ...prev, lastName: e.target.value }))}
                  required
                    variant="outlined"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Email"
                  type="email"
                  value={shippingAddress.email}
                  onChange={(e) => setShippingAddress(prev => ({ ...prev, email: e.target.value }))}
                  required
                    variant="outlined"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                    label="Số điện thoại"
                  value={shippingAddress.phone}
                  onChange={(e) => setShippingAddress(prev => ({ ...prev, phone: e.target.value }))}
                  required
                    variant="outlined"
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                    label="Địa chỉ chi tiết"
                  value={shippingAddress.address}
                  onChange={(e) => setShippingAddress(prev => ({ ...prev, address: e.target.value }))}
                  required
                    variant="outlined"
                    placeholder="Số nhà, tên đường, tên khu phố..."
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Autocomplete
                  options={provinces}
                  getOptionLabel={option => option.ProvinceName || ''}
                  value={selectedProvince}
                  onChange={(_e, value) => handleProvinceChange(value)}
                    renderInput={params => <TextField {...params} label="Tỉnh/Thành phố *" required variant="outlined" />}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Autocomplete
                  options={districts}
                  getOptionLabel={option => option.DistrictName || ''}
                  value={selectedDistrict}
                  onChange={(_e, value) => handleDistrictChange(value)}
                    renderInput={params => <TextField {...params} label="Quận/Huyện *" required variant="outlined" />}
                  disabled={!selectedProvince}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Autocomplete
                  options={wards}
                  getOptionLabel={option => option.WardName || ''}
                  value={selectedWard}
                  onChange={(_e, value) => handleWardChange(value)}
                    renderInput={params => <TextField {...params} label="Xã/Phường *" required variant="outlined" />}
                  disabled={!selectedDistrict}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                    label="Ghi chú (tùy chọn)"
                  value={shippingAddress.note}
                  onChange={(e) => setShippingAddress(prev => ({ ...prev, note: e.target.value }))}
                  multiline
                  minRows={2}
                    placeholder="Hướng dẫn giao hàng, thời gian nhận hàng..."
                    variant="outlined"
                />
              </Grid>
              <Grid item xs={12}>
                  <Button 
                    variant="outlined" 
                    onClick={() => saveCurrentAddress()}
                    sx={{ borderRadius: 2 }}
                  >
                    💾 Lưu địa chỉ này
                </Button>
              </Grid>
            </Grid>
            </Box>
            
            {shippingFee !== null && (
              <Alert severity="success" sx={{ mt: 2, borderRadius: 2 }}>
                <Typography variant="body2">
                  ✅ Phí vận chuyển: {formatPrice(shippingFee)}
                </Typography>
              </Alert>
            )}
          </Box>
        )

      case 1:
        return (
          <Box>
            {/* Voucher Section */}
            <Box sx={{ mb: 4 }}>
              <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                🎟️ Voucher
            </Typography>
              
              {appliedVoucher ? (
                <Box sx={{ 
                  p: 3, 
                  borderRadius: 2, 
                  bgcolor: 'success.light', 
                  border: '1px solid',
                  borderColor: 'success.main'
                }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Box>
                      <Typography variant="body1" fontWeight="bold" color="success.dark">
                        ✅ Voucher đã áp dụng: {appliedVoucher.code}
                  </Typography>
                      <Typography variant="body2" color="success.dark">
                        Giảm {formatPrice(appliedVoucher.discountAmount)}
                      </Typography>
            </Box>
                    <Button 
                      variant="outlined" 
                      size="small" 
                      onClick={handleRemoveVoucher}
                      sx={{ color: 'success.dark', borderColor: 'success.dark' }}
                    >
                      Xóa
                    </Button>
            </Box>
            </Box>
              ) : (
                <Box 
                  sx={{ 
                    p: 3, 
                    borderRadius: 2, 
                    bgcolor: 'grey.50', 
                    border: '2px dashed',
                    borderColor: 'grey.300',
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    '&:hover': {
                      borderColor: '#ff9800',
                      bgcolor: '#fff3e0',
                      transform: 'translateY(-2px)',
                      boxShadow: 2
                    }
                  }}
                  onClick={() => setVoucherSelectorOpen(true)}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Box sx={{ flex: 1 }}>
                      <Typography variant="body1" fontWeight="bold" sx={{ mb: 0.5 }}>
                        Chọn hoặc nhập mã giảm giá
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        Click để xem danh sách voucher có sẵn
                      </Typography>
                    </Box>
                    <Button
                      variant="outlined"
                      onClick={(e) => {
                        e.stopPropagation()
                        setVoucherSelectorOpen(true)
                      }}
                      sx={{ ml: 2 }}
                    >
                      Chọn voucher
                    </Button>
                  </Box>
                  
                  {/* Manual input option */}
                  <Box sx={{ mt: 2, pt: 2, borderTop: '1px solid', borderColor: 'divider' }}>
                    <Grid container spacing={2} alignItems="center">
                      <Grid item xs={12} sm={8}>
                        <TextField
                          fullWidth
                          size="small"
                          label="Hoặc nhập mã voucher"
                          value={voucherCode}
                          onChange={(e) => {
                            e.stopPropagation()
                            setVoucherCode(e.target.value)
                          }}
                          onClick={(e) => e.stopPropagation()}
                          placeholder="Ví dụ: SAVE10, WELCOME20"
                          disabled={voucherLoading}
                        />
                      </Grid>
                      <Grid item xs={12} sm={4}>
                        <Button
                          fullWidth
                          variant="contained"
                          size="small"
                          onClick={(e) => {
                            e.stopPropagation()
                            handleApplyVoucher()
                          }}
                          disabled={!voucherCode.trim() || voucherLoading}
                          sx={{ height: '40px' }}
                        >
                          {voucherLoading ? <CircularProgress size={20} /> : 'Áp dụng'}
                        </Button>
                      </Grid>
                    </Grid>
                  </Box>
                  
                  <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                    💡 Mã voucher có thể có điều kiện áp dụng. Vui lòng kiểm tra chi tiết trước khi sử dụng.
                  </Typography>
                </Box>
              )}
              
              {/* Voucher Selector Dialog */}
              <VoucherSelector
                open={voucherSelectorOpen}
                onClose={() => setVoucherSelectorOpen(false)}
                onSelectVoucher={handleSelectVoucherFromList}
                cartTotal={cart?.subtotal || 0}
                appliedVoucherCode={appliedVoucher?.code}
              />
          </Box>

            {/* Payment Method Section */}
            <Box>
              <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                💳 Phương thức thanh toán
              </Typography>
          <PaymentMethodSelector
            selectedMethod={paymentMethod}
            creditCardGateway={creditCardGateway}
            selectedBank={selectedBank}
            onMethodChange={handlePaymentMethodChange}
            onGatewayChange={handleGatewayChange}
            onBankChange={handleBankChange}
          />
            </Box>
          </Box>
        )

      default:
        return null
    }
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4, pt: '40px' }}>
      <Typography variant="h4" gutterBottom align="center" fontWeight="bold">
        Checkout
      </Typography>
      
      <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
        {steps.map((label, index) => (
          <Step key={label}>
            <StepLabel 
              sx={{
                '& .MuiStepLabel-label': {
                  fontSize: '0.9rem',
                  fontWeight: activeStep === index ? 'bold' : 'normal'
                }
              }}
            >
              {label}
            </StepLabel>
          </Step>
        ))}
      </Stepper>

      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 3, borderRadius: 3 }}>
            {renderStepContent(activeStep)}
            
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 4 }}>
              <Button
                disabled={activeStep === 0 || loading || processingRef.current}
                onClick={handleBack}
                variant="outlined"
                size="large"
                sx={{ 
                  borderRadius: 2,
                  px: 4,
                  py: 1.5,
                  fontSize: '1rem',
                  fontWeight: 'bold'
                }}
              >
                ← Quay lại
              </Button>
              <Button
                variant="contained"
                onClick={handleNext}
                disabled={!isStepValid(activeStep) || loading || processingRef.current}
                endIcon={loading ? <CircularProgress size={20} /> : null}
                size="large"
                sx={{ 
                  borderRadius: 2,
                  px: 4,
                  py: 1.5,
                  fontSize: '1rem',
                  fontWeight: 'bold',
                  background: activeStep === steps.length - 1 
                    ? 'linear-gradient(45deg, #FF6B35 30%, #F7931E 90%)'
                    : 'linear-gradient(45deg, #2196F3 30%, #21CBF3 90%)',
                  boxShadow: '0 4px 8px 3px rgba(33, 150, 243, .3)',
                  '&:hover': {
                    background: activeStep === steps.length - 1 
                      ? 'linear-gradient(45deg, #E55A2B 30%, #E8821A 90%)'
                      : 'linear-gradient(45deg, #1976D2 30%, #1CB5E0 90%)',
                    boxShadow: '0 8px 16px 6px rgba(33, 150, 243, .4)',
                    transform: 'translateY(-2px)'
                  },
                  transition: 'all 0.3s ease'
                }}
              >
                {activeStep === steps.length - 1 
                  ? (processingRef.current ? 'Đang xử lý...' : 'Đặt hàng') 
                  : 'Tiếp tục →'
                }
              </Button>
            </Box>
            
            {/* Show validation errors for step 0 */}
            {activeStep === 0 && !isStepValid(0) && (
              <Alert severity="warning" sx={{ mt: 2 }}>
                <Typography variant="body2">
                  Vui lòng điền đầy đủ thông tin địa chỉ giao hàng
                </Typography>
              </Alert>
            )}
            
            {/* Show validation errors for step 1 */}
            {activeStep === 1 && !isStepValid(1) && (
              <Alert severity="warning" sx={{ mt: 2 }}>
                <Typography variant="body2">
                  Vui lòng chọn phương thức thanh toán
                </Typography>
              </Alert>
            )}
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 3, borderRadius: 3, position: 'sticky', top: 20 }}>
            <Typography variant="h6" gutterBottom>
              Order Summary
            </Typography>
            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                {cart?.items?.length || 0} item(s)
              </Typography>
            </Box>
            <Divider sx={{ my: 2 }} />
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
              <Typography fontWeight="bold">Subtotal:</Typography>
              <Typography fontWeight="bold" sx={{ textAlign: 'right', minWidth: '120px' }}>
                {formatPrice(cart?.subtotal || 0)}
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
              <Typography fontWeight="bold">Shipping:</Typography>
              <Typography fontWeight="bold" sx={{ textAlign: 'right', minWidth: '120px' }}>
                {shippingFee !== null ? formatPrice(shippingFee) : 'Chưa xác định'}
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
              <Typography fontWeight="bold">Tax:</Typography>
              <Typography fontWeight="bold" sx={{ textAlign: 'right', minWidth: '120px' }}>
                {formatPrice(priceBreakdown.taxAmount)}
              </Typography>
            </Box>
            {appliedVoucher && (
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1, color: 'success.main' }}>
                <Typography fontWeight="bold">Voucher ({appliedVoucher.code}):</Typography>
                <Typography fontWeight="bold" sx={{ textAlign: 'right', minWidth: '120px' }}>
                  -{formatPrice(appliedVoucher.discountAmount)}
                </Typography>
              </Box>
            )}
            <Divider sx={{ my: 1 }} />
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="h6" fontWeight="bold">Total:</Typography>
              <Typography variant="h6" fontWeight="bold" sx={{ textAlign: 'right', minWidth: '120px' }}>
                {formatPrice(priceBreakdown.totalAmount)}
              </Typography>
            </Box>
          </Paper>
        </Grid>
      </Grid>
    </Container>
  )
}

export default CheckoutPage 