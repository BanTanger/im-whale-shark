package com.bantanger.im.domain.group.model.req;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.common.enums.command.Command;
import com.bantanger.im.common.model.ClientInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/6 11:10
 */
@Data
@Builder
public class GroupMsgReq {

    private String userId;

    private Command command;

    private Object data;

    private ClientInfo clientInfo;

    private List<String> groupMemberId;

    private JSONObject o;

    private String groupId;

}
