package com.whisky.yupicturebackend.api.text2image;

import com.whisky.yupicturebackend.api.text2image.model.Text2ImageTaskRequest;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQProducer {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendImageTask(Text2ImageTaskRequest request) {
        rabbitTemplate.convertAndSend(
            "image_generation_tasks", // exchange
            request,
            message -> {
                message.getMessageProperties().setPriority(request.getPriority());
                message.getMessageProperties().setCorrelationId(request.getTaskId());
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return message;
            }
        );
    }
}