package io.spring.identityadmin.admin.iam.controller;

import io.spring.identityadmin.admin.iam.service.impl.DocumentService;
import io.spring.identityadmin.domain.entity.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/admin/docs") // 그룹 관리를 위한 공통 경로 설정
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public String createDocument(Document document) {
        documentService.createDocument(document);
        return "admin/document";
    }
}
