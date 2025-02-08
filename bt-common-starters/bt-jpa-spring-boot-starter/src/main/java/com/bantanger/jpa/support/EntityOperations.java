package com.bantanger.jpa.support;

import org.springframework.data.repository.CrudRepository;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
@SuppressWarnings("unchecked")
public abstract class EntityOperations {

    public static <T, ID> EntityUpdater<T, ID> doUpdate(CrudRepository<T, ID> repository) {
        return new EntityUpdater<>(repository);
    }

    public static <T, ID> EntityCreator<T, ID> doCreate(CrudRepository<T, ID> repository) {
        return new EntityCreator<>(repository);
    }


}
