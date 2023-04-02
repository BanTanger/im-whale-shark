package com.bantanger.im.service.utils;

import com.bantanger.im.common.BaseErrorCode;
import com.bantanger.im.common.exception.ApplicationException;
import com.bantanger.im.service.route.RouteInfo;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/29 9:14
 */
public class RouteInfoParseUtil {

    public static RouteInfo parse(String info) {
        try {
            String[] serverInfo = info.split(":");
            RouteInfo routeInfo = new RouteInfo(serverInfo[0], Integer.parseInt(serverInfo[1]));
            return routeInfo;
        } catch (Exception e) {
            throw new ApplicationException(BaseErrorCode.PARAMETER_ERROR);
        }
    }

}
