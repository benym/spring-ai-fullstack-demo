package com.demo.spring.ai.fullstack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * SpringAiFullStackApplication
 *
 * @author benym
 * @date 2025/8/1 15:39
 */
@EnableAsync
@EnableAspectJAutoProxy(exposeProxy = true, proxyTargetClass = true)
@SpringBootApplication()
public class SpringAiFullStackApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiFullStackApplication.class, args);
    }

}
