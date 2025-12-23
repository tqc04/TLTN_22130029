import React, { useState, useCallback } from 'react';
import {
  Box,
  Dialog,
  DialogContent,
  IconButton,
  useTheme,
  useMediaQuery,
  Typography,
  Chip
} from '@mui/material';
import {
  Close,
  NavigateBefore,
  NavigateNext,
  Fullscreen
} from '@mui/icons-material';
import LazyImage from './LazyImage';

interface ImageGalleryProps {
  images: Array<{
    id: number;
    imageUrl: string;
    alt?: string;
    isPrimary?: boolean;
  }>;
  showThumbnails?: boolean;
  showFullscreen?: boolean;
  maxThumbnails?: number;
  height?: number | string;
  width?: number | string;
  onImageClick?: (imageUrl: string, index: number) => void;
}

const ImageGallery: React.FC<ImageGalleryProps> = ({
  images,
  showThumbnails = true,
  showFullscreen = true,
  maxThumbnails = 5,
  height = 400,
  width = '100%',
  onImageClick
}) => {
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [isFullscreenOpen, setIsFullscreenOpen] = useState(false);
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const handleImageClick = useCallback((index: number) => {
    setSelectedIndex(index);
    if (onImageClick) {
      onImageClick(images[index].imageUrl, index);
    }
  }, [images, onImageClick]);

  const handlePrevious = useCallback(() => {
    setSelectedIndex((prev) => 
      prev === 0 ? images.length - 1 : prev - 1
    );
  }, [images.length]);

  const handleNext = useCallback(() => {
    setSelectedIndex((prev) => 
      prev === images.length - 1 ? 0 : prev + 1
    );
  }, [images.length]);

  const handleFullscreenOpen = useCallback(() => {
    setIsFullscreenOpen(true);
  }, []);

  const handleFullscreenClose = useCallback(() => {
    setIsFullscreenOpen(false);
  }, []);

  if (!images || images.length === 0) {
    return (
      <Box
        sx={{
          width,
          height,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: '#f5f5f5',
          border: '1px dashed #ccc',
          borderRadius: 1
        }}
      >
        <Typography color="text.secondary">
          No images available
        </Typography>
      </Box>
    );
  }

  const currentImage = images[selectedIndex];
  const thumbnailImages = images.slice(0, maxThumbnails);

  return (
    <Box sx={{ width, height }}>
      {/* Main Image */}
      <Box
        sx={{
          position: 'relative',
          width: '100%',
          height: '100%',
          borderRadius: 1,
          overflow: 'hidden',
          cursor: showFullscreen ? 'pointer' : 'default'
        }}
        onClick={showFullscreen ? handleFullscreenOpen : undefined}
      >
        <LazyImage
          src={currentImage.imageUrl}
          alt={currentImage.alt || `Product image ${selectedIndex + 1}`}
          width="100%"
          height="100%"
          style={{
            objectFit: 'cover',
            transition: 'transform 0.3s ease-in-out'
          }}
          fadeIn={true}
          fadeInDuration={300}
        />

        {/* Navigation Arrows */}
        {images.length > 1 && (
          <>
            <IconButton
              onClick={(e) => {
                e.stopPropagation();
                handlePrevious();
              }}
              sx={{
                position: 'absolute',
                left: 8,
                top: '50%',
                transform: 'translateY(-50%)',
                backgroundColor: 'rgba(0, 0, 0, 0.5)',
                color: 'white',
                '&:hover': {
                  backgroundColor: 'rgba(0, 0, 0, 0.7)'
                }
              }}
            >
              <NavigateBefore />
            </IconButton>

            <IconButton
              onClick={(e) => {
                e.stopPropagation();
                handleNext();
              }}
              sx={{
                position: 'absolute',
                right: 8,
                top: '50%',
                transform: 'translateY(-50%)',
                backgroundColor: 'rgba(0, 0, 0, 0.5)',
                color: 'white',
                '&:hover': {
                  backgroundColor: 'rgba(0, 0, 0, 0.7)'
                }
              }}
            >
              <NavigateNext />
            </IconButton>
          </>
        )}

        {/* Fullscreen Button */}
        {showFullscreen && (
          <IconButton
            onClick={(e) => {
              e.stopPropagation();
              handleFullscreenOpen();
            }}
            sx={{
              position: 'absolute',
              top: 8,
              right: 8,
              backgroundColor: 'rgba(0, 0, 0, 0.5)',
              color: 'white',
              '&:hover': {
                backgroundColor: 'rgba(0, 0, 0, 0.7)'
              }
            }}
          >
            <Fullscreen />
          </IconButton>
        )}

        {/* Image Counter */}
        {images.length > 1 && (
          <Chip
            label={`${selectedIndex + 1} / ${images.length}`}
            sx={{
              position: 'absolute',
              bottom: 8,
              right: 8,
              backgroundColor: 'rgba(0, 0, 0, 0.7)',
              color: 'white'
            }}
          />
        )}
      </Box>

      {/* Thumbnails */}
      {showThumbnails && images.length > 1 && (
        <Box
          sx={{
            display: 'flex',
            gap: 1,
            mt: 2,
            overflowX: 'auto',
            '&::-webkit-scrollbar': {
              height: 4
            },
            '&::-webkit-scrollbar-track': {
              backgroundColor: '#f1f1f1'
            },
            '&::-webkit-scrollbar-thumb': {
              backgroundColor: '#c1c1c1',
              borderRadius: 2
            }
          }}
        >
          {thumbnailImages.map((image, index) => (
            <Box
              key={image.id}
              onClick={() => handleImageClick(index)}
              sx={{
                position: 'relative',
                minWidth: 80,
                height: 80,
                borderRadius: 1,
                overflow: 'hidden',
                cursor: 'pointer',
                border: selectedIndex === index ? 2 : 1,
                borderColor: selectedIndex === index ? 'primary.main' : 'divider',
                transition: 'border-color 0.2s ease-in-out',
                '&:hover': {
                  borderColor: 'primary.main'
                }
              }}
            >
              <LazyImage
                src={image.imageUrl}
                alt={image.alt || `Thumbnail ${index + 1}`}
                width="100%"
                height="100%"
                style={{
                  objectFit: 'cover'
                }}
                fadeIn={true}
                fadeInDuration={200}
              />
            </Box>
          ))}
        </Box>
      )}

      {/* Fullscreen Dialog */}
      <Dialog
        open={isFullscreenOpen}
        onClose={handleFullscreenClose}
        maxWidth="lg"
        fullWidth
        fullScreen={isMobile}
        sx={{
          '& .MuiDialog-paper': {
            backgroundColor: 'rgba(0, 0, 0, 0.9)',
            color: 'white'
          }
        }}
      >
        <DialogContent
          sx={{
            position: 'relative',
            padding: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: '80vh'
          }}
        >
          {/* Close Button */}
          <IconButton
            onClick={handleFullscreenClose}
            sx={{
              position: 'absolute',
              top: 16,
              right: 16,
              zIndex: 1,
              color: 'white',
              backgroundColor: 'rgba(0, 0, 0, 0.5)',
              '&:hover': {
                backgroundColor: 'rgba(0, 0, 0, 0.7)'
              }
            }}
          >
            <Close />
          </IconButton>

          {/* Fullscreen Image */}
          <Box
            sx={{
              position: 'relative',
              maxWidth: '90%',
              maxHeight: '90%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <LazyImage
              src={currentImage.imageUrl}
              alt={currentImage.alt || `Fullscreen image ${selectedIndex + 1}`}
              width="100%"
              height="100%"
              style={{
                objectFit: 'contain',
                maxHeight: '80vh'
              }}
              fadeIn={true}
              fadeInDuration={300}
            />

            {/* Navigation in Fullscreen */}
            {images.length > 1 && (
              <>
                <IconButton
                  onClick={handlePrevious}
                  sx={{
                    position: 'absolute',
                    left: -60,
                    top: '50%',
                    transform: 'translateY(-50%)',
                    backgroundColor: 'rgba(0, 0, 0, 0.5)',
                    color: 'white',
                    '&:hover': {
                      backgroundColor: 'rgba(0, 0, 0, 0.7)'
                    }
                  }}
                >
                  <NavigateBefore />
                </IconButton>

                <IconButton
                  onClick={handleNext}
                  sx={{
                    position: 'absolute',
                    right: -60,
                    top: '50%',
                    transform: 'translateY(-50%)',
                    backgroundColor: 'rgba(0, 0, 0, 0.5)',
                    color: 'white',
                    '&:hover': {
                      backgroundColor: 'rgba(0, 0, 0, 0.7)'
                    }
                  }}
                >
                  <NavigateNext />
                </IconButton>
              </>
            )}
          </Box>
        </DialogContent>
      </Dialog>
    </Box>
  );
};

export default ImageGallery;

