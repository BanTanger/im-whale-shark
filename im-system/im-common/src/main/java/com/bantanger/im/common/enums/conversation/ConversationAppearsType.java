package com.bantanger.im.common.enums.conversation;

/**
 * @author chensongmin
 * @description 会话显示位置类型
 * @date 2025/6/3
 */

import com.bantanger.common.enums.BaseEnum;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum ConversationAppearsType implements BaseEnum<ConversationAppearsType> {

    NORMAL(0, "正常"),
    TOP(1, "置顶"),

    ;

    ConversationAppearsType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private final Integer code;
    private final String name;

    public static Optional<ConversationAppearsType> of(Integer code) {
        return Optional.ofNullable(BaseEnum.parseByCode(ConversationAppearsType.class, code));
    }

}
