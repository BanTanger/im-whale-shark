package com.bantanger.im.domain.group.dao.mapper;

import com.bantanger.im.domain.group.dao.ImGroupEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * @author bantanger 半糖
 */
@Repository
public interface ImGroupMapper extends BaseMapper<ImGroupEntity> {

    /**
     * 增量拉取用户被拉入群组通知列表中最大的序列号
     * @param data
     * @param appId
     * @return
     */
    Long getJoinGroupMaxSeq(@Param("groupIds") Collection<String> data, @Param("appId") Integer appId);

}
