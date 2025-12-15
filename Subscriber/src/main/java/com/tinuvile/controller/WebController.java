package com.tinuvile.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web页面控制器
 * 
 * @author tinuvile
 */
@Controller
public class WebController {
    
    /**
     * 首页 - 重定向到静态HTML文件
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/index.html";
    }
    
    /**
     * 订阅者页面 - 重定向到静态HTML文件
     */
    @GetMapping("/subscriber")
    public String subscriber() {
        return "redirect:/subscriber.html";
    }
    
    /**
     * 监控页面 - 重定向到静态HTML文件
     */
    @GetMapping("/monitor")
    public String monitor() {
        return "redirect:/monitor.html";
    }
}
