package com.peerislands.ecommerce.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private String productId;
    private Integer quantity;
    private Double price;
    private String productName;
} 