package com.bantanger.codegen.processor.updater;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
public @interface GenUpdater {

    String pkgName();

    String sourcePath() default "src/main/java";

    boolean overrideSource() default false;
}
