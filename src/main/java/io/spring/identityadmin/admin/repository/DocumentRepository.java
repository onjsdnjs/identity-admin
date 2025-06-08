package io.spring.identityadmin.admin.repository;

import io.spring.identityadmin.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> { }