package com.openlinkhub.iot.product;

import com.openlinkhub.iot.common.ApiException;
import com.openlinkhub.iot.common.PageResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> findAll() {
        return repository.findAll();
    }

    public PageResult<Product> findPage(String keyword, String category, int page, int size) {
        return repository.findPage(keyword, category, page, size);
    }

    public Product findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    public Product create(ProductRequest request) {
        return repository.create(request);
    }

    public Product update(Long id, ProductRequest request) {
        findById(id);
        return repository.update(id, request);
    }
}
