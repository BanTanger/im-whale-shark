package com.bantanger.jpa.support;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
public interface Executor<T> {

    Optional<T> execute();

    Executor<T> successHook(Consumer<T> consumer);

    Executor<T> errorHook(Consumer<? super Throwable> consumer);

}
