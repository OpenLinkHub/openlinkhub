package com.openlinkhub.iot.device;

import com.openlinkhub.iot.common.ApiException;
import com.openlinkhub.iot.common.PageResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository repository;

    public DeviceService(DeviceRepository repository) {
        this.repository = repository;
    }

    public List<Device> findAll() {
        return repository.findAll();
    }

    public PageResult<Device> findPage(String keyword, Long productId, String status, int page, int size) {
        return repository.findPage(keyword, productId, status, page, size);
    }

    public Device findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Device not found"));
    }

    public Device findByDeviceKey(String deviceKey) {
        return repository.findByDeviceKey(deviceKey)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Device not found"));
    }

    public Device create(DeviceRequest request) {
        return repository.create(request);
    }

    public Device update(Long id, DeviceRequest request) {
        findById(id);
        return repository.update(id, request);
    }
}
