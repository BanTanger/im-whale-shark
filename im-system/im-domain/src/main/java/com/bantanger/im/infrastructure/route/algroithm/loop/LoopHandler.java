package com.bantanger.im.infrastructure.route.algroithm.loop;

import com.bantanger.im.common.BaseErrorCode;
import com.bantanger.im.common.exception.ApplicationException;
import com.bantanger.im.infrastructure.route.RouteHandler;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/29 10:30
 */
public class LoopHandler implements RouteHandler {

    private AtomicLong index = new AtomicLong();

    @Override
    public String routeServer(List<String> values, String key) {
        int size = values.size();
        if (size == 0) {
            throw new ApplicationException(BaseErrorCode.PARAMETER_ERROR);
        }
        Long l = index.incrementAndGet() % size;
        if (l < 0) {
            l = 0L;
        }
        return values.get(l.intValue());
    }
}
