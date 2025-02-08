package com.bantanger.common.model;

import java.util.Map;
import lombok.Data;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
@Data
public class PageRequestWrapper<T> {

    private T bean;
    private Integer pageSize;
    private Integer page;
    private Map<String, String> sorts;
}
