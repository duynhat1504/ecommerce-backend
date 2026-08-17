package com.duynhat.ecommerce_backend.modules.order.scheduler;

import com.duynhat.ecommerce_backend.modules.order.OrderRepository;
import com.duynhat.ecommerce_backend.modules.order.OrderService;
import com.duynhat.ecommerce_backend.modules.order.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Value("${app.order.payment-timeout-minutes:15}")
    private long paymentTimeoutMinutes;

    @Value("${app.order.expiration-batch-size:50}")
    private int batchSize;

    @Scheduled(
            fixedDelayString = "${app.order.expiration-scan-delay-ms:60000}",
            initialDelayString = "${app.order.expiration-initial-delay-ms:30000}"
    )
    public void expireUnpaidOrders() {
        LocalDateTime cutoff = LocalDateTime.now()
                .minusMinutes(paymentTimeoutMinutes);

        int safeBatchSize =
                Math.max(1, batchSize);

        List<UUID> candidateIds = orderRepository
                .findExpiredOrderIds(
                        OrderStatus.PENDING,
                        cutoff,
                        PageRequest.of(0, safeBatchSize)
                );

        if (candidateIds.isEmpty()) {
            return;
        }

        int expiredCount = 0;

        for (UUID orderId : candidateIds) {
            try {
                boolean expired = orderService
                        .expireOrderIfEligible(orderId, cutoff);

                if (expired) {
                    expiredCount++;
                }

            } catch (Exception ex) {
                log.error("Failed to expire order: {}", orderId, ex);
            }
        }

        log.info(
                "Order expiration scan completed. candidates={}, expired={}",
                candidateIds.size(),
                expiredCount
        );
    }
}
