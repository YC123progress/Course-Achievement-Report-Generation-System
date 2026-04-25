package com.ruoyi;

// 必须导入的核心类
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 若使用路径变量注入
import org.springframework.beans.factory.annotation.Value;
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${ruoyi.luckysheet-path}")
    private String luckysheetPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置 luckysheet 静态资源映射
        registry.addResourceHandler("/luckysheet/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/ruoyi-ui/public/luckysheet/")
                .setCachePeriod(3600);
                
        // 配置默认的静态资源映射
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
                
        // 配置根路径重定向
        registry.addResourceHandler("/luckysheet")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/ruoyi-ui/public/luckysheet/index.html")
                .setCachePeriod(3600);
    }
}
