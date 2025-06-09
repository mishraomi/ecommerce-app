package com.peerislands.ecommerce.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Document(collection = "cart")
public class Cart {

    @Id
    @Generated
    private String id;

    @Indexed(unique = true)
    private String userId; // Assuming a userId to associate the cart with a user
    private List<CartItem> cartItems;
}
