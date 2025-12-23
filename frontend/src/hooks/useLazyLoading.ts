import { useState, useEffect, useRef, useCallback } from 'react';

interface UseLazyLoadingOptions {
  threshold?: number;
  rootMargin?: string;
  triggerOnce?: boolean;
}

interface UseLazyLoadingReturn {
  isVisible: boolean;
  ref: React.RefObject<HTMLElement>;
  hasIntersected: boolean;
}

export const useLazyLoading = (options: UseLazyLoadingOptions = {}): UseLazyLoadingReturn => {
  const {
    threshold = 0.1,
    rootMargin = '50px',
    triggerOnce = true
  } = options;

  const [isVisible, setIsVisible] = useState(false);
  const [hasIntersected, setHasIntersected] = useState(false);
  const ref = useRef<HTMLElement>(null);

  const handleIntersection = useCallback((entries: IntersectionObserverEntry[]) => {
    const [entry] = entries;
    
          if (entry.isIntersecting) {
        setIsVisible(true);
        setHasIntersected(true);
        
        if (triggerOnce) {
          // Disconnect observer after first intersection
          if (ref.current && observer.current) {
            observer.current.unobserve(ref.current);
          }
        }
      } else if (!triggerOnce) {
        setIsVisible(false);
      }
  }, [triggerOnce]);

  const observer = useRef<IntersectionObserver | null>(null);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;

    observer.current = new IntersectionObserver(handleIntersection, {
      threshold,
      rootMargin
    });

    observer.current.observe(element);

    return () => {
      if (observer.current) {
        observer.current.disconnect();
      }
    };
  }, [handleIntersection, threshold, rootMargin]);

  return {
    isVisible,
    ref,
    hasIntersected
  };
};

export default useLazyLoading;
