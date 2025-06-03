package com.bantanger.im.domain.user.model.resp;

import lombok.Data;

import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
@Data
public class ImportUserResp {

    private List<String> successId;

    private List<String> errorId;

}
