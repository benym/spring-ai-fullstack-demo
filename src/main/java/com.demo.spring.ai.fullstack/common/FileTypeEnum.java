package com.demo.spring.ai.fullstack.common;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @date 2025/6/29 15:19
 */
public enum FileTypeEnum {

    HTML("html", "html"),

    JSON("json", "json"),

    MARKDOWN("md", "markdown"),

    PDF("pdf", "pdf"),

    TXT("txt", "txt"),

    DEFAULT("default", "default");

    FileTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private static final Map<String, FileTypeEnum> CODE_MAP = new ConcurrentHashMap<>();

    static {
        for (FileTypeEnum fileTypeEnum : EnumSet.allOf(FileTypeEnum.class)) {
            CODE_MAP.put(fileTypeEnum.getCode(), fileTypeEnum);
        }
    }

    public static FileTypeEnum getEnumsByCode(String code) {
        return CODE_MAP.get(code);
    }

    /**
     * code
     */
    private final String code;

    /**
     * desc
     */
    private final String desc;


    public String getCode() {
        return code;
    }


    public String getDesc() {
        return desc;
    }
}