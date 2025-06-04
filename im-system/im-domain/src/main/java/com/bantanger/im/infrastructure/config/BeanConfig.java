package com.bantanger.im.infrastructure.config;

import com.bantanger.im.common.enums.route.RouteHashMethodEnum;
import com.bantanger.im.common.enums.route.UrlRouteModelEnum;
import com.bantanger.im.infrastructure.route.RouteHandler;
import com.bantanger.im.infrastructure.route.algroithm.hash.AbstractConsistentHash;
import com.bantanger.im.infrastructure.support.ids.SnowflakeIdWorker;
import jakarta.annotation.Resource;
import java.lang.reflect.Method;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
@Configuration
public class BeanConfig {

    @Resource
    private AppConfig appConfig;

    @Bean
    public RouteHandler routeHandler() throws Exception {

        Integer imRouteModel = appConfig.getImRouteModel();
        String routeModel = "";

        // 配置文件指定使用哪种路由策略
        UrlRouteModelEnum handler = UrlRouteModelEnum.getHandler(imRouteModel);
        routeModel = handler.getClazz();

        // 反射机制调用具体的类对象执行对应方法
        RouteHandler routeHandler = (RouteHandler) Class.forName(routeModel).newInstance();
        // 特判，一致性哈希可以指定底层数据结构
        if (UrlRouteModelEnum.HASH.equals(handler)) {

            Method setHash = Class.forName(routeModel).getMethod("setHash", AbstractConsistentHash.class);
            Integer consistentHashModel = appConfig.getConsistentHashModel();
            String hashModel = "";

            RouteHashMethodEnum hashHandler = RouteHashMethodEnum.getHandler(consistentHashModel);
            hashModel = hashHandler.getClazz();
            AbstractConsistentHash consistentHash = (AbstractConsistentHash) Class.forName(hashModel).newInstance();
            setHash.invoke(routeHandler, consistentHash);
        }
        return routeHandler;
    }

    @Bean
    public SnowflakeIdWorker buildSnowflakeSeq() {
        return new SnowflakeIdWorker(0);
    }

}
