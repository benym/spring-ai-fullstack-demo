package com.demo.spring.ai.fullstack.vector;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * VectorProperties
 *
 * @author benym
 * @date 2025/8/1 17:57
 */
@ConfigurationProperties(prefix = VectorProperties.VECTOR)
@Setter
@Getter
public class VectorProperties {

    public static final String VECTOR = "demo.ai.vector";

    private VectorType vectorType;

    public enum VectorType {

        /**
         * Use the SIMPLE
         */
        SIMPLE,

        /**
         * Use the ELASTICSEARCH
         */
        ELASTICSEARCH,

    }
}
