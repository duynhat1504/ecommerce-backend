package com.duynhat.ecommerce_backend.modules.auth.scheduler;

import com.duynhat.ecommerce_backend.modules.auth.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCleanupScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(RefreshTokenCleanupScheduler.class);

    @Autowired
    private RefreshTokenService refreshTokenService;

    public RefreshTokenCleanupScheduler(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @Scheduled(
            cron = "0 0 3 * * *",
            zone = "Asia/Ho_Chi_Minh"
    )
    public void cleanupExpiredRefreshTokens() {
        long deletedCount = refreshTokenService.deleteExpiredRefreshTokens();

        if (deletedCount > 0) {
            log.info(
                    "Deleted {} expired refresh token(s)",
                    deletedCount
            );
        }
    }
}
