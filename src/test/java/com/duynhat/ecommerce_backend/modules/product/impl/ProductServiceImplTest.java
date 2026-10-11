package com.duynhat.ecommerce_backend.modules.product.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.modules.category.CategoryService;
import com.duynhat.ecommerce_backend.modules.category.entity.Category;
import com.duynhat.ecommerce_backend.modules.inventory.InventoryTransactionRepository;
import com.duynhat.ecommerce_backend.modules.media.MediaStorageService;
import com.duynhat.ecommerce_backend.modules.media.MediaUrlService;
import com.duynhat.ecommerce_backend.modules.product.ProductRepository;
import com.duynhat.ecommerce_backend.modules.product.dto.response.ProductResponse;
import com.duynhat.ecommerce_backend.modules.product.entity.Product;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MediaStorageService mediaStorageService;

    @Mock
    private MediaUrlService mediaUrlService;

    @InjectMocks
    private ProductServiceImpl productService;

    private UUID productId;
    private Category category;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();

        category = Category.builder()
                .id(UUID.randomUUID())
                .name("Keyboard")
                .active(true)
                .build();
    }

    @Test
    void uploadImage_shouldReplaceOldImageAndDeleteOldObject() {
        String oldObjectKey =
                "products/" + productId + "/old.jpg";

        String newObjectKey =
                "products/" + productId + "/new.jpg";

        String publicUrl =
                "http://localhost:8080/api/media?key=new";

        Product product = createProduct(oldObjectKey);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "new.jpg",
                "image/jpeg",
                "new-image-content".getBytes()
        );

        when(productRepository.findByIdForUpdate(productId))
                .thenReturn(Optional.of(product));

        when(mediaStorageService.upload(
                file,
                "products/" + productId
        )).thenReturn(newObjectKey);

        when(productRepository.save(product))
                .thenReturn(product);

        when(mediaUrlService.toPublicUrl(newObjectKey))
                .thenReturn(publicUrl);

        ProductResponse response =
                productService.uploadImage(productId, file);

        assertThat(product.getImageUrl())
                .isEqualTo(newObjectKey);

        assertThat(response.getImageUrl())
                .isEqualTo(publicUrl);

        verify(mediaStorageService).upload(
                file,
                "products/" + productId
        );

        verify(mediaStorageService)
                .delete(oldObjectKey);

        verify(productRepository)
                .save(product);
    }

    @Test
    void uploadImage_shouldNotDeleteWhenProductHasNoOldImage() {
        String newObjectKey =
                "products/" + productId + "/new.jpg";

        String publicUrl =
                "http://localhost:8080/api/media?key=new";

        Product product = createProduct(null);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "new.jpg",
                "image/jpeg",
                "new-image-content".getBytes()
        );

        when(productRepository.findByIdForUpdate(productId))
                .thenReturn(Optional.of(product));

        when(mediaStorageService.upload(
                file,
                "products/" + productId
        )).thenReturn(newObjectKey);

        when(productRepository.save(product))
                .thenReturn(product);

        when(mediaUrlService.toPublicUrl(newObjectKey))
                .thenReturn(publicUrl);

        ProductResponse response =
                productService.uploadImage(productId, file);

        assertThat(product.getImageUrl())
                .isEqualTo(newObjectKey);

        assertThat(response.getImageUrl())
                .isEqualTo(publicUrl);

        verify(mediaStorageService, never())
                .delete(anyString());

        verify(productRepository)
                .save(product);
    }

    @Test
    void uploadImage_shouldThrowWhenProductDoesNotExist() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "new.jpg",
                "image/jpeg",
                "new-image-content".getBytes()
        );

        when(productRepository.findByIdForUpdate(productId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                productService.uploadImage(productId, file)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");

        verifyNoInteractions(mediaStorageService);
        verify(productRepository, never())
                .save(any());
    }

    @Test
    void deleteImage_shouldDeleteObjectAndClearImageUrl() {
        String objectKey =
                "products/" + productId + "/image.jpg";

        Product product = createProduct(objectKey);

        when(productRepository.findByIdForUpdate(productId))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product))
                .thenReturn(product);

        ProductResponse response =
                productService.deleteImage(productId);

        verify(mediaStorageService)
                .delete(objectKey);

        verify(productRepository)
                .save(product);

        assertThat(product.getImageUrl())
                .isNull();

        assertThat(response.getImageUrl())
                .isNull();
    }

    @Test
    void deleteImage_shouldRejectProductWithoutImage() {
        Product product = createProduct(null);

        when(productRepository.findByIdForUpdate(productId))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() ->
                productService.deleteImage(productId)
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Product does not have an image");

        verifyNoInteractions(mediaStorageService);

        verify(productRepository, never())
                .save(any());
    }

    @Test
    void deleteImage_shouldThrowWhenProductDoesNotExist() {
        when(productRepository.findByIdForUpdate(productId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                productService.deleteImage(productId)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");

        verifyNoInteractions(mediaStorageService);

        verify(productRepository, never())
                .save(any());
    }

    private Product createProduct(String imageUrl) {
        return Product.builder()
                .id(productId)
                .name("Logitech MX Keys")
                .description("Wireless keyboard")
                .price(BigDecimal.valueOf(109.99))
                .stock(35)
                .imageUrl(imageUrl)
                .active(true)
                .category(category)
                .build();
    }
}