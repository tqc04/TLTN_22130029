package com.example.ai.repository;

import com.example.ai.entity.ChatLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {
    
    // Find by user ID
    Page<ChatLog> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    // Find by session ID
    List<ChatLog> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    
    // Find product-related queries
    Page<ChatLog> findByIsProductRelatedTrueOrderByCreatedAtDesc(Pageable pageable);
    
    // Find queries with feedback
    Page<ChatLog> findByFeedbackIsNotNullOrderByCreatedAtDesc(Pageable pageable);
    
    // Find queries where products were not found
    Page<ChatLog> findByIsProductRelatedTrueAndFoundProductsFalseOrderByCreatedAtDesc(Pageable pageable);
    
    // Statistics queries
    @Query("SELECT COUNT(c) FROM ChatLog c WHERE c.isProductRelated = true AND c.foundProducts = false")
    Long countProductQueriesWithoutResults();
    
    @Query("SELECT COUNT(c) FROM ChatLog c WHERE c.isProductRelated = true AND c.foundProducts = true")
    Long countProductQueriesWithResults();
    
    @Query("SELECT COUNT(c) FROM ChatLog c WHERE c.usedAI = true")
    Long countAIQueries();
    
    // Find most asked product queries (for analysis)
    @Query("SELECT c.userMessage, COUNT(c) as count FROM ChatLog c " +
           "WHERE c.isProductRelated = true AND c.foundProducts = false " +
           "GROUP BY c.userMessage ORDER BY count DESC")
    List<Object[]> findMostAskedProductQueries(Pageable pageable);
    
    // Find by date range
    Page<ChatLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime startDate, 
        LocalDateTime endDate, 
        Pageable pageable
    );
    
    // Find by user and date range
    Page<ChatLog> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        String userId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );
    
    // Count by response source
    @Query("SELECT c.responseSource, COUNT(c) FROM ChatLog c GROUP BY c.responseSource")
    List<Object[]> countByResponseSource();
    
    // Average rating
    @Query("SELECT AVG(c.rating) FROM ChatLog c WHERE c.rating IS NOT NULL")
    Double getAverageRating();
}

