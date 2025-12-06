package com.lebvest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CacheService {

    @Cacheable(value = "investments", key = "#page + '_' + #size + '_' + #category + '_' + #status")
    public <T> List<T> getCachedInvestments(int page, int size, String category, String status, 
                                            java.util.function.Supplier<List<T>> supplier) {
        log.debug("Cache miss for investments: page={}, size={}, category={}, status={}", 
                page, size, category, status);
        return supplier.get();
    }

    @Cacheable(value = "searchSuggestions", key = "#query")
    public <T> List<T> getCachedSearchSuggestions(String query, 
                                                   java.util.function.Supplier<List<T>> supplier) {
        log.debug("Cache miss for search suggestions: query={}", query);
        return supplier.get();
    }

    @Cacheable(value = "dashboardSummaries", key = "#userId + '_' + #role")
    public <T> T getCachedDashboardSummary(Long userId, String role, 
                                          java.util.function.Supplier<T> supplier) {
        log.debug("Cache miss for dashboard summary: userId={}, role={}", userId, role);
        return supplier.get();
    }

    @CacheEvict(value = "investments", allEntries = true)
    public void evictInvestmentsCache() {
        log.debug("Evicting investments cache");
    }

    @CacheEvict(value = "searchSuggestions", allEntries = true)
    public void evictSearchSuggestionsCache() {
        log.debug("Evicting search suggestions cache");
    }

    @CacheEvict(value = "dashboardSummaries", allEntries = true)
    public void evictDashboardSummariesCache() {
        log.debug("Evicting dashboard summaries cache");
    }

    @CacheEvict(allEntries = true)
    public void evictAllCaches() {
        log.debug("Evicting all caches");
    }
}

