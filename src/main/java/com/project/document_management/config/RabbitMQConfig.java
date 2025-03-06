package com.project.document_management.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DOCUMENT_QUEUE = "documentQueue";

    @Bean
    public Queue documentQueue() {
        return new Queue(DOCUMENT_QUEUE, true);
    }
}