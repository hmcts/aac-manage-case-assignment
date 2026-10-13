package uk.gov.hmcts.reform.managecase.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigurationTest {

    @Test
    void shouldDisallowNullCacheValues() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        new CacheConfiguration().cacheManagerCustomizer().customize(cacheManager);

        assertThat(cacheManager.isAllowNullValues()).isFalse();
    }
}
