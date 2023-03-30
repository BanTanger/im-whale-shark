package com.bantanger.im.domain.group.model.req.callback;

import com.bantanger.im.domain.group.model.resp.AddMemberResp;
import lombok.Data;

import java.util.List;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Data
public class AddMemberAfterCallback {
    private String groupId;
    private Integer groupType;
    private String operater;
    private List<AddMemberResp> memberId;
}
