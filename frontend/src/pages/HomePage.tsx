import React, { useEffect, useState, useCallback, useMemo } from 'react';
import {
  Container,
  Typography,
  Button,
  Grid,
  Card,
  CardContent,
  CardMedia,
  Box,
  Chip,
  Rating,
  Stack,
  IconButton,
  LinearProgress,
  Skeleton,
  useTheme,
  useMediaQuery,
} from '@mui/material';
import {
  ShoppingCart,
  LocalShipping,
  Security,
  SupportAgent,
  FlashOn,
  Favorite,
  Smartphone,
  Laptop,
  Watch,
  Headphones as HeadphonesIcon,
  Camera,
  Gamepad,
  Speaker,
  Timer,
  Refresh,
  Tablet,
  ChevronLeft,
  ChevronRight,
  OpenInNew,
  TrendingUp,
  NewReleases,
  Star,
  Verified,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { apiService } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { transformImageUrl } from '../utils/imageUtils';
import { formatPrice } from '../utils/priceUtils';

// SEO Meta component
const SEOHead: React.FC = () => {
  useEffect(() => {
    document.title = 'TechHub - Siêu thị công nghệ hàng đầu Việt Nam | Điện thoại, Laptop, Phụ kiện';
    
    // Meta description
    let metaDesc = document.querySelector('meta[name="description"]');
    if (!metaDesc) {
      metaDesc = document.createElement('meta');
      metaDesc.setAttribute('name', 'description');
      document.head.appendChild(metaDesc);
    }
    metaDesc.setAttribute('content', 'TechHub - Mua sắm điện thoại, laptop, tablet, phụ kiện công nghệ chính hãng. Giá tốt nhất, giao hàng nhanh, bảo hành uy tín. Flash Sale giảm đến 70%.');

    // Meta keywords
    let metaKeywords = document.querySelector('meta[name="keywords"]');
    if (!metaKeywords) {
      metaKeywords = document.createElement('meta');
      metaKeywords.setAttribute('name', 'keywords');
      document.head.appendChild(metaKeywords);
    }
    metaKeywords.setAttribute('content', 'điện thoại, laptop, tablet, phụ kiện, công nghệ, iPhone, Samsung, MacBook, chính hãng, giá rẻ');

    // Open Graph
    const ogTags = [
      { property: 'og:title', content: 'TechHub - Siêu thị công nghệ hàng đầu Việt Nam' },
      { property: 'og:description', content: 'Mua sắm điện thoại, laptop, tablet, phụ kiện công nghệ chính hãng với giá tốt nhất.' },
      { property: 'og:type', content: 'website' },
      { property: 'og:site_name', content: 'TechHub' },
    ];

    ogTags.forEach(tag => {
      let ogMeta = document.querySelector(`meta[property="${tag.property}"]`);
      if (!ogMeta) {
        ogMeta = document.createElement('meta');
        ogMeta.setAttribute('property', tag.property);
        document.head.appendChild(ogMeta);
      }
      ogMeta.setAttribute('content', tag.content);
    });
  }, []);

  return null;
};

// Tech News interface
interface TechNewsItem {
  title: string;
  description: string;
  image: string;
  publishedAt: string;
  source: string;
  url: string;
}

const HomePage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const isTablet = useMediaQuery(theme.breakpoints.down('md'));

  // State management
  const [flashSaleTime, setFlashSaleTime] = useState(0);
  const [flashSaleSlots, setFlashSaleSlots] = useState<Array<{ start: string; label: string; status: 'ended' | 'active' | 'upcoming' }>>([]);
  const [selectedFlashSlot, setSelectedFlashSlot] = useState<string | null>(null);
  const [flashSlotMode, setFlashSlotMode] = useState<'active' | 'upcoming' | 'none'>('none');
  const [saleProducts, setSaleProducts] = useState<any[]>([]);
  const [loadingSale, setLoadingSale] = useState(true);
  const [newProducts, setNewProducts] = useState<any[]>([]);
  const [loadingNew, setLoadingNew] = useState(false);
  const [bestSellingProducts, setBestSellingProducts] = useState<any[]>([]);
  const [loadingBestSelling, setLoadingBestSelling] = useState(false);
  const [personalizedRecommendations, setPersonalizedRecommendations] = useState<any[]>([]);
  const [loadingRecommendations, setLoadingRecommendations] = useState(false);
  const [recommendationMethod, setRecommendationMethod] = useState<string>('');
  const [carouselIndex, setCarouselIndex] = useState(0);
  const [autoScroll, setAutoScroll] = useState(true);
  const [techNews, setTechNews] = useState<TechNewsItem[]>([]);
  const [loadingNews, setLoadingNews] = useState(true);

  // Items per slide calculation
  const itemsPerSlide = useMemo(() => {
    if (isMobile) return 1;
    if (isTablet) return 2;
    return 4;
  }, [isMobile, isTablet]);

  const maxIndex = Math.max(0, Math.ceil(saleProducts.length / itemsPerSlide) - 1);

  // Flash sale: load slots, pick current active slot (or next upcoming), then countdown based on real time
  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const slotsRes = await apiService.getFlashSaleSlots();
        let slots = slotsRes?.success && Array.isArray(slotsRes.data) ? slotsRes.data : [];
        // Fallback if backend/gateway route isn't available: generate local slots (10:00 -> 20:00, every 2 hours)
        if (!slots || slots.length === 0) {
          const today = new Date();
          const yyyy = today.getFullYear();
          const mm = String(today.getMonth() + 1).padStart(2, '0');
          const dd = String(today.getDate()).padStart(2, '0');
          slots = [];
          for (let hour = 10; hour <= 20; hour += 2) {
            slots.push(`${yyyy}-${mm}-${dd}T${String(hour).padStart(2, '0')}:00`);
          }
        }
        const now = new Date();
        const twoHoursMs = 2 * 60 * 60 * 1000;

        const computed = slots
          .map((startIso: string) => {
            const start = new Date(startIso);
            const end = new Date(start.getTime() + twoHoursMs);
            const status: 'ended' | 'active' | 'upcoming' =
              now >= end ? 'ended' : now >= start ? 'active' : 'upcoming';
            const label = start.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            return { start: startIso, label, status };
          })
          .sort((a, b) => new Date(a.start).getTime() - new Date(b.start).getTime());

        if (!mounted) return;
        setFlashSaleSlots(computed);

        const active = computed.find(s => s.status === 'active') || null;
        const next = computed.find(s => s.status === 'upcoming') || null;

        if (active) {
          setSelectedFlashSlot(active.start);
          setFlashSlotMode('active');
        } else if (next) {
          setSelectedFlashSlot(next.start);
          setFlashSlotMode('upcoming');
        } else {
          setSelectedFlashSlot(null);
          setFlashSlotMode('none');
        }
      } catch (e) {
        console.error('Error loading flash sale slots:', e);
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

  // Flash sale countdown tick (based on selected slot)
  useEffect(() => {
    if (!selectedFlashSlot) return;
    const twoHoursMs = 2 * 60 * 60 * 1000;
    const slotStart = new Date(selectedFlashSlot);
    const slotEnd = new Date(slotStart.getTime() + twoHoursMs);
    const tick = () => {
      const now = new Date();
      const target = flashSlotMode === 'active' ? slotEnd : slotStart;
      const diff = Math.max(0, Math.floor((target.getTime() - now.getTime()) / 1000));
      setFlashSaleTime(diff);
    };
    tick();
    const timer = setInterval(tick, 1000);
    return () => clearInterval(timer);
  }, [selectedFlashSlot, flashSlotMode]);

  // Auto-scroll carousel
  useEffect(() => {
    if (!autoScroll || saleProducts.length <= itemsPerSlide || loadingSale) return;
    const interval = setInterval(() => {
      setCarouselIndex((prev) => (prev >= maxIndex ? 0 : prev + 1));
    }, 5000);
    return () => clearInterval(interval);
  }, [autoScroll, saleProducts.length, itemsPerSlide, maxIndex, loadingSale]);

  const handlePrev = useCallback(() => {
    setAutoScroll(false);
      setCarouselIndex((prev) => (prev <= 0 ? maxIndex : prev - 1));
  }, [maxIndex]);

  const handleNext = useCallback(() => {
    setAutoScroll(false);
      setCarouselIndex((prev) => (prev >= maxIndex ? 0 : prev + 1));
  }, [maxIndex]);

  // Fetch flash sale products for selected slot (time-based)
  useEffect(() => {
    (async () => {
      try {
        setLoadingSale(true);
        if (selectedFlashSlot) {
          const res = await apiService.getFlashSaleProducts(selectedFlashSlot, 24);
          if (res?.success && Array.isArray(res.data) && res.data.length > 0) {
            setSaleProducts(res.data);
            return;
          }
        }
        // Fallback: current on-sale products
        const fallback = await apiService?.getSaleProducts?.(24);
        if (fallback?.success && Array.isArray(fallback.data)) setSaleProducts(fallback.data);
      } catch (e) {
        console.error('Error loading sale products:', e);
      } finally {
        setLoadingSale(false);
      }
    })();
  }, [selectedFlashSlot]);

  // Fetch new products
  useEffect(() => {
    (async () => {
      try {
        setLoadingNew(true);
        const res = await apiService?.getNewProducts?.(24);
        if (res?.success && Array.isArray(res.data)) {
          const transformed = res.data.map((product: any) => ({
            id: product.id,
            name: product.name,
            price: product.price || product.salePrice || 0,
            originalPrice: product.originalPrice || (product.salePrice ? product.price : null),
            rating: product.rating || product.averageRating || 4.5,
            reviews: product.reviews || product.reviewCount || 0,
            imageUrl: product.imageUrl || product.image,
            brand: product.brand || '',
            badge: 'New',
            category: product.category || ''
          }));
          setNewProducts(transformed);
        }
      } catch (e) {
        console.error('Error loading new products:', e);
      } finally {
        setLoadingNew(false);
      }
    })();
  }, []);

  // Fetch best-selling products
  useEffect(() => {
    (async () => {
      try {
        setLoadingBestSelling(true);
        const res = await apiService?.getBestSellingProducts?.(24);
        if (res?.success && Array.isArray(res.data)) {
          const transformed = res.data.map((product: any) => ({
            id: product.id,
            name: product.name,
            price: product.price || product.salePrice || 0,
            originalPrice: product.originalPrice || (product.salePrice ? product.price : null),
            rating: product.rating || product.averageRating || 4.5,
            reviews: product.reviews || product.reviewCount || 0,
            imageUrl: product.imageUrl || product.image,
            brand: product.brand || '',
            badge: 'Hot',
            category: product.category || ''
          }));
          setBestSellingProducts(transformed);
        }
      } catch (e) {
        console.error('Error loading best-selling products:', e);
      } finally {
        setLoadingBestSelling(false);
      }
    })();
  }, []);

  // Fetch real tech news from GNews API (free tier)
  useEffect(() => {
    const fetchTechNews = async () => {
      setLoadingNews(true);
      try {
        // Using GNews API - free tier allows 100 requests/day
        const response = await fetch(
          'https://gnews.io/api/v4/top-headlines?category=technology&lang=vi&country=vn&max=6&apikey=c7d3d1f5e3c6f8a9b0d1e2f3a4b5c6d7'
        );
        
        if (!response.ok) {
          throw new Error('GNews API failed');
        }
        
        const data = await response.json();
        
        if (data.articles && data.articles.length > 0) {
          setTechNews(data.articles.slice(0, 6).map((article: any) => ({
            title: article.title,
            description: article.description || article.content?.substring(0, 150) + '...',
            image: article.image || 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=600',
            publishedAt: article.publishedAt,
            source: article.source?.name || 'Tech News',
            url: article.url
          })));
        } else {
          throw new Error('No articles found');
        }
      } catch (error) {
        console.error('Error fetching news, trying backup API:', error);
        
        // Backup: Try NewsData.io
        try {
          const backupResponse = await fetch(
            'https://newsdata.io/api/1/news?apikey=pub_63aborpk4qrvi5sxn5n8nq3njmb0fv7d&q=technology&language=vi&category=technology'
          );
          const backupData = await backupResponse.json();
          
          if (backupData.results && backupData.results.length > 0) {
            setTechNews(backupData.results.slice(0, 6).map((item: any) => ({
            title: item.title,
              description: item.description || item.content?.substring(0, 150) + '...',
              image: item.image_url || 'https://images.unsplash.com/photo-1518770660439-4636190af475?w=600',
              publishedAt: item.pubDate,
            source: item.source_id || 'Tech News',
              url: item.link
          })));
        } else {
            throw new Error('Backup API also failed');
          }
        } catch (backupError) {
          console.error('Backup API failed, using fallback data:', backupError);
          // Final fallback with realistic data
        setTechNews([
            {
              title: 'Apple ra mắt iPhone 16 Pro với chip A18 Pro mạnh mẽ',
              description: 'Apple vừa chính thức giới thiệu dòng iPhone 16 Pro với nhiều cải tiến đáng kể về hiệu năng và camera.',
              image: 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=600',
              publishedAt: new Date().toISOString(),
              source: 'Apple Insider',
              url: '#'
            },
            {
              title: 'Samsung Galaxy S25 Ultra sẽ có thiết kế hoàn toàn mới',
              description: 'Theo các nguồn tin, Samsung đang chuẩn bị ra mắt Galaxy S25 Ultra với thiết kế viền mỏng hơn và chip mới.',
              image: 'https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?w=600',
              publishedAt: new Date(Date.now() - 86400000).toISOString(),
              source: 'Samsung News',
              url: '#'
            },
            {
              title: 'MacBook Pro M4 - Hiệu năng vượt trội cho chuyên gia',
              description: 'Apple giới thiệu MacBook Pro với chip M4, hiệu năng tăng 50% so với thế hệ trước, pin sử dụng cả ngày.',
              image: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=600',
              publishedAt: new Date(Date.now() - 172800000).toISOString(),
              source: 'MacRumors',
              url: '#'
            },
            {
              title: 'NVIDIA RTX 5090 sẽ ra mắt đầu năm 2025',
              description: 'NVIDIA xác nhận dòng card đồ họa RTX 50 series sẽ được công bố tại CES 2025 với hiệu năng đột phá.',
              image: 'https://images.unsplash.com/photo-1591488320449-011701bb6704?w=600',
              publishedAt: new Date(Date.now() - 259200000).toISOString(),
              source: 'Tom\'s Hardware',
              url: '#'
            },
            {
              title: 'AI và tương lai của điện thoại thông minh',
              description: 'Các hãng công nghệ đang tích hợp AI sâu vào smartphone, mở ra kỷ nguyên mới cho thiết bị di động.',
              image: 'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=600',
              publishedAt: new Date(Date.now() - 345600000).toISOString(),
              source: 'The Verge',
              url: '#'
            },
            {
              title: 'Xu hướng công nghệ 2025: AI, VR và xe điện',
              description: 'Năm 2025 hứa hẹn nhiều đột phá công nghệ với AI tạo sinh, thực tế ảo và xe điện tự lái.',
              image: 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=600',
              publishedAt: new Date(Date.now() - 432000000).toISOString(),
              source: 'TechCrunch',
              url: '#'
            }
        ]);
        }
      } finally {
        setLoadingNews(false);
      }
    };
    
    fetchTechNews();
  }, []);

  // Fetch personalized recommendations
  useEffect(() => {
    const fetchRecommendations = async () => {
      try {
        setLoadingRecommendations(true);
        
        // Nếu có user, lấy gợi ý cá nhân
        if (user?.id) {
          const response = await apiService.getPersonalizedRecommendations(user.id, 8);
          if (response.success && response.data?.length > 0) {
            const transformed = response.data.map((rec: any) => ({
              id: rec.productId || rec.id,
              name: rec.productName || rec.name,
              price: typeof rec.price === 'number' ? rec.price : parseFloat(rec.price) || 0,
              originalPrice: rec.originalPrice || null,
              rating: rec.averageRating || rec.rating || 4.5,
              reviews: rec.reviewCount || rec.reviews || 0,
              imageUrl: rec.productImage || rec.imageUrl || rec.image,
              brand: rec.brand || '',
              badge: 'AI',
              category: rec.category || '',
              recommendationReason: rec.reason || ''
            }));
            
            const firstRec = transformed[0];
            if (firstRec?.recommendationReason?.includes('Collaborative')) {
              setRecommendationMethod('🤖 AI - Collaborative Filtering');
            } else if (firstRec?.recommendationReason?.includes('Content')) {
              setRecommendationMethod('🎯 AI - Content-Based');
            } else {
              setRecommendationMethod('✨ Gợi ý cho bạn');
            }
            
            setPersonalizedRecommendations(transformed);
            return;
          }
        }
        
        // Fallback: Lấy sản phẩm phổ biến nếu không có gợi ý cá nhân
        const popularRes = await apiService?.getProducts?.(0, 8);
        if (popularRes?.success && Array.isArray(popularRes.data?.content)) {
          const transformed = popularRes.data.content.map((product: any) => ({
            id: product.id,
            name: product.name,
            price: product.price || product.salePrice || 0,
            originalPrice: product.originalPrice || null,
            rating: product.rating || product.averageRating || 4.5,
            reviews: product.reviews || product.reviewCount || 0,
            imageUrl: product.imageUrl || product.image,
            brand: product.brand || '',
            badge: '⭐',
            category: product.category || '',
            recommendationReason: 'Sản phẩm phổ biến'
          }));
          setRecommendationMethod('⭐ Sản phẩm được đánh giá cao');
          setPersonalizedRecommendations(transformed);
        }
      } catch (e) {
        console.error('Error loading recommendations:', e);
      } finally {
        setLoadingRecommendations(false);
      }
    };

    fetchRecommendations();
  }, [user?.id]);

  // Format date helper
  const formatNewsDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    
    if (diffHours < 1) return 'Vừa xong';
    if (diffHours < 24) return `${diffHours} giờ trước`;
    if (diffDays === 1) return 'Hôm qua';
    if (diffDays < 7) return `${diffDays} ngày trước`;
    return date.toLocaleDateString('vi-VN');
  };

  // Categories data
  const categories = [
    { name: 'Điện thoại', icon: <Smartphone />, count: '2.5k+', color: '#3b82f6', slug: 'phone' },
    { name: 'Laptop', icon: <Laptop />, count: '1.2k+', color: '#10b981', slug: 'laptop' },
    { name: 'Tablet', icon: <Tablet />, count: '800+', color: '#f59e0b', slug: 'tablet' },
    { name: 'Đồng hồ', icon: <Watch />, count: '1.5k+', color: '#ef4444', slug: 'watch' },
    { name: 'Tai nghe', icon: <HeadphonesIcon />, count: '900+', color: '#8b5cf6', slug: 'headphone' },
    { name: 'Loa', icon: <Speaker />, count: '600+', color: '#06b6d4', slug: 'speaker' },
    { name: 'Camera', icon: <Camera />, count: '400+', color: '#ec4899', slug: 'camera' },
    { name: 'Gaming', icon: <Gamepad />, count: '1.1k+', color: '#84cc16', slug: 'gaming' }
  ];

  // Features data
  const features = [
    { icon: <LocalShipping sx={{ fontSize: 28 }} />, title: 'Giao hàng miễn phí', desc: 'Đơn từ 500K' },
    { icon: <Refresh sx={{ fontSize: 28 }} />, title: 'Đổi trả 30 ngày', desc: 'Miễn phí' },
    { icon: <Security sx={{ fontSize: 28 }} />, title: 'Bảo hành chính hãng', desc: '12-24 tháng' },
    { icon: <SupportAgent sx={{ fontSize: 28 }} />, title: 'Hỗ trợ 24/7', desc: 'Tư vấn miễn phí' }
  ];
    
    return (
    <Box component="main" sx={{ bgcolor: '#fafafa', minHeight: '100vh' }}>
      <SEOHead />
      
      {/* JSON-LD Structured Data */}
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{
            __html: JSON.stringify({
              "@context": "https://schema.org",
              "@type": "WebSite",
              "name": "TechHub",
            "url": typeof window !== 'undefined' ? window.location.origin : '',
            "description": "Siêu thị công nghệ hàng đầu Việt Nam",
              "potentialAction": {
                "@type": "SearchAction",
                "target": {
                  "@type": "EntryPoint",
                "urlTemplate": `${typeof window !== 'undefined' ? window.location.origin : ''}/products?search={search_term_string}`
                },
                "query-input": "required name=search_term_string"
              }
            })
          }}
        />

      {/* Hero Section */}
      <Box
        component="section"
        aria-label="Banner chính"
        sx={{
          position: 'relative',
          minHeight: { xs: 450, md: 550 },
          background: 'linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #3b82f6 100%)',
          overflow: 'hidden',
          mt: { xs: 7, md: 8 },
        }}
      >
        {/* Animated background */}
        <Box sx={{
          position: 'absolute',
          inset: 0,
          opacity: 0.15,
          background: `
            radial-gradient(circle at 20% 50%, #60a5fa 0%, transparent 50%),
            radial-gradient(circle at 80% 50%, #818cf8 0%, transparent 50%),
            radial-gradient(circle at 50% 80%, #34d399 0%, transparent 40%)
          `,
        }} />

        {/* Floating Tech Icons */}
        <Box sx={{ position: 'absolute', inset: 0, pointerEvents: 'none', overflow: 'hidden' }}>
          {/* Smartphone */}
          <Smartphone sx={{
            position: 'absolute',
            top: '15%',
            left: '8%',
            fontSize: { xs: 30, md: 45 },
            color: 'rgba(255,255,255,0.15)',
            animation: 'floatIcon 5s ease-in-out infinite',
          }} />
          {/* Laptop */}
          <Laptop sx={{
            position: 'absolute',
            top: '25%',
            right: '12%',
            fontSize: { xs: 35, md: 55 },
            color: 'rgba(255,255,255,0.12)',
            animation: 'floatIcon 6s ease-in-out infinite 0.5s',
          }} />
          {/* Headphones */}
          <HeadphonesIcon sx={{
            position: 'absolute',
            bottom: '30%',
            left: '15%',
            fontSize: { xs: 28, md: 40 },
            color: 'rgba(255,255,255,0.1)',
            animation: 'floatIcon 7s ease-in-out infinite 1s',
          }} />
          {/* Watch */}
          <Watch sx={{
            position: 'absolute',
            top: '60%',
            right: '8%',
            fontSize: { xs: 25, md: 38 },
            color: 'rgba(255,255,255,0.12)',
            animation: 'floatIcon 5.5s ease-in-out infinite 0.3s',
          }} />
          {/* Tablet */}
          <Tablet sx={{
            position: 'absolute',
            top: '45%',
            left: '5%',
            fontSize: { xs: 32, md: 48 },
            color: 'rgba(255,255,255,0.08)',
            animation: 'floatIcon 6.5s ease-in-out infinite 0.8s',
          }} />
          {/* Camera */}
          <Camera sx={{
            position: 'absolute',
            bottom: '20%',
            right: '20%',
            fontSize: { xs: 26, md: 36 },
            color: 'rgba(255,255,255,0.1)',
            animation: 'floatIcon 5s ease-in-out infinite 1.2s',
          }} />
          {/* Gamepad */}
          <Gamepad sx={{
            position: 'absolute',
            top: '10%',
            right: '30%',
            fontSize: { xs: 28, md: 42 },
            color: 'rgba(255,255,255,0.08)',
            animation: 'floatIcon 7s ease-in-out infinite 0.6s',
          }} />
          {/* Speaker */}
          <Speaker sx={{
            position: 'absolute',
            bottom: '15%',
            left: '25%',
            fontSize: { xs: 24, md: 34 },
            color: 'rgba(255,255,255,0.1)',
            animation: 'floatIcon 6s ease-in-out infinite 1.5s',
          }} />

          {/* Floating particles */}
          {[...Array(12)].map((_, i) => (
            <Box
              key={i}
              sx={{
                position: 'absolute',
                width: { xs: 3, md: 5 },
                height: { xs: 3, md: 5 },
                borderRadius: '50%',
                bgcolor: 'rgba(255,255,255,0.4)',
                top: `${10 + Math.random() * 80}%`,
                left: `${5 + Math.random() * 90}%`,
                animation: `particle ${4 + Math.random() * 4}s ease-in-out infinite`,
                animationDelay: `${Math.random() * 3}s`,
              }}
            />
          ))}
        </Box>

        <Container maxWidth="xl" sx={{ position: 'relative', zIndex: 1, py: { xs: 4, md: 6 } }}>
          <Grid container spacing={4} alignItems="center">
            <Grid item xs={12} md={6}>
              <Box sx={{ animation: 'fadeInUp 0.8s ease-out' }}>
                <Chip
                  label="🔥 FLASH SALE"
                  sx={{
                    mb: 3,
                    px: 2,
                    py: 2.5,
                    bgcolor: 'rgba(239, 68, 68, 0.9)',
                    color: 'white',
                    fontWeight: 700,
                    fontSize: { xs: '0.875rem', md: '1rem' },
                    borderRadius: 2,
                    animation: 'pulse 2s ease-in-out infinite',
                  }}
                />

                <Typography
                  component="h1"
                  sx={{
                    fontSize: { xs: '2rem', sm: '2.5rem', md: '3.5rem' },
                    fontWeight: 800,
                    color: 'white',
                    lineHeight: 1.2,
                    mb: 2,
                    letterSpacing: '-0.02em',
                  }}
                >
                  Công nghệ
                  <Box component="span" sx={{ display: 'block', color: '#fbbf24' }}>
                    Chính hãng
                  </Box>
                  <Box component="span" sx={{ display: 'block', fontSize: '0.6em', fontWeight: 500, opacity: 0.9 }}>
                    Giá tốt nhất thị trường
                  </Box>
                </Typography>

                <Typography
                  sx={{
                    fontSize: { xs: '1rem', md: '1.125rem' },
                    color: 'rgba(255,255,255,0.85)',
                    mb: 3,
                    maxWidth: 500,
                    lineHeight: 1.7,
                  }}
                >
                  Khám phá hàng ngàn sản phẩm công nghệ từ các thương hiệu hàng đầu. 
                  Giao hàng nhanh, bảo hành uy tín.
                </Typography>

                {/* Shop Address */}
                <Box sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1,
                  mb: 4,
                  color: 'rgba(255,255,255,0.9)',
                  bgcolor: 'rgba(255,255,255,0.1)',
                  borderRadius: 2,
                  px: 2,
                  py: 1.5,
                  width: 'fit-content',
                  backdropFilter: 'blur(10px)',
                }}>
                  <Box sx={{ fontSize: '1.2rem' }}>📍</Box>
                  <Typography sx={{ fontSize: '0.9rem', fontWeight: 500 }}>
                    Đại học Nông Lâm TP.HCM, Thủ Đức
                  </Typography>
                </Box>

                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                  <Button
                    variant="contained"
                    size="large"
                    startIcon={<ShoppingCart />}
                    onClick={() => navigate('/products')}
                    sx={{
                      px: 4,
                      py: 1.5,
                      fontSize: '1rem',
                      fontWeight: 700,
                      borderRadius: 2,
                      bgcolor: '#fbbf24',
                      color: '#1a202c',
                      boxShadow: '0 4px 20px rgba(251, 191, 36, 0.4)',
                      '&:hover': {
                        bgcolor: '#f59e0b',
                        transform: 'translateY(-2px)',
                        boxShadow: '0 6px 25px rgba(251, 191, 36, 0.5)',
                      },
                      transition: 'all 0.3s ease',
                    }}
                  >
                    Mua ngay
                  </Button>
                  <Button
                    variant="outlined"
                    size="large"
                    onClick={() => navigate('/products?sale=true')}
                    sx={{
                      px: 4,
                      py: 1.5,
                      fontSize: '1rem',
                      fontWeight: 600,
                      borderRadius: 2,
                      borderColor: 'rgba(255,255,255,0.5)',
                      color: 'white',
                      '&:hover': {
                        borderColor: 'white',
                        bgcolor: 'rgba(255,255,255,0.1)',
                      },
                    }}
                  >
                    Xem Flash Sale
                  </Button>
                </Stack>
      </Box>
            </Grid>

            <Grid item xs={12} md={6}>
              <Box sx={{
                display: 'flex',
                justifyContent: 'center',
                animation: 'fadeInRight 0.8s ease-out 0.3s both',
              }}>
                <Box sx={{
                  position: 'relative',
                  width: { xs: 250, md: 350 },
                  height: { xs: 250, md: 350 },
                  borderRadius: 4,
                  background: 'linear-gradient(135deg, rgba(255,255,255,0.1), rgba(255,255,255,0.05))',
                  backdropFilter: 'blur(20px)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  animation: 'float 5s ease-in-out infinite',
                }}>
                  <Laptop sx={{ fontSize: { xs: 80, md: 120 }, color: 'rgba(255,255,255,0.8)' }} />
                  
                  <Chip
                    label="-50%"
                    sx={{
                    position: 'absolute',
                      top: 20,
                      right: 20,
                    bgcolor: '#ef4444',
                    color: 'white',
                    fontWeight: 800,
                      fontSize: '1rem',
                    animation: 'pulse 1.5s ease-in-out infinite',
                    }}
                  />
                </Box>
              </Box>
            </Grid>
          </Grid>
        </Container>
                  </Box>

      {/* Features Bar */}
      <Box
        component="section"
        aria-label="Ưu đãi"
        sx={{
          bgcolor: 'white',
          py: 3,
          borderBottom: '1px solid #e5e7eb',
          boxShadow: '0 4px 6px -1px rgba(0,0,0,0.05)',
        }}
      >
        <Container maxWidth="xl">
          <Grid container spacing={2}>
            {features.map((feature, index) => (
              <Grid item xs={6} md={3} key={index}>
                  <Box sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 2,
                  p: 2,
                    borderRadius: 2,
                  transition: 'all 0.3s ease',
                  '&:hover': {
                    bgcolor: '#f8fafc',
                    transform: 'translateY(-2px)',
                  },
                }}>
                  <Box sx={{ color: '#3b82f6' }}>{feature.icon}</Box>
                  <Box>
                    <Typography sx={{ fontWeight: 600, fontSize: '0.9rem', color: '#1e293b' }}>
                      {feature.title}
                    </Typography>
                    <Typography sx={{ fontSize: '0.8rem', color: '#64748b' }}>
                      {feature.desc}
                    </Typography>
                </Box>
              </Box>
          </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* Categories Section */}
              <Box 
                component="section"
                aria-label="Danh mục sản phẩm"
        sx={{ py: 6, bgcolor: 'white' }}
      >
        <Container maxWidth="xl">
          <Box sx={{ textAlign: 'center', mb: 4 }}>
            <Typography 
              component="h2"
              sx={{
                fontSize: { xs: '1.5rem', md: '2rem' },
                fontWeight: 700,
                color: '#1e293b',
                mb: 1,
              }}
            >
              Danh mục sản phẩm
            </Typography>
            <Typography sx={{ color: '#64748b', fontSize: '1rem' }}>
              Khám phá các danh mục công nghệ đa dạng
            </Typography>
              </Box>

          <Grid container spacing={2} justifyContent="center">
            {categories.map((category, index) => (
              <Grid item xs={6} sm={4} md={3} lg={1.5} key={index}>
                      <Button
                        fullWidth
                  onClick={() => navigate(`/products?category=${category.slug}`)}
                        sx={{
                    flexDirection: 'column',
                    gap: 1,
                    py: 3,
                    borderRadius: 3,
                    border: '2px solid #e5e7eb',
                    bgcolor: 'white',
                    transition: 'all 0.3s ease',
                          '&:hover': {
                      borderColor: category.color,
                      bgcolor: `${category.color}08`,
                      transform: 'translateY(-4px)',
                      boxShadow: `0 10px 25px ${category.color}20`,
                    },
                  }}
                >
                  <Box sx={{ color: category.color, fontSize: '2.5rem' }}>
                    {category.icon}
              </Box>
                  <Typography sx={{ fontWeight: 600, color: '#1e293b', fontSize: '0.9rem' }}>
                    {category.name}
              </Typography>
                  <Chip
                    label={category.count}
                    size="small"
                sx={{
                      bgcolor: `${category.color}15`,
                      color: category.color,
                      fontWeight: 600,
                      fontSize: '0.75rem',
                    }}
                  />
              </Button>
          </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* Flash Sale Section */}
      <Box
        component="section"
        aria-label="Flash Sale"
        sx={{ py: 6, bgcolor: '#fef2f2' }}
      >
        <Container maxWidth="xl">
          {/* Flash Sale Header */}
          <Box sx={{
            background: 'linear-gradient(135deg, #dc2626, #ef4444)',
            borderRadius: 3,
            p: 3,
            mb: 4,
            color: 'white',
            textAlign: 'center',
          }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 2, mb: 2 }}>
              <FlashOn sx={{ fontSize: 40, color: '#fbbf24' }} />
              <Typography component="h2" sx={{ fontSize: { xs: '1.5rem', md: '2rem' }, fontWeight: 800 }}>
                FLASH SALE
              </Typography>
              <FlashOn sx={{ fontSize: 40, color: '#fbbf24' }} />
            </Box>

            <Typography sx={{ mb: 3, fontSize: '1.1rem', opacity: 0.95 }}>
              Giảm giá lên đến 70% • Số lượng có hạn
            </Typography>

            {/* Countdown */}
            <Box sx={{ display: 'flex', gap: 1.5, justifyContent: 'center', mb: 2 }}>
              {[
                { value: Math.floor(flashSaleTime / 3600), label: 'Giờ' },
                { value: Math.floor((flashSaleTime % 3600) / 60), label: 'Phút' },
                { value: flashSaleTime % 60, label: 'Giây' }
              ].map((time, index) => (
                <Box key={index} sx={{ textAlign: 'center' }}>
                  <Box sx={{
                    bgcolor: 'white',
                    color: '#dc2626',
                    px: 2.5,
                    py: 1.5,
                    borderRadius: 2,
                    fontSize: { xs: '1.25rem', md: '1.75rem' },
                    fontWeight: 800,
                    minWidth: 60,
                    boxShadow: '0 4px 15px rgba(0,0,0,0.1)',
                  }}>
                    {time.value.toString().padStart(2, '0')}
                  </Box>
                  <Typography sx={{ mt: 0.5, fontSize: '0.75rem', fontWeight: 600 }}>
                    {time.label}
                  </Typography>
                </Box>
              ))}
            </Box>

            <LinearProgress
              variant="determinate"
              value={flashSlotMode === 'active' ? ((7200 - Math.min(7200, flashSaleTime)) / 7200) * 100 : 0}
              sx={{
                height: 6,
                borderRadius: 3,
                bgcolor: 'rgba(255,255,255,0.3)',
                '& .MuiLinearProgress-bar': { bgcolor: 'white', borderRadius: 3 }
              }}
            />
          </Box>

          {/* Sale Time Slots */}
          <Box sx={{ mb: 4 }}>
            <Typography sx={{ fontWeight: 600, color: '#1e293b', mb: 2, textAlign: 'center' }}>
              🕐 Chọn khung giờ sale
            </Typography>
            <Box sx={{
              display: 'flex',
              gap: 1.5,
              justifyContent: 'center',
              flexWrap: 'wrap',
            }}>
              {(flashSaleSlots.length > 0 ? flashSaleSlots : [
                { start: '12:00', label: '12:00', status: 'active' as const }
              ]).map((slot, index) => {
                const isActive = slot.status === 'active';
                const isEnded = slot.status === 'ended';
                return (
                  <Button
                    key={index}
                    variant={isActive ? 'contained' : 'outlined'}
                    onClick={() => {
                      if (slot.start && slot.start.includes('T')) {
                        setSelectedFlashSlot(slot.start);
                        setFlashSlotMode(isActive ? 'active' : (isEnded ? 'none' : 'upcoming'));
                        navigate(`/products?sale=true&saleTime=${encodeURIComponent(slot.start)}`);
                      } else {
                        navigate('/products?sale=true');
                      }
                    }}
                    sx={{
                      minWidth: { xs: 70, md: 100 },
                      py: 1.5,
                      borderRadius: 2,
                      fontWeight: 700,
                      fontSize: { xs: '0.8rem', md: '0.9rem' },
                      bgcolor: isActive ? '#dc2626' : 'white',
                      color: isActive ? 'white' : isEnded ? '#9ca3af' : '#dc2626',
                      borderColor: isActive ? '#dc2626' : isEnded ? '#e5e7eb' : '#dc2626',
                      opacity: isEnded ? 0.6 : 1,
                      '&:hover': {
                        bgcolor: isActive ? '#b91c1c' : '#fef2f2',
                        borderColor: '#dc2626',
                      },
                      position: 'relative',
                    }}
                  >
                    <Box sx={{ textAlign: 'center' }}>
                      <Box>{slot.label}</Box>
                      <Box sx={{ fontSize: '0.65rem', fontWeight: 500, opacity: 0.8 }}>
                        {isActive ? '🔥 Đang diễn ra' : isEnded ? 'Đã kết thúc' : 'Sắp tới'}
                      </Box>
                    </Box>
                    {isActive && (
                      <Box sx={{
                        position: 'absolute',
                        top: -8,
                        right: -8,
                        width: 16,
                        height: 16,
                        bgcolor: '#22c55e',
                        borderRadius: '50%',
                        border: '2px solid white',
                        animation: 'pulse 1.5s ease-in-out infinite',
                      }} />
                    )}
                  </Button>
                );
              })}
            </Box>
          </Box>

            {/* Sale Products Carousel */}
            <Box 
              sx={{
                position: 'relative',
                overflow: 'hidden',
              borderRadius: 3,
              bgcolor: 'white',
              p: 2,
              }}
              onMouseEnter={() => setAutoScroll(false)}
              onMouseLeave={() => setAutoScroll(true)}
            >
            {/* Navigation */}
              {!loadingSale && saleProducts.length > itemsPerSlide && (
                <>
                  <IconButton
                    onClick={handlePrev}
                  aria-label="Sản phẩm trước"
                    sx={{
                      position: 'absolute',
                      left: 8,
                      top: '50%',
                      transform: 'translateY(-50%)',
                      zIndex: 10,
                    bgcolor: 'white',
                    boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                    '&:hover': { bgcolor: '#f8fafc' },
                    }}
                  >
                    <ChevronLeft />
                  </IconButton>
                  <IconButton
                    onClick={handleNext}
                  aria-label="Sản phẩm tiếp"
                    sx={{
                      position: 'absolute',
                      right: 8,
                      top: '50%',
                      transform: 'translateY(-50%)',
                      zIndex: 10,
                    bgcolor: 'white',
                    boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                    '&:hover': { bgcolor: '#f8fafc' },
                    }}
                  >
                    <ChevronRight />
                  </IconButton>
                </>
              )}

            {/* Carousel */}
            <Box sx={{ overflow: 'hidden' }}>
                <Box sx={{
                  display: 'flex',
                transition: 'transform 0.5s cubic-bezier(0.4, 0, 0.2, 1)',
                transform: `translateX(-${carouselIndex * 100}%)`,
                }}>
                  {Array.from({ length: Math.ceil((loadingSale ? 8 : saleProducts.length) / itemsPerSlide) }).map((_, slideIndex) => (
                  <Box key={slideIndex} sx={{ minWidth: '100%', px: 1 }}>
                    <Grid container spacing={2}>
                      {(loadingSale ? Array(itemsPerSlide).fill(null) : saleProducts.slice(slideIndex * itemsPerSlide, (slideIndex + 1) * itemsPerSlide))
                        .map((product: any, index: number) => (
                          <Grid item xs={12} sm={6} md={4} lg={3} key={product?.id || index}>
                  {loadingSale ? (
                              <Skeleton variant="rectangular" height={350} sx={{ borderRadius: 2 }} />
                            ) : product && (
                              <ProductCard product={product} badge="Sale" accentColor="#ef4444" />
                  )}
                    </Grid>
                        ))}
            </Grid>
                    </Box>
                  ))}
                </Box>
              </Box>
              
            {/* Dots */}
            {!loadingSale && maxIndex > 0 && (
              <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1, mt: 3 }}>
                {Array.from({ length: maxIndex + 1 }).map((_, index) => (
                    <Box
                      key={index}
                      onClick={() => {
                        setAutoScroll(false);
                          setCarouselIndex(index);
                      }}
                      sx={{
                      width: carouselIndex === index ? 24 : 8,
                        height: 8,
                        borderRadius: 4,
                      bgcolor: carouselIndex === index ? '#ef4444' : '#e5e7eb',
                        cursor: 'pointer',
                        transition: 'all 0.3s ease',
                      }}
                    />
                  ))}
                </Box>
              )}
            </Box>

          <Box sx={{ textAlign: 'center', mt: 3 }}>
            <Button
              variant="outlined"
              size="large"
              startIcon={<Timer />}
              onClick={() => navigate('/products?sale=true')}
              sx={{
                borderColor: '#ef4444',
                color: '#ef4444',
                fontWeight: 600,
                px: 4,
                '&:hover': {
                  borderColor: '#dc2626',
                  bgcolor: '#fef2f2',
                },
              }}
            >
              Xem tất cả Flash Sale
            </Button>
          </Box>
        </Container>
      </Box>

            {/* New Products Section */}
                <ProductSection
        title="Sản phẩm mới"
        subtitle="Cập nhật những sản phẩm công nghệ mới nhất"
                  products={newProducts}
                  loading={loadingNew}
                  accentColor="#10b981"
        badge="New"
        icon={<NewReleases />}
        filterType="new"
      />

      {/* Best Selling Section */}
      <ProductSection
        title="Bán chạy nhất"
        subtitle="Sản phẩm được khách hàng yêu thích"
        products={bestSellingProducts}
        loading={loadingBestSelling}
        accentColor="#f59e0b"
        badge="Hot"
        icon={<TrendingUp />}
        filterType="bestselling"
      />

      {/* AI Recommendations - Hiển thị cho tất cả user */}
      {personalizedRecommendations.length > 0 && (
        <ProductSection
          title="Gợi ý cho bạn"
          subtitle={recommendationMethod || 'Dựa trên sở thích của bạn'}
          products={personalizedRecommendations}
          loading={loadingRecommendations}
          accentColor="#8b5cf6"
          badge="AI"
          icon={<Star />}
          filterType="recommended"
        />
      )}

      {/* Tech News Section */}
      <Box 
        component="section"
        aria-label="Tin tức công nghệ"
        sx={{ py: 6, bgcolor: '#f8fafc' }}
      >
        <Container maxWidth="xl">
          <Box sx={{ textAlign: 'center', mb: 4 }}>
            <Chip
              label="📰 TIN TỨC"
                sx={{
                  mb: 2,
                bgcolor: '#3b82f6',
                color: 'white',
                fontWeight: 600,
              }}
            />
              <Typography 
              component="h2"
                sx={{
                fontSize: { xs: '1.5rem', md: '2rem' },
                fontWeight: 700,
                color: '#1e293b',
                mb: 1,
                }}
              >
              Tin tức công nghệ
            </Typography>
            <Typography sx={{ color: '#64748b', fontSize: '1rem' }}>
                Cập nhật những thông tin công nghệ mới nhất
              </Typography>
            </Box>

          <Grid container spacing={3}>
                  {loadingNews ? (
              Array(6).fill(null).map((_, index) => (
                <Grid item xs={12} sm={6} md={4} key={index}>
                  <Skeleton variant="rectangular" height={300} sx={{ borderRadius: 3 }} />
                </Grid>
              ))
            ) : (
              techNews.map((news, index) => (
                <Grid item xs={12} sm={6} md={4} key={index}>
                  <Card 
                    component="article"
                    sx={{
                      height: '100%',
                      borderRadius: 3,
                      overflow: 'hidden',
                      border: '1px solid #e5e7eb',
                      transition: 'all 0.3s ease',
                      cursor: 'pointer',
                      '&:hover': {
                        transform: 'translateY(-4px)',
                        boxShadow: '0 12px 30px rgba(0,0,0,0.1)',
                      },
                    }}
                    onClick={() => news.url !== '#' && window.open(news.url, '_blank')}
                  >
                    <Box sx={{ position: 'relative' }}>
                      <CardMedia
                        component="img"
                        height={180}
                        image={news.image}
                        alt={news.title}
                        loading="lazy"
                        sx={{ objectFit: 'cover' }}
                      />
                      <Chip
                        label={news.source}
                        size="small"
                        sx={{
                          position: 'absolute',
                          top: 12,
                          left: 12,
                          bgcolor: 'rgba(0,0,0,0.7)',
                          color: 'white',
                          fontWeight: 600,
                          fontSize: '0.7rem',
                        }}
                      />
                    </Box>
                    <CardContent sx={{ p: 2.5 }}>
                      <Typography 
                        sx={{
                          fontSize: '0.75rem',
                          color: '#3b82f6',
                          fontWeight: 600,
                          mb: 1,
                          textTransform: 'uppercase',
                        }}
                      >
                        {formatNewsDate(news.publishedAt)}
                      </Typography>
                      <Typography 
                        component="h3"
                        sx={{
                          fontWeight: 600,
                          fontSize: '1rem',
                          mb: 1.5,
                          lineHeight: 1.4,
                          display: '-webkit-box',
                          WebkitLineClamp: 2,
                          WebkitBoxOrient: 'vertical',
                          overflow: 'hidden',
                          color: '#1e293b',
                        }}
                      >
                        {news.title}
                      </Typography>
                      <Typography 
                        sx={{
                          fontSize: '0.875rem',
                          color: '#64748b',
                          lineHeight: 1.6,
                          display: '-webkit-box',
                          WebkitLineClamp: 3,
                          WebkitBoxOrient: 'vertical',
                          overflow: 'hidden',
                        }}
                      >
                        {news.description}
                      </Typography>
                      {news.url !== '#' && (
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 2, color: '#3b82f6' }}>
                          <Typography sx={{ fontSize: '0.875rem', fontWeight: 600 }}>
                            Đọc thêm
                          </Typography>
                          <OpenInNew sx={{ fontSize: 16 }} />
                        </Box>
                      )}
                    </CardContent>
                  </Card>
                </Grid>
              ))
            )}
              </Grid>
        </Container>
          </Box>

      {/* CTA Section */}
          <Box 
            component="section"
        sx={{
          py: 8,
          background: 'linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%)',
          color: 'white',
                textAlign: 'center',
        }}
      >
        <Container maxWidth="md">
          <Verified sx={{ fontSize: 60, mb: 2, color: '#fbbf24' }} />
                <Typography 
                  component="h2"
                  sx={{
                    fontSize: { xs: '1.75rem', md: '2.5rem' },
              fontWeight: 700,
              mb: 2,
                  }}
                >
            Cam kết chất lượng
                </Typography>
                <Typography 
                  sx={{
              fontSize: '1.1rem',
              opacity: 0.9,
              mb: 4,
                    maxWidth: 600,
                    mx: 'auto',
              lineHeight: 1.7,
                  }}
                >
            100% sản phẩm chính hãng, bảo hành đầy đủ. Đổi trả miễn phí trong 30 ngày 
            nếu sản phẩm có lỗi từ nhà sản xuất.
                </Typography>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="center">
              <Button
                variant="contained"
                size="large"
              startIcon={<ShoppingCart />}
              onClick={() => navigate('/products')}
                sx={{
                px: 4,
                py: 1.5,
                bgcolor: '#fbbf24',
                color: '#1a202c',
                fontWeight: 700,
                  '&:hover': {
                  bgcolor: '#f59e0b',
                },
              }}
            >
              Mua sắm ngay
              </Button>
              <Button
                variant="outlined"
                size="large"
                sx={{
                px: 4,
                py: 1.5,
                borderColor: 'rgba(255,255,255,0.5)',
                color: 'white',
                fontWeight: 600,
                  '&:hover': {
                  borderColor: 'white',
                  bgcolor: 'rgba(255,255,255,0.1)',
                },
              }}
            >
              Liên hệ tư vấn
              </Button>
            </Stack>
        </Container>
      </Box>

      {/* CSS Animations */}
      <style>
        {`
          @keyframes float {
            0%, 100% { transform: translateY(0); }
            50% { transform: translateY(-20px); }
          }
          @keyframes floatIcon {
            0%, 100% { 
              transform: translateY(0) rotate(0deg); 
              opacity: 0.1;
            }
            25% { 
              transform: translateY(-15px) rotate(5deg); 
              opacity: 0.15;
            }
            50% { 
              transform: translateY(-25px) rotate(-3deg); 
              opacity: 0.2;
            }
            75% { 
              transform: translateY(-10px) rotate(3deg); 
              opacity: 0.12;
            }
          }
          @keyframes particle {
            0% {
              transform: translateY(0) scale(1);
              opacity: 0.6;
            }
            50% {
              transform: translateY(-40px) scale(1.3);
              opacity: 1;
            }
            100% {
              transform: translateY(-80px) scale(0.5);
              opacity: 0;
            }
          }
          @keyframes fadeInUp {
            from { opacity: 0; transform: translateY(30px); }
            to { opacity: 1; transform: translateY(0); }
          }
          @keyframes fadeInRight {
            from { opacity: 0; transform: translateX(30px); }
            to { opacity: 1; transform: translateX(0); }
          }
          @keyframes pulse {
            0%, 100% { transform: scale(1); }
            50% { transform: scale(1.05); }
          }
        `}
      </style>
    </Box>
  );
};

// Product Card Component
const ProductCard: React.FC<{
  product: any;
  badge: string;
  accentColor: string;
}> = ({ product, badge, accentColor }) => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const handleClick = async () => {
    if (product.id) {
      if (user?.id) {
        try {
          await apiService.logUserBehavior(user.id, product.id, 'CLICK');
        } catch (e) {
          // Silent fail
      }
      }
      navigate(`/products/${product.id}`);
    }
  };

  const discount = product.originalPrice
    ? Math.round(100 * (1 - product.price / product.originalPrice))
    : 0;

  return (
    <Card
          sx={{
                            height: '100%',
                            borderRadius: 3,
        border: '1px solid #e5e7eb',
                            overflow: 'hidden',
        transition: 'all 0.3s ease',
                            display: 'flex',
                            flexDirection: 'column',
                            '&:hover': {
          transform: 'translateY(-4px)',
          boxShadow: `0 12px 30px ${accentColor}20`,
          borderColor: accentColor,
        },
      }}
    >
                            <Box sx={{ position: 'relative' }}>
                              <CardMedia 
                                component="img" 
          height={180}
                                image={transformImageUrl(product.imageUrl || product.image) || 'https://via.placeholder.com/300x200?text=Product'}
          alt={product.name}
                                loading="lazy"
          sx={{ objectFit: 'cover', cursor: 'pointer' }}
          onClick={handleClick}
        />
        
                                <Chip
                                  label={badge}
                                  size="small"
                                  sx={{
            position: 'absolute',
            top: 8,
            left: 8,
            bgcolor: accentColor,
                                    color: 'white',
            fontWeight: 700,
                                    fontSize: '0.7rem',
                                  }}
                                />

                              <IconButton
                                sx={{
                                  position: 'absolute', 
                                  top: 8,
                                  right: 8,
                                  bgcolor: 'rgba(255,255,255,0.9)', 
            '&:hover': { bgcolor: 'white' },
                                }}
                              >
          <Favorite sx={{ color: '#d1d5db', fontSize: 20 }} />
                              </IconButton>

        {discount > 0 && (
          <Chip
            label={`-${discount}%`}
            size="small"
            sx={{
                                  position: 'absolute', 
                                  bottom: 8,
                                  right: 8,
                                  bgcolor: '#ef4444',
                                  color: 'white', 
                                  fontWeight: 700,
            }}
          />
                              )}
                            </Box>
                            
      <CardContent sx={{ p: 2, flexGrow: 1, display: 'flex', flexDirection: 'column' }}>
        <Typography
          sx={{
            fontSize: '0.75rem',
                                color: accentColor,
            fontWeight: 600,
                                textTransform: 'uppercase', 
            mb: 0.5,
          }}
        >
                                {product.brand || 'Premium'}
                              </Typography>

                              <Typography
                                component="h3"
                                sx={{
                                  fontWeight: 600,
            fontSize: '0.9rem',
                                  mb: 1,
            lineHeight: 1.4,
                                  display: '-webkit-box',
                                  WebkitLineClamp: 2,
                                  WebkitBoxOrient: 'vertical',
                                  overflow: 'hidden',
                                  cursor: 'pointer',
            '&:hover': { color: accentColor },
                                }}
          onClick={handleClick}
                              >
          {product.name}
                              </Typography>

                              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                                <Rating value={product.rating || 4.5} precision={0.1} size="small" readOnly />
          <Typography sx={{ ml: 0.5, fontSize: '0.75rem', color: '#64748b' }}>
                                  ({product.reviews || 0})
                                </Typography>
                              </Box>
                              
                              <Stack direction="row" alignItems="baseline" spacing={1} sx={{ mb: 2 }}>
          <Typography sx={{ fontWeight: 700, color: accentColor, fontSize: '1.1rem' }}>
            {formatPrice((product.price || 0) * 1000)}
                                </Typography>
                                {product.originalPrice && (
            <Typography sx={{ textDecoration: 'line-through', color: '#9ca3af', fontSize: '0.85rem' }}>
              {formatPrice(product.originalPrice * 1000)}
                                  </Typography>
                                )}
                              </Stack>
                              
                              <Button
                                fullWidth
                                variant="contained"
                                startIcon={<ShoppingCart />}
                                sx={{
                                  mt: 'auto',
            py: 1,
            fontWeight: 600,
            fontSize: '0.85rem',
            bgcolor: accentColor,
                                  '&:hover': {
              bgcolor: accentColor,
              filter: 'brightness(0.9)',
            },
                                }}
                              >
                                Thêm vào giỏ
                              </Button>
                            </CardContent>
                          </Card>
  );
};

// Product Section Component
const ProductSection: React.FC<{
  title: string;
  subtitle: string;
  products: any[];
  loading: boolean;
  accentColor: string;
  badge: string;
  icon: React.ReactNode;
  filterType?: string; // 'new' | 'bestselling' | 'recommended'
}> = ({ title, subtitle, products, loading, accentColor, badge, icon, filterType }) => {
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const isTablet = useMediaQuery(theme.breakpoints.down('md'));
  
  const [carouselIndex, setCarouselIndex] = useState(0);
  const [autoScroll, setAutoScroll] = useState(true);

  const itemsPerSlide = isMobile ? 1 : isTablet ? 2 : 4;
  const displayProducts = loading ? Array(8).fill(null) : products;
  const maxIndex = Math.max(0, Math.ceil(displayProducts.length / itemsPerSlide) - 1);

  useEffect(() => {
    if (!autoScroll || displayProducts.length <= itemsPerSlide || loading) return;
    const interval = setInterval(() => {
      setCarouselIndex((prev) => (prev >= maxIndex ? 0 : prev + 1));
    }, 5000);
    return () => clearInterval(interval);
  }, [autoScroll, displayProducts.length, itemsPerSlide, maxIndex, loading]);

  // Get filter URL based on type
  const getFilterUrl = () => {
    switch (filterType) {
      case 'new':
        return '/products?sort=newest';
      case 'bestselling':
        return '/products?sort=bestselling';
      case 'recommended':
        return '/products?recommended=true';
      default:
        return '/products';
    }
  };

  if (!loading && products.length === 0) return null;

  return (
    <Box
      component="section"
      aria-label={title}
      sx={{ py: 6, bgcolor: 'white' }}
    >
      <Container maxWidth="xl">
        <Box sx={{ textAlign: 'center', mb: 4 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, mb: 1 }}>
            <Box sx={{ color: accentColor }}>{icon}</Box>
            <Typography
              component="h2"
              sx={{
                fontSize: { xs: '1.5rem', md: '2rem' },
                fontWeight: 700,
                color: '#1e293b',
              }}
            >
              {title}
            </Typography>
          </Box>
          <Typography sx={{ color: '#64748b', fontSize: '1rem' }}>
            {subtitle}
          </Typography>
        </Box>

        <Box
          sx={{ position: 'relative', overflow: 'hidden' }}
          onMouseEnter={() => setAutoScroll(false)}
          onMouseLeave={() => setAutoScroll(true)}
        >
          {/* Navigation */}
          {!loading && displayProducts.length > itemsPerSlide && (
            <>
              <IconButton
                onClick={() => {
                  setAutoScroll(false);
                  setCarouselIndex((prev) => (prev <= 0 ? maxIndex : prev - 1));
                }}
                aria-label="Sản phẩm trước"
                sx={{
                  position: 'absolute',
                  left: 0,
                  top: '50%',
                  transform: 'translateY(-50%)',
                  zIndex: 10,
                  bgcolor: 'white',
                  boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                  border: `2px solid ${accentColor}`,
                  color: accentColor,
                  '&:hover': { 
                    bgcolor: accentColor,
                    color: 'white',
                  },
                }}
              >
                <ChevronLeft />
              </IconButton>
              <IconButton
                onClick={() => {
                  setAutoScroll(false);
                  setCarouselIndex((prev) => (prev >= maxIndex ? 0 : prev + 1));
                }}
                aria-label="Sản phẩm tiếp"
                sx={{
                  position: 'absolute',
                  right: 0,
                  top: '50%',
                  transform: 'translateY(-50%)',
                  zIndex: 10,
                  bgcolor: 'white',
                  boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                  border: `2px solid ${accentColor}`,
                  color: accentColor,
                  '&:hover': { 
                    bgcolor: accentColor,
                    color: 'white',
                  },
                }}
              >
                <ChevronRight />
              </IconButton>
            </>
          )}

          {/* Carousel */}
          <Box sx={{ overflow: 'hidden', mx: 5 }}>
            <Box sx={{
              display: 'flex',
              transition: 'transform 0.5s cubic-bezier(0.4, 0, 0.2, 1)',
              transform: `translateX(-${carouselIndex * 100}%)`,
            }}>
              {Array.from({ length: Math.ceil(displayProducts.length / itemsPerSlide) }).map((_, slideIndex) => (
                <Box key={slideIndex} sx={{ minWidth: '100%', px: 1 }}>
                  <Grid container spacing={2}>
                    {displayProducts.slice(slideIndex * itemsPerSlide, (slideIndex + 1) * itemsPerSlide)
                      .map((product: any, index: number) => (
                        <Grid item xs={12} sm={6} md={4} lg={3} key={product?.id || index}>
                          {loading ? (
                            <Skeleton variant="rectangular" height={350} sx={{ borderRadius: 3 }} />
                          ) : product && (
                            <ProductCard product={product} badge={badge} accentColor={accentColor} />
                          )}
                        </Grid>
                      ))}
                  </Grid>
                </Box>
              ))}
            </Box>
          </Box>

          {/* Dots */}
          {!loading && maxIndex > 0 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1, mt: 3 }}>
              {Array.from({ length: maxIndex + 1 }).map((_, index) => (
                <Box
                  key={index}
                  onClick={() => {
                    setAutoScroll(false);
                    setCarouselIndex(index);
                  }}
                  sx={{
                    width: carouselIndex === index ? 24 : 8,
                    height: 8,
                    borderRadius: 4,
                    bgcolor: carouselIndex === index ? accentColor : '#e5e7eb',
                    cursor: 'pointer',
                    transition: 'all 0.3s ease',
                  }}
                />
              ))}
            </Box>
          )}
        </Box>

        <Box sx={{ textAlign: 'center', mt: 3 }}>
          <Button
            variant="outlined"
            size="large"
            onClick={() => navigate(getFilterUrl())}
            sx={{
              borderColor: accentColor,
              color: accentColor,
              fontWeight: 600,
              px: 4,
              '&:hover': {
                borderColor: accentColor,
                bgcolor: `${accentColor}08`,
              },
            }}
          >
            Xem tất cả {title.toLowerCase()}
          </Button>
        </Box>
      </Container>
    </Box>
  );
};

export default HomePage; 
