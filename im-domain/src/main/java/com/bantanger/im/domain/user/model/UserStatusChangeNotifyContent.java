package com.bantanger.im.domain.user.model;

import com.bantanger.im.common.model.ClientInfo;
import lombok.Data;

/**
 * status 区分是上线还是下线
 * @author BanTanger 半糖
 * @Date 2023/3/16 20:07
 */
@Data
public class UserStatusChangeNotifyContent extends ClientInfo {


    private String userId;

    /**
     * 服务端状态 1上线 2离线
     */
    private Integer status;

}
