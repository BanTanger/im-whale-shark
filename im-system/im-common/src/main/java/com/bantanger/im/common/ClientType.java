package com.bantanger.im.common;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 **/

import com.bantanger.common.enums.BaseEnum;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum ClientType implements BaseEnum<ClientType> {

    WEBAPI(0, "webApi"),
    WEB(1, "web"),
    IOS(2, "ios"),
    ANDROID(3, "android"),
    WINDOWS(4, "windows"),
    MAC(5, "mac"),

    ;

    ClientType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private final Integer code;
    private final String name;

    public static Optional<ClientType> of(Integer code) {
        return Optional.ofNullable(BaseEnum.parseByCode(ClientType.class, code));
    }

}
