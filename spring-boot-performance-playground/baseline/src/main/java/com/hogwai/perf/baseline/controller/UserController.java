package com.hogwai.perf.baseline.controller;

import com.hogwai.perf.common.dto.DashboardResponse;
import com.hogwai.perf.common.dto.UserResponse;
import com.hogwai.perf.common.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/dashboard/{id}")
    public DashboardResponse getDashboard(@PathVariable Long id) {
        return userService.getDashboard(id);
    }
}
