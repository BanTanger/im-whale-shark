package com.bantanger.im.domain.conversation.service;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.model.SyncReq;
import com.bantanger.im.common.model.message.read.MessageReadContent;
import com.bantanger.im.domain.conversation.model.DeleteConversationReq;
import com.bantanger.im.domain.conversation.model.UpdateConversationReq;
import com.sun.corba.se.impl.orbutil.concurrent.Sync;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 14:22
 */
public interface ConversationService {

    /**
     * 标记用户已读消息情况，记录 Seq 消息偏序
     * @param messageReadContent
     */
    void messageMarkRead(MessageReadContent messageReadContent);

    /**
     * 删除会话
     * @param req
     * @return
     */
    ResponseVO deleteConversation(DeleteConversationReq req);

    /**
     * 更新会话: 置顶、免打扰
     * @param req
     * @return
     */
    ResponseVO updateConversation(UpdateConversationReq req);

    /**
     * 同步客户端本地 Seq 与服务端最大 Seq
     * @param req 数据结构为{客户端最大 Seq，服务端一次响应的最大次数}
     * @return
     */
    ResponseVO syncConversationSet(SyncReq req);
}
