package com.bantanger.im.tcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.support.GenericApplicationContext;

/**
 * =================================== IM-WhaleShark ========================================
 * 运行该模块需要配置 parameter arguments
 * window 版本
 * --customOption=E:\JavaProjectManager\im-whale-shark\im-tcp\src\main\resources\config.yml
 * mac 版本
 * --customOption=/Users/xxx/IdeaProjects/im-whale-shark/im-tcp/src/main/resources/config.yml
 * ==========================================================================================
 *
 * @author: BanTanger 半糖
 * @create: 2023-09-18 15:31
 */
@SpringBootApplication(scanBasePackages = {
        "com.bantanger.im.tcp",
        // 这里是因为 feign 是在 tcp 层做的，需要被 tcp 层的 springboot 扫描管理
        "com.bantanger.im.service.**.extensionpost.impl"
})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        GenericApplicationContext context = new GenericApplicationContext();
        // 允许Bean覆盖，后面的BeanDefinition能覆盖前面的
        context.setAllowBeanDefinitionOverriding(true) ;
    }

}
