package com.duoc.enrollmentplatform.enrollment.infrastructure.adapters;

import com.duoc.enrollmentplatform.factory.RabbitMQConfiguration;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class EnrollmentSummaryListener {

    private final JpaEnrollmentSummaryRecordRepository repository;

    // Inyectamos el repositorio que creaste en el paso anterior
    public EnrollmentSummaryListener(JpaEnrollmentSummaryRecordRepository repository) {
        this.repository = repository;
    }

    // se queda escuchando la cola automáticamente
    @RabbitListener(queues = RabbitMQConfiguration.QUEUE_NAME)
    public void receiveMessage(Map<String, String> payload) {
        System.out.println("📩 Mensaje asíncrono recibido desde RabbitMQ: " + payload);
        
        // Extraemos los datos del JSON que mandó el Productor
        String enrollmentId = payload.get("enrollmentId");
        String studentId = payload.get("studentId");
        String status = payload.get("status");
        
        // Generamos un ID unico para el registro en la nueva tabla
        String id = UUID.randomUUID().toString(); 
        
        // Creamos la entidad
        EnrollmentSummaryRecord record = new EnrollmentSummaryRecord(id, enrollmentId, studentId, status);
        
        // Guardamos en la base de datos (Oracle)
        repository.save(record);
        
        System.out.println("✅ Resumen guardado satisfactoriamente en la nueva tabla de la BD.");
    }
}