package com.krushikranti.service;

import com.krushikranti.model.AdminLog;
import com.krushikranti.repository.AdminLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final AdminLogRepository adminLogRepository;

    public void log(Long adminId, String adminName, String action, String target, String type) {
        AdminLog entry = AdminLog.builder()
                .adminId(adminId)
                .adminName(adminName)
                .action(action)
                .target(target)
                .type(type)
                .build();
        adminLogRepository.save(entry);
    }

    public Page<AdminLog> getLogs(int page, int size, String search, String type) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (search != null && !search.isBlank()) {
            return adminLogRepository.searchLogs(search.trim(), pageRequest);
        }
        if (type != null && !type.isBlank()) {
            return adminLogRepository.findByType(type.trim(), pageRequest);
        }
        return adminLogRepository.findAll(pageRequest);
    }
}
