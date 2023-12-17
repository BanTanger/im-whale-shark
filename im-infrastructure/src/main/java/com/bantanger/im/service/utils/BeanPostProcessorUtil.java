package com.bantanger.im.service.utils;

import com.bantanger.im.service.support.postprocessor.BasePostProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author BanTanger 半糖
 * @Date 2023/12/17 16:52
 */
public class BeanPostProcessorUtil {

    public static Map<Class<? extends BasePostProcessor>, List<BasePostProcessor>>
            postProcessorMap = new ConcurrentHashMap<>();

    public static Map<Class<? extends BasePostProcessor>, List<BasePostProcessor>> getPostProcessorMap() {
        return postProcessorMap;
    }

    public static void addPostProcessor(Class<? extends BasePostProcessor> father, Object son) {
        List<BasePostProcessor> orDefault = postProcessorMap.getOrDefault(father, new ArrayList<>());
        if (son instanceof BasePostProcessor) {
            orDefault.add((BasePostProcessor) son);
        }
        postProcessorMap.putIfAbsent(father, orDefault);
    }

}
