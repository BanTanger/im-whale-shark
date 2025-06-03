package com.bantanger.im.domain.user.model.req;

import com.bantanger.im.common.model.RequestBase;
import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
@Data
public class GetUserSequenceReq extends RequestBase {

    private String userId;

}
