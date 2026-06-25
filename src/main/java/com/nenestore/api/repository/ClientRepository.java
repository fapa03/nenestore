package com.nenestore.api.repository;

import com.nenestore.api.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    @Query("""
            SELECT c FROM Client c
            WHERE (COALESCE(:search, '') = '' OR
                   LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(c.whatsapp) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(c.instagram) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY c.name ASC
            """)
    List<Client> searchClients(@Param("search") String search);
}