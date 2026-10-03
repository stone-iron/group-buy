package com.livegroupbuy.backend.purchase;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class PurchaseRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseRateLimitService.class);
    private static final long WINDOW_MILLIS = 1_000;
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            if count > tonumber(ARGV[2]) then
              local ttl = redis.call('PTTL', KEYS[1])
              if ttl < 1 then ttl = tonumber(ARGV[1]) end
              return ttl
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final int userLimit;
    private final int ipLimit;
    private final AtomicBoolean redisWarningLogged = new AtomicBoolean(false);

    public PurchaseRateLimitService(StringRedisTemplate redisTemplate,
                                    @Value("${purchase.abuse.rate-limit.enabled:true}") boolean enabled,
                                    @Value("${purchase.abuse.rate-limit.user-per-second:3}") int userLimit,
                                    @Value("${purchase.abuse.rate-limit.ip-per-second:20}") int ipLimit) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.userLimit = userLimit;
        this.ipLimit = ipLimit;
    }

    public void assertRequestAllowed(Long memberId, String clientIp) {
        if (!enabled) return;
        try {
            enforce("purchase:rate:member:" + memberId, userLimit);
            enforce("purchase:rate:ip:" + normalizeIp(clientIp), ipLimit);
            redisWarningLogged.set(false);
        } catch (PurchaseAccessDeniedException exception) {
            throw exception;
        } catch (RedisConnectionFailureException exception) {
            logRedisFailOpen(exception);
        } catch (RuntimeException exception) {
            logRedisFailOpen(exception);
        }
    }

    private void enforce(String key, int limit) {
        Long retryAfterMillis = redisTemplate.execute(
                RATE_LIMIT_SCRIPT, List.of(key), String.valueOf(WINDOW_MILLIS), String.valueOf(limit)
        );
        if (retryAfterMillis != null && retryAfterMillis > 0) {
            long retryAfterSeconds = Math.max(1, (retryAfterMillis + 999) / 1_000);
            throw new PurchaseAccessDeniedException(
                    "PURCHASE_RATE_LIMITED",
                    "구매 요청이 너무 많습니다. " + retryAfterSeconds + "초 후 다시 시도해 주세요.",
                    retryAfterSeconds
            );
        }
    }

    private String normalizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) return "unknown";
        return clientIp.replaceAll("[^0-9a-fA-F:._-]", "_");
    }

    private void logRedisFailOpen(RuntimeException exception) {
        if (redisWarningLogged.compareAndSet(false, true)) {
            log.warn("Redis rate limiter is unavailable. Purchase requests will fail open while DB safeguards remain active: {}",
                    exception.getMessage());
        }
    }
}
