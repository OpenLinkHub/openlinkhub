package com.openlinkhub.iot.rule;

import com.openlinkhub.iot.common.ApiException;
import com.openlinkhub.iot.common.PageResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuleService {

    private final RuleRepository repository;

    public RuleService(RuleRepository repository) {
        this.repository = repository;
    }

    public List<Rule> findAll() {
        return repository.findAll();
    }

    public PageResult<Rule> findPage(String keyword, String deviceKey, String metric, Boolean enabled, int page, int size) {
        return repository.findPage(keyword, deviceKey, metric, enabled, page, size);
    }

    public Rule findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rule not found"));
    }

    public Rule create(RuleRequest request) {
        return repository.create(request);
    }

    public Rule update(Long id, RuleRequest request) {
        findById(id);
        return repository.update(id, request);
    }

    public Rule enable(Long id) {
        findById(id);
        return repository.setEnabled(id, true);
    }

    public Rule disable(Long id) {
        findById(id);
        return repository.setEnabled(id, false);
    }
}
