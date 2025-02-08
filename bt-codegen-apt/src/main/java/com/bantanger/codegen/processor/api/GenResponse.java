package com.bantanger.codegen.processor.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GenResponse {

    String pkgName();

    String sourcePath() default "src/main/java";

    boolean overrideSource() default false;
}
