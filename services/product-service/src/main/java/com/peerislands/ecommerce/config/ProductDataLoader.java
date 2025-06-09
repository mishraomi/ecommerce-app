package com.peerislands.ecommerce.config;

import com.github.javafaker.Faker;
import com.peerislands.ecommerce.entity.Product;
import com.peerislands.ecommerce.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Random;

@Configuration
public class ProductDataLoader {
    @Bean
    public CommandLineRunner loadProducts(ProductRepository productRepository) {
        return args -> {
            Faker faker = new Faker();
            Random random = new Random();

            for (int i = 0; i < 100; i++) {
                Product product = Product.builder()
                        .name(faker.commerce().productName())
                        .description(faker.lorem().sentence())
                        .price(BigDecimal.valueOf(10 + (1000 - 10) * random.nextDouble()))
                        .availableStock(random.nextInt(1000))
                        .build();
                productRepository.save(product);
            }
        };
    }
}
