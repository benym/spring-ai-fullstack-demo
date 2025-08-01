package com.demo.spring.ai.fullstack.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池设置
 */
@Configuration
public class ThreadPoolExecutorConfig {

    @Bean("threadExecutor")
    public ThreadPoolTaskExecutor asyncTaskExecutor() {
        int cpu = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("threadExecutor-");
        threadPoolTaskExecutor.setCorePoolSize(cpu/2);
        threadPoolTaskExecutor.setMaxPoolSize(cpu);
        threadPoolTaskExecutor.setQueueCapacity(1000);
        threadPoolTaskExecutor.setKeepAliveSeconds(30);
        threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }
}
