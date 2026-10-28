package com.bantanger.im.codec.wire;

import com.bantanger.im.codec.v1.ChatMessage;
import com.bantanger.im.codec.v1.LoginRequest;
import com.bantanger.im.codec.v1.LoginResponse;
import com.bantanger.im.codec.v1.MessageNotify;
import com.bantanger.im.codec.v1.SendMessageRequest;
import com.bantanger.im.codec.v1.TextContent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 线协议 v1 的契约测试：帧对称、半包、协议违规、protobuf 帧体往返。
 */
class FrameCodecTest {

    @Test
    void headerIsTwentyBytesAndSymmetric() {
        Frame request = Frame.builder()
                .type(FrameType.REQUEST)
                .command(WireCommand.LOGIN)
                .seq(1)
                .body(LoginRequest.newBuilder().setAppId(10001).setUserId("10001").build())
                .build();
        Frame response = Frame.builder()
                .type(FrameType.RESPONSE)
                .command(WireCommand.LOGIN)
                .seq(1)
                .body(LoginResponse.newBuilder().setCode(0).setSessionId("s-1").build())
                .build();

        byte[] reqBytes = FrameCodec.encodeArray(request);
        byte[] resBytes = FrameCodec.encodeArray(response);

        // 上下行同一套头：长度差只来自帧体
        assertEquals(FrameCodec.HEADER_LENGTH, reqBytes.length - request.bodyLength());
        assertEquals(FrameCodec.HEADER_LENGTH, resBytes.length - response.bodyLength());
    }

    @Test
    void roundTripKeepsEveryField() {
        ChatMessage message = ChatMessage.newBuilder()
                .setMessageId("c-1")
                .setAppId(10001)
                .setFromId("10001")
                .setToId("10002")
                .setConversationType(1)
                .setSequence(7)
                .setMessageTime(1791198000412L)
                .setContent(com.bantanger.im.codec.v1.MessageContent.newBuilder()
                        .setText(TextContent.newBuilder().setText("我到楼下了").build())
                        .build())
                .putExtra("trace", "abc")
                .build();

        Frame frame = Frame.builder()
                .type(FrameType.REQUEST)
                .command(WireCommand.SEND_MESSAGE)
                .seq(42)
                .body(SendMessageRequest.newBuilder().setMessage(message).build())
                .build();

        Frame decoded = FrameCodec.decodeArray(FrameCodec.encodeArray(frame));

        assertEquals(FrameType.REQUEST, decoded.getType());
        assertEquals(WireCommand.SEND_MESSAGE.getCode(), decoded.getCommand());
        assertEquals(42, decoded.getSeq());
        assertEquals(0, decoded.getStatus());

        SendMessageRequest body = (SendMessageRequest) WirePayloads.parse(decoded);
        assertEquals("c-1", body.getMessage().getMessageId());
        assertEquals("我到楼下了", body.getMessage().getContent().getText().getText());
        assertEquals("abc", body.getMessage().getExtraMap().get("trace"));
    }

    @Test
    void notifyFrameCarriesServerSequence() {
        MessageNotify notify = MessageNotify.newBuilder()
                .setMessage(ChatMessage.newBuilder()
                        .setMessageKey(987654321L)
                        .setSequence(8)
                        .setToId("10002")
                        .setContent(com.bantanger.im.codec.v1.MessageContent.newBuilder()
                                .setText(TextContent.newBuilder().setText("在吗").build())
                                .build())
                        .build())
                .build();

        Frame frame = Frame.builder()
                .type(FrameType.NOTIFY)
                .command(WireCommand.P2P_MESSAGE)
                .seq(1001)
                .body(notify)
                .build();

        Frame decoded = FrameCodec.decodeArray(FrameCodec.encodeArray(frame));
        MessageNotify parsed = (MessageNotify) WirePayloads.parse(decoded);
        assertEquals(987654321L, parsed.getMessage().getMessageKey());
        assertEquals(FrameType.NOTIFY, decoded.getType());
    }

    @Test
    void halfPacketReturnsNullAndDoesNotConsumeBytes() {
        Frame frame = Frame.builder()
                .type(FrameType.REQUEST)
                .command(WireCommand.LOGIN)
                .seq(9)
                .body(LoginRequest.newBuilder().setAppId(10001).setUserId("10001").build())
                .build();
        byte[] full = FrameCodec.encodeArray(frame);
        byte[] head = Arrays.copyOfRange(full, 0, full.length - 3); // 少最后 3 字节

        ByteBuf buf = Unpooled.wrappedBuffer(head);
        try {
            assertNull(FrameCodec.decode(buf), "半包必须返回 null");
            assertEquals(0, buf.readerIndex(), "半包不得消费读指针");
        } finally {
            buf.release();
        }

        // 补齐后续字节后能正常解出
        ByteBuf both = Unpooled.wrappedBuffer(full);
        try {
            Frame decoded = FrameCodec.decode(both);
            assertEquals(WireCommand.LOGIN.getCode(), decoded.getCommand());
        } finally {
            both.release();
        }
    }

    @Test
    void tcpDecoderDrainsMultipleFramesFromOneBuffer() {
        Frame first = Frame.builder().command(WireCommand.PING).seq(1)
                .body(com.bantanger.im.codec.v1.PingRequest.newBuilder().setClientTime(100L).build()).build();
        Frame second = Frame.builder().command(WireCommand.PING).seq(2)
                .body(com.bantanger.im.codec.v1.PingRequest.newBuilder().setClientTime(200L).build()).build();

        byte[] a = FrameCodec.encodeArray(first);
        byte[] b = FrameCodec.encodeArray(second);
        ByteBuf buf = Unpooled.buffer(a.length + b.length);
        buf.writeBytes(a);
        buf.writeBytes(b);

        int count = 0;
        while (true) {
            Frame frame = FrameCodec.decode(buf);
            if (frame == null) {
                break;
            }
            count++;
        }
        buf.release();
        assertEquals(2, count, "一次 read 里有两个整帧时必须全部取出");
    }

    @Test
    void badMagicIsAViolationNotASkip() {
        byte[] bytes = FrameCodec.encodeArray(Frame.builder().command(WireCommand.PING).seq(1).build());
        bytes[0] = 0x00;
        assertThrows(FrameCodec.ProtocolViolationException.class, () -> FrameCodec.decodeArray(bytes));
    }

    @Test
    void oversizedBodyIsRejectedBeforeAllocation() {
        byte[] bytes = FrameCodec.encodeArray(Frame.builder().command(WireCommand.PING).seq(1).build());
        // 把 bodyLen 改成 5 MiB
        int big = 5 * 1024 * 1024;
        bytes[16] = (byte) (big >>> 24);
        bytes[17] = (byte) (big >>> 16);
        bytes[18] = (byte) (big >>> 8);
        bytes[19] = (byte) big;
        assertThrows(FrameCodec.ProtocolViolationException.class, () -> FrameCodec.decodeArray(bytes));
    }

    @Test
    void emptyBodyIsLegal() {
        Frame frame = Frame.builder().type(FrameType.RESPONSE).command(WireCommand.ACK).seq(3).build();
        byte[] bytes = FrameCodec.encodeArray(frame);
        assertEquals(FrameCodec.HEADER_LENGTH, bytes.length);
        Frame decoded = FrameCodec.decodeArray(bytes);
        assertEquals(0, decoded.bodyLength());
        assertNull(WirePayloads.parse(decoded));
    }

    @Test
    void loginPayloadRoundTripThroughWirePayloads() {
        LoginRequest request = LoginRequest.newBuilder()
                .setAppId(10001).setUserId("10001").setClientType(3).setImei("200").build();
        Frame frame = Frame.builder().type(FrameType.REQUEST).command(WireCommand.LOGIN).seq(1).body(request).build();
        LoginRequest decoded = (LoginRequest) WirePayloads.parse(FrameCodec.decodeArray(FrameCodec.encodeArray(frame)));
        assertTrue(decoded.getAppId() == 10001);
        assertEquals("200", decoded.getImei());
    }
}
