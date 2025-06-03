package com.bantanger.im.infrastructure.support.postprocessor;

import java.util.Comparator;
import java.util.List;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

/**
 * 扩展点管理类 Spring IoC 版
 * 负责扩展点的管理和驱动，通过扩展点自定义的优先级进行排序调度
 *
 * @author BanTanger 半糖
 * @Date 2023/12/16 23:08
 */
@Configuration
public class PostProcessorContainerIoC<T> implements ApplicationContextAware {

    private Class<? extends BasePostProcessor<T>> monitorPostProcessorClass;

    private ApplicationContext applicationContext;

    public static <T> PostProcessorContainerIoC getInstance(
            Class<? extends BasePostProcessor<T>> monitorPostProcessorClass) {

        PostProcessorContainerIoC<T> postProcessorContainer = new PostProcessorContainerIoC<>();
        postProcessorContainer.monitorPostProcessorClass = monitorPostProcessorClass;
        return postProcessorContainer;
    }

    public boolean handleBefore(
        PostContext<T> postContext) {
        List<? extends BasePostProcessor<T>> postProcessors = (List<? extends BasePostProcessor<T>>)
                applicationContext.getBeansOfType(monitorPostProcessorClass).values();

        if (CollectionUtils.isEmpty(postProcessors)) {
            return true;
        }

        // 优先级越高越靠近内核
        postProcessors.sort(Comparator.comparingInt((BasePostProcessor<T> o) -> o.getPriority()));

        // 执行所有扩展点
        for (BasePostProcessor<T> postProcessor : postProcessors) {
            postProcessor.handleBefore(postContext);
        }

        return false;
    }

    public void handleAfter(PostContext<T> postContext) {
        List<? extends BasePostProcessor<T>> postProcessors = (List<? extends BasePostProcessor<T>>)
                applicationContext.getBeansOfType(monitorPostProcessorClass).values();

        if (CollectionUtils.isEmpty(postProcessors)) {
            return;
        }

        // 优先级越高越靠近内核
        postProcessors.sort(Comparator.comparingInt((BasePostProcessor<T> o) -> o.getPriority()));

        // 执行所有扩展点
        for (BasePostProcessor<T> postProcessor : postProcessors) {
            postProcessor.handleAfter(postContext);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
