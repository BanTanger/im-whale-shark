package com.bantanger.im.service.strategy.command.system;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.bantanger.im.codec.pack.command.LoginPack;
import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.device.ConnectState;
import com.bantanger.im.common.model.UserClientDto;
import com.bantanger.im.common.model.UserSession;
import com.bantanger.im.service.strategy.command.BaseCommandStrategy;
import com.bantanger.im.service.redis.RedisManager;
import com.bantanger.im.service.strategy.command.model.CommandExecution;
import com.bantanger.im.service.utils.UserChannelRepository;
import io.netty.channel.ChannelHandlerContext;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 用户登录逻辑
 * @author BanTanger 半糖
 * @Date 2023/3/25 9:52
 */
public class LoginCommand extends BaseCommandStrategy {

    @Override
    public void systemStrategy(CommandExecution commandExecution) {
        ChannelHandlerContext ctx = commandExecution.getCtx();
        Message msg = commandExecution.getMsg();
        Integer brokeId = commandExecution.getBrokeId();

        // 解析 msg
        LoginPack loginPack = JSON.parseObject(JSONObject.toJSONString(msg.getMessagePack()),
                new TypeReference<LoginPack>() {

                }.getType());
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setUserId(loginPack.getUserId());
        userClientDto.setAppId(msg.getMessageHeader().getAppId());
        userClientDto.setClientType(msg.getMessageHeader().getClientType());
        userClientDto.setImei(msg.getMessageHeader().getImei());

        // 双向绑定
        UserChannelRepository.bind(userClientDto, ctx.channel());

        // Redisson 高速存储用户 Session
        UserSession userSession = new UserSession();
        userSession.setUserId(loginPack.getUserId());
        userSession.setAppId(msg.getMessageHeader().getAppId());
        userSession.setClientType(msg.getMessageHeader().getClientType());
        userSession.setConnectState(ConnectState.CONNECT_STATE_ONLINE.getCode());
        userSession.setImei(userClientDto.getImei());
        userSession.setBrokerId(brokeId);

        try {
            InetAddress localHost = InetAddress.getLocalHost();
            userSession.setBrokerHost(localHost.getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        // 存储到 Redis
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<String, String> map = redissonClient.getMap(
                msg.getMessageHeader().getAppId() +
                        Constants.RedisConstants.UserSessionConstants +
                        loginPack.getUserId());
        map.put(msg.getMessageHeader().getClientType() + ":"
                        + msg.getMessageHeader().getImei(),
                JSONObject.toJSONString(userSession));

        // 使用 redisson 发布订阅模式实现用户上线通知的消息广播
        RTopic topic = redissonClient.getTopic(Constants.RedisConstants.UserLoginChannel);
        topic.publish(JSONObject.toJSONString(userClientDto));
    }

}