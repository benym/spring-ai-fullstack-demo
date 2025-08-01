package com.demo.spring.ai.fullstack.etl.read.impl;

import com.demo.spring.ai.fullstack.etl.read.DocumentReaderStrategy;
import com.demo.spring.ai.fullstack.domain.enums.FileTypeEnum;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @date 2025/6/29 15:09
 */
@Component
public class PdfStrategy implements DocumentReaderStrategy {
    @Override
    public List<Document> read(MultipartFile file) {
        try {
            Resource resource = new ByteArrayResource(file.getBytes());
            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
            return reader.get();
        } catch (Exception e) {
            throw ExceptionFactory.sysException(e);
        }
    }

    @Override
    public String fileType() {
        return FileTypeEnum.PDF.getCode();
    }
}
