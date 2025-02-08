package com.bantanger.im.service.rabbitmq.process;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/2 10:50
 */
public class ProcessFactory {

    private static BaseProcess defaultProcess;

    static {
        defaultProcess = new BaseProcess() {
            @Override
            public void processBefore() {

            }

            @Override
            public void processAfter() {

            }
        };
    }

    public static BaseProcess getMessageProcess(Integer command) {
        // TODO 简易策略模式，后期可自由调节使用什么策略
        return defaultProcess;
    }

}
