package com.bantanger.im.service.strategy.pay;

import org.springframework.beans.factory.config.ServiceLocatorFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 定义配置类，使用 @ServiceLocatorFactoryBean 来创建一个动态代理对象，实现支付工厂接口
 * @author BanTanger 半糖
 * @Date 2023/7/28 17:17
 */
@Configuration
public class PaymentConfig {

    @Bean(name = "paymentFactoryBean")
    public ServiceLocatorFactoryBean serviceLocatorFactoryBean() {
        ServiceLocatorFactoryBean factoryBean = new ServiceLocatorFactoryBean();
        // 设置要代理的接口
        factoryBean.setServiceLocatorInterface(PaymentFactory.class);
        return factoryBean;
    }

}
