package com.hogwai.kafka.sync.sender.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hogwai.kafka.sync.model.Request;
import com.hogwai.kafka.sync.model.Response;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.topic.replies}")
    private String replyTopic;

    // ---------------------------------------------------------------
    // ObjectMapper with Java 8 date/time support
    // ---------------------------------------------------------------

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    // ---------------------------------------------------------------
    // Producer factories
    // ---------------------------------------------------------------

    @Bean
    public ProducerFactory<String, Request> requestProducerFactory(ObjectMapper objectMapper) {
        return producerFactory(objectMapper);
    }

    @Bean
    public ProducerFactory<String, Object> objectProducerFactory(ObjectMapper objectMapper) {
        return producerFactory(objectMapper);
    }

    private <T> ProducerFactory<String, T> producerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        var factory = new DefaultKafkaProducerFactory<String, T>(props);
        factory.setValueSerializer(new JsonSerializer<>(objectMapper));
        return factory;
    }

    // ---------------------------------------------------------------
    // Consumer factory
    // ---------------------------------------------------------------

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-sync-sender");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class.getName());
        var factory = new DefaultKafkaConsumerFactory<String, Object>(props);
        factory.setValueDeserializer(new JsonDeserializer<>(objectMapper));
        return factory;
    }

    @SuppressWarnings("unchecked")
    private <T> ConsumerFactory<String, T> castConsumerFactory(ObjectMapper objectMapper) {
        return (ConsumerFactory<String, T>) consumerFactory(objectMapper);
    }

    // ---------------------------------------------------------------
    // Listener container factory for @KafkaListener
    // ---------------------------------------------------------------

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> cf) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(cf);
        return factory;
    }

    // ---------------------------------------------------------------
    // Templates
    // ---------------------------------------------------------------

    @Bean
    @Primary
    public KafkaTemplate<String, Request> requestKafkaTemplate(
            ProducerFactory<String, Request> pf) {
        return new KafkaTemplate<>(pf);
    }

    @Bean
    public KafkaTemplate<String, Object> objectKafkaTemplate(
            ProducerFactory<String, Object> pf) {
        return new KafkaTemplate<>(pf);
    }

    // ---------------------------------------------------------------
    // ReplyingKafkaTemplate (Spring Kafka's built-in request/reply)
    // ---------------------------------------------------------------

    @Bean
    public ReplyingKafkaTemplate<String, Request, Response> replyingKafkaTemplate(
            ProducerFactory<String, Request> producerFactory,
            ObjectMapper objectMapper) {

        ConcurrentMessageListenerContainer<String, Response> replyContainer =
                new ConcurrentMessageListenerContainer<>(
                        castConsumerFactory(objectMapper),
                        new ContainerProperties(replyTopic));
        replyContainer.getContainerProperties().setGroupId("reply-listener");
        return new ReplyingKafkaTemplate<>(producerFactory, replyContainer);
    }
}
