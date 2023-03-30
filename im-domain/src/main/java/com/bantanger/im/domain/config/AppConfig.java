package com.bantanger.im.domain.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/29 8:57
 */
@Data
@Component
@ConfigurationProperties(prefix = "appconfig")
public class AppConfig {

    /**
     * zk 连接地址
     */
    private String zkAddr;

    /**
     * zk 最大超时时长
     */
    private Integer zkConnectTimeOut;

    /**
     * im 管道路由策略
     */
    private Integer imRouteModel;

    /**
     * 一致性哈希所使用的底层数据结构
     */
    private Integer consistentHashModel;
}
