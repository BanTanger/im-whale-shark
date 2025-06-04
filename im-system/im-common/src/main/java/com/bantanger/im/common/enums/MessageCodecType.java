package com.bantanger.im.common.enums;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 19:57
 */

import com.bantanger.common.enums.BaseEnum;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum MessageCodecType implements BaseEnum<MessageCodecType> {

    DATA_TYPE_JSON(0x0, "json"),
    DATA_TYPE_PROTOBUF(0x1, "protobuf"),
    DATA_TYPE_XML(0x2, "xml");

    ;

    MessageCodecType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private final Integer code;
    private final String name;

    public static Optional<MessageCodecType> of(Integer code) {
        return Optional.ofNullable(BaseEnum.parseByCode(MessageCodecType.class, code));
    }

}
