package com.bantanger.im.message.dao.mapper;

import com.bantanger.im.message.dao.ImGroupMessageHistoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/5 15:02
 */
@Repository("groupMessageHistoryMapper")
public interface ImGroupMessageHistoryMapper extends BaseMapper<ImGroupMessageHistoryEntity> {

    /**
     * 获取指定会话的最大消息序列号
     * @param appId 应用ID
     * @param groupId 群ID
     * @return 最大序列号
     */
    @Select("SELECT MAX(sequence) FROM im_group_message_history WHERE app_id = #{appId} AND group_id = #{groupId}")
    Long getMaxSequenceByToId(@Param("appId") Integer appId, @Param("groupId") String groupId);


}
