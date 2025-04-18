package com.bantanger.im.message.dao.mapper;

import com.bantanger.im.message.dao.ImMessageHistoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/5 13:02
 */
@Repository("messageHistoryMapper")
public interface ImMessageHistoryMapper extends BaseMapper<ImMessageHistoryEntity> {

    /**
     * 批量插入（mysql）
     * @param entityList
     * @return
     */
    Integer insertBatchSomeColumn(Collection<ImMessageHistoryEntity> entityList);

    /**
     * 获取用户最大消息序列号
     * @param appId 应用ID
     * @param userId 用户ID
     * @return 最大序列号
     */
    @Select("SELECT MAX(sequence) FROM im_message_history WHERE app_id = #{appId} AND owner_id = #{userId}")
    Long getMaxSequence(@Param("appId") Integer appId, @Param("userId") String userId);

    /**
     * 获取指定会话的最大消息序列号
     * @param appId 应用ID
     * @param userId 用户ID
     * @param toId 对方ID
     * @return 最大序列号
     */
    @Select("SELECT MAX(sequence) FROM im_message_history WHERE app_id = #{appId} AND owner_id = #{userId} AND to_id = #{toId}")
    Long getMaxSequenceByToId(@Param("appId") Integer appId, @Param("userId") String userId, @Param("toId") String toId);
}
