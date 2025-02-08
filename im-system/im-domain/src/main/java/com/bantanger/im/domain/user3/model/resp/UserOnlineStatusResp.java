package com.bantanger.im.domain.user3.model.resp;

import com.bantanger.im.common.model.UserSession;
import lombok.Data;

import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
@Data
public class UserOnlineStatusResp {

    private List<UserSession> session;

    private String customText;

    private Integer customStatus;

}
