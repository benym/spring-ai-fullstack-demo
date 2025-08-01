package com.demo.spring.ai.fullstack.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;


@Data
@AllArgsConstructor
public class DocumentDataResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = -8629965827056437727L;

    private List<String> docId;

    private String fileName;

    private String fileType;
}
