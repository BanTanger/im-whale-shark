package com.bantanger.im.codec.wire;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;

/**
 * 线协议编解码。上下行同一套头、同一套字节序（大端）。
 *
 * <pre>
 * 偏移  宽度  字段          说明
 *  0    4    magic         0x494D5753 ("IMWS")，粘包/半包与协议识别
 *  4    2    version       协议版本，当前 1；不匹配直接拒帧
 *  6    1    type          0 REQUEST / 1 RESPONSE / 2 NOTIFY
 *  7    1    codec         0 = protobuf（预留给未来换编码）
 *  8    2    command       业务命令码，见 {@link WireCommand}
 * 10    2    status        0 = 成功；RESPONSE/错误通知里才有意义
 * 12    4    seq           请求-响应关联；NOTIFY 时由服务端单调递增
 * 16    4    bodyLen       帧体字节数（不含头）
 * 20    n    body          protobuf 序列化后的帧体
 * </pre>
 *
 * <p>相比旧协议修掉的四件事：上下行对称、头定长且与 imei 解耦、requestId(seq) 明确、
 * version 真正参与兼容判断。</p>
 */
public final class FrameCodec {

    /** "IMWS" */
    public static final int MAGIC = 0x494D5753;
    public static final short VERSION = 1;
    public static final byte CODEC_PROTOBUF = 0;
    public static final int HEADER_LENGTH = 20;

    /** 帧体上限：4 MiB。超过视为协议错误，直接断链，避免内存打爆。 */
    public static final int MAX_BODY_LENGTH = 4 * 1024 * 1024;

    private FrameCodec() {
    }

    // ───────────────────────────── 编码 ─────────────────────────────

    /** 编码进给定 ByteBuf（写指针前移，返回写入字节数）。 */
    public static int encode(Frame frame, ByteBuf out) {
        byte[] body = frame.getBody();
        if (body.length > MAX_BODY_LENGTH) {
            throw new IllegalArgumentException("frame body too large: " + body.length);
        }
        out.writeInt(frame.getMagic());
        out.writeShort(frame.getVersion());
        out.writeByte(frame.getType().getCode());
        out.writeByte(frame.getCodec());
        out.writeShort(frame.getCommand());
        out.writeShort(frame.getStatus());
        out.writeInt(frame.getSeq());
        out.writeInt(body.length);
        out.writeBytes(body);
        return HEADER_LENGTH + body.length;
    }

    public static byte[] encodeArray(Frame frame) {
        ByteBuf buf = Unpooled.buffer(HEADER_LENGTH + frame.bodyLength());
        try {
            encode(frame, buf);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } finally {
            buf.release();
        }
    }

    // ───────────────────────────── 解码 ─────────────────────────────

    /**
     * 从 ByteBuf 解一帧。
     *
     * <p>可读字节不足时返回 null 且<b>不移动读指针</b>（半包），交给 Netty 下次再喂；
     * 魔数或版本不对直接抛 {@link ProtocolViolationException}，调用方应关闭连接。</p>
     */
    public static Frame decode(ByteBuf in) {
        if (in.readableBytes() < HEADER_LENGTH) {
            return null;
        }
        int start = in.readerIndex();
        int magic = in.getInt(start);
        if (magic != MAGIC) {
            throw new ProtocolViolationException("bad magic: 0x" + Integer.toHexString(magic));
        }
        short version = in.getShort(start + 4);
        if (version != VERSION) {
            throw new ProtocolViolationException("unsupported version: " + version);
        }
        int bodyLen = in.getInt(start + 16);
        if (bodyLen < 0 || bodyLen > MAX_BODY_LENGTH) {
            throw new ProtocolViolationException("illegal body length: " + bodyLen);
        }
        if (in.readableBytes() < HEADER_LENGTH + bodyLen) {
            return null;
        }

        FrameType type = FrameType.of(in.getByte(start + 6));
        byte codec = in.getByte(start + 7);
        int command = in.getShort(start + 8) & 0xFFFF;
        short status = in.getShort(start + 10);
        int seq = in.getInt(start + 12);

        in.skipBytes(HEADER_LENGTH);
        byte[] body = new byte[bodyLen];
        in.readBytes(body);

        return Frame.builder()
                .type(type)
                .codec(codec)
                .command(command)
                .status(status)
                .seq(seq)
                .body(body)
                .build();
    }

    /** 一次性解码一个完整字节数组（客户端/测试用）。 */
    public static Frame decodeArray(byte[] bytes) {
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        try {
            Frame frame = decode(buf);
            if (frame == null) {
                throw new ProtocolViolationException("incomplete frame: " + bytes.length + " bytes");
            }
            return frame;
        } finally {
            buf.release();
        }
    }

    /** 校验当前字节数组是否恰好是一整帧（调试用）。 */
    public static boolean isComplete(byte[] bytes) {
        if (bytes.length < HEADER_LENGTH) {
            return false;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int bodyLen = buffer.getInt(16);
        return bytes.length >= HEADER_LENGTH + bodyLen;
    }

    /** 协议错误：魔数错、版本不识别、帧体越界。调用方应断链而不是跳过。 */
    public static class ProtocolViolationException extends RuntimeException {
        public ProtocolViolationException(String message) {
            super(message);
        }
    }
}
