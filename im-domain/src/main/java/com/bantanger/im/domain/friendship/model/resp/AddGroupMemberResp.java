package com.bantanger.im.domain.friendship.model.resp;

import lombok.Data;

import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/18 21:02
 */
@Data
public class AddGroupMemberResp {

    private List<String> successId;

    private List<String> errorId;

}
