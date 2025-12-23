package com.example.product.controller;

import com.example.product.entity.ProductReview;
import com.example.product.service.ReviewService;
import com.example.shared.util.AuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import jakarta.annotation.security.PermitAll;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/product/{productId}")
    @PermitAll
    public ResponseEntity<Page<ProductReview>> getReviewsByProduct(@PathVariable String productId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size,
                                                                   @RequestParam(defaultValue = "createdAt") String sortBy,
                                                                   @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<ProductReview> reviews = reviewService.getApprovedProductReviews(productId, pageRequest);

            // Update product rating after fetching reviews
            reviewService.updateProductRating(productId);

            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/product/{productId}/summary")
    @PermitAll
    public ResponseEntity<Map<String, Object>> getReviewSummary(@PathVariable String productId) {
        try {
            Map<String, Object> summary = reviewService.getReviewSummary(productId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ProductReview>> myReviews(Authentication auth,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        try {
            // Extract user ID from JWT token
            String userId = extractUserIdFromAuth(auth);

            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<ProductReview> reviews = reviewService.getUserReviews(userId, pageRequest);

            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            String userId = extractUserIdFromAuth(auth);
            String productId = body.get("productId").toString();
            Integer rating = Integer.valueOf(body.get("rating").toString());
            String title = (String) body.getOrDefault("title", "");
            String content = (String) body.getOrDefault("content", "");
            String pros = (String) body.getOrDefault("pros", null);
            String cons = (String) body.getOrDefault("cons", null);

            // Validation
            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "Rating must be between 1 and 5"));
            }

            if (title.trim().isEmpty() || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Title and content are required"));
            }

            // Check if user already reviewed this product
            if (reviewService.hasUserReviewedProduct(userId, productId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bạn đã đánh giá sản phẩm này rồi!"));
            }

           
           
            boolean hasPurchased = reviewService.checkIfUserPurchasedProduct(userId, productId);
            if (!hasPurchased) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Bạn chỉ có thể đánh giá sản phẩm đã mua!",
                    "message", "Vui lòng mua sản phẩm này trước khi đánh giá."
                ));
            }
            

            ProductReview created = reviewService.create(userId, productId, rating, title.trim(), content.trim());

            // Set pros and cons if provided
            if (pros != null || cons != null) {
                if (pros != null) created.setPros(pros.trim());
                if (cons != null) created.setCons(cons.trim());
                created = reviewService.save(created);
            }

            // Update product rating
            reviewService.updateProductRating(productId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Review created successfully",
                "review", created
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long reviewId,
                                                     @RequestBody Map<String, Object> body,
                                                     Authentication auth) {
        try {
            String userId = extractUserIdFromAuth(auth);
        Integer rating = body.get("rating") != null ? Integer.valueOf(body.get("rating").toString()) : null;
            String title = (String) body.getOrDefault("title", "");
            String content = (String) body.getOrDefault("content", "");
            String pros = (String) body.getOrDefault("pros", null);
            String cons = (String) body.getOrDefault("cons", null);

            // Validation
            if (rating != null && (rating < 1 || rating > 5)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Rating must be between 1 and 5"));
            }

            ProductReview updated = reviewService.updateOwnWithProsCons(reviewId, userId, rating, title.trim(), content.trim(), pros, cons);

            if (updated != null) {
                // Update product rating
                reviewService.updateProductRating(updated.getProductId());

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review updated successfully",
                    "review", updated
                ));
            } else {
                return ResponseEntity.status(403).body(Map.of("error", "You can only update your own reviews"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long reviewId, Authentication auth) {
        try {
            String userId = extractUserIdFromAuth(auth);
            String productId = reviewService.getProductIdByReviewId(reviewId);

            boolean deleted = reviewService.deleteOwn(reviewId, userId);

            if (deleted && productId != null) {
                // Update product rating after deletion
                reviewService.updateProductRating(productId);

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review deleted successfully"
                ));
            } else {
                return ResponseEntity.status(403).body(Map.of("error", "You can only delete your own reviews"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{reviewId}/helpful")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> helpful(@PathVariable Long reviewId, Authentication auth) {
        try {
            String userId = extractUserIdFromAuth(auth);

            // Check if user already marked this review as helpful
            if (reviewService.hasUserMarkedHelpful(reviewId, userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "You have already marked this review as helpful"));
            }

        ProductReview updated = reviewService.incrementHelpful(reviewId);

            if (updated != null) {
                reviewService.markHelpful(reviewId, userId);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review marked as helpful",
                    "helpfulCount", updated.getHelpfulCount()
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Review not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getReviewStats() {
        try {
            Map<String, Object> stats = reviewService.getReviewStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{reviewId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable Long reviewId) {
        try {
            ProductReview approved = reviewService.approve(reviewId);
            if (approved != null) {
                // Update product rating after approval
                reviewService.updateProductRating(approved.getProductId());

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review approved successfully",
                    "review", approved
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Review not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{reviewId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long reviewId, @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : "Rejected by admin";
            ProductReview rejected = reviewService.reject(reviewId, reason);

            if (rejected != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review rejected successfully",
                    "review", rejected
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Review not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ProductReview>> pending(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(reviewService.getAllProductReviews(null, pageRequest));
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminReviewStats() {
        try {
            Map<String, Object> stats = reviewService.getAdminReviewStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private String extractUserIdFromAuth(Authentication auth) {
        String userId = AuthUtils.extractUserIdFromAuth(auth);
        if (userId == null) {
            throw new RuntimeException("Cannot extract user ID from authentication");
        }
        return userId;
    }
}


