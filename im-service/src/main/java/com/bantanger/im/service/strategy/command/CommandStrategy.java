package com.bantanger.im.service.strategy.command;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.common.enums.command.Command;
import com.bantanger.im.common.model.ClientInfo;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 9:49
 */
public interface CommandStrategy {

    /**
     * 系统命令执行策略接口
     *
     * @param ctx
     * @param msg
     * @param brokeId
     */
    default void systemStrategy(ChannelHandlerContext ctx, Message msg, Integer brokeId) {

    }

    /**
     * 群组命令执行策略接口
     *
     * @param userId
     * @param command
     * @param data
     * @param clientInfo
     * @param groupMemberId
     * @param o
     * @param groupId
     */
    default void groupStrategy(String userId, Command command, Object data, ClientInfo clientInfo,
                               List<String> groupMemberId, JSONObject o, String groupId) {

    }

}
