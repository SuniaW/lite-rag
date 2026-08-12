package com.wx.rag.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库检索工具（Spring AI @Tool 风格）
 * <p>
 * 把本项目的核心能力 —— Milvus 向量检索 —— 发布成 MCP 工具，
 * 任何 MCP 客户端（Claude Desktop、Cursor、其他 Agent）都能直接检索知识库。
 */
@Component
@Slf4j
public class KnowledgeBaseTools {

    private final VectorStore vectorStore;

    public KnowledgeBaseTools(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 从政策文档知识库中检索与问题相关的文档片段
     *
     * @param query 检索的问题或关键词
     * @return 命中的文档片段列表（按相似度排序）
     */
    @Tool(description = "从政策文档知识库中检索与问题相关的文档片段。当用户询问知识库中可能存在的政策、制度、文档内容时使用。")
    public String searchKnowledgeBase(@ToolParam(description = "检索的问题或关键词") String query) {
        log.info("MCP 工具调用知识库检索，query: {}", query);
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(3)
                .similarityThreshold(0.5) // 过滤杂音，减少上下文长度
                .build();

            List<Document> docs = vectorStore.similaritySearch(searchRequest);
            if (docs.isEmpty()) {
                return "知识库中未找到相关内容。";
            }

            return docs.stream()
                .map(doc -> "- " + doc.getText())
                .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("知识库检索工具异常", e);
            return "知识库检索失败：" + e.getMessage();
        }
    }
}
