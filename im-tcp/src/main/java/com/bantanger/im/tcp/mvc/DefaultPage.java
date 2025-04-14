package com.bantanger.im.tcp.mvc;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author: BanTanger 半糖
 * @create: 2023-09-18 15:46
 */
@Configuration
public class DefaultPage implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        // 添加增强版微信风格IM系统页面的映射
        registry.addViewController("/wechat-plus").setViewName("forward:/wechat-plus.html");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
        WebMvcConfigurer.super.addViewControllers(registry);
    }

}
