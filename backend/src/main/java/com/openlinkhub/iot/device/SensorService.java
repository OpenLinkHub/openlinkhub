package com.openlinkhub.iot.device;

import com.openlinkhub.iot.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SensorService {

    private final DeviceService deviceService;
    private final SensorRepository repository;

    public SensorService(DeviceService deviceService, SensorRepository repository) {
        this.deviceService = deviceService;
        this.repository = repository;
    }

    public List<Sensor> findByDeviceId(Long deviceId) {
        deviceService.findById(deviceId);
        return repository.findByDeviceId(deviceId);
    }

    public Sensor create(Long deviceId, SensorRequest request) {
        deviceService.findById(deviceId);
        return repository.create(deviceId, request);
    }

    public Sensor update(Long id, SensorRequest request) {
        repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sensor not found"));
        return repository.update(id, request);
    }

    public void delete(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sensor not found"));
        repository.delete(id);
    }
}
