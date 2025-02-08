package com.bantanger.common.model;

import java.util.List;
import lombok.Data;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
@SuppressWarnings("all")
@Data
public class PageResult<T> {

    public PageResult() {
    }

    private Long total;
    private Integer totalPages;
    private Integer pageSize;
    private Integer pageNumber;
    private List<T> list;

    public PageResult(List<T> list, Long total, Integer pageSize, Integer pageNumber) {
        this.list = list;
        this.total = total;
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
    }

    public static <T> PageResult of(List<T> list, Long total, Integer pageSize,
        Integer pageNumber) {
        return new PageResult(list, total, pageSize, pageNumber);
    }
}