package com.nenestore.api.repository;

import com.nenestore.api.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.creditSale.id = :creditSaleId")
    BigDecimal sumPaymentsByCreditSaleId(@Param("creditSaleId") Long creditSaleId);
}