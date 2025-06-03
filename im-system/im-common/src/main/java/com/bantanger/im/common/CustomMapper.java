package com.bantanger.im.common;

import com.bantanger.im.common.enums.conversation.ConversationAppearsType;
import com.bantanger.im.common.enums.conversation.ConversationNoticeType;
import com.bantanger.im.common.enums.conversation.ConversationType;
import com.bantanger.im.common.enums.message.MsgType;
import com.bantanger.im.common.enums.user.FriendAllowType;
import com.bantanger.im.common.enums.user.UserType;

/**
 * @author chensongmin
 * @description
 * @date 2025/6/3
 */
public class CustomMapper {

    public Integer UserType2Int(UserType userType) {
        return userType.getCode();
    }

    public UserType int2UserType(Integer code) {
        return UserType.of(code).orElse(UserType.ORDINARY);
    }

    public Integer FriendAllowType2Int(FriendAllowType friendAllowType) {
        return friendAllowType.getCode();
    }

    public FriendAllowType int2FriendAllowType(Integer code) {
        return FriendAllowType.of(code).orElse(null);
    }

    public Integer ConversationAppearsType2Int(ConversationAppearsType conversationAppearsType) {
        return conversationAppearsType.getCode();
    }

    public ConversationAppearsType int2ConversationAppearsType(Integer code) {
        return ConversationAppearsType.of(code).orElse(null);
    }

    public Integer ConversationNoticeType2Int(ConversationNoticeType conversationNoticeType) {
        return conversationNoticeType.getCode();
    }

    public ConversationNoticeType int2ConversationNoticeType(Integer code) {
        return ConversationNoticeType.of(code).orElse(null);
    }

    public Integer ConversationType2Int(ConversationType conversationType) {
        return conversationType.getCode();
    }

    public ConversationType int2ConversationType(Integer code) {
        return ConversationType.of(code).orElse(null);
    }

    public Integer MsgType2Int(MsgType msgType) {
        return msgType.getCode();
    }

    public MsgType int2MsgType(Integer code) {
        return MsgType.of(code).orElse(null);
    }



}
