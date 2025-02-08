package com.bantanger.jpa.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>,
    JpaSpecificationExecutor<T>, QuerydslPredicateExecutor<T> {

}