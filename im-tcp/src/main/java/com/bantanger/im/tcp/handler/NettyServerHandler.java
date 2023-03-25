package com.bantanger.im.tcp.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.bantanger.im.codec.pack.LoginPack;
import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.common.comstant.Constants;
import com.bantanger.im.common.enums.command.ImSystemCommand;
import com.bantanger.im.common.enums.connect.ImSystemConnectState;
import com.bantanger.im.common.model.UserClientDto;
import com.bantanger.im.common.model.UserSession;
import com.bantanger.im.service.redis.RedisManager;
import com.bantanger.im.service.utils.SessionSocketHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 19:23
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

//    private final static Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);

//    @Override
//    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
//        Integer command = parseCommand(msg);
//        CommandFactory commandFactory = new CommandFactory();
//        CommandStrategy commandStrategy = commandFactory.getCommandStrategy(command);
//        commandStrategy.doStrategy(ctx, msg);
//    }

    //String
    //Map
    // userId client1 session
    // userId client2 session
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {

        Integer command = msg.getMessageHeader().getCommand();
        //登录command
        if(ImSystemCommand.COMMAND_LOGIN.getCode().equals(command)){

            LoginPack loginPack = JSON.parseObject(JSONObject.toJSONString(msg.getMessagePack()),
                    new TypeReference<LoginPack>() {
                    }.getType());
            /** 登陸事件 **/
            String userId = loginPack.getUserId();
            /** 为channel设置用户id **/
            ctx.channel().attr(AttributeKey.valueOf(Constants.ChannelConstants.UserId)).set(userId);
            /** 为channel设置appId **/
            ctx.channel().attr(AttributeKey.valueOf(Constants.ChannelConstants.AppId)).set(msg.getMessageHeader().getAppId());
            /** 为channel设置ClientType **/
            ctx.channel().attr(AttributeKey.valueOf(Constants.ChannelConstants.ClientType))
                    .set(msg.getMessageHeader().getClientType());
            //Redis map

            UserSession userSession = new UserSession();
            userSession.setAppId(msg.getMessageHeader().getAppId());
            userSession.setClientType(msg.getMessageHeader().getClientType());
            userSession.setUserId(loginPack.getUserId());
            userSession.setConnectState(ImSystemConnectState.CONNECT_STATE_ONLINE.getCode());

            RedissonClient redissonClient = RedisManager.getRedissonClient();
            RMap<String, String> map = redissonClient.getMap(msg.getMessageHeader().getAppId() + Constants.RedisConstants.UserSessionConstants + loginPack.getUserId());
            map.put(msg.getMessageHeader().getClientType()+":" + msg.getMessageHeader().getImei()
                    ,JSONObject.toJSONString(userSession));
            UserClientDto userClientDto = new UserClientDto();
            userClientDto.setUserId(loginPack.getUserId());
            userClientDto.setAppId(msg.getMessageHeader().getAppId());
            userClientDto.setClientType(msg.getMessageHeader().getClientType());
            SessionSocketHolder
                    .put(userClientDto, (NioSocketChannel) ctx.channel());

        }
        else if(ImSystemCommand.COMMAND_LOGOUT.getCode().equals(command)){
            //删除session
            //redis 删除
            SessionSocketHolder.removeUserSession((NioSocketChannel) ctx.channel());
        }else if(ImSystemCommand.COMMAND_PING.getCode().equals(command)){
            ctx.channel()
                    .attr(AttributeKey.valueOf(Constants.ChannelConstants.ReadTime)).set(System.currentTimeMillis());
        }
    }

    protected Integer parseCommand(Message msg) {
        return msg.getMessageHeader().getCommand();
    }

}
