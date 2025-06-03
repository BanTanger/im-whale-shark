package com.bantanger.im.common.enums.message;

/**
 * @author chensongmin
 * @description
 * @date 2025/6/3
 */

import com.bantanger.common.enums.BaseEnum;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum MsgType implements BaseEnum<MsgType> {

    P2P(1, "单聊消息"),
    GROUP(2, "群聊消息"),
    SYSTEM(3, "系统消息"), // 可用于客服系统、通知系统

    ;

    MsgType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private final Integer code;
    private final String name;

    public static Optional<MsgType> of(Integer code) {
        return Optional.ofNullable(BaseEnum.parseByCode(MsgType.class, code));
    }

}
