package com.example.clientsapi.client.repository;

import com.example.clientsapi.client.domain.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);

    Optional<Client> findByTaxIdentifier(String taxIdentifier);
}
