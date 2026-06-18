package com.openlinkhub.iot.product;

import com.openlinkhub.iot.common.ApiResponse;
import com.openlinkhub.iot.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<Product>> list(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String category,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.findPage(keyword, category, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.findById(id));
    }

    @PostMapping
    public ApiResponse<Product> create(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Product> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }
}
