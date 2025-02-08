package com.bantanger.codegen.context;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * @author chensongmin
 * @description 工具类上下文holder
 * @date 2025/1/7
 */
public class ProcessingEnvironmentHolder {

    /**
     * 注解处理器上下文环境
     */
    public static final ThreadLocal<ProcessingEnvironment> environment = new ThreadLocal<>();

    public static void setEnvironment(ProcessingEnvironment pe){
        environment.set(pe);
    }

    public static ProcessingEnvironment getEnvironment(){
        return environment.get();
    }

}