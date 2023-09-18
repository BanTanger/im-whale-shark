package com.bantanger.im.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author BanTanger 半糖
 * @Date 2023/9/16 8:57
 */
@Data
@Component
@ConfigurationProperties(prefix = "im")
public class IMConfig {

    /**
     * tcp 绑定的端口号
     */
    private Integer tcpPort;
    /**
     * websocket 绑定的端口号
     */
    private Integer webSocketPort;
    /**
     * boss 线程数 默认为 1
     */
    private Integer bossThreadSize;
    /**
     * work 线程数
     */
    private Integer workThreadSize;
    /**
     * 心跳超时时间
     */
    private Long heartBeatTime;
    /**
     * 分布式 Id 区分服务
     */
    private Integer brokerId;
    /**
     * Feign RPC 连接 TCP层和业务层内部地址
     */
    private String logicUrl;

    /**
     * 端登录策略类型
     */
    private Integer loginModel;

}
