package com.bantanger.im.service.strategy.command.model;

import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.service.feign.FeignMessageService;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/6 10:28
 */
@Data
public class CommandExecution {

    private ChannelHandlerContext ctx;

    private Message msg;

    private Integer brokeId;

    private FeignMessageService feignMessageService;

}
