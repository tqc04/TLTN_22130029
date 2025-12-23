package com.example.user.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class StaticResourceController {

    private static final Logger logger = LoggerFactory.getLogger(StaticResourceController.class);

    // Primary upload directory (admin-service uploads for products)
    private static final String DEFAULT_UPLOAD_DIR = "D:/Buildd24_10/Buildd30_7/Buildd43/services/admin-service/uploads";
    
    // Secondary upload directory (user-service uploads for avatars)
    private static final String USER_UPLOAD_DIR = "D:/Buildd24_10/Buildd30_7/Buildd43/services/user-service/uploads";
    
    @Value("${file.upload-dir:" + DEFAULT_UPLOAD_DIR + "}")
    private String uploadDir;
    
    @PostConstruct
    public void init() {
        // Force use default if config value is relative path (like ./uploads)
        if (uploadDir == null || uploadDir.startsWith("./") || uploadDir.equals("uploads")) {
            uploadDir = DEFAULT_UPLOAD_DIR;
            logger.warn("⚠️ Overriding uploadDir to use absolute path: {}", uploadDir);
        }
        
        logger.info("🔧 StaticResourceController initialized with uploadDir: {}", uploadDir);
        Path uploadDirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        logger.info("🔧 Upload dir (absolute): {}", uploadDirPath);
        File uploadDirFile = uploadDirPath.toFile();
        logger.info("🔧 Upload dir exists: {}, Is directory: {}", uploadDirFile.exists(), uploadDirFile.isDirectory());
    }

    /**
     * Serve avatar and other uploaded files
     * Supports nested paths like: /api/uploads/products/{productId}/{date}/{filename}
     * or simple paths like: /api/uploads/{filename}
     */
    @GetMapping("/uploads/**")
    @PermitAll
    public ResponseEntity<?> serveFile(HttpServletRequest request) {
        try {
            // Extract path from request URI
            String requestURI = request.getRequestURI();
            String requestPath = null;
            
            // Remove /api/uploads prefix
            if (requestURI.startsWith("/api/uploads/")) {
                requestPath = requestURI.substring("/api/uploads/".length());
            } else if (requestURI.startsWith("/api/uploads")) {
                requestPath = requestURI.substring("/api/uploads".length());
                if (requestPath.startsWith("/")) {
                    requestPath = requestPath.substring(1);
                }
            }
            
            if (requestPath == null || requestPath.isEmpty()) {
                return ResponseEntity.badRequest().body("File path is required");
            }
            
            logger.info("📥 Serving file request - URI: {}, Request path: {}", requestURI, requestPath);
            
            // Try to find file in multiple directories
            File file = null;
            Path filePath = null;
            Path uploadDirPath = null;
            
            // List of directories to search (order matters - check user uploads first for avatars)
            String[] searchDirs = {
                USER_UPLOAD_DIR,  // User avatars
                uploadDir,        // Admin uploads (products)
                DEFAULT_UPLOAD_DIR
            };
            
            for (String dir : searchDirs) {
                Path dirPath = Paths.get(dir).toAbsolutePath().normalize();
                Path candidatePath = dirPath.resolve(requestPath).normalize();
                
                // Security check: ensure file is within upload directory
                if (!candidatePath.startsWith(dirPath)) {
                    continue;
                }
                
                File candidateFile = candidatePath.toFile();
                if (candidateFile.exists() && candidateFile.isFile()) {
                    file = candidateFile;
                    filePath = candidatePath;
                    uploadDirPath = dirPath;
                    logger.info("✅ Found file in: {}", dir);
                    break;
                }
            }
            
            if (file == null) {
                logger.error("❌ File not found in any upload directory: {}", requestPath);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("✅ File found: {} (size: {} bytes)", filePath, file.length());
            
            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                // Fallback based on file extension
                String lowerFilename = requestPath.toLowerCase();
                if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (lowerFilename.endsWith(".png")) {
                    contentType = "image/png";
                } else if (lowerFilename.endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (lowerFilename.endsWith(".webp")) {
                    contentType = "image/webp";
                } else {
                    contentType = "application/octet-stream";
                }
            }
            
            Resource resource = new FileSystemResource(file);
            
            // Extract just the filename for Content-Disposition header
            String filename = filePath.getFileName().toString();
            
            // Set cache control to prevent caching of images (for realtime updates)
            // Or use a short cache time if performance is a concern
            CacheControl cacheControl = CacheControl.noCache().mustRevalidate();
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .cacheControl(cacheControl)
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(resource);
        } catch (Exception e) {
            logger.error("Error serving file", e);
            return ResponseEntity.status(500).body("Error serving file: " + e.getMessage());
        }
    }
}
