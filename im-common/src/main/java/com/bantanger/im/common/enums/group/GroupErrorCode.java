package com.bantanger.im.common.enums.group;

import com.bantanger.im.common.exception.ApplicationExceptionEnum;

/**
 * @author: Chackylee
 * @description:
 **/
public enum GroupErrorCode implements ApplicationExceptionEnum {

    GROUP_IS_NOT_EXIST(40000,"群不存在"),

    GROUP_IS_EXIST(40001,"群已存在"),

    GROUP_IS_HAVE_OWNER(40002,"群已存在群主"),

    USER_IS_JOINED_GROUP(40003,"该用户已经进入该群"),

    USER_JOIN_GROUP_ERROR(40004,"群成员添加失败"),

    GROUP_MEMBER_IS_BEYOND(40005,"群成员已达到上限"),

    MEMBER_IS_NOT_JOINED_GROUP(40006,"该用户不在群内"),

    THIS_OPERATE_NEED_MANAGER_ROLE(40007,"该操作只允许群主/管理员操作"),

    THIS_OPERATE_NEED_APPMANAGER_ROLE(40008,"该操作只允许APP管理员操作"),

    THIS_OPERATE_NEED_OWNER_ROLE(40009,"该操作只允许群主操作"),

    GROUP_OWNER_IS_NOT_REMOVE(40010,"群主无法移除"),

    UPDATE_GROUP_BASE_INFO_ERROR(40011,"更新群信息失败"),

    THIS_GROUP_IS_MUTE(40012,"该群禁止发言"),

    IMPORT_GROUP_ERROR(40013,"导入群组失败"),

    THIS_OPERATE_NEED_ONESELF(40014,"该操作只允许自己操作"),

    PRIVATE_GROUP_CAN_NOT_DESTORY(40015,"私有群不允许解散"),

    PUBLIC_GROUP_MUST_HAVE_OWNER(40016,"公开群必须指定群主"),

    GROUP_MEMBER_IS_SPEAK(40017,"群成员被禁言"),

    GROUP_IS_DESTROY(40018,"群组已解散"),

    ;

    private int code;
    private String error;

    GroupErrorCode(int code, String error){
        this.code = code;
        this.error = error;
    }
    public int getCode() {
        return this.code;
    }

    public String getError() {
        return this.error;
    }

}
