package com.hisagent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 * 为不同业务场景提供专用线程池
 */
@Slf4j
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    /**
     * 核心业务线程池
     * 用于处理核心业务流程
     */
    @Bean(name = "coreBusinessExecutor")
    public Executor coreBusinessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("core-business-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("Core business thread pool initialized");
        return executor;
    }

    /**
     * 语音处理线程池
     * 用于处理语音转写等耗时操作
     */
    @Bean(name = "speechProcessingExecutor")
    public Executor speechProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("speech-processing-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("Speech processing thread pool initialized");
        return executor;
    }

    /**
     * LLM 调用线程池
     * 用于处理大模型 API 调用
     */
    @Bean(name = "llmCallsExecutor")
    public Executor llmCallsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(150);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("llm-calls-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("LLM calls thread pool initialized");
        return executor;
    }

    /**
     * HIS 集成线程池
     * 用于处理外部 HIS 系统调用
     */
    @Bean(name = "hisIntegrationExecutor")
    public Executor hisIntegrationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("his-integration-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("HIS integration thread pool initialized");
        return executor;
    }

    /**
     * 异步任务线程池
     * 用于处理一般异步任务
     */
    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-tasks-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("Async tasks thread pool initialized");
        return executor;
    }
}
