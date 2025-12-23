package com.example.admin.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminUploadController {

    @Value("${file.upload.path:./uploads}")
    private String uploadDir;

    @Value("${FRONTEND_BASE_URL:http://localhost:3000}")
    private String frontendBaseUrl;

    /**
     * Upload a single file and return its local URL path
     */
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No file provided"));
        }

        String filename = file.getOriginalFilename();
        String original = StringUtils.cleanPath(filename != null ? filename : "file");
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot);
        }
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path dated = base.resolve(datePrefix);
        Files.createDirectories(dated);

        Path target = dated.resolve(fileName);
        file.transferTo(target.toFile());

        // Expose via static file server or reverse proxy; here we return a logical path under /uploads
        String publicPath = "/uploads/" + datePrefix + "/" + fileName;

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("url", publicPath);
        res.put("filename", original);
        res.put("size", file.getSize());
        res.put("contentType", file.getContentType());
        return ResponseEntity.ok(res);
    }
}
