package com.hogwai.perf.optimized.cache;

import com.hogwai.perf.common.dto.StatsResponse;
import com.hogwai.perf.common.dto.UserResponse;
import com.hogwai.perf.common.service.StatsService;
import com.hogwai.perf.common.service.UserService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Pre-warms caches after application startup.
 * This ensures the first real user requests hit cache, not DB.
 */
@Component
public class CacheWarmer {

    private final StatsService statsService;
    private final UserService userService;
    private final MultiLevelCache multiLevelCache;

    public CacheWarmer(StatsService statsService, UserService userService, MultiLevelCache multiLevelCache) {
        this.statsService = statsService;
        this.userService = userService;
        this.multiLevelCache = multiLevelCache;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmCaches() {
        // Pre-warm stats (single value, high traffic)
        statsService.getStats();

        // Pre-warm common users (1 and 2 are always seeded)
        for (long id = 1; id <= 2; id++) {
            userService.getUserById(id);
        }
    }
}
