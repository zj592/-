package com.campus.lostfound.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 枚举字典：前后端统一的中文文案，避免小程序端各处硬编码
 */
public final class Labels {

    private Labels() {
    }

    /** 物品分类 */
    private static final Map<String, String> CATEGORY = new LinkedHashMap<>();
    /** 信息类型 */
    private static final Map<String, String> TYPE = new LinkedHashMap<>();
    /** 信息审核状态 */
    private static final Map<String, String> ITEM_STATUS = new LinkedHashMap<>();
    /** 认领申请状态 */
    private static final Map<String, String> CLAIM_STATUS = new LinkedHashMap<>();

    static {
        CATEGORY.put("card", "证件卡类");
        CATEGORY.put("digital", "数码产品");
        CATEGORY.put("device", "生活用品");
        CATEGORY.put("key", "钥匙门禁");
        CATEGORY.put("book", "书籍资料");
        CATEGORY.put("other", "其他");

        TYPE.put("LOST", "寻物启事");
        TYPE.put("FOUND", "失物招领");

        ITEM_STATUS.put("PENDING", "待审核");
        ITEM_STATUS.put("APPROVED", "已通过");
        ITEM_STATUS.put("REJECTED", "已驳回");
        ITEM_STATUS.put("FINISHED", "已完成");

        CLAIM_STATUS.put("PENDING", "待处理");
        CLAIM_STATUS.put("APPROVED", "已同意");
        CLAIM_STATUS.put("REJECTED", "已驳回");
        CLAIM_STATUS.put("CANCELED", "已撤销");
    }

    public static String category(String code) {
        return CATEGORY.getOrDefault(code, "其他");
    }

    public static String type(String code) {
        return TYPE.getOrDefault(code, "未知");
    }

    public static String itemStatus(String code) {
        return ITEM_STATUS.getOrDefault(code, "未知");
    }

    public static String claimStatus(String code) {
        return CLAIM_STATUS.getOrDefault(code, "未知");
    }

    /**
     * 供小程序端拉取的字典数据
     */
    public static Map<String, Object> all() {
        Map<String, Object> map = new LinkedHashMap<>();
        List<Map<String, String>> categories = new ArrayList<>();
        CATEGORY.forEach((k, v) -> categories.add(entry(k, v)));
        List<Map<String, String>> types = new ArrayList<>();
        TYPE.forEach((k, v) -> types.add(entry(k, v)));
        map.put("categories", categories);
        map.put("types", types);
        return map;
    }

    private static Map<String, String> entry(String value, String label) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("value", value);
        item.put("label", label);
        return item;
    }
}
