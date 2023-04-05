package com.bantanger.im.domain.group;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.pack.group.AddGroupMemberPack;
import com.bantanger.im.codec.pack.group.RemoveGroupMemberPack;
import com.bantanger.im.codec.pack.group.UpdateGroupMemberPack;
import com.bantanger.im.common.enums.device.ClientType;
import com.bantanger.im.common.enums.command.Command;
import com.bantanger.im.common.enums.command.GroupEventCommand;
import com.bantanger.im.common.model.ClientInfo;
import com.bantanger.im.domain.group.model.req.GroupMemberDto;
import com.bantanger.im.domain.group.model.req.GroupMsgReq;
import com.bantanger.im.domain.group.service.ImGroupMemberService;
import com.bantanger.im.service.sendmsg.MessageProducer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 本来这个类应该是写在 im-service 层的 sendmsg 包下的，但引入 groupMemberService 的话会报循环依赖，还是耦合在这里吧
 * 并且这个明显可以沿用 command Strategy 优化，还是那个原因，会报循环依赖，所以还是堆 if - else 吧
 *
 * @author BanTanger 半糖
 * @Date 2023/4/1 18:53
 */
@Component
public class GroupMessageProducer {

    @Resource
    MessageProducer messageProducer;

    @Resource
    ImGroupMemberService groupMemberService;

    public void producer(String userId, Command command, Object data, ClientInfo clientInfo) {
        JSONObject o = (JSONObject) JSONObject.toJSON(data);
        String groupId = o.getString("groupId");
        // 获取所有群成员
        List<String> groupMemberId = groupMemberService.getGroupMemberId(groupId, clientInfo.getAppId());

        GroupMsgReq groupMsgReq = GroupMsgReq.builder()
                .userId(userId)
                .command(command)
                .data(data)
                .clientInfo(clientInfo)
                .groupMemberId(groupMemberId)
                .o(o)
                .groupId(groupId)
                .build();

        if (GroupEventCommand.ADDED_MEMBER.equals(command)) {
            addMemberMsg(groupMsgReq);
        } else if (GroupEventCommand.DELETED_MEMBER.equals(command)) {
            deleteMemberMsg(groupMsgReq);
        } else if (GroupEventCommand.UPDATED_MEMBER.equals(command)) {
            updateMemberMsg(groupMsgReq);
        } else {
            defaultMsg(groupMsgReq);
        }
    }

    private void addMemberMsg(GroupMsgReq groupMsgReq) {
        // TCP 通知发送给管理员和被加入人本身
        AddGroupMemberPack addGroupMemberPack = groupMsgReq.getO().toJavaObject(AddGroupMemberPack.class);

        groupMemberService.getGroupManager(groupMsgReq.getGroupId(), groupMsgReq.getClientInfo().getAppId()).stream()
                .filter(Objects::nonNull)
                .forEach(groupMemberDto -> {
                    if (ClientType.WEBAPI.getCode().equals(groupMsgReq.getClientInfo().getClientType())
                            && groupMemberDto.getMemberId().equals(groupMsgReq.getUserId())) {
                        messageProducer.sendToUserExceptClient(
                                groupMemberDto.getMemberId(), groupMsgReq.getCommand(),
                                groupMsgReq.getData(), groupMsgReq.getClientInfo());
                    } else {
                        messageProducer.sendToUserAllClient(
                                groupMemberDto.getMemberId(), groupMsgReq.getCommand(),
                                groupMsgReq.getData(), groupMsgReq.getClientInfo().getAppId());
                    }
                });
        addGroupMemberPack.getMembers().stream()
                .filter(Objects::nonNull)
                .forEach(member -> {
                    if (ClientType.WEBAPI.getCode().equals(groupMsgReq.getClientInfo().getClientType()) && groupMsgReq.getUserId().equals(member)) {
                        messageProducer.sendToUserExceptClient(
                                member, groupMsgReq.getCommand(),
                                groupMsgReq.getData(), groupMsgReq.getClientInfo());
                    } else {
                        messageProducer.sendToUserAllClient(
                                member, groupMsgReq.getCommand(),
                                groupMsgReq.getData(), groupMsgReq.getClientInfo().getAppId());
                    }
                });
    }

    private void deleteMemberMsg(GroupMsgReq groupMsgReq) {
        RemoveGroupMemberPack pack = groupMsgReq.getO().toJavaObject(RemoveGroupMemberPack.class);
        String memberId = pack.getMember();
        // 退出群聊用户需要加入群信息进行遍历
        List<String> memberIds = groupMemberService.getGroupMemberId(
                groupMsgReq.getGroupId(), groupMsgReq.getClientInfo().getAppId());
        memberIds.add(memberId);
        memberIds.stream()
                .filter(Objects::nonNull)
                .forEach(member -> {
                    if (ClientType.WEBAPI.getCode().equals(groupMsgReq.getClientInfo().getClientType())
                            && member.equals(groupMsgReq.getUserId())) {
                        messageProducer.sendToUserExceptClient(
                                memberId, groupMsgReq.getCommand(),
                                groupMsgReq.getData(), groupMsgReq.getClientInfo());
                    } else {
                        messageProducer.sendToUserAllClient(
                                memberId, groupMsgReq.getCommand(),
                                groupMsgReq.getData(), groupMsgReq.getClientInfo().getAppId());
                    }
                });
    }

    private void updateMemberMsg(GroupMsgReq groupMsgReq) {
        UpdateGroupMemberPack pack = groupMsgReq.getO().toJavaObject(UpdateGroupMemberPack.class);
        String memberId = pack.getMemberId();

        List<GroupMemberDto> groupManager = groupMemberService.getGroupManager(
                groupMsgReq.getGroupId(), groupMsgReq.getClientInfo().getAppId());
        GroupMemberDto groupMemberDto = new GroupMemberDto();
        groupMemberDto.setMemberId(memberId);
        groupManager.add(groupMemberDto);
        groupManager.stream().forEach(member -> {
            if (ClientType.WEBAPI.getCode().equals(groupMsgReq.getClientInfo().getClientType())
                    && member.equals(groupMsgReq.getUserId())) {
                messageProducer.sendToUserExceptClient(
                        memberId, groupMsgReq.getCommand(),
                        groupMsgReq.getData(), groupMsgReq.getClientInfo());
            } else {
                messageProducer.sendToUserAllClient(
                        memberId, groupMsgReq.getCommand(),
                        groupMsgReq.getData(), groupMsgReq.getClientInfo().getAppId());
            }
        });

    }

    private void defaultMsg(GroupMsgReq groupMsgReq) {
        groupMsgReq.getGroupMemberId().stream().forEach(memberId -> {
            if (ClientType.WEBAPI.getCode().equals(groupMsgReq.getClientInfo().getClientType())
                    && memberId.equals(groupMsgReq.getUserId())) {
                messageProducer.sendToUserExceptClient(
                        memberId, groupMsgReq.getCommand(),
                        groupMsgReq.getData(), groupMsgReq.getClientInfo());
            } else {
                messageProducer.sendToUserAllClient(
                        memberId, groupMsgReq.getCommand(),
                        groupMsgReq.getData(), groupMsgReq.getClientInfo().getAppId());
            }
        });
    }

}
