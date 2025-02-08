package com.bantanger.jpa.support;

import java.util.function.Consumer;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
public interface UpdateHandler<T> {

    Executor<T> update(Consumer<T> consumer);

}
