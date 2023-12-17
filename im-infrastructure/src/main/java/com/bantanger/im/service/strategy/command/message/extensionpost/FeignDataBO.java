package com.bantanger.im.service.strategy.command.message.extensionpost;

import com.bantanger.im.codec.proto.Message;
import io.netty.channel.ChannelHandlerContext;
import lombok.*;

/**
 * @author BanTanger 半糖
 * @Date 2023/12/17 0:46
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FeignDataBO {

    private ChannelHandlerContext ctx;

    private Message msg;

}
