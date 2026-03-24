package com.wx.rag.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import java.util.Random;
import java.util.function.Function;

@Configuration // 💡 改为配置类
public class WeatherFunction {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeatherFunction.class);
    private final Random random = new Random();
    final int[] temperatures = {-125, 15, -255, 25};

    // 1. 定义入参结构
    public record Request(String location) {}
    // 2. 定义出参结构
    public record Response(String content) {}

    @Bean
    @Description("Get the current weather for a given location") // 💡 必须有描述
    public Function<Request, Response> weatherFunctionBean() {
        return request -> {
            LOGGER.info("WeatherTool called with location: {}", request.location());
            int temperature = temperatures[random.nextInt(temperatures.length)];
            String result = "The current weather in " + request.location() + " is sunny, " + temperature + "°C.";
            return new Response(result);
        };
    }
}