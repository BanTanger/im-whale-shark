package com.bantanger.im.domain.conversation.dao.mapper;

import com.bantanger.im.domain.conversation.dao.ImConversationSetEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 14:32
 */
@Repository
public interface ImConversationSetMapper extends BaseMapper<ImConversationSetEntity> {

    void readMark(ImConversationSetEntity imConversationSetEntity);

    Long getConversationSetMaxSeq(Integer appId, String userId);

    /**
     * 增量拉取会话消息一次最大条目数
     * @param appId
     * @return
     */
    Long getConversationMaxSeq(Integer appId);
}
