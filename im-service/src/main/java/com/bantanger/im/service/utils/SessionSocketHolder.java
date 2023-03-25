package com.bantanger.im.service.utils;

import com.bantanger.im.common.model.UserClientDto;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 20:51
 */
public class SessionSocketHolder {

    private static final Map<UserClientDto, NioSocketChannel> CHANNELS = new ConcurrentHashMap<>();
    private UserClientDto userClientDto = new UserClientDto();

    public static void put(UserClientDto userClientDto, NioSocketChannel channel) {
        CHANNELS.put(userClientDto, channel);
    }

    public static NioSocketChannel get(UserClientDto userClientDto) {
        return CHANNELS.get(userClientDto);
    }

    public static void remove(UserClientDto userClientDto) {
        CHANNELS.remove(userClientDto);
    }

    public static void remove(NioSocketChannel channel) {
        CHANNELS.entrySet().stream().filter(entitiy -> entitiy.getValue() == channel)
                .forEach(entry -> CHANNELS.remove(entry.getKey()));
    }



}
