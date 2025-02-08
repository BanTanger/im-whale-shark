package com.bantanger.im.service.support.postprocessor;

/**
 * 扩展点用于主流程和分支流程的连接
 * 也是后续扩展的切入点(或称之为对后续功能扩展的"预判")
 * @author BanTanger 半糖
 * @Date 2023/12/16 23:00
 */
public interface BasePostProcessor <T> {

    /**
     * 功能执行前的扩展功能执行 <br>
     *
     * @param postContext 数据流转上下文
     * @return 扩展逻辑失败是否会影响主流程执行，默认不影响，因此失败与否都会放权
     */
    default boolean handleBefore(PostContext<T> postContext) {
        return true;
    }

    /**
     * 功能执行后的扩展功能执行 <br>
     *
     * @param postContext 数据流转上下文
     */
    default void handleAfter(PostContext<T> postContext) {

    }

    /**
     * 扩展点有多个，通过优先级来管理执行顺序
     * @return
     */
    default int getPriority() {
        return 0;
    }

}
