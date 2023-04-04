package com.bantanger.im.common.enums.friend;

import com.bantanger.im.common.exception.ApplicationExceptionEnum;

public enum FriendShipErrorCode implements ApplicationExceptionEnum {


    IMPORT_SIZE_BEYOND(30000,"导入數量超出上限"),

    ADD_FRIEND_ERROR(30001,"添加好友失败"),

    TO_IS_YOUR_FRIEND(30002,"对方已经是你的好友"),

    TO_IS_NOT_YOUR_FRIEND(30003,"对方不是你的好友"),

    FRIEND_IS_DELETED(30004,"好友已被删除"),

    FRIEND_IS_DELETED_YOU(30005,"你已被对方删除"),

    FRIEND_IS_BLACK(30006,"好友已被拉黑"),

    TARGET_IS_BLACK_YOU(30007,"对方把你拉黑"),

    REPEATSHIP_IS_NOT_EXIST(30008,"关系链记录不存在"),

    ADD_BLACK_ERROR(30009,"添加黑名單失败"),

    FRIEND_IS_NOT_YOUR_BLACK(30010,"好友已經不在你的黑名單内"),

    NOT_APPROVER_OTHER_MAN_REQUEST(30011,"无法审批其他人的好友请求"),

    FRIEND_REQUEST_IS_NOT_EXIST(30012,"好友申请不存在"),

    FRIEND_SHIP_GROUP_CREATE_ERROR(30014,"好友分组创建失败"),

    FRIEND_SHIP_GROUP_IS_EXIST(30015,"好友分组已存在"),

    FRIEND_SHIP_GROUP_IS_NOT_EXIST(30016,"好友分组不存在"),



    ;

    private int code;
    private String error;

    FriendShipErrorCode(int code, String error){
        this.code = code;
        this.error = error;
    }
    public int getCode() {
        return this.code;
    }

    @Override
    public String getError() {
        return this.error;
    }

}
