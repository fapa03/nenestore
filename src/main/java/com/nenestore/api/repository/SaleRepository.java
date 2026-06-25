package com.nenestore.api.repository;

import com.nenestore.api.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("""
            SELECT s FROM Sale s
            LEFT JOIN FETCH s.client c
            LEFT JOIN FETCH s.saleItems si
            LEFT JOIN FETCH si.item i
            ORDER BY s.saleDate DESC
            """)
    List<Sale> findAllWithDetails();

    @Query("""
            SELECT s FROM Sale s
            LEFT JOIN FETCH s.client c
            LEFT JOIN FETCH s.saleItems si
            LEFT JOIN FETCH si.item i
            WHERE s.client.id = :clientId
            ORDER BY s.saleDate DESC
            """)
    List<Sale> findByClientId(@Param("clientId") Long clientId);
}