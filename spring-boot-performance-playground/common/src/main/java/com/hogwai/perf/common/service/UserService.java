package com.hogwai.perf.common.service;

import com.hogwai.perf.common.dto.DashboardResponse;
import com.hogwai.perf.common.dto.UserResponse;

public interface UserService {
    UserResponse getUserById(Long id);
    DashboardResponse getDashboard(Long id);
}
