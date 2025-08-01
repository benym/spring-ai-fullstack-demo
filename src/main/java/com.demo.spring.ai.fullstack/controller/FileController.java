package com.demo.spring.ai.fullstack.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.demo.spring.ai.fullstack.domain.DocumentDataResponse;
import com.demo.spring.ai.fullstack.domain.EsDocument;
import com.demo.spring.ai.fullstack.domain.enums.TransformerTypeEnum;
import com.demo.spring.ai.fullstack.etl.read.DocumentReaderStrategy;
import com.demo.spring.ai.fullstack.etl.read.ReaderFactory;
import com.demo.spring.ai.fullstack.etl.transformer.DocumentTransformerFactory;
import com.demo.spring.ai.fullstack.etl.transformer.DocumentTransformerStrategy;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

/**
 * 文件
 */
@RestController
@RequestMapping("/file")
public class FileController {

    private final ReaderFactory readerFactory;

    private final ElasticsearchVectorStore elasticsearchVectorStore;

    private final DocumentTransformerFactory documentTransformerFactory;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
    private String indexName;

    public FileController(@Qualifier("elasticsearchVectorStore") ElasticsearchVectorStore elasticsearchVectorStore,
                          ReaderFactory readerFactory, DocumentTransformerFactory documentTransformerFactory) {
        this.elasticsearchVectorStore = elasticsearchVectorStore;
        this.readerFactory = readerFactory;
        this.documentTransformerFactory = documentTransformerFactory;
    }

    @PostMapping("/import/documents")
    public RemoteResult<String> handleFileUpload(@RequestParam("files") MultipartFile file,
                                                 @RequestParam("sessionId") @NotBlank(message = "sessionId不能为空") String sessionId,
                                                 @RequestParam("userId") @NotBlank(message = "用户Id不能为空") String userId) {
        Assert.isTrue(!file.isEmpty(), "文件为空，请上传有效的文件");
        String originalFilename = file.getOriginalFilename();
        Assert.isTrue(originalFilename.contains("."), "无法识别文件类型，请提供带扩展名的文件");
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        DocumentReaderStrategy documentReaderStrategy = readerFactory.getStrategy(extension);
        List<Document> documents = documentReaderStrategy.read(file);
        DocumentTransformerStrategy documentTransformerStrategy = documentTransformerFactory.getStrategy(TransformerTypeEnum.TOKEN.getCode());
        List<Document> transformDocuments = documentTransformerStrategy.transform(documents);
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("sessionId", sessionId);
        metadataMap.put("userId", userId);
        metadataMap.put("fileName", originalFilename);
        metadataMap.put("fileType", extension);
        metadataMap.put("uploadTime", Instant.now().toString());
        metadataMap.put("fileSize", String.valueOf(file.getSize()));
        DocumentTransformerStrategy metaDataTransformer = documentTransformerFactory.getStrategy(TransformerTypeEnum.META_DATA.getCode());
        List<Document> metaDataDocuments = metaDataTransformer.transformWithMeta(transformDocuments, metadataMap);
        elasticsearchVectorStore.add(metaDataDocuments);
        return RemoteResult.success("上传成功");
    }

    @GetMapping("/search/document")
    public RemoteResult<List<DocumentDataResponse>> searchUserDocument(@RequestParam("userId") @NotBlank(message = "用户Id不能为空") String userId) throws IOException {
        Assert.isTrue(!userId.isBlank(), "用户Id不能为空");
        Optional<ElasticsearchClient> nativeClient = elasticsearchVectorStore.getNativeClient();
        if (nativeClient.isEmpty()) {
            throw ExceptionFactory.bizNoStackException("Elasticsearch客户端未初始化");
        }
        ElasticsearchClient elasticsearchClient = nativeClient.get();
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(indexName)
                .query(q -> q
                        .term(TermQuery.of(t -> t
                                .field("metadata.userId.keyword")
                                .value(FieldValue.of(userId))
                        ))
                )
                .size(100)
                .sort(so -> so
                        .field(f -> f
                                .field("metadata.uploadTime")
                                .order(SortOrder.Asc)
                        )
                )
        );
        SearchResponse<EsDocument> response = elasticsearchClient.search(searchRequest, EsDocument.class);
        // 使用文件名和文件类型组合作为key
        Map<String, DocumentDataResponse> uniqueDocs = new LinkedHashMap<>();
        for (Hit<EsDocument> hit : response.hits().hits()) {
            EsDocument source = hit.source();
            if (source != null) {
                Map<String, Object> metadata = source.getMetadata();
                String filename = (String) metadata.get("fileName");
                String fileType = (String) metadata.get("fileType");
                String docId = source.getId();
                String compositeKey = filename + "|" + fileType;
                // 如果已存在同名同类型文档，添加到现有列表中
                if (uniqueDocs.containsKey(compositeKey)) {
                    uniqueDocs.get(compositeKey).getDocId().add(docId);
                } else {
                    // 创建新条目，初始化docId列表
                    List<String> docIds = new ArrayList<>();
                    docIds.add(docId);
                    uniqueDocs.put(compositeKey, new DocumentDataResponse(docIds, filename, fileType));
                }
            }
        }
        return RemoteResult.success(new ArrayList<>(uniqueDocs.values()));
    }

    @PostMapping("/delete/document")
    public RemoteResult<String> deleteDocument(@RequestParam("docIdList") List<String> docIdList) throws IOException {
        Assert.isTrue(!CollectionUtils.isEmpty(docIdList), "文档Id不能为空");
        Optional<ElasticsearchClient> nativeClient = elasticsearchVectorStore.getNativeClient();
        if (nativeClient.isEmpty()) {
            throw ExceptionFactory.bizNoStackException("Elasticsearch客户端未初始化");
        }
        ElasticsearchClient elasticsearchClient = nativeClient.get();
        BulkRequest.Builder bulkRequest = new BulkRequest.Builder();
        for (String docId : docIdList) {
            bulkRequest.operations(op -> op
                    .delete(d -> d
                            .index(indexName)
                            .id(docId)
                    )
            );
        }
        // 执行批量删除
        BulkResponse response = elasticsearchClient.bulk(bulkRequest.build());
        if (response.errors()) {
            throw ExceptionFactory.bizNoStackException("文档删除失败");

        }
        return RemoteResult.success("文档删除成功");
    }
}
