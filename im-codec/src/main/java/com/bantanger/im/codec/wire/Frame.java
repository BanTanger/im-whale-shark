package com.bantanger.im.codec.wire;

/**
 * 一帧线协议数据：定长头 + protobuf 帧体。
 *
 * <p>头是 20 字节定长二进制（{@link FrameCodec#HEADER_LENGTH}），上下行<b>完全对称</b>：
 * 旧协议上行 28 字节头、下行 8 字节头，两端要写两套编解码，这是要修掉的核心问题。</p>
 *
 * <p>不可变；用 {@link #builder()} 构造。</p>
 */
public final class Frame {

    private final int magic;
    private final short version;
    private final FrameType type;
    private final byte codec;
    private final int command;
    private final short status;
    private final int seq;
    private final byte[] body;

    private Frame(Builder builder) {
        this.magic = builder.magic;
        this.version = builder.version;
        this.type = builder.type;
        this.codec = builder.codec;
        this.command = builder.command;
        this.status = builder.status;
        this.seq = builder.seq;
        this.body = builder.body == null ? new byte[0] : builder.body;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 帧体长度（不含头）。 */
    public int bodyLength() {
        return body.length;
    }

    public int getMagic() {
        return magic;
    }

    public short getVersion() {
        return version;
    }

    public FrameType getType() {
        return type;
    }

    public byte getCodec() {
        return codec;
    }

    public int getCommand() {
        return command;
    }

    public short getStatus() {
        return status;
    }

    public int getSeq() {
        return seq;
    }

    public byte[] getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "Frame{type=" + type + ", command=" + command + ", status=" + status
                + ", seq=" + seq + ", bodyLen=" + body.length + '}';
    }

    public static final class Builder {
        private int magic = FrameCodec.MAGIC;
        private short version = FrameCodec.VERSION;
        private FrameType type = FrameType.REQUEST;
        private byte codec = FrameCodec.CODEC_PROTOBUF;
        private int command;
        private short status = 0;
        private int seq;
        private byte[] body;

        private Builder() {
        }

        public Builder type(FrameType type) {
            this.type = type;
            return this;
        }

        public Builder codec(byte codec) {
            this.codec = codec;
            return this;
        }

        public Builder command(WireCommand command) {
            this.command = command.getCode();
            return this;
        }

        public Builder command(int command) {
            this.command = command;
            return this;
        }

        public Builder status(int status) {
            this.status = (short) status;
            return this;
        }

        public Builder seq(int seq) {
            this.seq = seq;
            return this;
        }

        public Builder body(byte[] body) {
            this.body = body;
            return this;
        }

        public Builder body(com.google.protobuf.MessageLite message) {
            this.body = message.toByteArray();
            return this;
        }

        public Frame build() {
            return new Frame(this);
        }
    }
}
