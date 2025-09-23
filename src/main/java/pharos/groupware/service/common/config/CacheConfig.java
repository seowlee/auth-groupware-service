package pharos.groupware.service.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        var mgr = new CaffeineCacheManager("holidaysByYear");
        mgr.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(50)
                        .expireAfterWrite(Duration.ofHours(12))
        );
        return mgr;
    }
}
