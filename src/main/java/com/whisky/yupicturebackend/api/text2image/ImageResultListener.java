package com.whisky.yupicturebackend.api.text2image;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.whisky.yupicturebackend.api.text2image.model.TaskResponse;
import com.whisky.yupicturebackend.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
public class ImageResultListener {

    @Resource
    private PictureService pictureService;
    @Resource
    private ObjectMapper objectMapper;

    @RabbitListener(queues = "image_generation_results")
    public void handleResultMessage(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 1. 安全反序列化
            TaskResponse response = parseMessage(message.getBody());
            if (response == null) {
                rejectMessage(channel, deliveryTag);
                return;
            }

            // 2. 校验correlationId
            String correlationId = message.getMessageProperties().getCorrelationId();
            if (correlationId == null || !correlationId.equals(response.getTaskId())) {
                log.warn("Invalid correlationId, expected: {}, actual: {}",
                        response.getTaskId(), correlationId);
                rejectMessage(channel, deliveryTag);
                return;
            }

            // 3. 处理业务
            pictureService.updateTaskResult(response);
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Message processing failed", e);
            safeReject(channel, deliveryTag);
        }
    }

    private TaskResponse parseMessage(byte[] body) {
        try {
            return objectMapper.readValue(body, TaskResponse.class);
        } catch (IOException e) {
            log.error("Message deserialization failed", e);
            return null;
        }
    }

    private void rejectMessage(Channel channel, long deliveryTag) {
        try {
            channel.basicReject(deliveryTag, false);
        } catch (IOException e) {
            log.error("Failed to reject message", e);
        }
    }

    private void safeReject(Channel channel, long deliveryTag) {
        try {
            channel.basicNack(deliveryTag, false, true); // 允许重试
        } catch (IOException e) {
            log.error("Failed to send NACK", e);
        }
    }
}