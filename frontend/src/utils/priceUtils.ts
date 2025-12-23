/**
 * Utility functions for price calculations
 * Centralized logic to ensure consistency across the application
 */

export interface PriceBreakdown {
  subtotal: number;
  taxAmount: number;
  shippingAmount: number;
  discountAmount: number;
  totalAmount: number;
}

/**
 * Calculate total amount with consistent formula
 * Formula: subtotal + tax + shipping - discount
 */
export const calculateTotalAmount = (
  subtotal: number,
  shippingAmount: number = 0,
  discountAmount: number = 0,
  taxRate: number = 0.1 // 10% VAT by default
): PriceBreakdown => {
  const taxAmount = subtotal * taxRate;
  const totalAmount = subtotal + taxAmount + shippingAmount - discountAmount;

  return {
    subtotal,
    taxAmount,
    shippingAmount,
    discountAmount,
    totalAmount
  };
};

/**
 * Format price for display in VND (Vietnamese Dong)
 * Format: 1.234.567 đ (with thousand separators and đ at the end)
 */
export const formatPrice = (amount: number | null | undefined): string => {
  const value = Number(amount) || 0;
  // Format with thousand separators (dots) and add đ at the end
  return new Intl.NumberFormat('vi-VN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value) + ' đ';
};

/**
 * Calculate shipping fee based on subtotal
 * Free shipping if subtotal > 500,000 VND
 */
export const calculateShippingFee = (subtotal: number): number => {
  return subtotal > 500000 ? 0 : 30000;
};

/**
 * Validate price calculation consistency
 */
export const validatePriceCalculation = (
  subtotal: number,
  _taxAmount: number,
  shippingAmount: number,
  discountAmount: number,
  totalAmount: number,
  taxRate: number = 0.1
): boolean => {
  const expectedTax = subtotal * taxRate;
  const expectedTotal = subtotal + expectedTax + shippingAmount - discountAmount;
  
  return Math.abs(expectedTotal - totalAmount) < 0.01; // Allow small floating point differences
};
