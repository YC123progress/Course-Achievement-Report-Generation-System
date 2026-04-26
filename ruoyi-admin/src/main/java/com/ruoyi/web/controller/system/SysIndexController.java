package com.ruoyi.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ruoyi.common.config.RuoYiConfig;

@Controller
public class SysIndexController
{
    @Autowired
    private RuoYiConfig ruoyiConfig;

    /**
     * 访问首页
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    /**
     * 登录页刷新回退
     */
    @GetMapping("/login")
    public String login() {
        return "forward:/index.html";
    }

    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "OK";
    }
}