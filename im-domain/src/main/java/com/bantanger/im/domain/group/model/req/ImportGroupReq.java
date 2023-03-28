package com.bantanger.im.domain.group.model.req;

import com.bantanger.im.common.model.RequestBase;
import lombok.Data;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotBlank;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
@Data
public class ImportGroupReq extends RequestBase {

    private String groupId;

    @NotBlank(message = "群名称不能为空")
    private String groupName;

    /**
     * 群主id
     */
    private String ownerId;

    /**
     * 群类型 1私有群（类似微信） 2公开群(类似qq）
     */
    private Integer groupType;

    /**
     * 是否全员禁言，0 不禁言；1 全员禁言。
     */
    private Integer mute;

    /**
     * 加入群权限，0 所有人可以加入；1 群成员可以拉人；2 群管理员或群组可以拉人。
     */
    private Integer applyJoinType;

    /**
     * 群简介
     */
    private String introduction;

    /**
     * 群公告
     */
    private String notification;

    /**
     * 群头像
     */
    private String photo;

    /**
     * 群成员上限
     */
    private Integer MaxMemberCount;

    private Long createTime;

    private String extra;

}
