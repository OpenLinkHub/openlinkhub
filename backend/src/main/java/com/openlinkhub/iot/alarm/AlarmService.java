package com.openlinkhub.iot.alarm;

import com.openlinkhub.iot.common.ApiException;
import com.openlinkhub.iot.common.PageResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlarmService {

    private final AlarmRepository repository;

    public AlarmService(AlarmRepository repository) {
        this.repository = repository;
    }

    public List<Alarm> findAll(String status) {
        return repository.findAll(status);
    }

    public PageResult<Alarm> findPage(String keyword, String status, String severity, int page, int size) {
        return repository.findPage(keyword, status, severity, page, size);
    }

    public Alarm acknowledge(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alarm not found"));
        return repository.acknowledge(id);
    }
}
