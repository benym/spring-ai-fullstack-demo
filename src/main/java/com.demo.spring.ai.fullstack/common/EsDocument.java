package com.demo.spring.ai.fullstack.common;

import lombok.Data;

import java.util.Map;


@Data
public class EsDocument {

    private String id;

    private String content;

    private Map<String, Object> metadata;

    private float[] embedding;
}
