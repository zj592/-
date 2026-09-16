package com.campus.lostfound.vo;

import java.util.List;

/**
 * 统一分页返回结构
 */
public record PageVO<T>(List<T> records, long total, long current, long size, long pages) {

    public static <T> PageVO<T> of(List<T> records, long total, long current, long size) {
        long pages = size <= 0 ? 0 : (total + size - 1) / size;
        return new PageVO<>(records, total, current, size, pages);
    }
}
