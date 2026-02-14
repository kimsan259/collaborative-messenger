package com.messenger.common.config;

import com.messenger.common.debug.RecentErrorLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    private final RecentErrorLogService recentErrorLogService;

    @Value("${async.report.core:1}")
    private int reportCore;
    @Value("${async.report.max:2}")
    private int reportMax;
    @Value("${async.report.queue:20}")
    private int reportQueue;

    @Value("${async.notification.core:1}")
    private int notificationCore;
    @Value("${async.notification.max:2}")
    private int notificationMax;
    @Value("${async.notification.queue:50}")
    private int notificationQueue;

    @Bean("reportGenerationExecutor")
    public Executor reportGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(reportCore);
        executor.setMaxPoolSize(reportMax);
        executor.setQueueCapacity(reportQueue);
        executor.setThreadNamePrefix("report-gen-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(notificationCore);
        executor.setMaxPoolSize(notificationMax);
        executor.setQueueCapacity(notificationQueue);
        executor.setThreadNamePrefix("notification-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable throwable, Method method, Object... params) -> {
            log.error("[async-error] method={}, params={}, error={}",
                    method.getName(), params, throwable.getMessage(), throwable);
            recentErrorLogService.capture("async", throwable.getMessage(), method.getName());
        };
    }
}
