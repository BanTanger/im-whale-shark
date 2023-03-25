package com.bantanger.im.common.model;

import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 12:04
 */
@Data
public class UserClientDto {

    private String userId;

    private Integer appId;

    private Integer clientType;

}
