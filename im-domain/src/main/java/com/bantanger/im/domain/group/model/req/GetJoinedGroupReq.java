package com.bantanger.im.domain.group.model.req;

import com.bantanger.im.common.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 查询用户加入的所有群列表请求
 * @author bantanger 半糖
 **/
@Data
public class GetJoinedGroupReq extends RequestBase {

    @NotBlank(message = "用户id不能为空")
    private String memberId;

    /**
     * 群类型
     */
    private List<Integer> groupType;

    /**
     * 单次拉取的群组数量，如果不填代表所有群组
     */
    private Integer limit;

    /**
     * 第几页
     */
    private Integer offset;


}
