package com.bantanger.im.domain.conversation.service;

import com.bantanger.im.codec.pack.conversation.DeleteConversationPack;
import com.bantanger.im.codec.pack.conversation.UpdateConversationPack;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.command.ConversationEventCommand;
import com.bantanger.im.common.enums.conversation.ConversationType;
import com.bantanger.im.common.enums.error.ConversationErrorCode;
import com.bantanger.im.common.model.ClientInfo;
import com.bantanger.im.common.model.SyncReq;
import com.bantanger.im.common.model.SyncResp;
import com.bantanger.im.common.model.message.read.MessageReadContent;
import com.bantanger.im.domain.conversation.dao.ImConversationSetEntity;
import com.bantanger.im.domain.conversation.dao.mapper.ImConversationSetMapper;
import com.bantanger.im.domain.conversation.model.CreateConversationReq;
import com.bantanger.im.domain.conversation.model.DeleteConversationReq;
import com.bantanger.im.domain.conversation.model.UpdateConversationReq;
import com.bantanger.im.domain.messageddd.domainservice.sendmsg.MessageProducer;
import com.bantanger.im.infrastructure.config.AppConfig;
import com.bantanger.im.infrastructure.support.ids.SequenceIdWorker;
import com.bantanger.im.infrastructure.utils.UserSequenceRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.validation.Valid;
import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 14:22
 */
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final AppConfig appConfig;
    private final SequenceIdWorker sequenceIdWorker;
    private final MessageProducer messageProducer;
    private final UserSequenceRepository userSequenceRepository;
    private final ImConversationSetMapper imConversationSetMapper;

    @Override
    public ResponseVO createConversation(@Valid CreateConversationReq req) {
        Integer conversationType = req.getConversationType();
        String conversationId = ConversationService.convertConversationId(
                conversationType, req.getFromId(), req.getToId());

        // 创建双方会话
        ImConversationSetEntity imConversationSetEntity = new ImConversationSetEntity();
        imConversationSetEntity.setAppId(req.getAppId());
        imConversationSetEntity.setFromId(req.getFromId());
        imConversationSetEntity.setToId(req.getToId());
        imConversationSetEntity.setConversationId(conversationId);
        imConversationSetEntity.setConversationType(conversationType);
        int insert = imConversationSetMapper.insert(imConversationSetEntity);
        if (insert != 1) {
            return ResponseVO.errorResponse(ConversationErrorCode.CONVERSATION_CREATE_FAIL);
        }
        return ResponseVO.successResponse();
    }

    @Override
    public void messageMarkRead(MessageReadContent messageReadContent) {
        // 抽离 toId, 有不同情况
        String toId = getToIdOrGroupId(messageReadContent);

        // conversationId: 1_fromId_toId
        String conversationId = ConversationService.convertConversationId(
                messageReadContent.getConversationType(), messageReadContent.getFromId(), toId);

        /* key：appid + Seq
         * 这是因为 conversation seq 是为了对所有会话进行排序的，
         * 即客户端看到的消息从高到低是按照会话里的最新消息进行排序（置顶另外讨论）最新会话在最前
         * 而 p2p，group 的 seq 是为了对一个会话里的消息进行排序
         */
        long seq = sequenceIdWorker.doGetSeq(
                messageReadContent.getAppId() + Constants.SeqConstants.ConversationSeq);

        imConversationSetMapper.readMark(buildReadMarkModel(messageReadContent, conversationId, seq));

        userSequenceRepository.writeUserSeq(messageReadContent.getAppId(),
                messageReadContent.getFromId(), Constants.SeqConstants.ConversationSeq, seq);
    }


    @Override
    public ResponseVO deleteConversation(@Valid DeleteConversationReq req) {
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
    public ResponseVO updateConversation(@Valid UpdateConversationReq req) {
        if (req.getIsTop() == null && req.getIsMute() == null) {
            return ResponseVO.errorResponse(ConversationErrorCode.CONVERSATION_UPDATE_PARAM_ERROR);
        }
        QueryWrapper<ImConversationSetEntity> query = new QueryWrapper<>();
        query.eq("conversation_id", req.getConversationId());
        query.eq("app_id", req.getAppId());
        ImConversationSetEntity imConversationSetEntity = imConversationSetMapper.selectOne(query);
        if (imConversationSetEntity != null) {
            long seq = sequenceIdWorker.doGetSeq(req.getAppId() + Constants.SeqConstants.ConversationSeq);
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

            userSequenceRepository.writeUserSeq(req.getAppId(),
            req.getFromId(), Constants.SeqConstants.ConversationSeq, seq);

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
    @Override
    public ResponseVO syncConversationSet(SyncReq req) {
        if (req.getMaxLimit() > appConfig.getConversationMaxCount()) {
            req.setMaxLimit(appConfig.getConversationMaxCount());
        }

        SyncResp<ImConversationSetEntity> resp = new SyncResp<>();

        QueryWrapper<ImConversationSetEntity> query = new QueryWrapper<>();
        query.eq("from_id", req.getOperator());
        query.gt("sequence", req.getLastSequence());
        query.eq("app_id", req.getAppId());
        query.last("limit " + req.getMaxLimit());
        query.orderByAsc("sequence");
        List<ImConversationSetEntity> list = imConversationSetMapper.selectList(query);

        if (!CollectionUtils.isEmpty(list)) {
            ImConversationSetEntity maxSeqEntity = list.get(list.size() - 1);
            resp.setDataList(list);
            // 设置最大 Seq
            Long conversationMaxSeq = imConversationSetMapper
                    .getConversationMaxSeq(req.getAppId());
            resp.setMaxSequence(conversationMaxSeq);
            // 设置是否拉取完毕
            resp.setCompleted(maxSeqEntity.getSequence() >= conversationMaxSeq);
            return ResponseVO.successResponse(resp);
        }
        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }

    private static ImConversationSetEntity buildReadMarkModel(MessageReadContent messageReadContent, String conversationId, long seq) {
        ImConversationSetEntity imConversationSetEntity = new ImConversationSetEntity();
        imConversationSetEntity.setAppId(messageReadContent.getAppId());
        imConversationSetEntity.setConversationId(conversationId);
        imConversationSetEntity.setSequence(seq);
        imConversationSetEntity.setReadSequence(messageReadContent.getMessageSequence());
        return imConversationSetEntity;
    }

    private static String getToIdOrGroupId(MessageReadContent messageReadContent) {
        // 会话类型为单聊，toId 赋值为目标用户
        String toId = messageReadContent.getToId();
        if (ConversationType.GROUP.getCode().equals(messageReadContent.getConversationType())) {
            // 会话类型为群聊，toId 赋值为 groupId
            toId = messageReadContent.getGroupId();
        }
        return toId;
    }
}
