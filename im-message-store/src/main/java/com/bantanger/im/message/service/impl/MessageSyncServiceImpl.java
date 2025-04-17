package com.bantanger.im.message.service.impl;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.model.SyncResp;
import com.bantanger.im.message.dao.ImGroupMessageHistoryEntity;
import com.bantanger.im.message.dao.ImMessageHistoryEntity;
import com.bantanger.im.message.dao.mapper.ImGroupMessageHistoryMapper;
import com.bantanger.im.message.dao.mapper.ImMessageHistoryMapper;
import com.bantanger.im.message.model.*;
import com.bantanger.im.message.service.MessageSyncService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息同步服务实现类
 * 
 * @author BanTanger 半糖
 * @Date 2023/4/13 23:33
 */
@Slf4j
@Service
public class MessageSyncServiceImpl implements MessageSyncService {

    @Resource
    private ImMessageHistoryMapper messageHistoryMapper;

    @Resource
    private ImGroupMessageHistoryMapper groupMessageHistoryMapper;

    /**
     * 会话类型：单聊
     */
    private static final int CONVERSATION_TYPE_P2P = 1;
    
    /**
     * 会话类型：群聊
     */
    private static final int CONVERSATION_TYPE_GROUP = 2;

    @Override
    public ResponseVO syncP2PMessage(SyncP2PMessageReq req) {
        if (req.getMaxLimit() == null) {
            req.setMaxLimit(100);
        }

        SyncResp<ImMessageHistoryEntity> resp = new SyncResp<>();

        // 查询条件：appId + ownerId + sequence > lastSequence + 排序 + 限制
        LambdaQueryWrapper<ImMessageHistoryEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImMessageHistoryEntity::getAppId, req.getAppId());
        queryWrapper.eq(ImMessageHistoryEntity::getOwnerId, req.getUserId());
        
        // 如果指定了 toId，则添加条件
        if (req.getToId() != null && !req.getToId().isEmpty()) {
            queryWrapper.eq(ImMessageHistoryEntity::getToId, req.getToId());
        }
        
        queryWrapper.gt(ImMessageHistoryEntity::getSequence, req.getLastSequence());
        queryWrapper.orderByAsc(ImMessageHistoryEntity::getSequence);
        queryWrapper.last("limit " + req.getMaxLimit());
        
        List<ImMessageHistoryEntity> list = messageHistoryMapper.selectList(queryWrapper);

        if (!CollectionUtils.isEmpty(list)) {
            ImMessageHistoryEntity maxSeqEntity = list.get(list.size() - 1);
            resp.setDataList(list);
            
            // 获取用户最大消息序列号
            Long maxSeq = messageHistoryMapper.getMaxSequence(req.getAppId(), req.getUserId());
            resp.setMaxSequence(maxSeq);
            
            // 判断是否拉取完成
            resp.setCompleted(maxSeqEntity.getSequence() >= maxSeq);
            return ResponseVO.successResponse(resp);
        }
        
        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO syncGroupMessage(SyncGroupMessageReq req) {
        if (req.getMaxLimit() == null) {
            req.setMaxLimit(100);
        }

        SyncResp<ImGroupMessageHistoryEntity> resp = new SyncResp<>();

        // 查询条件：appId + groupId + sequence > lastSequence + 排序 + 限制
        LambdaQueryWrapper<ImGroupMessageHistoryEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImGroupMessageHistoryEntity::getAppId, req.getAppId());
        queryWrapper.eq(ImGroupMessageHistoryEntity::getGroupId, req.getGroupId());
        queryWrapper.gt(ImGroupMessageHistoryEntity::getSequence, req.getLastSequence());
        queryWrapper.orderByAsc(ImGroupMessageHistoryEntity::getSequence);
        queryWrapper.last("limit " + req.getMaxLimit());
        
        List<ImGroupMessageHistoryEntity> list = groupMessageHistoryMapper.selectList(queryWrapper);

        if (!CollectionUtils.isEmpty(list)) {
            ImGroupMessageHistoryEntity maxSeqEntity = list.get(list.size() - 1);
            resp.setDataList(list);
            
            // 获取群组最大消息序列号
            Long maxSeq = groupMessageHistoryMapper.getMaxSequenceByToId(req.getAppId(), req.getGroupId());
            resp.setMaxSequence(maxSeq);
            
            // 判断是否拉取完成
            resp.setCompleted(maxSeqEntity.getSequence() >= maxSeq);
            return ResponseVO.successResponse(resp);
        }
        
        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO batchSyncMessage(BatchSyncMessageReq req) {
        if (req.getMaxCount() == null) {
            req.setMaxCount(100);
        }

        BatchSyncMessageResp resp = new BatchSyncMessageResp();
        List<Object> conversationList = new ArrayList<>();

        if (!CollectionUtils.isEmpty(req.getConversations())) {
            for (ConversationSyncInfo conversationInfo : req.getConversations()) {
                // 根据会话类型分别处理
                if (CONVERSATION_TYPE_P2P == conversationInfo.getConversationType()) {
                    // 单聊消息
                    ConversationMessageResp<ImMessageHistoryEntity> p2pResp = syncP2PConversationMessage(
                            req.getAppId(), 
                            req.getUserId(), 
                            conversationInfo.getConversationId(), 
                            conversationInfo.getSequence(), 
                            req.getMaxCount()
                    );
                    p2pResp.setConversationId(conversationInfo.getConversationId());
                    p2pResp.setConversationType(CONVERSATION_TYPE_P2P);
                    conversationList.add(p2pResp);
                } else if (CONVERSATION_TYPE_GROUP == conversationInfo.getConversationType()) {
                    // 群聊消息
                    ConversationMessageResp<ImGroupMessageHistoryEntity> groupResp = syncGroupConversationMessage(
                            req.getAppId(), 
                            conversationInfo.getConversationId(), 
                            conversationInfo.getSequence(), 
                            req.getMaxCount()
                    );
                    groupResp.setConversationId(conversationInfo.getConversationId());
                    groupResp.setConversationType(CONVERSATION_TYPE_GROUP);
                    conversationList.add(groupResp);
                }
            }
        }

        resp.setConversationList(conversationList);
        return ResponseVO.successResponse(resp);
    }

    /**
     * 同步单聊会话消息
     * 
     * @param appId 应用ID
     * @param userId 用户ID
     * @param toId 对方ID
     * @param sequence 客户端序列号
     * @param maxCount 最大消息数
     * @return 会话消息响应
     */
    private ConversationMessageResp<ImMessageHistoryEntity> syncP2PConversationMessage(
            Integer appId, String userId, String toId, Long sequence, Integer maxCount) {
        
        ConversationMessageResp<ImMessageHistoryEntity> resp = new ConversationMessageResp<>();
        
        // 查询条件：appId + ownerId + toId + sequence > clientSequence + 排序 + 限制
        LambdaQueryWrapper<ImMessageHistoryEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImMessageHistoryEntity::getAppId, appId);
        queryWrapper.eq(ImMessageHistoryEntity::getOwnerId, userId);
        queryWrapper.eq(ImMessageHistoryEntity::getToId, toId);
        queryWrapper.gt(ImMessageHistoryEntity::getSequence, sequence);
        queryWrapper.orderByAsc(ImMessageHistoryEntity::getSequence);
        queryWrapper.last("limit " + maxCount);
        
        List<ImMessageHistoryEntity> messages = messageHistoryMapper.selectList(queryWrapper);
        resp.setMessages(messages);
        
        // 获取最大序列号
        Long maxSeq = messageHistoryMapper.getMaxSequenceByToId(appId, userId, toId);
        resp.setMaxSequence(maxSeq);
        
        // 判断是否拉取完成
        if (!CollectionUtils.isEmpty(messages)) {
            ImMessageHistoryEntity maxSeqEntity = messages.get(messages.size() - 1);
            resp.setCompleted(maxSeqEntity.getSequence() >= maxSeq);
        } else {
            resp.setCompleted(true);
        }
        
        return resp;
    }

    /**
     * 同步群聊会话消息
     * 
     * @param appId 应用ID
     * @param groupId 群组ID
     * @param sequence 客户端序列号
     * @param maxCount 最大消息数
     * @return 会话消息响应
     */
    private ConversationMessageResp<ImGroupMessageHistoryEntity> syncGroupConversationMessage(
            Integer appId, String groupId, Long sequence, Integer maxCount) {
        
        ConversationMessageResp<ImGroupMessageHistoryEntity> resp = new ConversationMessageResp<>();
        
        // 查询条件：appId + groupId + sequence > clientSequence + 排序 + 限制
        LambdaQueryWrapper<ImGroupMessageHistoryEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImGroupMessageHistoryEntity::getAppId, appId);
        queryWrapper.eq(ImGroupMessageHistoryEntity::getGroupId, groupId);
        queryWrapper.gt(ImGroupMessageHistoryEntity::getSequence, sequence);
        queryWrapper.orderByAsc(ImGroupMessageHistoryEntity::getSequence);
        queryWrapper.last("limit " + maxCount);
        
        List<ImGroupMessageHistoryEntity> messages = groupMessageHistoryMapper.selectList(queryWrapper);
        resp.setMessages(messages);
        
        // 获取最大序列号
        Long maxSeq = groupMessageHistoryMapper.getMaxSequenceByToId(appId, groupId);
        resp.setMaxSequence(maxSeq);
        
        // 判断是否拉取完成
        if (!CollectionUtils.isEmpty(messages)) {
            ImGroupMessageHistoryEntity maxSeqEntity = messages.get(messages.size() - 1);
            resp.setCompleted(maxSeqEntity.getSequence() >= maxSeq);
        } else {
            resp.setCompleted(true);
        }
        
        return resp;
    }
}