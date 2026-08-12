package com.wx.rag.tool;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 天气查询工具（Spring AI @Tool 风格）
 * <p>
 * 通过 {@code @Tool} 注解暴露给大模型 / MCP 客户端：
 * 1. 本地 ChatClient 通过 ToolCallbackProvider 使用
 * 2. MCP Server 自动收集并发布为 MCP 工具（工具名 = getWeather）
 */
@Component
public class WeatherTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeatherTools.class);

    // 💡 生产环境请写在 application.yml / 环境变量里
    @Value("${amap.api.key}")
    private String amapApiKey;

    private static final String AMAP_URL = "https://restapi.amap.com/v3/weather/weatherInfo";

    private final RestClient restClient = RestClient.builder()
        .requestFactory(new JdkClientHttpRequestFactory())
        .build();

    // 改为 ConcurrentHashMap，保证线程安全
    private final Map<String, String> cityCodeMap = new ConcurrentHashMap<>();

    // 高德 API 的返回结构
    public record AmapWeatherResponse(String status, String info, List<Live> lives) {}

    public record Live(
        String province, String city, String weather,
        String temperature, String winddirection,
        String windpower, String humidity, String reporttime
    ) {}

    /**
     * 💡 在 Spring Boot 启动时，自动读取 resources 下的 adcode.csv 加载到内存中
     */
    @PostConstruct
    public void initCityCodeMap() {
        LOGGER.info("====== 开始加载城市 adcode 映射表 ======");
        try {
            // 读取 classpath 下的资源，兼容 Jar 包运行
            ClassPathResource resource = new ClassPathResource("adcode.csv");
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                boolean isFirstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) { // 跳过 CSV 表头
                        isFirstLine = false;
                        continue;
                    }

                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        String cityName = parts[0].trim();
                        String adcode = parts[1].trim();

                        // 1. 存入原始名称 (例如 "西安市")
                        cityCodeMap.put(cityName, adcode);

                        // 2. 贴心小优化：如果名字带"市"、"县"、"区"，顺便存一份不带后缀的
                        // 这样大模型传 "西安" 也能精准匹配到 "610100"
                        if (cityName.endsWith("市") || cityName.endsWith("县") || cityName.endsWith("区")) {
                            cityCodeMap.put(cityName.substring(0, cityName.length() - 1), adcode);
                        }
                    }
                }
            }
            LOGGER.info("====== adcode 映射表加载完成，共加载 {} 个城市节点 ======", cityCodeMap.size());
        } catch (Exception e) {
            LOGGER.error("加载 adcode.csv 失败，请检查文件格式和路径！", e);
        }
    }

    /**
     * 查询指定中国城市的实时天气
     *
     * @param location 标准的城市名称，例如 "西安"、"北京"、"上海"
     * @return 天气情况描述文本
     */
    @Tool(description = "查询指定中国城市的实时天气情况。参数必须是标准的城市名称，例如'西安'、'北京'、'上海'。")
    public String getWeather(@ToolParam(description = "城市名称，例如：西安") String location) {
        LOGGER.info("大模型请求查询天气，目标城市: {}", location);

        // 直接从内存 Map 中极速查找
        String adcode = cityCodeMap.get(location);

        if (adcode == null) {
            return "无法查询该城市天气，因为找不到对应城市的行政区划代码：" + location;
        }

        try {
            // 调用高德 API
            AmapWeatherResponse apiResponse = restClient.get()
                .uri(AMAP_URL + "?key={key}&city={city}&extensions=base",
                    amapApiKey, adcode)
                .retrieve()
                .body(AmapWeatherResponse.class);

            if (apiResponse != null && "1".equals(apiResponse.status()) && !apiResponse.lives().isEmpty()) {
                Live live = apiResponse.lives().get(0);
                String resultContext = String.format(
                    "%s%s当前天气：%s，气温：%s°C，风向：%s，风力：%s，湿度：%s%%，数据发布时间：%s",
                    live.province(), live.city(), live.weather(),
                    live.temperature(), live.winddirection(),
                    live.windpower(), live.humidity(), live.reporttime()
                );
                LOGGER.info("高德 API 返回: {}", resultContext);
                return resultContext;
            } else {
                return "调用高德天气 API 失败，未获取到有效数据。";
            }
        } catch (Exception e) {
            LOGGER.error("查询天气异常", e);
            return "天气查询服务出现网络异常，请稍后再试。";
        }
    }
}
