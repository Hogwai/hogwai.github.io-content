package com.hogwai.kafka.sync.sender.gateway.custom;

import com.hogwai.kafka.sync.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CustomReplyListener {

    private static final Logger log = LoggerFactory.getLogger(CustomReplyListener.class);

    private final CustomSyncGateway gateway;

    public CustomReplyListener(CustomSyncGateway gateway) {
        this.gateway = gateway;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.results}",
            groupId = "custom-reply-listener",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onReply(Response response) {
        log.info("<<< Reply listener received response for [{}]", response.requestId());
        gateway.complete(response);
    }
}
