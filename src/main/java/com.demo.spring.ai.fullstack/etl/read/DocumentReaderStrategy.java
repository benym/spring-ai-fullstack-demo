package com.demo.spring.ai.fullstack.etl.read;

import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentReaderStrategy {

    List<Document> read(MultipartFile file);

    String fileType();
}
