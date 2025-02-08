package com.bantanger.codegen.spi;

import java.lang.annotation.Annotation;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * @author chensongmin
 * @description 工具类上下文holder
 * @date 2025/1/7
 */
public interface CodeGenProcessor {

    /**
     * 获取需要解析的类上的注解信息
     *
     * @return
     */
    Class<? extends Annotation> getAnnotation();

    /**
     * 获取注解上指定的包路径信息
     *
     * @return
     */
    String generatePackage(TypeElement typeElement);

    /**
     * 代码生成逻辑
     * @param typeElement 目标类对象 or 接口对象
     * @param roundEnvironment
     * @throws Exception
     */
    void generate(TypeElement typeElement, RoundEnvironment roundEnvironment) throws Exception;
}
