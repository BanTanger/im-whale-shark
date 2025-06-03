package com.bantanger.im.common.enums.conversation;

/**
 * @author chensongmin
 * @description
 * @date 2025/6/3
 */

import com.bantanger.common.enums.BaseEnum;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum ConversationNoticeType implements BaseEnum<ConversationNoticeType> {

    NORMAL(0, "正常"),
    MUTE(1, "免打扰"),
    SPECIAL_REMINDER(2, "特别提醒"),

    ;

    ConversationNoticeType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private final Integer code;
    private final String name;

    public static Optional<ConversationNoticeType> of(Integer code) {
        return Optional.ofNullable(BaseEnum.parseByCode(ConversationNoticeType.class, code));
    }

}