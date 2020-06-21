package com.ly.learn.esdemo.util;

import lombok.Data;

import java.util.List;

@Data
public class Page<T> {
    private Integer pageNum;

    private Integer pageSize;

    private Long totalRecord;

    private List<T> Results;

}
