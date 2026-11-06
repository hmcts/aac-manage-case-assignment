package uk.gov.hmcts.reform.managecase.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import uk.gov.hmcts.reform.managecase.ApplicationParams;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotifyClientConfigurationTest {

    private final NotifyClientConfiguration configuration = new NotifyClientConfiguration();

    @Test
    void shouldCreateNotificationClientWithConfiguredKey() {
        ApplicationParams applicationParams = mock(ApplicationParams.class);
        when(applicationParams.getNotifyApiKey()).thenReturn("notify-api-key");

        assertThat(configuration.notificationClient(applicationParams)).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "-1, 1", "1, -1"})
    void shouldRejectInvalidPoolConfiguration(int poolSize, int queueCapacity) {
        assertThrows(IllegalArgumentException.class,
            () -> configuration.notificationTaskExecutor(poolSize, queueCapacity));
    }

    @Test
    void shouldExecuteOverflowOnCallerAndDrainQueuedTasksOnShutdown() throws Exception {
        ThreadPoolTaskExecutor executor = configuration.notificationTaskExecutor(1, 1);
        executor.initialize();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Thread> overflowThread = new AtomicReference<>();
        CompletableFuture<Void> queued;
        try {
            executor.execute(() -> {
                started.countDown();
                try {
                    if (!release.await(5, SECONDS)) {
                        throw new IllegalStateException("Worker was not released by the test");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            });
            assertTrue(started.await(5, SECONDS), "The worker must start before filling the queue");
            queued = CompletableFuture.runAsync(() -> { }, executor);
            executor.execute(() -> overflowThread.set(Thread.currentThread()));

            assertThat(overflowThread.get()).isSameAs(Thread.currentThread());
            assertThat(executor.getThreadPoolExecutor().getQueue()).hasSize(1);
        } finally {
            release.countDown();
            executor.shutdown();
        }

        assertThat(queued).isCompleted();
    }

    @Test
    void shouldRejectTasksAfterShutdown() {
        ThreadPoolTaskExecutor executor = configuration.notificationTaskExecutor(1, 1);
        executor.initialize();
        executor.shutdown();

        assertThrows(RejectedExecutionException.class, () -> executor.execute(() -> { }));
    }
}
