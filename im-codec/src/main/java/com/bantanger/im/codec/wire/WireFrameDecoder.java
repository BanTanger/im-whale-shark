package com.bantanger.im.codec.wire;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * TCP 通道解码：裸字节流 → {@link Frame}。
 *
 * <p>半包留在 ByteBuf 里等下次；协议违规直接抛异常由 pipeline 关连接。</p>
 */
public class WireFrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        while (true) {
            Frame frame = FrameCodec.decode(in);
            if (frame == null) {
                return;
            }
            out.add(frame);
        }
    }
}
