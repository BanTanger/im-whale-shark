package com.bantanger.im.codec;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.codec.proto.MessageHeader;
import com.bantanger.im.common.enums.MessageCodecType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import static com.bantanger.im.codec.utils.ByteBufToMessageUtils.PACKET_CODEC_LENGTH;

/**
 * Socket 消息解码类
 *
 * @author BanTanger 半糖
 * @Date 2023/3/24 18:48
 */
public class MessageDecoderHandler extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx,
                          ByteBuf in, List<Object> out) throws Exception {
        // 解析私有协议
        if (in.readableBytes() < PACKET_CODEC_LENGTH) {
            ctx.channel().close();
            return;
        }
        // 解析请求头
        // 指令
        int command = in.readInt();
        // 版本
        int version = in.readInt();
        // 设备类型
        int clientType = in.readInt();
        // 消息解析类型
        int messageCodecType = in.readInt();
        // appId 平台ID
        int appId = in.readInt();
        // imei 长度
        int imeiLen = in.readInt();
        // 请求体长度
        int bodyLen = in.readInt();

        // 处理粘包、半包
        if (in.readableBytes() < bodyLen + imeiLen) {
            in.resetReaderIndex();
            return;
        }

        // 解析请求体
        byte[] imeiData = new byte[imeiLen];
        in.readBytes(imeiData);
        String imei = new String(imeiData);

        byte[] bodyData = new byte[bodyLen];
        in.readBytes(bodyData);

        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setCommand(command);
        messageHeader.setVersion(version);
        messageHeader.setMessageType(messageCodecType);
        messageHeader.setClientType(clientType);
        messageHeader.setAppId(appId);
        messageHeader.setImei(imei);
        messageHeader.setImeiLength(imeiLen);
        messageHeader.setLength(bodyLen);

        Message message = new Message();
        message.setMessageHeader(messageHeader);

        if (messageCodecType == MessageCodecType.DATA_TYPE_JSON.getCode()) {
            String body = new String(bodyData);
            JSONObject parse = (JSONObject) JSONObject.parse(body);
            message.setMessagePack(parse);
        }

        // 标记当前读索引
        in.markReaderIndex();
        // 将消息写入消息管道，传递给下一个 Handler
        out.add(message);
    }
}
