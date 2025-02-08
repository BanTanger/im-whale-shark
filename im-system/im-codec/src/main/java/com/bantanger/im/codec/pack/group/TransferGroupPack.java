package com.bantanger.im.codec.pack.group;

import lombok.Data;

/**
 * 转让群主通知报文
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
@Data
public class TransferGroupPack {

    private String groupId;

    private String ownerId;

}
