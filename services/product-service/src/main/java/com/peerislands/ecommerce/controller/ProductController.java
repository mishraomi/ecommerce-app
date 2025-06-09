package com.peerislands.ecommerce.controller;

import com.peerislands.ecommerce.dto.StockUpdateDTO;
import com.peerislands.ecommerce.entity.Product;
import com.peerislands.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        return new ResponseEntity<>(productService.createProduct(product), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Integer id,
            @Valid @RequestBody Product product) {
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<Integer> getStockQuantity(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getStockQuantity(id));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> updateStock(
            @PathVariable Integer id,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(productService.updateStock(id, quantity));
    }

    @PostMapping("/{productId}/stock/update")
    public ResponseEntity<Product> updateProductStock(
            @PathVariable @Positive Integer productId,
            @RequestBody StockUpdateDTO stockUpdateDTO) {
        return ResponseEntity.ok(productService.updateProductStock(productId, stockUpdateDTO));
    }

    @PostMapping("/{productId}/cart")
    public ResponseEntity<Void> addProductToCart(
            @PathVariable @Positive Integer productId,
            @RequestParam @NotBlank String userId,
            @RequestParam @Positive Integer quantity) {
        productService.addProductToCart(userId, productId, quantity);
        return ResponseEntity.ok().build();
    }
}
