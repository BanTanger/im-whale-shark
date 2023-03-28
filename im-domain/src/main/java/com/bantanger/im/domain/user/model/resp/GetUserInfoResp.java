package com.bantanger.im.domain.user.model.resp;

import com.bantanger.im.domain.user.dao.ImUserDataEntity;
import lombok.Data;

import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
@Data
public class GetUserInfoResp {

    private List<ImUserDataEntity> userDataItem;

    private List<String> failUser;


}
