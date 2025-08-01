package com.demo.spring.ai.fullstack.etl.read.impl;

import com.demo.spring.ai.fullstack.etl.read.DocumentReaderStrategy;
import com.demo.spring.ai.fullstack.domain.enums.FileTypeEnum;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class TikaStrategy implements DocumentReaderStrategy {

    @Override
    public List<Document> read(MultipartFile file) {
        try {
            Resource resource = new ByteArrayResource(file.getBytes());
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            return reader.get();
        } catch (Exception e) {
            throw ExceptionFactory.sysException(e);
        }
    }

    @Override
    public String fileType() {
        return FileTypeEnum.DEFAULT.getCode();
    }
}
