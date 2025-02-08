package com.bantanger.im.domain.group.dao.mapper;

import com.bantanger.im.domain.group.dao.ImGroupMemberEntity;
import com.bantanger.im.domain.group.model.req.GroupMemberDto;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author bantanger 半糖
 */
@Repository
public interface ImGroupMemberMapper extends BaseMapper<ImGroupMemberEntity> {

    /**
     * 拉取用户所加入的所有群组 id
     * @param appId
     * @param memberId
     * @return
     */
    List<String> getJoinedGroupId(@Param("appId") Integer appId, @Param("memberId") String memberId);

    /**
     * 拉取群组所有成员(基本)信息
     * @param appId
     * @param groupId
     * @return
     */
    List<GroupMemberDto> getGroupMember(@Param("appId") Integer appId, @Param("groupId") String groupId);

    /**
     * 拉取群组所有成员 id
     * @param appId
     * @param groupId
     * @return
     */
    List<String> getGroupMemberId(@Param("appId") Integer appId, @Param("groupId") String groupId);

    /**
     * 拉取群组管理员(基本)信息
     * @param groupId
     * @param appId
     * @return
     */
    List<GroupMemberDto> getGroupManager(@Param("appId") Integer appId, @Param("groupId") String groupId);

    /**
     * 增量拉取用户加入群组的 Id
     * @param appId
     * @param memberId
     * @param role
     * @return
     */
    List<String> syncJoinedGroupId(@Param("appId") Integer appId, @Param("memberId") String memberId, @Param("role") Integer role);
}
