package com.bantanger.im.service.support.ids;


/**
 * @author BanTanger 半糖
 * @Date 2023/4/7 17:56
 */
public class ConversationIdGenerate {

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
            return toId + "_" + fromId;
        } else if (i > 0) {
            return fromId + "_" + toId;
        }

        throw new RuntimeException("");
    }
}
