package com.peerislands.ecommerce.service;

import com.peerislands.ecommerce.dto.CartDTO;
import com.peerislands.ecommerce.dto.CartItemDTO;
import com.peerislands.ecommerce.entity.Cart;
import com.peerislands.ecommerce.entity.CartItem;
import com.peerislands.ecommerce.exception.CartNotFoundException;
import com.peerislands.ecommerce.repository.CartRepository;
import com.peerislands.ecommerce.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart cart;
    private CartDTO cartDTO;
    private CartItem cartItem;
    private CartItemDTO cartItemDTO;

    @BeforeEach
    void setUp() {
        // Setup cart item
        cartItem = new CartItem();
        cartItem.setProductId("PROD-001");
        cartItem.setQuantity(2);
        cartItem.setPrice(99.99);

        // Setup cart
        cart = new Cart("CART-001", "user123", new ArrayList<>());
        cart.getCartItems().add(cartItem);

        // Setup cart item DTO
        cartItemDTO = new CartItemDTO();
        cartItemDTO.setProductId("PROD-001");
        cartItemDTO.setQuantity(2);
        cartItemDTO.setPrice(99.99);

        // Setup cart DTO
        cartDTO = new CartDTO("CART-001", "user123", new ArrayList<>(), 199.98);
        cartDTO.getCartItems().add(cartItemDTO);
    }

    @Test
    void getCart_WhenCartExists_ShouldReturnCart() {
        // Arrange
        when(cartRepository.findByUserId("user123")).thenReturn(Optional.of(cart));

        // Act
        CartDTO result = cartService.getCart("user123");

        // Assert
        assertNotNull(result);
        assertEquals("user123", result.getUserId());
        assertEquals(1, result.getCartItems().size());
        verify(cartRepository).findByUserId("user123");
    }

    @Test
    void removeItemFromCart_WhenItemExists_ShouldRemoveItem() {
        // Arrange
        when(cartRepository.findByUserId("user123")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        CartDTO result = cartService.removeItemFromCart("user123", "PROD-001");

        // Assert
        assertNotNull(result);
        assertTrue(result.getCartItems().isEmpty());
        verify(cartRepository).findByUserId("user123");
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void clearCart_WhenCartExists_ShouldClearCart() {
        // Arrange
        when(cartRepository.findByUserId("user123")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        cartService.clearCart("user123");
        CartDTO result = cartService.getCart("user123");

        // Assert
        assertNotNull(result);
        assertTrue(result.getCartItems().isEmpty());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void clearCart_WhenCartDoesNotExist_ShouldThrowException() {
        // Arrange
        when(cartRepository.findByUserId("user123")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CartNotFoundException.class, () -> 
            cartService.clearCart("user123")
        );
        verify(cartRepository).findByUserId("user123");
        verify(cartRepository, never()).save(any(Cart.class));
    }
} 