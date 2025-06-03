package com.bantanger.im.common.enums.conversation;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 14:26
 */

import com.bantanger.common.enums.BaseEnum;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum ConversationType implements BaseEnum<ConversationType> {

    P2P(0, "单聊会话"),
    GROUP(1, "群聊会话"),
    ROBOT(2, "机器人会话"),
    PUBLIC_SERVICE(3, "公众号会话"),

    ;

    ConversationType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private final Integer code;
    private final String name;

    public static Optional<ConversationType> of(Integer code) {
        return Optional.ofNullable(BaseEnum.parseByCode(ConversationType.class, code));
    }

}
