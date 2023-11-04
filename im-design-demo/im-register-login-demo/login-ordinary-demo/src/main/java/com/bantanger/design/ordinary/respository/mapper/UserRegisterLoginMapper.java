package com.bantanger.design.ordinary.respository.mapper;

import com.bantanger.design.ordinary.respository.dao.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/3 23:46
 */
@Mapper
public interface UserRegisterLoginMapper extends BaseMapper<UserEntity> {

}
