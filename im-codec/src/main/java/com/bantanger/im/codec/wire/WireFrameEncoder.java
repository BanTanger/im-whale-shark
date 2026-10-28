package com.bantanger.im.codec.wire;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * TCP 通道编码：{@link Frame} → 裸字节。
 */
public class WireFrameEncoder extends MessageToByteEncoder<Frame> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Frame msg, ByteBuf out) {
        FrameCodec.encode(msg, out);
    }
}
