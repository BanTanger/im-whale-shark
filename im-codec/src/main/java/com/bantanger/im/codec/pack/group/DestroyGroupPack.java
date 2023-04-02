package com.bantanger.im.codec.pack.group;

import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54 解散群通知报文
 **/
@Data
public class DestroyGroupPack {

    private String groupId;

    private Long sequence;

}
