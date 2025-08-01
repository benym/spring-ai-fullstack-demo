package com.demo.spring.ai.fullstack.controller;

import com.demo.spring.ai.fullstack.common.McpInfo;
import com.demo.spring.ai.fullstack.mcp.ToolRegistryService;
import com.rpamis.common.dto.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * mcp
 */
@RestController
@RequestMapping("/mcp")
@Slf4j
public class McpController {

    @Resource
    private ToolRegistryService toolRegistryService;

    @GetMapping("/services")
    public Response<List<McpInfo>> getAllMcpServices() {
        List<ToolRegistryService.ToolDescription> registeredTools = toolRegistryService.getRegisteredTools();
        List<McpInfo> result = registeredTools.stream().map(registeredTool -> {
            McpInfo mcpInfo = new McpInfo();
            mcpInfo.setMcpName(registeredTool.name());
            mcpInfo.setMcpDesc(registeredTool.description());
            return mcpInfo;
        }).collect(Collectors.toList());
        log.info("getAllMcpServices: {}", result);
        return Response.success(result);
    }
}
