package com.project.document_management.repository;

import com.project.document_management.model.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("SELECT d FROM Document d " +
            "WHERE LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR CAST(d.ocrText AS text) LIKE CONCAT('%', :query, '%')")
    List<Document> findByTitleContainingIgnoreCaseOrOcrTextContaining(
            @Param("query") String query,
            Sort sort);

}