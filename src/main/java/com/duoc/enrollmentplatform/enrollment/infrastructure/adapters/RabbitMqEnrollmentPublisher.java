package com.duoc.enrollmentplatform.enrollment.infrastructure.adapters;

import com.duoc.enrollmentplatform.enrollment.application.ports.EnrollmentMessagePublisher;
import com.duoc.enrollmentplatform.enrollment.domain.entities.Enrollment;
import com.duoc.enrollmentplatform.factory.RabbitMQConfiguration;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RabbitMqEnrollmentPublisher implements EnrollmentMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqEnrollmentPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(Enrollment enrollment) {
        // Preparamos un payload simple en formato JSON
        Map<String, String> payload = new HashMap<>();
        payload.put("enrollmentId", enrollment.getId().getValue());
        payload.put("studentId", enrollment.getStudentId().getValue());
        payload.put("status", "CREATED");

        rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.EXCHANGE_NAME,
                RabbitMQConfiguration.ROUTING_KEY,
                payload
        );
        
        System.out.println("Mensaje enviado a RabbitMQ exitosamente: " + payload);
    }
    
}