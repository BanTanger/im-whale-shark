package com.bantanger.codegen.processor;

import lombok.Data;

/**
 * @author chensongmin
 * @description 默认名称上下文
 * @date 2025/1/8
 */
@Data
public class DefaultNameContext {

    private String voPackageName;

    private String voClassName;

    private String queryPackageName;

    private String queryClassName;

    private String updaterPackageName;

    private String updaterClassName;

    private String creatorPackageName;

    private String creatorClassName;

    private String mapperPackageName;

    private String mapperClassName;

    private String repositoryPackageName;

    private String repositoryClassName;

    private String servicePackageName;

    private String serviceClassName;

    private String implPackageName;

    private String implClassName;

    private String controllerPackageName;

    private String controllerClassName;

    /**
     * API 相关
     */
    private String createPackageName;

    private String createClassName;

    private String updatePackageName;

    private String updateClassName;

    private String queryRequestPackageName;

    private String queryRequestClassName;

    private String responsePackageName;

    private String responseClassName;

    private String feignPackageName;

    private String feignClassName;

}
