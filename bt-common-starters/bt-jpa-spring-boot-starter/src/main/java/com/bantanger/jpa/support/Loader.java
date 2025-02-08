package com.bantanger.jpa.support;

import java.util.function.Supplier;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
public interface Loader<T, ID> {

    UpdateHandler<T> loadById(ID id);

    UpdateHandler<T> load(Supplier<T> t);

}
