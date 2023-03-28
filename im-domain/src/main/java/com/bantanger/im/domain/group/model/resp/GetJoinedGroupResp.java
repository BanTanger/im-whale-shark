package com.bantanger.im.domain.group.model.resp;

import com.bantanger.im.domain.group.dao.ImGroupEntity;
import lombok.Data;

import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
@Data
public class GetJoinedGroupResp {

    private Integer totalCount;

    private List<ImGroupEntity> groupList;

}
