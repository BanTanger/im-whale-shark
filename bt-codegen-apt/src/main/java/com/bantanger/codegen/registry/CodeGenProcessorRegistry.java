package com.bantanger.codegen.registry;

import com.bantanger.codegen.spi.CodeGenProcessor;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author chensongmin
 * @description 注解处理器注册中心 通过SPI 加载所有的CodeGenProcessor 识别要处理的annotation标记类
 * @date 2025/1/7
 */
public final class CodeGenProcessorRegistry {

    private static Map<String, ? extends CodeGenProcessor> PROCESSORS;


    private CodeGenProcessorRegistry() {
        throw new UnsupportedOperationException();
    }

    /**
     * 注解处理器要处理的注解集合
     *
     * @return
     */
    public static Set<String> getSupportedAnnotations() {
        return PROCESSORS.keySet();
    }

    public static CodeGenProcessor find(String annotationClassName) {
        return PROCESSORS.get(annotationClassName);
    }

    /**
     * SPI 机制加载所有的 processor
     *
     * @return
     */
    public static void initProcessors() {
        final Map<String, CodeGenProcessor> map = new LinkedHashMap<>();
        // 通过 ServiceLoader (SPI 机制)加载所有的CodeGenProcessor
        ServiceLoader.load(CodeGenProcessor.class, CodeGenProcessor.class.getClassLoader())
            .forEach(next -> {
                Class<? extends Annotation> annotation = next.getAnnotation();
                map.put(annotation.getName(), next);
            });
        PROCESSORS = map;
    }

}
