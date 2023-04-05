package com.bantanger.im.service.strategy.login;

import com.bantanger.im.codec.proto.MessagePack;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.device.ClientType;
import com.bantanger.im.common.enums.command.SystemCommand;
import com.bantanger.im.common.model.UserClientDto;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/27 15:17
 */
@Slf4j
public abstract class LoginStatus {

    protected static Map<Integer, String> map = new ConcurrentHashMap<>();

    static {
        map.put(ClientType.ANDROID.getCode(), ClientType.ANDROID.getInfo());
        map.put(ClientType.WEB.getCode(), ClientType.WEB.getInfo());
        map.put(ClientType.IOS.getCode(), ClientType.IOS.getInfo());
        map.put(ClientType.WINDOWS.getCode(), ClientType.WINDOWS.getInfo());
        map.put(ClientType.MAC.getCode(), ClientType.MAC.getInfo());
        map.put(ClientType.WEBAPI.getCode(), ClientType.WEBAPI.getInfo());
    }

    protected LoginContext context;

    public void setContext(LoginContext context) {
        this.context = context;
    }

    public abstract void switchStatus(LoginStatus status);

    public abstract void handleUserLogin(UserClientDto dto);

    /**
     * 发送用户下线消息
     * 并不是真正粗暴清除 channel 里的旧信息，因为需要等待数据包停止传输
     * 在服务器行为中，能清除 channel 里旧信息的方式只有 用户登出 Logout 和 心跳超时 Ping-out
     * @param userChannel
     */
    public static void sendMutualLoginMsg(Channel userChannel, Integer channelClientType, String channelImei, UserClientDto dto) {
        // 踢掉 channel 所绑定的旧的同端登录状态
        String channelDevice = parseClientType(channelClientType) + ":" + channelImei;
        String newChannelDevice = parseClientType(dto.getClientType()) + ":" + dto.getImei();
        if (!(channelDevice).equals(newChannelDevice)) {
            // 行为埋点
            log.info("第三方平台(appId) [{}] 用户(userId) [{}] 从新端 [{}] 登录(login) , 旧端 [{}] 下线(line) ",
                    dto.getAppId(), dto.getUserId(), newChannelDevice, channelDevice);
            MessagePack<Object> pack = new MessagePack<>();
            pack.setToId((String) userChannel.attr(AttributeKey.valueOf(Constants.ChannelConstants.UserId)).get());
            pack.setUserId((String) userChannel.attr(AttributeKey.valueOf(Constants.ChannelConstants.UserId)).get());
            pack.setCommand(SystemCommand.MUTALOGIN.getCommand());
            userChannel.writeAndFlush(pack);
        }
    }

    public static String parseClientType(Integer clientType) {
        return map.get(clientType);
    }

}
