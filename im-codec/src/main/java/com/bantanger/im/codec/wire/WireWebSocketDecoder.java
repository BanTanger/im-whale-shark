package com.bantanger.im.codec.wire;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

/**
 * WebSocket 通道解码：BinaryWebSocketFrame → {@link Frame}。
 *
 * <p>WS 一帧就是一条消息，所以不需要循环；但一帧之内仍可能是半包（客户端分片发送的错误实现），
 * 因此同样用 {@link FrameCodec#decode} 的不完整返回 null 语义兜住。</p>
 */
public class WireWebSocketDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {

    @Override
    protected void decode(ChannelHandlerContext ctx, BinaryWebSocketFrame msg, List<Object> out) {
        ByteBuf content = msg.content();
        Frame frame = FrameCodec.decode(content);
        if (frame == null) {
            throw new FrameCodec.ProtocolViolationException(
                    "truncated websocket frame: " + content.readableBytes() + " bytes");
        }
        out.add(frame);
        if (content.isReadable()) {
            throw new FrameCodec.ProtocolViolationException(
                    "trailing bytes in websocket frame: " + content.readableBytes());
        }
    }
}
