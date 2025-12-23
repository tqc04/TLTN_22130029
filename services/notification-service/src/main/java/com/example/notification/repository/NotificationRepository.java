package com.example.notification.repository;

import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByUserId(String userId);
    
    List<Notification> findByUserIdAndIsRead(String userId, Boolean isRead);
    
    List<Notification> findByUserIdAndType(String userId, NotificationType type);
    
    List<Notification> findByUserIdAndChannel(String userId, String channel);
    
    List<Notification> findByIsSent(Boolean isSent);
    
    List<Notification> findByIsSentAndRetryCountLessThan(Boolean isSent, Integer maxRetries);
    
    List<Notification> findByScheduledAtLessThanEqualAndIsSent(LocalDateTime now, Boolean isSent);
    
    List<Notification> findByExpiresAtLessThanAndIsSent(LocalDateTime now, Boolean isSent);
    
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isRead = false ORDER BY n.priority DESC, n.createdAt DESC")
    List<Notification> findUnreadByUserIdOrderByPriorityAndCreatedAt(@Param("userId") String userId);
    
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.isRead = false")
    Long countUnreadByUserId(@Param("userId") String userId);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.type = :type")
    Long countByUserIdAndType(@Param("userId") String userId, @Param("type") NotificationType type);
    
    @Query("SELECT n FROM Notification n WHERE n.isSent = false AND n.retryCount < n.maxRetries AND (n.scheduledAt IS NULL OR n.scheduledAt <= :now)")
    List<Notification> findPendingNotifications(@Param("now") LocalDateTime now);
    
    @Query("SELECT n FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now AND n.isSent = false")
    List<Notification> findExpiredNotifications(@Param("now") LocalDateTime now);
    
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.createdAt >= :startDate ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndCreatedAtAfter(@Param("userId") String userId, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT n FROM Notification n WHERE n.type = :type AND n.createdAt >= :startDate ORDER BY n.createdAt DESC")
    List<Notification> findByTypeAndCreatedAtAfter(@Param("type") NotificationType type, @Param("startDate") LocalDateTime startDate);
    
    // Statistics methods
    Long countByIsSent(Boolean isSent);
    
    Long countByIsRead(Boolean isRead);
}
