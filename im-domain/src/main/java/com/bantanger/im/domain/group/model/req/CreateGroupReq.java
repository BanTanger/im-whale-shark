package com.bantanger.im.domain.group.model.req;

import com.bantanger.im.common.model.RequestBase;
import lombok.Data;
import reactor.util.annotation.Nullable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
@Data
public class CreateGroupReq extends RequestBase {

    // 允许为空, 如果有值表示该群是导入的，会重新生成符合规范的新 groupId
    private String groupId;

    //群类型 1私有群（类似微信） 2公开群(类似qq）
    private Integer groupType;

    //群主id
    private String ownerId;

    @NotBlank(message = "群聊名称不能为空")
    private String groupName;

    @NotEmpty(message = "群成员不能为空")
    private List<GroupMemberDto> member;

    /**
     * 是否全员禁言
     * 0 不禁言；
     * 1 全员禁言。
     */
    private Integer mute;

    /** 加入群权限
     *  0 所有人可以加入；
     *  1 群成员可以拉人；
     *  2 群管理员或群组可以拉人。
     */
    private Integer applyJoinType;

    //群简介
    private String introduction;

    //群公告
    private String notification;

    //群头像
    private String photo;

    private Integer MaxMemberCount;

    private String extra;

}
