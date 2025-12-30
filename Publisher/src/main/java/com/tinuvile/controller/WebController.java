package com.tinuvile.controller;

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

    /**
     * 首页 - 重定向到静态HTML文件
     */
    @GetMapping("/")
    public String index(Model model) {
        return "redirect:/index.html";
    }

    /**
     * 发布控制页面 - 重定向到静态HTML文件
     */
    @GetMapping("/publisher")
    public String publisher(Model model) {
        return "redirect:/publisher.html";
    }

    /**
     * 系统监控页面 - 重定向到静态HTML文件
     */
    @GetMapping("/monitor")
    public String monitor(Model model) {
        return "redirect:/monitor.html";
    }

    /**
     * 系统布局页面 - 重定向到静态HTML文件
     */
    @GetMapping("/layout")
    public String layout(Model model) {
        return "redirect:/layout.html";
    }
}