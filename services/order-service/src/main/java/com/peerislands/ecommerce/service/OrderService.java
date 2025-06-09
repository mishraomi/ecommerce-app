package com.peerislands.ecommerce.service;

import com.peerislands.ecommerce.dto.OrderDTO;
import com.peerislands.ecommerce.entity.Order;

import java.util.List;

public interface OrderService {
    List<OrderDTO> getAllOrders();
    OrderDTO getOrderById(Long id);
    OrderDTO createOrder(OrderDTO orderDTO);
    OrderDTO updateOrder(Long id, OrderDTO orderDTO);
    void deleteOrder(Long id);
    List<OrderDTO> getOrdersByUserId(String userId);
    OrderDTO updateOrderStatus(Long id, Order.OrderStatus status);
    List<OrderDTO> getOrdersByStatus(Order.OrderStatus status);
    List<OrderDTO> getOrders(Order.OrderStatus status);
}
