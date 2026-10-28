package com.bantanger.im.codec.wire;

/**
 * 线协议命令码。
 *
 * <p>数值沿用旧 {@code com.bantanger.im.common.enums.command.*} 里的常量，
 * 迁移期服务端策略表（{@code CommandFactoryConfig}）可以 1:1 复用，不需要重排。</p>
 *
 * <p>命令码只表示「帧体是哪个 protobuf message」，不再承担方向语义——方向由
 * {@link FrameType} 表达。</p>
 */
public enum WireCommand {

    /** 登录。REQUEST/LoginRequest，RESPONSE/LoginResponse */
    LOGIN(9000),
    /** 多端互踢通知。NOTIFY/LoginResponse(code=MUTUAL_LOGIN) */
    MUTUAL_LOGIN(9002),
    /** 登出。REQUEST/LogoutRequest，RESPONSE/LogoutResponse */
    LOGOUT(9003),
    /** 通用发送入口（旧协议的 9005）。REQUEST/SendMessageRequest */
    SEND_MESSAGE(9005),
    /** 单聊消息下行。NOTIFY/MessageNotify */
    P2P_MESSAGE(1103),
    /** 群聊消息下行。NOTIFY/MessageNotify */
    GROUP_MESSAGE(2104),
    /** 送达确认。REQUEST/AckRequest */
    ACK(1046),
    /** 已读上报。REQUEST/ReadRequest */
    READ(1106),
    /** 已读通知（同步端与发送方）。NOTIFY/ReadNotify */
    READ_NOTIFY(1053),
    /** 心跳。REQUEST/PingRequest，RESPONSE/PongResponse */
    PING(9999);

    private final int code;

    WireCommand(int code) {
        this.code = code;
    }

    /**
     * 未知命令返回 null：协议允许新命令码先于客户端上线，调用方按 {@link FrameCodec#ERROR}
     * 或 {@code ErrorResponse} 兜底，不做抛异常。
     */
    public int getCode() {
        return code;
    }

    public static WireCommand of(int code) {
        for (WireCommand command : values()) {
            if (command.code == code) {
                return command;
            }
        }
        return null;
    }
}
