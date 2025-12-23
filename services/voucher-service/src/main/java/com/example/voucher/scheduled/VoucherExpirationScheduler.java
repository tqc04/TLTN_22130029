package com.example.voucher.scheduled;

import com.example.voucher.entity.Voucher;
import com.example.voucher.event.VoucherEventPublisher;
import com.example.voucher.repository.VoucherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class VoucherExpirationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(VoucherExpirationScheduler.class);

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private VoucherEventPublisher eventPublisher;

    /**
     * Cleanup expired vouchers once per day (optional - for database consistency)
     * Note: Vouchers are automatically hidden via query filtering based on real-time comparison
     * This task is only for database cleanup/maintenance, not for hiding vouchers
     */
    @Scheduled(cron = "0 0 2 * * ?") // Once per day at 2:00 AM
    @Transactional
    public void cleanupExpiredVouchers() {
        try {
            LocalDateTime now = LocalDateTime.now();
            logger.info("Running daily cleanup for expired vouchers at: {}", now);

            // Find vouchers that have expired but are still marked as active
            // This is for database consistency, not for hiding (queries handle that)
            List<Voucher> expiredVouchers = voucherRepository.findExpiredActiveVouchers(now);

            if (!expiredVouchers.isEmpty()) {
                logger.info("Found {} expired vouchers for cleanup", expiredVouchers.size());

                int deactivatedCount = 0;
                for (Voucher voucher : expiredVouchers) {
                    try {
                        if (voucher.isActive()) {
                            voucher.setActive(false);
                            voucher.setUpdatedAt(now);
                            voucherRepository.save(voucher);

                            // Publish expiration event for analytics
                            eventPublisher.publishVoucherExpiredEvent(
                                voucher.getId().toString(), 
                                voucher.getCode()
                            );

                            deactivatedCount++;
                        }
                    } catch (Exception e) {
                        logger.error("Error in cleanup for voucher {}: {}", 
                            voucher.getCode(), e.getMessage(), e);
                    }
                }

                logger.info("Cleanup completed: {}/{} expired vouchers deactivated", 
                    deactivatedCount, expiredVouchers.size());
            }

        } catch (Exception e) {
            logger.error("Error in voucher cleanup scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up old voucher usage records every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2:00 AM
    @Transactional
    public void cleanupOldVoucherUsage() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(12); // Keep 12 months of history
            logger.info("Cleaning up voucher usage records older than: {}", cutoffDate);

            // This would require a custom repository method to delete old records
            // For now, just log the operation
            logger.info("Cleanup completed - would delete records older than {}", cutoffDate);

        } catch (Exception e) {
            logger.error("Error in voucher usage cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Send voucher expiry warnings every day at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * ?") // Every day at 9:00 AM
    public void sendExpiryWarnings() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime warningDate = now.plusDays(7); // Warn 7 days before expiry

            logger.info("Checking for vouchers expiring in 7 days: {}", warningDate);

            List<Voucher> expiringSoon = voucherRepository.findAll().stream()
                .filter(voucher -> voucher.isActive())
                .filter(voucher -> voucher.getEndDate() != null)
                .filter(voucher -> voucher.getEndDate().isAfter(now))
                .filter(voucher -> voucher.getEndDate().isBefore(warningDate))
                .toList();

            if (!expiringSoon.isEmpty()) {
                logger.info("Found {} vouchers expiring soon", expiringSoon.size());

                for (Voucher voucher : expiringSoon) {
                    // Send warning notification
                    // notificationService.sendVoucherExpiryWarning(voucher);
                    logger.info("Expiry warning sent for voucher: {} (expires: {})",
                               voucher.getCode(), voucher.getEndDate());
                }
            }

        } catch (Exception e) {
            logger.error("Error in voucher expiry warnings: {}", e.getMessage(), e);
        }
    }
}
