package com.peerislands.ecommerce.service;

import com.peerislands.ecommerce.dto.CartDTO;
import com.peerislands.ecommerce.dto.CartItemDTO;
import com.peerislands.ecommerce.dto.StockUpdateDTO;
import com.peerislands.ecommerce.entity.Product;
import com.peerislands.ecommerce.exception.ResourceNotFoundException;
import com.peerislands.ecommerce.exception.ValidationException;
import com.peerislands.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Set cartServiceUrl via reflection since @Value is not processed in unit tests
        try {
            java.lang.reflect.Field field = ProductService.class.getDeclaredField("cartServiceUrl");
            field.setAccessible(true);
            field.set(productService, "http://cart-service");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Product sampleProduct() {
        Product p = new Product();
        p.setId(1);
        p.setName("Test Product");
        p.setDescription("Desc");
        p.setPrice(BigDecimal.valueOf(100));
        p.setAvailableStock(10);
        return p;
    }

    @Test
    void testGetAllProducts() {
        List<Product> products = Arrays.asList(sampleProduct());
        when(productRepository.findAll()).thenReturn(products);
        assertEquals(1, productService.getAllProducts().size());
    }

    @Test
    void testGetProductById_Found() {
        Product p = sampleProduct();
        when(productRepository.findById(1)).thenReturn(Optional.of(p));
        assertEquals(p, productService.getProductById(1));
    }

    @Test
    void testGetProductById_NotFound() {
        when(productRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(1));
    }

    @Test
    void testCreateProduct_Valid() {
        Product p = sampleProduct();
        when(productRepository.save(any())).thenReturn(p);
        assertEquals(p, productService.createProduct(p));
    }

    @Test
    void testCreateProduct_InvalidPrice() {
        Product p = sampleProduct();
        p.setPrice(BigDecimal.ZERO);
        assertThrows(ValidationException.class, () -> productService.createProduct(p));
    }

    @Test
    void testCreateProduct_InvalidStock() {
        Product p = sampleProduct();
        p.setAvailableStock(-1);
        assertThrows(ValidationException.class, () -> productService.createProduct(p));
    }

    @Test
    void testUpdateProduct_Found() {
        Product existing = sampleProduct();
        Product update = sampleProduct();
        update.setName("Updated");
        when(productRepository.findById(1)).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenReturn(existing);
        Product result = productService.updateProduct(1, update);
        assertEquals("Updated", result.getName());
    }

    @Test
    void testUpdateProduct_NotFound() {
        when(productRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productService.updateProduct(1, sampleProduct()));
    }

    @Test
    void testDeleteProduct_Found() {
        when(productRepository.existsById(1)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1);
        assertDoesNotThrow(() -> productService.deleteProduct(1));
    }

    @Test
    void testDeleteProduct_NotFound() {
        when(productRepository.existsById(1)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> productService.deleteProduct(1));
    }

    @Test
    void testUpdateStock_Valid() {
        Product p = sampleProduct();
        when(productRepository.findById(1)).thenReturn(Optional.of(p));
        when(productRepository.save(any())).thenReturn(p);
        assertEquals(p, productService.updateStock(1, 5));
    }

    @Test
    void testUpdateStock_Insufficient() {
        Product p = sampleProduct();
        when(productRepository.findById(1)).thenReturn(Optional.of(p));
        assertThrows(ValidationException.class, () -> productService.updateStock(1, -20));
    }

    @Test
    void testAddProductToCart_Success() {
        Product p = sampleProduct();
        when(productRepository.findById(1)).thenReturn(Optional.of(p));
        when(restTemplate.postForObject(anyString(), any(), eq(CartDTO.class))).thenReturn(new CartDTO());
        assertDoesNotThrow(() -> productService.addProductToCart("user1", 1, 2));
    }

    @Test
    void testAddProductToCart_InsufficientStock() {
        Product p = sampleProduct();
        p.setAvailableStock(1);
        when(productRepository.findById(1)).thenReturn(Optional.of(p));
        assertThrows(ValidationException.class, () -> productService.addProductToCart("user1", 1, 2));
    }

    @Test
    void testGetStockQuantity() {
        Product p = sampleProduct();
        when(productRepository.findById(1)).thenReturn(Optional.of(p));
        assertEquals(10, productService.getStockQuantity(1));
    }

    @Test
    void testUpdateProductStock_Increase() {
        Product p = sampleProduct();
        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setQuantity(5);
        dto.setOperation("INCREASE");
        when(productRepository.findById(1)).thenReturn(Optional.of(p));
        when(productRepository.save(any())).thenReturn(p);
        assertEquals(p, productService.updateProductStock(1, dto));
    }

    @Test
    void testUpdateProductStock_Decrease_Valid() {
        Product p = sampleProduct();
        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setQuantity(5);
        dto.setOperation("DECREASE");
        when(productRepository.findById(1)).thenReturn(Optional.of(p));
        when(productRepository.save(any())).thenReturn(p);
        assertEquals(p, productService.updateProductStock(1, dto));
    }

    @Test
    void testUpdateProductStock_Decrease_Insufficient() {
        Product p = sampleProduct();
        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setQuantity(20);
        dto.setOperation("DECREASE");
        when(productRepository.findById(1)).thenReturn(Optional.of(p));
        assertThrows(ValidationException.class, () -> productService.updateProductStock(1, dto));
    }

    @Test
    void testUpdateProductStock_InvalidOperation() {
        Product p = sampleProduct();
        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setQuantity(5);
        dto.setOperation("INVALID");
        when(productRepository.findById(1)).thenReturn(Optional.of(p));
        assertThrows(ValidationException.class, () -> productService.updateProductStock(1, dto));
    }

    @Test
    void testUpdateProductStock_InvalidQuantity() {
        Product p = sampleProduct();
        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setQuantity(-1);
        dto.setOperation("INCREASE");
        when(productRepository.findById(1)).thenReturn(Optional.of(p));
        assertThrows(ValidationException.class, () -> productService.updateProductStock(1, dto));
    }
}
