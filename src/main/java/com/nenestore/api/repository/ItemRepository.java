package com.nenestore.api.repository;

import com.nenestore.api.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    @Query("SELECT MAX(CAST(SUBSTRING(i.sku, LENGTH(i.order.orderId) + 2) AS int)) " +
           "FROM Item i WHERE i.order.orderId = :orderId")
    Integer findMaxUnitIndexByOrderId(@Param("orderId") String orderId);
}