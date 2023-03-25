package com.bantanger.im.service.strategy.command.impl;

import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.common.comstant.Constants;
import com.bantanger.im.common.model.UserClientDto;
import com.bantanger.im.service.redis.RedisManager;
import com.bantanger.im.service.strategy.command.BaseCommandStrategy;
import com.bantanger.im.service.utils.SessionSocketHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 11:56
 */
public class LogoutCommand extends BaseCommandStrategy {

    @Override
    public void doStrategy(ChannelHandlerContext ctx, Message msg) {
        // 删除 Session and redisson 里的 session
        String userId = (String) ctx.channel().attr(AttributeKey.valueOf(Constants.UserClientConstants.UserId)).get();
        Integer appId = (Integer) ctx.channel().attr(AttributeKey.valueOf(Constants.UserClientConstants.AppId)).get();
        Integer clientType = (Integer) ctx.channel().attr(AttributeKey.valueOf(Constants.UserClientConstants.ClientType)).get();

        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setUserId(userId);
        userClientDto.setAppId(appId);
        userClientDto.setClientType(clientType);

        SessionSocketHolder.remove(userClientDto);
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<Object, Object> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        // 删除 Hash 里的 key，key 存放用户的 Session
        map.remove(clientType);
        ctx.close();
    }

}
