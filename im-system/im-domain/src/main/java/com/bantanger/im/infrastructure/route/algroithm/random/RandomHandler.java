package com.bantanger.im.infrastructure.route.algroithm.random;

import com.bantanger.im.common.enums.user.UserErrorCode;
import com.bantanger.im.common.exception.ApplicationException;
import com.bantanger.im.infrastructure.route.RouteHandler;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机负载均衡
 * @author BanTanger 半糖
 * @Date 2023/3/29 8:49
 */
public class RandomHandler implements RouteHandler {
    @Override
    public String routeServer(List<String> values, String key) {
        int size = values.size();
        if (size == 0) {
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }
        int i = ThreadLocalRandom.current().nextInt(size);
        return values.get(i);
    }
}
