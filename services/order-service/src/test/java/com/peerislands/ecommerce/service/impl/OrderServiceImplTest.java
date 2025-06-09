package com.peerislands.ecommerce.service.impl;

import com.peerislands.ecommerce.dto.OrderDTO;
import com.peerislands.ecommerce.dto.OrderItemDTO;
import com.peerislands.ecommerce.entity.Order;
import com.peerislands.ecommerce.entity.OrderItem;
import com.peerislands.ecommerce.exception.OrderNotFoundException;
import com.peerislands.ecommerce.exception.ValidationException;
import com.peerislands.ecommerce.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        // Set URLs via reflection
        var productField = OrderServiceImpl.class.getDeclaredField("productServiceUrl");
        productField.setAccessible(true);
        productField.set(orderService, "http://product-service");
        var cartField = OrderServiceImpl.class.getDeclaredField("cartServiceUrl");
        cartField.setAccessible(true);
        cartField.set(orderService, "http://cart-service");
    }

    private OrderDTO sampleOrderDTO() {
        OrderItemDTO item = OrderItemDTO.builder()
                .id(1L)
                .productId(101L)
                .quantity(2)
                .price(BigDecimal.valueOf(50))
                .build();
        return OrderDTO.builder()
                .id(1L)
                .userId("user1")
                .orderStatus(Order.OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(100))
                .orderItems(List.of(item))
                .build();
    }

    private Order sampleOrder() {
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(101L);
        item.setQuantity(2);
        item.setPrice(BigDecimal.valueOf(50));
        Order order = new Order();
        order.setId(1L);
        order.setUserId("user1");
        order.setOrderStatus(Order.OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmount(BigDecimal.valueOf(100));
        order.addOrderItem(item);
        return order;
    }

    @Test
    void testGetAllOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(sampleOrder()));
        assertEquals(1, orderService.getAllOrders().size());
    }

    @Test
    void testGetOrdersByStatus() {
        when(orderRepository.findByOrderStatus(Order.OrderStatus.PENDING)).thenReturn(List.of(sampleOrder()));
        assertEquals(1, orderService.getOrdersByStatus(Order.OrderStatus.PENDING).size());
    }

    @Test
    void testGetOrderById_Found() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder()));
        assertEquals(1L, orderService.getOrderById(1L).getId());
    }

    @Test
    void testGetOrderById_NotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(1L));
    }

    @Test
    void testCreateOrder_Success() {
        OrderDTO dto = sampleOrderDTO();
        when(restTemplate.getForObject(anyString(), eq(Integer.class))).thenReturn(10);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(restTemplate).delete(anyString());
        doReturn(null).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Object.class));
        OrderDTO result = orderService.createOrder(dto);
        assertEquals(Order.OrderStatus.PENDING, result.getOrderStatus());
    }

    @Test
    void testCreateOrder_StockUnavailable() {
        OrderDTO dto = sampleOrderDTO();
        when(restTemplate.getForObject(anyString(), eq(Integer.class))).thenReturn(1);
        assertThrows(ValidationException.class, () -> orderService.createOrder(dto));
    }

    @Test
    void testCreateOrder_StockCheckNull() {
        OrderDTO dto = sampleOrderDTO();
        when(restTemplate.getForObject(anyString(), eq(Integer.class))).thenReturn(null);
        assertThrows(ValidationException.class, () -> orderService.createOrder(dto));
    }

    @Test
    void testCreateOrder_FailedStockUpdate() {
        OrderDTO dto = sampleOrderDTO();
        when(restTemplate.getForObject(anyString(), eq(Integer.class))).thenReturn(10);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("fail")).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Object.class));
        assertThrows(ValidationException.class, () -> orderService.createOrder(dto));
    }

    @Test
    void testCreateOrder_InvalidTotalAmount() {
        OrderDTO dto = sampleOrderDTO();
        dto.setTotalAmount(BigDecimal.valueOf(99));
        assertThrows(ValidationException.class, () -> orderService.createOrder(dto));
    }

    @Test
    void testCreateOrder_EmptyItems() {
        OrderDTO dto = sampleOrderDTO();
        dto.setOrderItems(new ArrayList<>());
        assertThrows(ValidationException.class, () -> orderService.createOrder(dto));
    }

    @Test
    void testUpdateOrder_Found() {
        OrderDTO dto = sampleOrderDTO();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder()));
        when(restTemplate.getForObject(anyString(), eq(Integer.class))).thenReturn(10);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        OrderDTO result = orderService.updateOrder(1L, dto);
        assertEquals(1L, result.getId());
    }

    @Test
    void testUpdateOrder_NotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class, () -> orderService.updateOrder(1L, sampleOrderDTO()));
    }

    @Test
    void testDeleteOrder_Found() {
        when(orderRepository.existsById(1L)).thenReturn(true);
        doNothing().when(orderRepository).deleteById(1L);
        assertDoesNotThrow(() -> orderService.deleteOrder(1L));
    }

    @Test
    void testDeleteOrder_NotFound() {
        when(orderRepository.existsById(1L)).thenReturn(false);
        assertThrows(OrderNotFoundException.class, () -> orderService.deleteOrder(1L));
    }

    @Test
    void testGetOrdersByUserId() {
        when(orderRepository.findByUserId("user1")).thenReturn(List.of(sampleOrder()));
        assertEquals(1, orderService.getOrdersByUserId("user1").size());
    }

    @Test
    void testUpdateOrderStatus_Found() {
        Order order = sampleOrder();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        OrderDTO result = orderService.updateOrderStatus(1L, Order.OrderStatus.PROCESSING);
        assertEquals(Order.OrderStatus.PROCESSING, result.getOrderStatus());
    }

    @Test
    void testUpdateOrderStatus_NotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class, () -> orderService.updateOrderStatus(1L, Order.OrderStatus.PROCESSING));
    }

    @Test
    void testUpdateOrderStatus_CancelledNotPending() {
        Order order = sampleOrder();
        order.setOrderStatus(Order.OrderStatus.PROCESSING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        assertThrows(ValidationException.class, () -> orderService.updateOrderStatus(1L, Order.OrderStatus.CANCELLED));
    }

    @Test
    void testGetOrders_WithStatus() {
        when(orderRepository.findByOrderStatus(Order.OrderStatus.PENDING)).thenReturn(List.of(sampleOrder()));
        assertEquals(1, orderService.getOrders(Order.OrderStatus.PENDING).size());
    }

    @Test
    void testGetOrders_WithoutStatus() {
        when(orderRepository.findAll()).thenReturn(List.of(sampleOrder()));
        assertEquals(1, orderService.getOrders(null).size());
    }

    @Test
    void testUpdatePendingOrdersToProcessing() {
        Order order = sampleOrder();
        order.setOrderStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findByOrderStatus(Order.OrderStatus.PENDING)).thenReturn(List.of(order));
        when(orderRepository.saveAll(anyList())).thenReturn(List.of(order));
        assertDoesNotThrow(() -> orderService.updatePendingOrdersToProcessing());
    }
}
