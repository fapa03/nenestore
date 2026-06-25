package com.nenestore.api.repository;

import com.nenestore.api.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderId(String orderId);

    @Query("SELECT o FROM Order o WHERE o.deletedAt IS NULL ORDER BY o.orderDate DESC")
    List<Order> findLastOrders(org.springframework.data.domain.Pageable pageable);

}