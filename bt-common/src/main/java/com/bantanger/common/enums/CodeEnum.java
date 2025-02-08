package com.bantanger.common.enums;

import java.util.Optional;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
public enum CodeEnum implements BaseEnum<CodeEnum> {
    /**
     * 整个系统通用编码 xx_xx_xxxx (服务标识_业务_错误编号，便于错误快速定位
     */
    Success(1, "操作成功"),
    Fail(0, "操作失败"),


    NotFindError(10001, "未查询到信息"),
    SaveError(10002, "保存信息失败"),
    UpdateError(10003, "更新信息失败"),
    ValidateError(10004, "数据检验失败"),
    StatusHasValid(10005, "状态已经被启用"),
    StatusHasInvalid(10006, "状态已经被禁用"),
    SystemError(10007, "系统异常"),
    BusinessError(10008, "业务异常"),
    ParamSetIllegal(10009, "参数设置非法"),
    TransferStatusError(10010, "当前状态不正确，请勿重复提交"),
    NotGrant(10011, "没有操作该功能的权限，请联系管理员");
    private final Integer code;
    private final String msg;

    CodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public Integer getCode() {
        return this.code;
    }

    @Override
    public String getName() {
        return this.msg;
    }

    public static Optional<CodeEnum> of(Integer code) {
        return Optional.ofNullable(BaseEnum.parseByCode(CodeEnum.class, code));
    }
}
