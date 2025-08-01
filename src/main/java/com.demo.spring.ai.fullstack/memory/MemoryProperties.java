package com.demo.spring.ai.fullstack.memory;

import com.demo.spring.ai.fullstack.mcp.MultiAgentMcpAssignProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MemoryProperties
 *
 * @author benym
 * @date 2025/8/1 17:31
 */
@ConfigurationProperties(prefix = MemoryProperties.MEMORY)
@Setter
@Getter
public class MemoryProperties {

    public static final String MEMORY = "demo.ai.memory";

    private MemoryType memoryType;

    public enum MemoryType {

        /**
         * Use the SIMPLE
         */
        SIMPLE,

        /**
         * Use the REDIS
         */
        REDIS,

    }
}
