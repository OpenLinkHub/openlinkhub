package com.openlinkhub.iot.device;

import com.openlinkhub.iot.common.ApiException;
import com.openlinkhub.iot.product.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SensorService {

    private final ProductService productService;
    private final SensorRepository repository;

    public SensorService(ProductService productService, SensorRepository repository) {
        this.productService = productService;
        this.repository = repository;
    }

    public List<Sensor> findByProductId(Long productId) {
        productService.findById(productId);
        return repository.findByProductId(productId);
    }

    public Sensor create(Long productId, SensorRequest request) {
        productService.findById(productId);
        return repository.create(productId, request);
    }

    public Sensor update(Long id, SensorRequest request) {
        repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sensor not found"));
        if (request.productId() != null) {
            productService.findById(request.productId());
        }
        return repository.update(id, request);
    }

    public void delete(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sensor not found"));
        repository.delete(id);
    }
}
