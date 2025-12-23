import React, { useState, useRef, useEffect } from 'react';
import { Box, Skeleton, Fade } from '@mui/material';

interface LazyImageProps {
  src: string;
  alt: string;
  width?: number | string;
  height?: number | string;
  className?: string;
  style?: React.CSSProperties;
  placeholder?: string;
  onLoad?: () => void;
  onError?: () => void;
  threshold?: number;
  rootMargin?: string;
  fadeIn?: boolean;
  fadeInDuration?: number;
}

const LazyImage: React.FC<LazyImageProps> = ({
  src,
  alt,
  width = '100%',
  height = 'auto',
  className,
  style,
  placeholder,
  onLoad,
  onError,
  threshold = 0.1,
  rootMargin = '50px',
  fadeIn = true,
  fadeInDuration = 300
}) => {
  const [isLoaded, setIsLoaded] = useState(false);
  const [isInView, setIsInView] = useState(false);
  const [hasError, setHasError] = useState(false);
  const imgRef = useRef<HTMLImageElement>(null);
  const observerRef = useRef<IntersectionObserver | null>(null);

  useEffect(() => {
    const img = imgRef.current;
    if (!img) return;

    // Create intersection observer
    observerRef.current = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            setIsInView(true);
            observerRef.current?.unobserve(entry.target);
          }
        });
      },
      {
        threshold,
        rootMargin
      }
    );

    observerRef.current.observe(img);

    return () => {
      if (observerRef.current) {
        observerRef.current.disconnect();
      }
    };
  }, [threshold, rootMargin]);

  const handleLoad = () => {
    setIsLoaded(true);
    onLoad?.();
  };

  const handleError = () => {
    setHasError(true);
    onError?.();
  };

  const imageStyle: React.CSSProperties = {
    width,
    height,
    objectFit: 'cover',
    transition: fadeIn ? `opacity ${fadeInDuration}ms ease-in-out` : 'none',
    opacity: isLoaded ? 1 : 0,
    ...style
  };

  const skeletonStyle: React.CSSProperties = {
    width,
    height,
    ...style
  };

  return (
    <Box
      ref={imgRef}
      className={className}
      style={{
        position: 'relative',
        overflow: 'hidden',
        width,
        height
      }}
    >
      {/* Loading skeleton */}
      {!isLoaded && !hasError && (
        <Skeleton
          variant="rectangular"
          width="100%"
          height="100%"
          animation="wave"
          style={skeletonStyle}
        />
      )}

      {/* Error placeholder */}
      {hasError && (
        <Box
          style={{
            ...skeletonStyle,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: '#f5f5f5',
            color: '#999',
            fontSize: '14px'
          }}
        >
          {placeholder || 'Image not available'}
        </Box>
      )}

      {/* Actual image */}
      {isInView && !hasError && (
        <Fade in={isLoaded} timeout={fadeInDuration}>
          <img
            src={src}
            alt={alt}
            style={imageStyle}
            onLoad={handleLoad}
            onError={handleError}
            loading="lazy"
          />
        </Fade>
      )}
    </Box>
  );
};

export default LazyImage;
