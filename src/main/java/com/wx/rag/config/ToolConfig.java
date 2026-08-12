package com.wx.rag.config;

import com.wx.rag.tool.KnowledgeBaseTools;
import com.wx.rag.tool.WeatherTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具注册中心
 * <p>
 * 1. 本地 ChatClient：AiConfig 中通过 defaultTools(provider) 绑定
 * 2. MCP Server：Spring AI 自动收集所有 ToolCallbackProvider Bean，
 *    把其中的 @Tool 方法发布为 MCP 工具（getWeather / searchKnowledgeBase）
 */
@Configuration
public class ToolConfig {

    @Bean
    public ToolCallbackProvider ragTools(WeatherTools weatherTools, KnowledgeBaseTools knowledgeBaseTools) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(weatherTools, knowledgeBaseTools)
            .build();
    }
}
