package com.tinuvile.controller;

import com.tinuvile.service.PublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web页面控制器
 * 
 * @author tinuvile
 */
@Controller
public class WebController {
    
    @Autowired
    private PublisherService publisherService;
    
    /**
     * 首页
     */
    @GetMapping("/")
    public String index(Model model) {
        try {
            PublisherService.SystemInfo systemInfo = publisherService.getSystemInfo();
            model.addAttribute("systemInfo", systemInfo);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        
        return "index";
    }
    
    /**
     * 发布控制页面
     */
    @GetMapping("/publisher")
    public String publisher(Model model) {
        return "publisher";
    }
    
    /**
     * 系统监控页面
     */
    @GetMapping("/monitor")
    public String monitor(Model model) {
        return "monitor";
    }
}