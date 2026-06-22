package com.hogwai.kafka.sync.sender.controller;

import com.hogwai.kafka.sync.model.Request;
import com.hogwai.kafka.sync.model.Response;
import com.hogwai.kafka.sync.sender.gateway.custom.CustomSyncGateway;
import com.hogwai.kafka.sync.sender.gateway.replying.ReplyingSyncGateway;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final CustomSyncGateway customGateway;
    private final ReplyingSyncGateway replyingGateway;

    public SyncController(CustomSyncGateway customGateway, ReplyingSyncGateway replyingGateway) {
        this.customGateway = customGateway;
        this.replyingGateway = replyingGateway;
    }

    @PostMapping
    public Response sendSync(
            @RequestParam String payload,
            @RequestParam(defaultValue = "custom") String mode) throws Exception {

        var request = new Request(payload);

        if ("replying".equals(mode)) {
            return replyingGateway.sendSync(request);
        }
        return customGateway.sendSync(request);
    }
}
