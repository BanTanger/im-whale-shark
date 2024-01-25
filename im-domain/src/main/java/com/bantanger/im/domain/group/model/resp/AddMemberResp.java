package com.bantanger.im.domain.group.model.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * @author bantanger 半糖
 */
@Data
public class AddMemberResp {

    private String memberId;

    /**
     * 加入结果：0 为成功；1 为失败；2 为已经是群成员
     */
    private Integer result;

    private String resultMessage;

    @Getter
    @AllArgsConstructor
    public enum AddGroupResultEnum {
        SUCCESS("加入群聊成功", 0),
        FAIL("加入群聊失败", 1),

        REPEAT_JOIN("重复添加! 您已经是群成员了", 2)
        ;
        private final String message;
        private final int code;

    }

}
