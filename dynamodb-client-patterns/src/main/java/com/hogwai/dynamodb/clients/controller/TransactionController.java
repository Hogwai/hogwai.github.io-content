package com.hogwai.dynamodb.clients.controller;

import com.hogwai.dynamodb.clients.service.MovieService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final MovieService service;

    public TransactionController(MovieService service) {
        this.service = service;
    }

    @PostMapping("/transaction/write/bad")
    public Map<String, Object> transactionBad(@RequestParam(defaultValue = "5") int count) {
        int written = service.transactionBad(count);
        return Map.of(
                "method", "individual putItem calls (no atomicity)",
                "requested", count,
                "written", written,
                "note", "Partial success possible — some items may be written while others fail");
    }

    @PostMapping("/transaction/write/good")
    public Map<String, Object> transactionGood(@RequestParam(defaultValue = "5") int count) {
        int written = service.transactionGood(count);
        return Map.of(
                "method", "TransactWriteItems (ACID)",
                "items", written,
                "note", "All-or-nothing — all items written atomically or none are");
    }
}
