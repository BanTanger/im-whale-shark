package com.bantanger.im.domain.friendship.model.req;

import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/16 20:07
 */
@Data
public class FriendDto {

    private String toId;

    private String remark;

    private String addSource;

    private String extra;

    private String addWording;

}
