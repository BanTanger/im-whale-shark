package com.bantanger.codegen.processor.mapper;

/**
 * @Author: Gim
 * @Date: 2019/11/25 14:14
 * @Description:
 */
public @interface GenMapper {

    String pkgName();

    String sourcePath() default "src/main/java";

    boolean overrideSource() default false;
}
