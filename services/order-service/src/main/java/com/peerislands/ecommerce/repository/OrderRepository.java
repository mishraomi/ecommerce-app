package com.peerislands.ecommerce.repository;

import com.peerislands.ecommerce.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(String userId);
    List<Order> findByOrderStatus(Order.OrderStatus status);
}
