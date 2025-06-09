package com.peerislands.ecommerce.controller;

import com.peerislands.ecommerce.dto.CartDTO;
import com.peerislands.ecommerce.dto.CartItemDTO;
import com.peerislands.ecommerce.dto.OrderDTO;
import com.peerislands.ecommerce.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{userId}")
    public ResponseEntity<CartDTO> getCart(@PathVariable @NotBlank String userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<CartDTO> addItemToCart(@PathVariable @NotBlank String userId, @RequestBody @Valid CartItemDTO cartItem) {
        return ResponseEntity.ok(cartService.addItemToCart(userId, cartItem));
    }

    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<CartDTO> updateCartItem(
            @PathVariable @NotBlank String userId,
            @PathVariable @NotBlank String productId,
            @RequestParam @Positive Integer quantity) {
        return ResponseEntity.ok(cartService.updateCartItem(userId, productId, quantity));
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<CartDTO> removeItemFromCart(
            @PathVariable @NotBlank String userId,
            @PathVariable @NotBlank String productId) {
        return ResponseEntity.ok(cartService.removeItemFromCart(userId, productId));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> clearCart(@PathVariable @NotBlank String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/checkout")
    public ResponseEntity<OrderDTO> checkout(@PathVariable @NotBlank String userId) {
        // This method would typically handle the checkout process, such as creating an order.
        // For now, we will just return the cart.
        return ResponseEntity.ok(cartService.checkoutCart(userId));
    }
}
