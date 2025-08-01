package com.demo.spring.ai.fullstack.etl.read.impl;

import com.demo.spring.ai.fullstack.common.FileTypeEnum;
import com.demo.spring.ai.fullstack.etl.read.DocumentReaderStrategy;
import com.rpamis.exception.dto.ExceptionFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class HtmlStrategy implements DocumentReaderStrategy {
    @Override
    public List<Document> read(MultipartFile file) {
        try {
            Resource resource = new ByteArrayResource(file.getBytes());
            JsoupDocumentReader reader = new JsoupDocumentReader(resource);
            return reader.get();
        } catch (Exception e) {
            throw ExceptionFactory.sysException(e);
        }
    }

    @Override
    public String fileType() {
        return FileTypeEnum.HTML.getCode();
    }
}
