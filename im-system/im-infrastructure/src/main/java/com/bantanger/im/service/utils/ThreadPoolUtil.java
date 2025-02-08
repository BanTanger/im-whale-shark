package com.bantanger.im.service.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池工具类
 * 封装开辟、优雅关闭方式
 *
 * @author BanTanger 半糖
 * @Date 2023/7/13 9:47
 */
@Slf4j
public class ThreadPoolUtil {

    /**
     * CPU 核数
     */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    /**
     * IO 处理线程数
     */
    private static final int IO_MAX = Math.max(2, 2 * CPU_COUNT);
    /**
     * 空闲线程最大保活时限，单位为秒
     */
    private static final int KEEP_ALIVE_SECOND = 60;
    /**
     * 有界阻塞队列容量上限
     */
    private static final int QUEUE_SIZE = 10000;

    /**
     * <p>看到一些开源代码是将线程池设置成懒汉式，只有等代码需要用到线程池再加载，
     * 我觉得并不是最正确的一种形式</p>
     * 原因是线程池应当在应用程序启动时就会初始化，因为它是一个全局资源，
     * 提前初始化可以避免再需要等待初始化的时间延迟<br />
     * 特别是在多线程环境下可能会导致竞态条件或者其他并发问题
     */
    private static class IoIntenseTargetThreadPoolHolder {

        private String threadName;

        private IoIntenseTargetThreadPoolHolder(String threadName) {
            this.threadName = threadName;
        }

        private final AtomicInteger NUM = new AtomicInteger(0);

        private final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
                IO_MAX,
                IO_MAX,
                KEEP_ALIVE_SECOND,
                TimeUnit.SECONDS,
                // 任务队列存储超过核心线程数的任务
                new LinkedBlockingDeque<>(QUEUE_SIZE),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setDaemon(true);
                    thread.setName("[" + threadName + "] message-process-thread-" + NUM.getAndIncrement());
                    return thread;
                }
        );

        {
            log.info("线程池已经初始化");
            EXECUTOR.allowCoreThreadTimeOut(true);
            // JVM 关闭时的钩子函数
            Runtime.getRuntime().addShutdownHook(
                    new ShutdownHookThread("IO 密集型任务线程池", (Callable<Void>) () -> {
                        shutdownThreadPoolGracefully(EXECUTOR);
                        return null;
                    })
            );
        }

    }

    public static ThreadPoolExecutor getIoTargetThreadPool(String threadPool) {
        return new IoIntenseTargetThreadPoolHolder(threadPool).EXECUTOR;
    }

    private static void shutdownThreadPoolGracefully(ThreadPoolExecutor threadPool) {
        // 如果已经关闭则返回
        if (!(threadPool instanceof ExecutorService) || threadPool.isTerminated()) {
            return;
        }
        try {
            // 拒绝接收新任务，线程池状态变成 SHUTDOWN
            threadPool.shutdown();
        } catch (SecurityException | NullPointerException e) {

            return;
        }
        try {
            // 等待 60 秒，用户程序主动调用 awaitTermination 等待线程池的任务执行完毕
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                // 调用 shutdownNow() 强制停止所有任务，线程池状态变成 STOP
                threadPool.shutdownNow();
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.out.println("线程池任务未执行完毕！");
                }
            }
        } catch (InterruptedException e) {
            // 捕获异常，重新调用 shutdownNow() 方法
            threadPool.shutdownNow();
        }
        // 仍然没有关闭，循环关闭 1000 次，每次等待 10 毫秒
        int loopCount = 1000;
        if (!threadPool.isTerminated()) {
            try {
                for (int i = 0; i < loopCount; i++) {
                    if (threadPool.awaitTermination(10, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                    threadPool.shutdownNow();
                }
            } catch (Throwable e) {
                log.error(e.getMessage());
            }
        }
    }

}
