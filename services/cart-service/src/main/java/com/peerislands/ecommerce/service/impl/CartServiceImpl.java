package com.peerislands.ecommerce.service.impl;

import com.peerislands.ecommerce.dto.*;
import com.peerislands.ecommerce.entity.Cart;
import com.peerislands.ecommerce.entity.CartItem;
import com.peerislands.ecommerce.exception.CartNotFoundException;
import com.peerislands.ecommerce.exception.ValidationException;
import com.peerislands.ecommerce.repository.CartRepository;
import com.peerislands.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${order.service.url}")
    private String orderServiceUrl;

    @Override
    public CartDTO getCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user: " + userId));
        return convertToDTO(cart);
    }

    @Override
    public CartDTO addItemToCart(String userId, CartItemDTO cartItemDTO) {
        // Check product stock availability
        checkProductStock(cartItemDTO.getProductId(), cartItemDTO.getQuantity());

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> Cart.builder()
                        .userId(userId)
                        .cartItems(new ArrayList<>())
                        .build());

        // Check if item already exists in cart
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(cartItemDTO.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // Check if total quantity (existing + new) is available
            int totalQuantity = existingItem.get().getQuantity() + cartItemDTO.getQuantity();
            checkProductStock(cartItemDTO.getProductId(), totalQuantity);
            
            // Update quantity if item exists
            existingItem.get().updateQuantity(totalQuantity);
        } else {
            // Add new item if it doesn't exist
            CartItem cartItem = convertToEntity(cartItemDTO);
            cart.getCartItems().add(cartItem);
        }
        
        Cart savedCart = cartRepository.save(cart);
        return convertToDTO(savedCart);
    }

    @Override
    public CartDTO updateCartItem(String userId, String productId, Integer quantity) {
        // Check product stock availability
        checkProductStock(productId, quantity);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user: " + userId));

        cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresent(item -> item.updateQuantity(quantity));

        Cart savedCart = cartRepository.save(cart);
        return convertToDTO(savedCart);
    }

    @Override
    public CartDTO removeItemFromCart(String userId, String productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user: " + userId));

        cart.getCartItems().removeIf(item -> item.getProductId().equals(productId));
        
        Cart savedCart = cartRepository.save(cart);
        return convertToDTO(savedCart);
    }

    @Override
    public void clearCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user: " + userId));
        
        cart.getCartItems().clear();
        cartRepository.save(cart);
    }

    @Override
    public OrderDTO checkoutCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user: " + userId));
        CartDTO cartDTO = convertToDTO(cart);
        String orderUrl = orderServiceUrl + "/api/orders";
        OrderDTO orderDTO = OrderDTO.builder()
                .userId(userId)
                .orderStatus(OrderDTO.OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .totalAmount(BigDecimal.valueOf(cartDTO.getTotalAmount()))
                .orderItems(cartDTO.getCartItems().stream()
                        .map(item -> OrderItemDTO.builder()
                                .productId(Long.valueOf(item.getProductId()))
                                .quantity(item.getQuantity())
                                .price(BigDecimal.valueOf(item.getPrice()))
                                .productName(item.getProductName())
                                .build())
                        .toList())
                .build();
        // Send order to order service
        OrderDTO orderDto = restTemplate.postForObject(orderUrl, orderDTO, OrderDTO.class);
        if (orderDto == null) {
            throw new ValidationException("Failed to create order");
        }
        return orderDto;
    }

    private CartDTO convertToDTO(Cart cart) {
        List<CartItemDTO> cartItemDTOs = cart.getCartItems().stream()
                .map(this::convertToDTO)
                .toList();

        double totalAmount = cartItemDTOs.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        return CartDTO.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .cartItems(cartItemDTOs)
                .totalAmount(totalAmount)
                .build();
    }

    private CartItemDTO convertToDTO(CartItem cartItem) {
        return CartItemDTO.builder()
                .productId(cartItem.getProductId())
                .quantity(cartItem.getQuantity())
                .price(cartItem.getPrice())
                .productName(cartItem.getProductName())
                .build();
    }

    private CartItem convertToEntity(CartItemDTO cartItemDTO) {
        return CartItem.builder()
                .productId(cartItemDTO.getProductId())
                .productName(cartItemDTO.getProductName())
                .quantity(cartItemDTO.getQuantity())
                .price(cartItemDTO.getPrice())
                .build();
    }

    private void checkProductStock(String productId, Integer quantity) {
        String url = productServiceUrl + "/api/products/" + productId + "/stock";
        Integer availableStock = restTemplate.getForObject(url, Integer.class);
        
        if (availableStock == null || availableStock < quantity) {
            throw new ValidationException("Insufficient stock available for product: " + productId);
        }
    }
} 