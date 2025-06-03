package com.bantanger.im.infrastructure.utils;

import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/18 9:21
 */
@Slf4j
public class ShutdownHookThread extends Thread {
    private volatile boolean hasShutdown = false;
    private final Callable<?> callback;

    /**
     * 创建标准钩子线程异步处理关闭线程池逻辑
     * @param name
     * @param callback 回调钩子方法
     */
    public ShutdownHookThread(String name, Callable<?> callback) {
        super("JVM关闭, 触发钩子函数处理(" + name + ")");
        this.callback = callback;
    }

    @Override
    public void run() {
        synchronized (this) {
            log.info(getName() + " 线程开始运转...");
            if (!this.hasShutdown) {
                this.hasShutdown = true;
                long beginTime = System.currentTimeMillis();
                try {
                    this.callback.call();
                } catch (Exception e) {
                    log.error(getName() + " 线程出现异常：", e.getMessage());
                }
                long consumingTimeTotal = System.currentTimeMillis() - beginTime;
                log.info(getName() + " 耗时(s): " + consumingTimeTotal);
            }
        }
    }
}
