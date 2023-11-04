package com.bantanger.design.bridge.respository;

import com.bantanger.design.bridge.respository.dao.UserEntity;
import com.bantanger.design.bridge.respository.mapper.UserRegisterLoginMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/3 23:44
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final UserRegisterLoginMapper userRegisterLoginMapper;

    public UserEntity findAccountByUserNameAndPassword(String username, String password) {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("username", username);
        queryMap.put("password", password);
        List<UserEntity> userEntities = userRegisterLoginMapper.selectByMap(queryMap);
        if (userEntities == null || userEntities.size() == 0) {
            return null;
        }
        return userEntities.get(0);
    }

    public UserEntity findAccountByUserName(String username) {
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("username", username);
        List<UserEntity> userEntities = userRegisterLoginMapper.selectByMap(queryMap);
        if (userEntities == null || userEntities.size() == 0) {
            return null;
        }
        return userEntities.get(0);
    }

    public int createAccount(UserEntity userEntity) {
        return userRegisterLoginMapper.insert(userEntity);
    }
}
