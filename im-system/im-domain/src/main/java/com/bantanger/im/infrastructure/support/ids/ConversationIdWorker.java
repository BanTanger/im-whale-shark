package com.bantanger.im.infrastructure.support.ids;


/**
 * @author BanTanger 半糖
 * @Date 2023/4/7 17:56
 */
public class ConversationIdWorker {

    /**
     * 小的 id 放前面
     *
     * @param fromId
     * @param toId
     * @return
     */
    public static String generateP2PId(String fromId, String toId) {
        int i = fromId.compareTo(toId);
        if (i < 0) {
            return "chat_" + toId + "_" + fromId;
        } else if (i > 0) {
            return "chat_" + fromId + "_" + toId;
        }

        throw new RuntimeException("");
    }

    public static String generateGroupId(String fromId, String groupId) {
        return "group_" + fromId + "_" + groupId;
    }
}
