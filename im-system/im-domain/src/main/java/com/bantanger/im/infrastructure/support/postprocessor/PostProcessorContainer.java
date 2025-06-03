package com.bantanger.im.infrastructure.support.postprocessor;

import com.bantanger.im.service.utils.BeanPostProcessorUtil;
import java.util.Comparator;
import java.util.List;
import org.springframework.util.CollectionUtils;

/**
 * 扩展点管理类 非 IoC 组件版
 * 负责扩展点的管理和驱动，通过扩展点自定义的优先级进行排序调度
 *
 * @author BanTanger 半糖
 * @Date 2023/12/16 23:08
 */
public class PostProcessorContainer<T> {

    private Class<? extends BasePostProcessor<T>> monitorPostProcessorClass;

    public static <T> PostProcessorContainer getInstance(
            Class<? extends BasePostProcessor<T>> monitorPostProcessorClass) {

        PostProcessorContainer<T> postProcessorContainer = new PostProcessorContainer<>();
        postProcessorContainer.monitorPostProcessorClass = monitorPostProcessorClass;
        return postProcessorContainer;
    }

    public boolean handleBefore(
        PostContext<T> postContext) {
        List<BasePostProcessor> postProcessors =
                BeanPostProcessorUtil.getPostProcessorMap().get(monitorPostProcessorClass);

        if (CollectionUtils.isEmpty(postProcessors)) {
            return true;
        }

        // 优先级越高越靠近内核
        postProcessors.sort(Comparator.comparingInt(
            BasePostProcessor::getPriority));

        boolean isContinue = true;
        // 执行所有扩展点
        for (BasePostProcessor<T> postProcessor : postProcessors) {
            isContinue |= postProcessor.handleBefore(postContext);
        }

        return isContinue;
    }

    public void handleAfter(PostContext<T> postContext) {
        List<BasePostProcessor> postProcessors =
                BeanPostProcessorUtil.getPostProcessorMap().get(monitorPostProcessorClass);

        if (CollectionUtils.isEmpty(postProcessors)) {
            return;
        }

        // 优先级越高越靠近内核
        postProcessors.sort(Comparator.comparingInt(
            BasePostProcessor::getPriority));

        // 执行所有扩展点
        for (BasePostProcessor<T> postProcessor : postProcessors) {
            postProcessor.handleAfter(postContext);
        }
    }

}
