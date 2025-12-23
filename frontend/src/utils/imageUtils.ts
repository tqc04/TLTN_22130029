/**
 * Transform image URL to ensure it's accessible via API gateway
 * - If URL starts with /uploads/, prepend /api
 * - If URL is already external (http/https), return as is
 * - Otherwise return as is
 */
export function transformImageUrl(imageUrl?: string | null): string {
  if (!imageUrl || imageUrl.trim() === '') {
    return 'https://via.placeholder.com/400x300?text=No+Image'
  }

  const trimmed = imageUrl.trim()

  // Already a full URL (external/cloud)
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    return trimmed
  }

  // Local path starting with /uploads/ - prepend /api for gateway routing
  if (trimmed.startsWith('/uploads/')) {
    return '/api' + trimmed
  }

  // If it doesn't start with /api/uploads/ but is a local path, try to fix it
  if (trimmed.startsWith('/api/uploads/')) {
    return trimmed
  }

  // Other cases - return as is (might be relative path)
  return trimmed
}
