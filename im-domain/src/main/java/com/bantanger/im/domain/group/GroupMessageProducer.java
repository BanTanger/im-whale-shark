package com.bantanger.im.domain.group;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.pack.group.AddGroupMemberPack;
import com.bantanger.im.codec.pack.group.RemoveGroupMemberPack;
import com.bantanger.im.codec.pack.group.UpdateGroupMemberPack;
import com.bantanger.im.common.enums.ClientType;
import com.bantanger.im.common.enums.command.Command;
import com.bantanger.im.common.enums.command.GroupEventCommand;
import com.bantanger.im.common.model.ClientInfo;
import com.bantanger.im.domain.group.model.req.GroupMemberDto;
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

        if (GroupEventCommand.ADDED_MEMBER.equals(command)) {
            addMemberMsg(userId, command, data, clientInfo, groupMemberId, o, groupId);
        } else if (GroupEventCommand.DELETED_MEMBER.equals(command)) {
            deleteMemberMsg(userId, command, data, clientInfo, groupMemberId, o, groupId);
        } else if (GroupEventCommand.UPDATED_MEMBER.equals(command)) {
            updateMemberMsg(userId, command, data, clientInfo, groupMemberId, o, groupId);
        } else {
            DefaultMsg(userId, command, data, clientInfo, groupMemberId, o, groupId);
        }
    }

    private void addMemberMsg(String userId, Command command, Object data, ClientInfo clientInfo,
                              List<String> groupMemberId, JSONObject o, String groupId) {
        // TCP 通知发送给管理员和被加入人本身
        AddGroupMemberPack addGroupMemberPack = o.toJavaObject(AddGroupMemberPack.class);

        groupMemberService.getGroupManager(groupId, clientInfo.getAppId()).stream()
                .filter(Objects::nonNull)
                .forEach(groupMemberDto -> {
                    if (ClientType.WEBAPI.getCode().equals(clientInfo.getClientType()) && groupMemberDto.getMemberId().equals(userId)) {
                        messageProducer.sendToUserExceptClient(groupMemberDto.getMemberId(), command, data, clientInfo);
                    } else {
                        messageProducer.sendToUserAllClient(groupMemberDto.getMemberId(), command, data, clientInfo.getAppId());
                    }
                });
        addGroupMemberPack.getMembers().stream()
                .filter(Objects::nonNull)
                .forEach(member -> {
                    if (ClientType.WEBAPI.getCode().equals(clientInfo.getClientType()) && userId.equals(member)) {
                        messageProducer.sendToUserExceptClient(member, command, data, clientInfo);
                    } else {
                        messageProducer.sendToUserAllClient(member, command, data, clientInfo.getAppId());
                    }
                });
    }

    private void deleteMemberMsg(String userId, Command command, Object data, ClientInfo clientInfo,
                                 List<String> groupMemberId, JSONObject o, String groupId) {
        RemoveGroupMemberPack pack = o.toJavaObject(RemoveGroupMemberPack.class);
        String memberId = pack.getMember();
        // 退出群聊用户需要加入群信息进行遍历
        List<String> memberIds = groupMemberService.getGroupMemberId(groupId, clientInfo.getAppId());
        memberIds.add(memberId);
        memberIds.stream()
                .filter(Objects::nonNull)
                .forEach(member -> {
                    if (ClientType.WEBAPI.getCode().equals(clientInfo.getClientType()) && member.equals(userId)) {
                        messageProducer.sendToUserExceptClient(member, command, data, clientInfo);
                    } else {
                        messageProducer.sendToUserAllClient(member, command, data, clientInfo.getAppId());
                    }
                });
    }

    private void updateMemberMsg(String userId, Command command, Object data, ClientInfo clientInfo,
                                 List<String> groupMemberId, JSONObject o, String groupId) {
        UpdateGroupMemberPack pack = o.toJavaObject(UpdateGroupMemberPack.class);
        String memberId = pack.getMemberId();

        List<GroupMemberDto> groupManager = groupMemberService.getGroupManager(groupId, clientInfo.getAppId());
        GroupMemberDto groupMemberDto = new GroupMemberDto();
        groupMemberDto.setMemberId(memberId);
        groupManager.add(groupMemberDto);
        groupManager.stream().forEach(member -> {
            if (ClientType.WEBAPI.getCode().equals(clientInfo.getClientType()) && member.equals(userId)) {
                messageProducer.sendToUserExceptClient(member.getMemberId(), command, data, clientInfo);
            } else {
                messageProducer.sendToUserAllClient(member.getMemberId(), command, data, clientInfo.getAppId());
            }
        });

    }

    private void DefaultMsg(String userId, Command command, Object data, ClientInfo clientInfo,
                            List<String> groupMemberId, JSONObject o, String groupId) {
        groupMemberId.stream().forEach(memberId -> {
            if (ClientType.WEBAPI.getCode().equals(clientInfo.getClientType()) && memberId.equals(userId)) {
                messageProducer.sendToUserExceptClient(memberId, command,
                        data, clientInfo);
            } else {
                messageProducer.sendToUserAllClient(memberId, command, data, clientInfo.getAppId());
            }
        });
    }

}
