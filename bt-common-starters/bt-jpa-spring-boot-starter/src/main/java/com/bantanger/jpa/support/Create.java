package com.bantanger.jpa.support;

import java.util.function.Supplier;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
public interface Create<T> {

    UpdateHandler<T> create(Supplier<T> supplier);

}
