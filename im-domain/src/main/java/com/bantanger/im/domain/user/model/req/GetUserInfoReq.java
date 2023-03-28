package com.bantanger.im.domain.user.model.req;

import com.bantanger.im.common.model.RequestBase;
import lombok.Data;

import java.util.List;

/**
 * @author BanTanger 半糖
 */
@Data
public class GetUserInfoReq extends RequestBase {

    private List<String> userIds;

}
