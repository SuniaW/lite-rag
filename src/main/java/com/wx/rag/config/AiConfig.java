/*
 * Copyright (c) 2026 the original author or authors. All rights reserved.
 *
 * @author wangxu
 * @since 2026
 */
package com.wx.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    // 2. 流式天气客户端：绑定 ToolCallbackProvider（含 getWeather 工具），用于 .stream()
    @Bean
    public ChatClient weatherStreamingChatClient(OpenAiChatModel openAiChatModel,
        ToolCallbackProvider toolCallbackProvider) { // 💡 显式注入 OpenAI 模型 + 工具
        return ChatClient.builder(openAiChatModel)
            .defaultSystem("你是一个专业的气象助手。请友好地回答天气情况。")
            .defaultToolCallbacks(toolCallbackProvider) // 💡 ToolCallbackProvider 必须用 defaultToolCallbacks 绑定（defaultTools(Object...) 不识别 provider）
            .build();
    }
}
