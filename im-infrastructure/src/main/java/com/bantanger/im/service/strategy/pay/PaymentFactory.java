package com.bantanger.im.service.strategy.pay;

import org.springframework.stereotype.Component;

/**
 * 定义支付工厂的接口，根据类型获取实例的方法
 * PaymentFactory接口也称为服务定位接口，
 * 程序在调用的时候可以通过该接口里面的方法中的参数值能够拿到属于该方法返回值类型的子类对象
 * @author BanTanger 半糖
 * @Date 2023/7/28 17:19
 */
@Component
public interface PaymentFactory {

    Payment getPayment(String type);

}
