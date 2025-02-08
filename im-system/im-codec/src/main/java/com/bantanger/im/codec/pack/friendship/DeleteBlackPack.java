package com.bantanger.im.codec.pack.friendship;

import lombok.Data;

/**
 * 删除黑名单通知报文
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
@Data
public class DeleteBlackPack {

    private String fromId;

    private String toId;

    private Long sequence;

}
