package com.example.ai.repository;

import com.example.ai.entity.AIRequest;
import com.example.ai.entity.AIProvider;
import com.example.ai.entity.AIRequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AIRequestRepository extends JpaRepository<AIRequest, Long> {
    
    List<AIRequest> findByUserId(String userId);
    
    List<AIRequest> findByUserIdAndStatus(String userId, String status);
    
    List<AIRequest> findByAiProvider(AIProvider aiProvider);
    
    List<AIRequest> findByRequestType(AIRequestType requestType);
    
    List<AIRequest> findByStatus(String status);
    
    List<AIRequest> findByUserIdAndAiProvider(String userId, AIProvider aiProvider);
    
    List<AIRequest> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT a FROM AIRequest a WHERE a.userId = :userId AND a.createdAt >= :startDate ORDER BY a.createdAt DESC")
    List<AIRequest> findByUserIdAndCreatedAtAfter(@Param("userId") String userId, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(a) FROM AIRequest a WHERE a.userId = :userId AND a.status = :status")
    Long countByUserIdAndStatus(@Param("userId") String userId, @Param("status") String status);
    
    @Query("SELECT SUM(a.tokensUsed) FROM AIRequest a WHERE a.userId = :userId AND a.status = 'COMPLETED'")
    Long sumTokensUsedByUserId(@Param("userId") String userId);
    
    @Query("SELECT SUM(a.cost) FROM AIRequest a WHERE a.userId = :userId AND a.status = 'COMPLETED'")
    Double sumCostByUserId(@Param("userId") String userId);
    
    @Query("SELECT a FROM AIRequest a WHERE a.status = 'PENDING' ORDER BY a.createdAt ASC")
    List<AIRequest> findPendingRequests();
    
    @Query("SELECT a FROM AIRequest a WHERE a.status = 'FAILED' AND a.createdAt >= :startDate ORDER BY a.createdAt DESC")
    List<AIRequest> findFailedRequests(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT a FROM AIRequest a WHERE a.aiProvider = :provider AND a.createdAt >= :startDate ORDER BY a.createdAt DESC")
    List<AIRequest> findByProviderAndCreatedAtAfter(@Param("provider") AIProvider provider, @Param("startDate") LocalDateTime startDate);
}
