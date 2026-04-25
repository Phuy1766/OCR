package vn.edu.congvan.integration.outbox;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Cấu hình RabbitMQ + scheduler cho OutboxPublisher. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(value = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxConfig {

    public static final String EXCHANGE_EVENTS = "congvan.events";

    @Bean
    public TopicExchange congvanEventsExchange() {
        return new TopicExchange(EXCHANGE_EVENTS, /* durable */ true, /* autoDelete */ false);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory factory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate t = new RabbitTemplate(factory);
        t.setMessageConverter(converter);
        return t;
    }
}
