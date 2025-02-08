package com.bantanger.im.service.route.algroithm.hash;

import com.bantanger.im.service.route.RouteHandler;

import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/29 16:17
 */
public class ConsistentHashHandler implements RouteHandler {

    private AbstractConsistentHash hash;

    public void setHash(AbstractConsistentHash hash) {
        this.hash = hash;
    }

    @Override
    public String routeServer(List<String> values, String key) {
        return hash.process(values, key);
    }

}
