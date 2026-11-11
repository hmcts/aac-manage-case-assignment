package uk.gov.hmcts.reform.managecase.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.service.notify.NotificationClient;

import java.util.concurrent.RejectedExecutionException;

@Configuration
public class NotifyClientConfiguration {

    @Bean
    public ThreadPoolTaskExecutor notificationTaskExecutor(
        @Value("${notify.email.pool-size:4}") int poolSize,
        @Value("${notify.email.queue-capacity:100}") int queueCapacity) {
        if (poolSize < 1 || queueCapacity < 0) {
            throw new IllegalArgumentException(
                "Notification pool size must be positive and queue capacity non-negative");
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("noc-notification-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler((task, pool) -> {
            if (pool.isShutdown()) {
                throw new RejectedExecutionException("Notification executor is shutting down");
            }
            task.run();
        });
        return executor;
    }

    @Bean
    public NotificationClient notificationClient(@Autowired ApplicationParams applicationParams) {
        return new NotificationClient(applicationParams.getNotifyApiKey());
    }
}
