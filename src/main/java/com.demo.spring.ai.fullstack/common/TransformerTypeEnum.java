package com.demo.spring.ai.fullstack.common;


import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @date 2025/6/29 16:07
 */
public enum TransformerTypeEnum{

    TOKEN("token", "token级切片"),

    SUMMARY("summary", "摘要填充"),

    KEYWORD("keyword", "关键词填充"),

    CONTENT_FORMAT("content", "内容统一格式化"),

    META_DATA("metaData", "元数据填充");

    private static final Map<String, TransformerTypeEnum> CODE_MAP = new ConcurrentHashMap<>();

    static {
        for (TransformerTypeEnum type : EnumSet.allOf(TransformerTypeEnum.class)) {
            CODE_MAP.put(type.getCode(), type);
        }
    }

    public static TransformerTypeEnum getEnumsByCode(String code) {
        return CODE_MAP.get(code);
    }

    private final String code;

    private final String desc;

    TransformerTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }


    public String getCode() {
        return code;
    }


    public String getDesc() {
        return desc;
    }
}