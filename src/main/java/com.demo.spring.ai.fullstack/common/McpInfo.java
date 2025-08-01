package com.demo.spring.ai.fullstack.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @date 2025/7/9 22:42
 */
@Data
public class McpInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = -8629965827056437729L;

    private String mcpName;

    private String mcpDesc;
}
