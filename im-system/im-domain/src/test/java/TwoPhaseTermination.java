import lombok.extern.slf4j.Slf4j;


/**
 * 两阶段终止模式
 * 概念：  在一个线程T1中“优雅”终止线程T2  ，这里优雅的意思是给T2一个料理后事的机会  而不是直接停止
 * 错误思路 ：
 * 1，使用线程的stop方法  stop方法会真正地杀死线程，如果这个时候线程锁住了共享资源，那么当他被杀死后就没有机会释放锁，其他线程也
 * 永远无法获得锁
 * 2，使用System.exit(in)方法终止线程
 * 目的仅仅是终止一个线程，但是该方法会让整个程序都停止
 * <p>
 * 区别
 * interrupted()： 判断当前线程是否被打断 会清除打断标记
 * isInterrupted(): 判断当前线程是否被打断 不会清除打断标记
 */
@Slf4j(topic = "TwoPhaseTermination")
public class TwoPhaseTermination {
    public static void main(String[] args) throws InterruptedException {
        //创建对象
        TwoPhaseTerminationMonitor twoPhaseTerminationMonitor = new TwoPhaseTerminationMonitor();
        //开启线程
        log.debug("开启线程");
        twoPhaseTerminationMonitor.start();
        Thread.sleep(3500);
        log.debug("关闭线程");
        twoPhaseTerminationMonitor.stop();

    }
}

/**
 * 监控类
 */
@Slf4j(topic = "TwoPhaseTerminationMonitor")
class TwoPhaseTerminationMonitor {
    private Thread monitor;
    //启动监控线程
    public void start() {
        monitor = new Thread(() -> {
            while (true) {
                //拿到当前线程
                if (Thread.currentThread().isInterrupted()) {
                    //说明此时被打断了
                    log.debug("料理后事");
                    return;
                } else {
                    try {
                        Thread.sleep(1000);//这里可能被打断   这里是不正常的打断
                        log.debug("实行监控记录");//这里是被正常的打断  被打断之后标记为true 那么可以正常的结束
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        //sleep被打断之后 会把打断标记变为false
                        //将打断标记变为true 才能结束此线程的工作
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        monitor.start();
    }

    //停止监控线程
    public void stop() {
        monitor.interrupt();
    }

}
