package com.nenestore.api.repository;

import com.nenestore.api.entity.CreditSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditSaleRepository extends JpaRepository<CreditSale, Long> {

    @Query("""
            SELECT cs FROM CreditSale cs
            LEFT JOIN FETCH cs.sale s
            LEFT JOIN FETCH s.client c
            WHERE cs.status != 'PAID'
            ORDER BY cs.balance DESC
            """)
    List<CreditSale> findAllOpen();
}