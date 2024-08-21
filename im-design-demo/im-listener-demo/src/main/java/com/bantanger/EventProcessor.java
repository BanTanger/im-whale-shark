package com.bantanger;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class EventProcessor {

    public EventProcessor(ApplicationEventPublisher eventPublisher) {
        Thread t = new Thread(() -> {
            eventPublisher.publishEvent(new PackApplicationEvent("自定义事件", EventProcessor.this.toString()));
        });
        t.start() ;
        try {
            System.out.println("线程启动，等待执行完成...");
            t.join() ;
        } catch (InterruptedException e) {
            System.err.printf("线程中断: %s, 错误: %s%n", Thread.currentThread().getName(), e.getMessage()) ;
        }
    }
}
