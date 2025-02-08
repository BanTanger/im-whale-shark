package com.bantanger.im.service.strategy.pay.impl;

import com.bantanger.im.service.strategy.pay.Payment;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author BanTanger 半糖
 * @Date 2023/7/28 17:22
 */
@Service("ali")
@Scope("prototype")
public class AliPayment implements Payment {
    @Override
    public void pay(double amount) {
        System.out.println("支付方式 ali, 消费 " + amount + " 元");
    }
}
