package com.bantanger.im.infrastructure.route;

import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/29 8:44
 */
public interface RouteHandler {

    String routeServer(List<String> values, String key);

}
