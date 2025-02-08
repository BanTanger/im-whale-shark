package com.bantanger.im.codec.pack.friendship;

import lombok.Data;

/**
 * 修改好友通知报文
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
@Data
public class UpdateFriendPack {

    public String fromId;

    private String toId;

    private String remark;

    private Long sequence;

}
