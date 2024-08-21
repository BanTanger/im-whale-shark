package com.bantanger;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class PackApplicationListener implements ApplicationListener<PackApplicationEvent> {

    @Override
    public void onApplicationEvent(PackApplicationEvent event) {
        System.out.printf("接收到事件消息: %s, 数据: %s%n", event.getMessage(), event.getSource().toString()) ;
    }

}
