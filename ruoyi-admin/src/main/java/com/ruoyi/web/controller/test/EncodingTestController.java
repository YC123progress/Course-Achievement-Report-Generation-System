package com.ruoyi.web.controller.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.Charset;

@RestController
@RequestMapping("/test")
public class EncodingTestController {
    
    private static final Logger log = LoggerFactory.getLogger(EncodingTestController.class);
    
    @GetMapping("/encoding")
    public String testEncoding() {
        String testText = "中文编码测试：课程目标达成评价系统";
        
        // 打印系统编码信息
        log.info("=== 编码测试 ===");
        log.info("默认字符集: {}", Charset.defaultCharset());
        log.info("file.encoding: {}", System.getProperty("file.encoding"));
        log.info("user.language: {}", System.getProperty("user.language"));
        log.info("user.country: {}", System.getProperty("user.country"));
        log.info("测试中文: {}", testText);
        
        return "编码测试完成，请查看控制台日志";
    }
} 