package com.bantanger.im.tcp.utils;

import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 20:51
 */
public class SessionSocketHolder {

    private static final Map<String, NioSocketChannel> CHANNELS = new ConcurrentHashMap<String, NioSocketChannel>();

    public static void put(String userId, NioSocketChannel channel) {
        CHANNELS.put(userId, channel);
    }

    public static NioSocketChannel get(String userId) {
        return CHANNELS.get(userId);
    }

}
