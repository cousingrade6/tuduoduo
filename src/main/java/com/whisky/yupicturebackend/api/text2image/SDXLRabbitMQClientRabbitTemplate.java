package com.whisky.yupicturebackend.api.text2image;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.whisky.yupicturebackend.api.text2image.model.TaskResponse;
import com.whisky.yupicturebackend.api.text2image.model.Text2ImageTaskRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.UUID;

@Slf4j
@Component
public class SDXLRabbitMQClientRabbitTemplate {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    // 配置参数
    private final String taskQueueName = "image_generation_tasks";
    private final String resultQueueName = "image_generation_results";
//    private String consumerQueueName;

    private SimpleMessageListenerContainer listenerContainer;

    @Autowired
    public SDXLRabbitMQClientRabbitTemplate(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        // 初始化时声明队列（确保存在）
        rabbitTemplate.execute(channel -> {
            channel.queueDeclare(taskQueueName, true, false, false, null);
            channel.queueDeclare(resultQueueName, true, false, false, null);
            return null;
        });
    }

    // 添加重试配置
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String submitTextToImageTask(Text2ImageTaskRequest request) {
        // 生成唯一correlationId
        String correlationId = UUID.randomUUID().toString();
        request.setTaskId(correlationId); // 将correlationId作为taskId传递
        // 发布任务消息
        rabbitTemplate.convertAndSend(taskQueueName, (Object) request, message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setContentType("application/json");
            message.getMessageProperties().setCorrelationId(correlationId); // 任务id
            message.getMessageProperties().setPriority(request.getPriority()); // 关键设置
            return message;
        });

        log.info("Submitted task {} with prompt: {}", request.getTaskId(), request.getPrompt());
        return request.getTaskId();
    }

    public void startConsuming(TaskResponseHandler handler) {
        // 直接监听持久化结果队列
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(rabbitTemplate.getConnectionFactory());
        container.setQueueNames(resultQueueName);
        container.setMessageListener((ChannelAwareMessageListener) (message, channel) -> {
            try {
                TaskResponse response = objectMapper.readValue(message.getBody(), TaskResponse.class);
                String receivedCorrelationId = message.getMessageProperties().getCorrelationId();
                String expectedCorrelationId = response.getTaskId(); // 原taskId现在作为correlationId

                // 仅处理与自己请求匹配的响应
                if (expectedCorrelationId.equals(receivedCorrelationId)) {
                    handler.handle(response);
                }
            } catch (Exception e) {
                log.error("Failed to process response", e);
            }
        });
        container.start();
    }

    public void stopConsuming() {
        if (listenerContainer != null) {
            listenerContainer.stop();
        }
    }

    @PreDestroy
    public void close() {
        stopConsuming();
    }

    @FunctionalInterface
    public interface TaskResponseHandler {
        void handle(TaskResponse response);
    }
}