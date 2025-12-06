package com.lebvest.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public Queue signupUploadQueue() {
        return new Queue("company.signup.upload");
    }

    @Bean
    public Queue signupEmailQueue() {
        return new Queue("company.signup.email");
    }

    @Bean
    public Queue signupAcceptedMoveQueue() {
        return new Queue("company.signup.accepted.move");
    }

    @Bean
    public Queue emailQueue() {
        return new Queue("email.queue", true); // durable
    }

    @Bean
    public Queue documentProcessingQueue() {
        return new Queue("document.processing.queue", true);
    }

    @Bean
    public Queue payoutCalculationQueue() {
        return new Queue("payout.calculation.queue", true);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
