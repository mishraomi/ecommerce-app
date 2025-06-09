package com.peerislands.ecommerce.service.impl;

import com.peerislands.ecommerce.dto.OrderDTO;
import com.peerislands.ecommerce.dto.OrderItemDTO;
import com.peerislands.ecommerce.dto.StockUpdateDTO;
import com.peerislands.ecommerce.entity.Order;
import com.peerislands.ecommerce.entity.OrderItem;
import com.peerislands.ecommerce.exception.OrderNotFoundException;
import com.peerislands.ecommerce.exception.ValidationException;
import com.peerislands.ecommerce.repository.OrderRepository;
import com.peerislands.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${cart.service.url}")
    private String cartServiceUrl;

    @Override
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getOrdersByStatus(Order.OrderStatus status){
        return orderRepository.findByOrderStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderDTO getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
    }

    @Override
    @Transactional
    public OrderDTO createOrder(OrderDTO orderDTO) {
        validateOrder(orderDTO);
        validateStockAvailability(orderDTO.getOrderItems());
        
        Order order = convertToEntity(orderDTO);
        order.setOrderStatus(Order.OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);

        try {
            // Update product stock
            updateProductStock(orderDTO.getOrderItems());
            
            // Clear user's cart
            clearUserCart(orderDTO.getUserId());
            savedOrder = orderRepository.save(savedOrder);
        } catch (Exception e) {
            // If stock update or cart clearing fails, mark order as FAILED
            savedOrder.setOrderStatus(Order.OrderStatus.FAILED);
            orderRepository.save(savedOrder);
            throw new ValidationException("Failed to process order: " + e.getMessage());
        }

        return convertToDTO(savedOrder);
    }

    @Override
    @Transactional
    public OrderDTO updateOrder(Long id, OrderDTO orderDTO) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));

        validateOrder(orderDTO);
        validateStockAvailability(orderDTO.getOrderItems());

        Order order = convertToEntity(orderDTO);
        order.setId(id);
        return convertToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException("Order not found with id: " + id);
        }
        orderRepository.deleteById(id);
    }

    @Override
    public List<OrderDTO> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long id, Order.OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        if (status.equals(Order.OrderStatus.CANCELLED) && !(order.getOrderStatus().equals(Order.OrderStatus.PENDING))) {
            throw new ValidationException("Order status cannot be changed to CANCELLED unless it is in PENDING state");
        }
        order.setOrderStatus(status);
        return convertToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getOrders(Order.OrderStatus status) {
        log.info("Fetching orders with status filter: {}", status);
        
        List<Order> orders;
        if (status != null) {
            orders = orderRepository.findByOrderStatus(status);
            log.info("Found {} orders with status: {}", orders.size(), status);
        } else {
            orders = orderRepository.findAll();
            log.info("Found {} total orders", orders.size());
        }

        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private void validateOrder(OrderDTO orderDTO) {
        if (orderDTO.getOrderItems() == null || orderDTO.getOrderItems().isEmpty()) {
            throw new ValidationException("Order must contain at least one item");
        }

        BigDecimal totalAmount = orderDTO.getOrderItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (orderDTO.getTotalAmount().subtract(totalAmount).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new ValidationException("Total amount does not match the sum of items");
        }
    }

    private void validateStockAvailability(List<OrderItemDTO> orderItems) {
        for (OrderItemDTO item : orderItems) {
            String url = productServiceUrl + "/api/products/" + item.getProductId() + "/stock";
            Integer response = restTemplate.getForObject(url, Integer.class);
            
            if (response == null) {
                throw new ValidationException("Failed to check stock for product: " + item.getProductId());
            }

            if (response == 0) {
                throw new ValidationException("Product " + item.getProductId() + " is out of stock");
            }

            if (response < item.getQuantity()) {
                throw new ValidationException("Insufficient stock for product " + item.getProductId() + 
                    ". Available: " + response + ", Requested: " + item.getQuantity());
            }
        }
    }

    private void updateProductStock(List<OrderItemDTO> orderItems) {
        for (OrderItemDTO item : orderItems) {
            String url = productServiceUrl + "/api/products/" + item.getProductId() + "/stock/update";
            StockUpdateDTO stockUpdateDTO = StockUpdateDTO.builder()
                .quantity(item.getQuantity())
                .operation("DECREASE")
                .build();
            
            try {
                restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(stockUpdateDTO),
                        Object.class
                );
            } catch (Exception e) {
                throw new ValidationException("Failed to update stock for product " + item.getProductId() + ": " + e.getMessage());
            }
        }
    }

    private void clearUserCart(String userId) {
        String url = cartServiceUrl + "/api/carts/" + userId;
        try {
            restTemplate.delete(url);
        } catch (Exception e) {
            throw new ValidationException("Failed to clear cart for user " + userId + ": " + e.getMessage());
        }
    }

    private OrderDTO convertToDTO(Order order) {
        List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return OrderDTO.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .orderItems(orderItemDTOs)
                .build();
    }

    private OrderItemDTO convertToDTO(OrderItem orderItem) {
        return OrderItemDTO.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProductId())
                .quantity(orderItem.getQuantity())
                .price(orderItem.getPrice())
                .build();
    }

    private Order convertToEntity(OrderDTO orderDTO) {
        Order order = new Order();
        order.setUserId(orderDTO.getUserId());
        order.setOrderStatus(orderDTO.getOrderStatus());
        order.setOrderDate(orderDTO.getOrderDate());
        order.setTotalAmount(orderDTO.getTotalAmount());

        List<OrderItem> orderItems = orderDTO.getOrderItems().stream()
                .map(this::convertToEntity)
                .toList();

        orderItems.forEach(order::addOrderItem);
        return order;
    }

    private OrderItem convertToEntity(OrderItemDTO orderItemDTO) {
        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(orderItemDTO.getProductId());
        orderItem.setQuantity(orderItemDTO.getQuantity());
        orderItem.setPrice(orderItemDTO.getPrice());
        return orderItem;
    }

    @Scheduled(fixedRate = 300000) // every 5 minutes
    @Transactional
    public void updatePendingOrdersToProcessing() {
        List<Order> pendingOrders = orderRepository.findByOrderStatus(Order.OrderStatus.PENDING);
        for (Order order : pendingOrders) {
            order.setOrderStatus(Order.OrderStatus.PROCESSING);
        }
        orderRepository.saveAll(pendingOrders);
    }
} 