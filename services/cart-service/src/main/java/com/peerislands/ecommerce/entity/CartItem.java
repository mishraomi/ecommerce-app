package com.peerislands.ecommerce.entity;

import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Data
public class CartItem {

    private String productId;
    private String productName;
    private Integer quantity;
    private Double price;

    public void updateQuantity(int quantity) {
        if (quantity > 0) {
            this.quantity = quantity;
        } else {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }
}
