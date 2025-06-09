package com.peerislands.ecommerce.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {
    private String id;
    private String userId;
    private List<CartItemDTO> cartItems;
    private Double totalAmount;
} 