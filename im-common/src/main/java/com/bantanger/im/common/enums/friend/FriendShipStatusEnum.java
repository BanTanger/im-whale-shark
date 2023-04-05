package com.bantanger.im.common.enums.friend;

public enum FriendShipStatusEnum {

    /**
     * 0未添加 1正常 2删除
     */
    FRIEND_STATUS_NO_FRIEND(0),

    FRIEND_STATUS_NORMAL(1),

    FRIEND_STATUS_DELETE(2),

    /**
     * 0未添加 1正常 2拉黑
     */
    BLACK_STATUS_NORMAL(1),

    BLACK_STATUS_BLACKED(2),
    ;

    private Integer code;

    FriendShipStatusEnum(int code){
        this.code=code;
    }

    public Integer getCode() {
        return code;
    }
}
