package com.bantanger.im.design.common.respository.dao;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/3 23:47
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@TableName("user")
public class UserEntity {

    @TableId("id")
    private String userId;

    @NonNull
    @TableField("username")
    private String userName;

    @NonNull
    private String password;

    private Date createTime;

    private String userEmail;

}
