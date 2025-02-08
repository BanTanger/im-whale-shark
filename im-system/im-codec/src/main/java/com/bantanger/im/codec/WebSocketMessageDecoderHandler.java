package com.bantanger.im.codec;

import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.codec.utils.ByteBufToMessageUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.bantanger.im.codec.utils.ByteBufToMessageUtils.PACKET_CODEC_LENGTH;

/**
 * WebSocket 解码器
 *
 * @author BanTanger 半糖
 * @Date 2023/4/1 13:30
 */
@Slf4j
public class WebSocketMessageDecoderHandler extends MessageToMessageDecoder<BinaryWebSocketFrame> {
    @Override
    protected void decode(ChannelHandlerContext ctx, BinaryWebSocketFrame msg, List<Object> out) {

        ByteBuf content = msg.content();
        if (content.readableBytes() < PACKET_CODEC_LENGTH) {
            ctx.channel().close();
            return;
        }
        Message message = null;
        try {
            message = ByteBufToMessageUtils.transition(content);
        } catch (Exception e) {
            log.error("", e);
        }
        if (message == null) {
            return;
        }
        out.add(message);
    }
}
