package com.peerislands.ecommerce.service;

import com.peerislands.ecommerce.dto.CartDTO;
import com.peerislands.ecommerce.dto.CartItemDTO;
import com.peerislands.ecommerce.dto.OrderDTO;
import jakarta.validation.constraints.NotBlank;

public interface CartService {
    CartDTO getCart(String userId);
    CartDTO addItemToCart(String userId, CartItemDTO cartItem);
    CartDTO updateCartItem(String userId, String productId, Integer quantity);
    CartDTO removeItemFromCart(String userId, String productId);
    void clearCart(String userId);

    OrderDTO checkoutCart(@NotBlank String userId);
}
