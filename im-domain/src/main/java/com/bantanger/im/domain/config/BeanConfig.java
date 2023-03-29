package com.bantanger.im.domain.config;

import com.bantanger.im.service.route.RouteHandler;
import com.bantanger.im.service.route.algroithm.hash.ConsistentHashHandler;
import com.bantanger.im.service.route.algroithm.hash.TreeMapConsistentHash;
import com.bantanger.im.service.route.algroithm.random.RandomHandler;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
@Configuration
public class BeanConfig {

    @Resource
    private AppConfig appConfig;

    @Bean
    public ZkClient buildZkClient() {
        return new ZkClient(appConfig.getZkAddr(), appConfig.getZkConnectTimeOut());
    }

    @Bean
    public RouteHandler routeHandler() {
//        return new RandomHandler();
        ConsistentHashHandler consistentHashHandler = new ConsistentHashHandler();
        TreeMapConsistentHash treeMapConsistentHash = new TreeMapConsistentHash();
        consistentHashHandler.setHash(treeMapConsistentHash);
        return consistentHashHandler;
    }

}
