package io.spring.identityadmin.repository;

import io.spring.identityadmin.domain.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> { }