package com.example.clientsapi.client.repository;

import com.example.clientsapi.client.domain.Document;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByNumber(String number);

    Optional<Document> findByClientIdAndId(Long clientId, Long documentId);
}
