package com.nenestore.api.repository;

import com.nenestore.api.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

        @Query("SELECT MAX(CAST(SUBSTRING(i.sku, LENGTH(i.order.orderId) + 2) AS int)) " +
                        "FROM Item i WHERE i.order.orderId = :orderId")
        Integer findMaxUnitIndexByOrderId(@Param("orderId") String orderId);

        @Query("""
                        SELECT i FROM Item i
                        JOIN FETCH i.order o
                        WHERE i.deletedAt IS NULL
                        AND (COALESCE(:search, '') = '' OR
                             LOWER(i.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR
                             LOWER(i.product) LIKE LOWER(CONCAT('%', :search, '%')) OR
                             LOWER(o.orderId) LIKE LOWER(CONCAT('%', :search, '%')))
                        AND (COALESCE(:status, '') = '' OR i.status = :status)
                        AND (COALESCE(:gender, '') = '' OR i.gender = :gender)
                        AND (COALESCE(:size, '') = '' OR i.size = :size)
                        ORDER BY i.createdAt DESC
                        """)

        List<Item> searchInventory(
                        @Param("search") String search,
                        @Param("status") String status,
                        @Param("gender") String gender,
                        @Param("size") String size);

        @Query("SELECT COUNT(i) FROM Item i WHERE i.deletedAt IS NULL AND i.status = 'Stock'")
        Long countStock();

        @Query("SELECT COUNT(i) FROM Item i WHERE i.deletedAt IS NULL AND i.status = 'Unavailable'")
        Long countSold();

        @Query("SELECT SUM(i.purchasePriceUsd) FROM Item i WHERE i.deletedAt IS NULL")
        BigDecimal sumPurchaseValue();

}