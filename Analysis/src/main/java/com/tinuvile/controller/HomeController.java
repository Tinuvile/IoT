package com.tinuvile.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 主页控制器
 * 
 * @author tinuvile
 */
@Controller
public class HomeController {
    
    /**
     * 根路径重定向到分析页面
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/analysis";
    }
}
