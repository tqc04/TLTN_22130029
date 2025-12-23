package com.example.admin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;
    
    @Value("${services.product.base-url:http://localhost:8083}")
    private String productServiceUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "File is empty"));
            }

            // Create upload directory if not exists
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;
            
            // Save file
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            // Return file info
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filename", filename);
            response.put("url", "/uploads/" + filename);
            response.put("originalName", originalFilename);
            response.put("size", file.getSize());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to upload file: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/products/{productId}/images", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> uploadProductImages(
            @PathVariable String productId,
            @RequestParam("files") MultipartFile[] files) {
        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "No files provided"));
            }

            // Validate productId
            if (productId == null || productId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Product ID is required"));
            }

            // Delete old images from disk before uploading new ones (for replace mode)
            Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path productDir = baseDir.resolve("products").resolve(productId);
            
            if (Files.exists(productDir)) {
                try {
                    logger.info("🗑️ Deleting ALL old images and subdirectories for product: {} from directory: {}", productId, productDir);
                    
                    // Count files before deletion
                    long fileCount = Files.walk(productDir)
                        .filter(Files::isRegularFile)
                        .count();
                    logger.info("📊 Found {} files to delete in product directory", fileCount);
                    
                    // Delete all files and subdirectories recursively in product directory
                    // Use reverseOrder to delete files before directories (required for directory deletion)
                    java.util.List<Path> pathsToDelete = Files.walk(productDir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .collect(java.util.stream.Collectors.toList());
                    
                    int deletedCount = 0;
                    int failedCount = 0;
                    
                    for (Path pathToDelete : pathsToDelete) {
                        try {
                            Files.delete(pathToDelete);
                            deletedCount++;
                            if (Files.isRegularFile(pathToDelete)) {
                                logger.debug("🗑️ Deleted file: {}", pathToDelete);
                            } else if (Files.isDirectory(pathToDelete)) {
                                logger.debug("🗑️ Deleted directory: {}", pathToDelete);
                            }
                        } catch (IOException e) {
                            failedCount++;
                            logger.warn("⚠️ Failed to delete {}: {}", pathToDelete, e.getMessage());
                        }
                    }
                    
                    logger.info("✅ Finished deleting old images: {} deleted, {} failed", deletedCount, failedCount);
                    
                    // Verify deletion - check if directory still exists
                    if (Files.exists(productDir)) {
                        logger.warn("⚠️ Product directory still exists after deletion attempt: {}", productDir);
                    } else {
                        logger.info("✅ Product directory completely removed: {}", productDir);
                    }
                } catch (IOException e) {
                    logger.error("❌ Failed to delete old images for product {}: {}", productId, e.getMessage(), e);
                    // Continue with upload even if deletion fails - new files will overwrite
                }
            }
            
            // Create product-specific upload directory (without date structure for easier management)
            logger.info("📤 Uploading images for product: {}", productId);
            logger.info("📂 Base upload path: {}", baseDir);
            logger.info("📂 Product directory: {}", productDir);
            
            if (!Files.exists(productDir)) {
                Files.createDirectories(productDir);
                logger.info("✅ Created product directory: {}", productDir);
            } else {
                logger.info("📂 Product directory already exists: {}", productDir);
            }

            List<Map<String, Object>> uploadedFiles = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    // Generate unique filename
                    String originalFilename = file.getOriginalFilename();
                    String extension = "";
                    if (originalFilename != null && originalFilename.contains(".")) {
                        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    }
                    String filename = UUID.randomUUID().toString() + extension;
                    
                    // Save file
                    Path filePath = productDir.resolve(filename);
                    Files.copy(file.getInputStream(), filePath);
                    
                    logger.info("💾 Saved file: {} (size: {} bytes)", filePath, file.getSize());
                    logger.info("📝 File URL: /uploads/products/{}/{}", productId, filename);

                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("id", uploadedFiles.size() + 1); // Simple ID for response
                    fileInfo.put("filename", filename);
                    String imageUrl = "/uploads/products/" + productId + "/" + filename;
                    fileInfo.put("imageUrl", imageUrl);
                    fileInfo.put("url", imageUrl);
                    fileInfo.put("originalName", originalFilename);
                    fileInfo.put("size", file.getSize());
                    fileInfo.put("contentType", file.getContentType());
                    fileInfo.put("isPrimary", uploadedFiles.isEmpty()); // First image is primary
                    uploadedFiles.add(fileInfo);
                }
            }

            if (uploadedFiles.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "No valid files to upload"));
            }

            // Prepare image data for database insertion
            List<Map<String, Object>> imageDataList = new ArrayList<>();
            for (int i = 0; i < uploadedFiles.size(); i++) {
                Map<String, Object> fileInfo = uploadedFiles.get(i);
                Map<String, Object> imageData = new HashMap<>();
                imageData.put("imageUrl", fileInfo.get("imageUrl"));
                imageData.put("isPrimary", i == 0); // First image is primary
                imageData.put("displayOrder", i);
                imageData.put("altText", fileInfo.getOrDefault("originalName", ""));
                imageDataList.add(imageData);
            }

            // Save images to database via product-service API
            // Use replace=true to delete old images before inserting new ones
            try {
                String productServiceApiUrl = productServiceUrl + "/api/products/" + productId + "/images?replace=true";
                logger.info("📤 Calling product-service to save images (replace mode): {}", productServiceApiUrl);
                logger.info("📦 Image data to save: {}", imageDataList);
                
                // Add headers for internal service communication
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                
                org.springframework.http.HttpEntity<List<Map<String, Object>>> requestEntity = 
                    new org.springframework.http.HttpEntity<>(imageDataList, headers);
                
                @SuppressWarnings("unchecked")
                ResponseEntity<Map<String, Object>> dbResponse = restTemplate.exchange(
                    productServiceApiUrl,
                    org.springframework.http.HttpMethod.POST,
                    requestEntity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
                );
                
                if (dbResponse.getStatusCode().is2xxSuccessful()) {
                    Map<String, Object> dbBody = dbResponse.getBody();
                    if (dbBody != null) {
                        logger.info("✅ Successfully saved images to database: {}", dbBody);
                        
                        // Update uploadedFiles with database IDs
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> dbImages = (List<Map<String, Object>>) dbBody.get("images");
                        if (dbImages != null && dbImages.size() == uploadedFiles.size()) {
                            for (int i = 0; i < uploadedFiles.size(); i++) {
                                uploadedFiles.get(i).put("id", dbImages.get(i).get("id"));
                            }
                        }
                    }
                } else {
                    logger.warn("⚠️ Failed to save images to database (status: {}), but files were uploaded", dbResponse.getStatusCode());
                }
            } catch (Exception e) {
                logger.error("❌ Failed to save images to database: {}", e.getMessage(), e);
                logger.error("❌ Stack trace:", e);
                // Don't fail the upload if database save fails - files are already uploaded
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("files", uploadedFiles);
            response.put("images", uploadedFiles); // Also include as "images" for compatibility
            response.put("count", uploadedFiles.size());
            response.put("savedToDatabase", true);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to upload files: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    @GetMapping("/products/{productId}/images")
    public ResponseEntity<Map<String, Object>> getProductImages(@PathVariable String productId) {
        try {
            Path productDir = Paths.get(uploadPath, "products", productId.toString());
            
            if (!Files.exists(productDir)) {
                return ResponseEntity.ok(Map.of("success", true, "images", new ArrayList<>()));
            }

            List<Map<String, Object>> images = new ArrayList<>();
            // Get all images directly in product directory (no date subfolders)
            Files.walk(productDir)
                .filter(Files::isRegularFile)
                .forEach(filePath -> {
                    String filename = filePath.getFileName().toString();
                    
                    Map<String, Object> imageInfo = new HashMap<>();
                    imageInfo.put("filename", filename);
                    // URL format: /uploads/products/{productId}/{filename}
                    imageInfo.put("url", "/uploads/products/" + productId + "/" + filename);
                    imageInfo.put("size", filePath.toFile().length());
                    images.add(imageInfo);
                });

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("images", images);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to get images: " + e.getMessage()));
        }
    }

    @DeleteMapping("/products/{productId}/images/{imageId}")
    public ResponseEntity<Map<String, Object>> deleteProductImage(
            @PathVariable String productId,
            @PathVariable String imageId) {
        try {
            // Note: This simple delete might fail if image is inside date subfolder
            // For now, we rely on database deletion via product-service
            // But if we want to delete physical file, we need full path
            
            Path productDir = Paths.get(uploadPath, "products", productId.toString());
            
            // Try to find file recursively
            Optional<Path> fileToDelete = Files.walk(productDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().equals(imageId)) // Assuming imageId is filename
                .findFirst();
                
            if (fileToDelete.isPresent()) {
                Files.delete(fileToDelete.get());
                return ResponseEntity.ok(Map.of("success", true, "message", "Image deleted successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to delete image: " + e.getMessage()));
        }
    }
}
