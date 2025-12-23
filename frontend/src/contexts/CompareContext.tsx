import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { Product } from '../services/api';
import { Product as ProductServiceProduct } from '../services/productService';

interface CompareContextType {
  compareProducts: (Product | ProductServiceProduct)[];
  addToCompare: (product: Product | ProductServiceProduct) => void;
  removeFromCompare: (productId: string) => void;
  clearCompare: () => void;
  isInCompare: (productId: string) => boolean;
  canAddMore: boolean;
}

const CompareContext = createContext<CompareContextType | undefined>(undefined);

const MAX_COMPARE_PRODUCTS = 4; // Giới hạn tối đa 4 sản phẩm để so sánh

export const CompareProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [compareProducts, setCompareProducts] = useState<(Product | ProductServiceProduct)[]>(() => {
    // Load from localStorage on init
    const saved = localStorage.getItem('compare_products');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        return [];
      }
    }
    return [];
  });

  // Save to localStorage whenever compareProducts changes
  React.useEffect(() => {
    localStorage.setItem('compare_products', JSON.stringify(compareProducts));
  }, [compareProducts]);

  const addToCompare = useCallback((product: Product | ProductServiceProduct) => {
    setCompareProducts(prev => {
      // Check if product already exists
      if (prev.some(p => p.id === product.id)) {
        // Show toast that product is already in compare
        const event = new CustomEvent('showToast', {
          detail: {
            type: 'info',
            message: `"${product.name}" đã có trong danh sách so sánh`
          }
        });
        window.dispatchEvent(event);
        return prev; // Already in compare list
      }
      
      // Check if reached max limit
      if (prev.length >= MAX_COMPARE_PRODUCTS) {
        // Remove first product and add new one
        const removedProduct = prev[0];
        const event = new CustomEvent('showToast', {
          detail: {
            type: 'info',
            message: `Đã thêm "${product.name}". Đã xóa "${removedProduct.name}" để thêm sản phẩm mới.`
          }
        });
        window.dispatchEvent(event);
        return [...prev.slice(1), product];
      }
      
      // Show success toast
      const event = new CustomEvent('showToast', {
        detail: {
          type: 'success',
          message: `Đã thêm "${product.name}" vào so sánh (${prev.length + 1}/${MAX_COMPARE_PRODUCTS})`
        }
      });
      window.dispatchEvent(event);
      return [...prev, product];
    });
  }, []);

  const removeFromCompare = useCallback((productId: string) => {
    setCompareProducts(prev => {
      const product = prev.find(p => p.id === productId);
      const newList = prev.filter(p => p.id !== productId);
      
      // Show toast notification
      if (product) {
        const event = new CustomEvent('showToast', {
          detail: {
            type: 'info',
            message: `Đã xóa "${product.name}" khỏi danh sách so sánh`
          }
        });
        window.dispatchEvent(event);
      }
      
      return newList;
    });
  }, []);

  const clearCompare = useCallback(() => {
    setCompareProducts(prev => {
      if (prev.length > 0) {
        const event = new CustomEvent('showToast', {
          detail: {
            type: 'info',
            message: `Đã xóa tất cả ${prev.length} sản phẩm khỏi danh sách so sánh`
          }
        });
        window.dispatchEvent(event);
      }
      return [];
    });
  }, []);

  const isInCompare = useCallback((productId: string) => {
    return compareProducts.some(p => p.id === productId);
  }, [compareProducts]);

  const canAddMore = compareProducts.length < MAX_COMPARE_PRODUCTS;

  return (
    <CompareContext.Provider
      value={{
        compareProducts,
        addToCompare,
        removeFromCompare,
        clearCompare,
        isInCompare,
        canAddMore
      }}
    >
      {children}
    </CompareContext.Provider>
  );
};

export const useCompare = () => {
  const context = useContext(CompareContext);
  if (context === undefined) {
    throw new Error('useCompare must be used within a CompareProvider');
  }
  return context;
};

