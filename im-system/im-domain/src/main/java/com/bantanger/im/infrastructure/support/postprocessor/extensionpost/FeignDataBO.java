package com.bantanger.im.infrastructure.support.postprocessor.extensionpost;

import com.bantanger.im.codec.proto.Message;
import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 【已废弃】
 * @author BanTanger 半糖
 * @Date 2023/12/17 0:46
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Deprecated
public class FeignDataBO {

    private ChannelHandlerContext ctx;

    private Message msg;

}
