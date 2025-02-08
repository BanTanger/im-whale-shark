package com.bantanger.common.model;

import com.bantanger.common.enums.BaseEnum;
import com.bantanger.common.enums.CodeEnum;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
@Data
public final class JsonObject<E> {

    @Setter(AccessLevel.PRIVATE)
    private Integer code;
    @Setter(AccessLevel.PRIVATE)
    private String msg;
    @Setter(AccessLevel.PRIVATE)
    private E result;

    private JsonObject() {
    }

    public static <E> JsonObject<E> success(E e) {
        JsonObject<E> object = new JsonObject<>();
        object.setCode(CodeEnum.Success.getCode());
        object.setMsg(CodeEnum.Success.getName());
        object.setResult(e);
        return object;
    }

    public static <E> JsonObject<E> success(E t, String msg) {
        JsonObject<E> obj = success(t);
        obj.setMsg(msg);
        return obj;
    }

    public static JsonObject fail(BaseEnum codeEnum) {
        JsonObject object = new JsonObject();
        object.setMsg(codeEnum.getName());
        object.setCode(codeEnum.getCode());
        return object;
    }

    public static JsonObject fail(String msg) {
        JsonObject object = new JsonObject();
        object.setMsg(msg);
        object.setCode(CodeEnum.Fail.getCode());
        return object;
    }

    public static <E> JsonObject<E> fail(E e, String msg) {
        JsonObject<E> object = new JsonObject<>();
        object.setCode(CodeEnum.Fail.getCode());
        object.setMsg(msg);
        object.setResult(e);
        return object;
    }

    public static <E> JsonObject<E> res(BaseEnum codeEnum, E e) {
        JsonObject<E> object = new JsonObject<>();
        object.setMsg(codeEnum.getName());
        object.setCode(codeEnum.getCode());
        object.setResult(e);
        return object;
    }


    public boolean isSuccess() {
        return Objects.equals(CodeEnum.Success.getCode(), this.getCode());
    }

}
