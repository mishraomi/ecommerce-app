package com.peerislands.ecommerce.service;

import com.peerislands.ecommerce.dto.CartDTO;
import com.peerislands.ecommerce.dto.CartItemDTO;
import com.peerislands.ecommerce.dto.StockUpdateDTO;
import com.peerislands.ecommerce.entity.Product;
import com.peerislands.ecommerce.exception.ResourceNotFoundException;
import com.peerislands.ecommerce.exception.ValidationException;
import com.peerislands.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RestTemplate restTemplate;

    @Value("${cart.service.url}")
    private String cartServiceUrl;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Transactional
    public Product createProduct(Product product) {
        validateProduct(product);
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Integer id, Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        validateProduct(productDetails);
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setAvailableStock(productDetails.getAvailableStock());
        
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Integer id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public Product updateStock(Integer id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        int newStock = product.getAvailableStock() + quantity;
        if (newStock < 0) {
            throw new ValidationException("Insufficient stock for product: " + id);
        }
        
        product.setAvailableStock(newStock);
        return productRepository.save(product);
    }

    @Transactional
    public void addProductToCart(String userId, Integer productId, Integer quantity) {
        Product product = getProductById(productId);
        
        // Validate stock availability
        if (product.getAvailableStock() < quantity) {
            throw new ValidationException("Insufficient stock available for product: " + productId);
        }

        // Create cart item DTO
        CartItemDTO cartItem = CartItemDTO.builder()
                .productId(productId.toString())
                .quantity(quantity)
                .price(product.getPrice().doubleValue())
                .productName(product.getName())
                .build();

        // Call cart service to add item
        String url = cartServiceUrl + "/api/v1/carts/" + userId + "/items";
        restTemplate.postForObject(url, cartItem, CartDTO.class);
    }

    private void validateProduct(Product product) {
        if (product.getPrice() != null && product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Price must be greater than zero");
        }
        if (product.getAvailableStock() != null && product.getAvailableStock() < 0) {
            throw new ValidationException("Available stock cannot be negative");
        }
    }

    public int getStockQuantity(Integer id) {
        Product product = getProductById(id);
        return product.getAvailableStock();
    }

    @Transactional
    public Product updateProductStock(Integer productId, StockUpdateDTO stockUpdateDTO) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        if (stockUpdateDTO.getQuantity() == null || stockUpdateDTO.getQuantity() < 0) {
            throw new ValidationException("Invalid stock quantity provided");
        }

        if (stockUpdateDTO.getOperation().equals("DECREASE")){
            if (product.getAvailableStock() < stockUpdateDTO.getQuantity()) {
                throw new ValidationException("Insufficient stock to decrease by " + stockUpdateDTO.getQuantity());
            }
            int newStock = product.getAvailableStock() - stockUpdateDTO.getQuantity();
            product.setAvailableStock(newStock);
        }
        else if (!stockUpdateDTO.getOperation().equals("INCREASE")) {
            throw new ValidationException("Invalid operation: " + stockUpdateDTO.getOperation());
        }
        else {
            int newStock = product.getAvailableStock() + stockUpdateDTO.getQuantity();
            product.setAvailableStock(newStock);
        }
        return productRepository.save(product);
    }
}
