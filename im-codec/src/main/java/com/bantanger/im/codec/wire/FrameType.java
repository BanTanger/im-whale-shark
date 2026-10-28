package com.bantanger.im.codec.wire;

/**
 * 帧类型。上下行共用一种帧结构，靠 type 区分方向语义。
 *
 * <pre>
 * REQUEST  客户端 → 服务端，seq 由客户端生成
 * RESPONSE 服务端 → 客户端，seq 原样回带请求的 seq
 * NOTIFY   服务端 → 客户端，无对应请求，seq 由服务端单调递增生成
 * </pre>
 */
public enum FrameType {

    REQUEST((byte) 0),
    RESPONSE((byte) 1),
    NOTIFY((byte) 2);

    private final byte code;

    FrameType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static FrameType of(byte code) {
        for (FrameType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown frame type: " + code);
    }
}
