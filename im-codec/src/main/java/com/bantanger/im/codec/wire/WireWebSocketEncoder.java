package com.bantanger.im.codec.wire;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

/**
 * WebSocket 通道编码：{@link Frame} → BinaryWebSocketFrame。
 *
 * <p>WS 只接受二进制帧，因此帧体照旧是二进制头 + protobuf，不走文本帧。</p>
 */
public class WireWebSocketEncoder extends MessageToMessageEncoder<Frame> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Frame msg, List<Object> out) {
        ByteBuf buf = ctx.alloc().buffer(FrameCodec.HEADER_LENGTH + msg.bodyLength());
        FrameCodec.encode(msg, buf);
        out.add(new BinaryWebSocketFrame(buf));
    }

    /** 供测试与客户端实现复用。 */
    public static byte[] toBytes(Frame frame) {
        ByteBuf buf = Unpooled.buffer(FrameCodec.HEADER_LENGTH + frame.bodyLength());
        try {
            FrameCodec.encode(frame, buf);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } finally {
            buf.release();
        }
    }
}
