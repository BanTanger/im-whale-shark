package com.bantanger.im.domain.conversation.service;

import com.bantanger.im.codec.pack.conversation.DeleteConversationPack;
import com.bantanger.im.codec.pack.conversation.UpdateConversationPack;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.command.ConversationEventCommand;
import com.bantanger.im.common.enums.conversation.ConversationTypeEnum;
import com.bantanger.im.common.enums.error.ConversationErrorCode;
import com.bantanger.im.common.model.ClientInfo;
import com.bantanger.im.common.model.message.read.MessageReadContent;
import com.bantanger.im.domain.conversation.dao.ImConversationSetEntity;
import com.bantanger.im.domain.conversation.dao.mapper.ImConversationSetMapper;
import com.bantanger.im.domain.conversation.model.DeleteConversationReq;
import com.bantanger.im.domain.conversation.model.UpdateConversationReq;
import com.bantanger.im.domain.message.seq.RedisSequence;
import com.bantanger.im.service.config.AppConfig;
import com.bantanger.im.service.sendmsg.MessageProducer;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 14:22
 */
@Service
public class ConversationServiceImpl implements ConversationService {

    @Resource
    ImConversationSetMapper imConversationSetMapper;

    @Resource
    MessageProducer messageProducer;

    @Resource
    RedisSequence redisSequence;

    @Resource
    AppConfig appConfig;

    public String convertConversationId(Integer type, String fromId, String toId) {
        return type + "_" + fromId + "_" + toId;
    }

    @Override
    public void messageMarkRead(MessageReadContent messageReadContent) {
        // 抽离 toId, 有不同情况
        // 会话类型为单聊，toId 赋值为目标用户
        String toId = messageReadContent.getToId();
        if (ConversationTypeEnum.GROUP.getCode().equals(messageReadContent.getConversationType())) {
            // 会话类型为群聊，toId 赋值为 groupId
            toId = messageReadContent.getGroupId();
        }
        // conversationId: 1_fromId_toId
        String conversationId = convertConversationId(
                messageReadContent.getConversationType(), messageReadContent.getFromId(), toId);
        QueryWrapper<ImConversationSetEntity> query = new QueryWrapper<>();
        query.eq("app_id", messageReadContent.getAppId());
        query.eq("conversation_id", conversationId);
        ImConversationSetEntity imConversationSetEntity = imConversationSetMapper.selectOne(query);
        if (imConversationSetEntity == null) {
            // 如果查询记录为空，代表不存在该会话，需要新建
            imConversationSetEntity = new ImConversationSetEntity();
            long seq = redisSequence.doGetSeq(
                    messageReadContent.getAppId() + ":" + Constants.SeqConstants.ConversationSeq);
            imConversationSetEntity.setConversationId(conversationId);
            imConversationSetEntity.setSequence(seq);
            imConversationSetEntity.setConversationType(messageReadContent.getConversationType());
            imConversationSetEntity.setFromId(messageReadContent.getFromId());
            imConversationSetEntity.setToId(toId);
            imConversationSetEntity.setAppId(messageReadContent.getAppId());
            imConversationSetEntity.setReadSequence(messageReadContent.getMessageSequence());

            imConversationSetMapper.insert(imConversationSetEntity);
        } else {
            long seq = redisSequence.doGetSeq(
                    messageReadContent.getAppId() + ":" + Constants.SeqConstants.ConversationSeq);
            imConversationSetEntity.setSequence(seq);
            imConversationSetEntity.setReadSequence(messageReadContent.getMessageSequence());
            imConversationSetMapper.readMark(imConversationSetEntity);
        }
    }

    @Override
    public ResponseVO deleteConversation(DeleteConversationReq req) {
        if (appConfig.getDeleteConversationSyncMode() == 1) {
            DeleteConversationPack pack = new DeleteConversationPack();
            pack.setConversationId(req.getConversationId());
            messageProducer.sendToUserExceptClient(req.getFromId(),
                    ConversationEventCommand.CONVERSATION_DELETE,
                    pack, new ClientInfo(req.getAppId(), req.getClientType(),
                            req.getImei()));
        }
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO updateConversation(UpdateConversationReq req) {
        if (req.getIsTop() == null && req.getIsMute() == null) {
            return ResponseVO.errorResponse(ConversationErrorCode.CONVERSATION_UPDATE_PARAM_ERROR);
        }
        QueryWrapper<ImConversationSetEntity> query = new QueryWrapper<>();
        query.eq("conversation_id", req.getConversationId());
        query.eq("app_id", req.getAppId());
        ImConversationSetEntity imConversationSetEntity = imConversationSetMapper.selectOne(query);
        if (imConversationSetEntity != null) {
            long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.ConversationSeq);
            if (req.getIsMute() != null) {
                // 更新禁言状态
                imConversationSetEntity.setIsMute(req.getIsMute());
            }
            if (req.getIsTop() != null) {
                // 更新置顶状态
                imConversationSetEntity.setIsTop(req.getIsTop());
            }
            imConversationSetEntity.setSequence(seq);
            imConversationSetMapper.update(imConversationSetEntity, query);

            UpdateConversationPack pack = new UpdateConversationPack();
            pack.setConversationId(req.getConversationId());
            pack.setIsMute(imConversationSetEntity.getIsMute());
            pack.setIsTop(imConversationSetEntity.getIsTop());
            pack.setSequence(seq);
            pack.setConversationType(imConversationSetEntity.getConversationType());
            messageProducer.sendToUserExceptClient(req.getFromId(),
                    ConversationEventCommand.CONVERSATION_UPDATE,
                    pack, new ClientInfo(req.getAppId(), req.getClientType(),
                            req.getImei()));
        }
        return ResponseVO.successResponse();
    }

}
