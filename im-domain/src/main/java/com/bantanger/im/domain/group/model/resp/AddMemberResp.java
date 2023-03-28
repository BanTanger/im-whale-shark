package com.bantanger.im.domain.group.model.resp;

import lombok.Data;

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

}
