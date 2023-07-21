package com.bantanger.im.service.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 优雅关闭线程池工具类
 *
 * @author BanTanger 半糖
 * @Date 2023/7/13 9:47
 */
public class ThreadPoolUtil {

    public static void shutdownThreadPoolGracefully(ExecutorService threadPool) {
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
        if (!threadPool.isTerminated()) {
            try {
                for (int i = 0; i < 1000; i++) {
                    if (threadPool.awaitTermination(10, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                    threadPool.shutdownNow();
                }
            } catch (Throwable e) {
                System.err.println(e.getMessage());
            }
        }
    }

}
