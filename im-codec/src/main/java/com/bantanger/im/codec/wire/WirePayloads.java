package com.bantanger.im.codec.wire;

import com.bantanger.im.codec.v1.AckRequest;
import com.bantanger.im.codec.v1.ErrorResponse;
import com.bantanger.im.codec.v1.LoginRequest;
import com.bantanger.im.codec.v1.LoginResponse;
import com.bantanger.im.codec.v1.LogoutRequest;
import com.bantanger.im.codec.v1.LogoutResponse;
import com.bantanger.im.codec.v1.MessageNotify;
import com.bantanger.im.codec.v1.PingRequest;
import com.bantanger.im.codec.v1.PongResponse;
import com.bantanger.im.codec.v1.ReadNotify;
import com.bantanger.im.codec.v1.ReadRequest;
import com.bantanger.im.codec.v1.SendMessageRequest;
import com.bantanger.im.codec.v1.SendMessageResponse;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;

/**
 * 命令码 → 帧体类型的映射。
 *
 * <p>帧体<b>不包一层 oneof 信封</b>：command 已经唯一确定类型，再包一层只会多两字节和一次解包。
 * 代价是新命令必须同时改客户端，因此未知 command 由 {@link WireCommand#of(int)} 返回 null，
 * 由上层回 {@link ErrorResponse}。</p>
 */
public final class WirePayloads {

    private WirePayloads() {
    }

    /**
     * 按 command + 帧类型解出帧体。
     *
     * @return 解出的 protobuf 对象；帧体为空或命令未知时返回 null
     */
    public static MessageLite parse(Frame frame) {
        if (frame.bodyLength() == 0) {
            return null;
        }
        Parser<? extends MessageLite> parser = parserOf(frame.getCommand(), frame.getType());
        if (parser == null) {
            return null;
        }
        try {
            return parser.parseFrom(frame.getBody());
        } catch (Exception e) {
            throw new FrameCodec.ProtocolViolationException(
                    "cannot parse body for command " + frame.getCommand() + ": " + e.getMessage());
        }
    }

    private static Parser<? extends MessageLite> parserOf(int command, FrameType type) {
        WireCommand cmd = WireCommand.of(command);
        if (cmd == null) {
            return null;
        }
        switch (cmd) {
            case LOGIN:
                return type == FrameType.RESPONSE ? LoginResponse.parser() : LoginRequest.parser();
            case MUTUAL_LOGIN:
                return LoginResponse.parser();
            case LOGOUT:
                return type == FrameType.RESPONSE ? LogoutResponse.parser() : LogoutRequest.parser();
            case PING:
                return type == FrameType.RESPONSE ? PongResponse.parser() : PingRequest.parser();
            case SEND_MESSAGE:
                return type == FrameType.RESPONSE ? SendMessageResponse.parser() : SendMessageRequest.parser();
            case P2P_MESSAGE:
            case GROUP_MESSAGE:
                return MessageNotify.parser();
            case ACK:
                return AckRequest.parser();
            case READ:
                return ReadRequest.parser();
            case READ_NOTIFY:
                return ReadNotify.parser();
            default:
                return null;
        }
    }
}
