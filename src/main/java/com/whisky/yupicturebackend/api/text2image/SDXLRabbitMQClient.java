package com.whisky.yupicturebackend.api.text2image;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.*;
import com.whisky.yupicturebackend.api.text2image.model.TaskResponse;
import com.whisky.yupicturebackend.api.text2image.model.Text2ImageTaskRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

//@Component
@Slf4j
public class SDXLRabbitMQClient  {
    private final Connection connection;
    private final Channel channel;
    private final ObjectMapper objectMapper;

    // 配置参数
    private final String taskQueueName = "image_generation_tasks";
    private final String resultExchangeName = "results_exchange";
    private String consumerQueueName;
    private String consumerTag;

    public SDXLRabbitMQClient () throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setVirtualHost("/sdxl");
        factory.setUsername("guest");
        factory.setPassword("123456");

        this.connection = factory.newConnection();
        this.channel = connection.createChannel();

        // 配置ObjectMapper支持Java 8时间类型
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 声明队列和交换机
        declareInfrastructure();
    }

    private void declareInfrastructure() throws IOException {
        // 任务队列（持久化）
        channel.queueDeclare(taskQueueName, true, false, false, null);

        // 结果交换机（直接类型，持久化）
        channel.exchangeDeclare(resultExchangeName, BuiltinExchangeType.DIRECT, true);

        // 为每个客户端创建临时队列
        this.consumerQueueName = channel.queueDeclare().getQueue();
    }

    public String submitTextToImageTask(Text2ImageTaskRequest request) throws IOException {
        // 绑定临时队列到结果交换机，使用taskId作为routing key
        channel.queueBind(consumerQueueName, resultExchangeName, request.getTaskId());

        // 发布任务消息
        channel.basicPublish("", taskQueueName,
                new AMQP.BasicProperties.Builder()
                        .deliveryMode(2) // 持久化消息
                        .contentType("application/json")
                        .build(),
                objectMapper.writeValueAsBytes(request));

        log.info("Submitted task {} with prompt: {}", request.getTaskId(), request.getPrompt());
        return request.getTaskId();
    }

    public void startConsuming(TaskResponseHandler handler) throws IOException {
        this.consumerTag = channel.basicConsume(consumerQueueName, true, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) {
                try {
                    TaskResponse response = objectMapper.readValue(body, TaskResponse.class);
                    log.debug("Received response for task {}: {}", response.getTaskId(), response.getTaskStatus());

                    handler.handle(response);

                    // 任务完成后解绑队列
                    if ("SUCCESS".equals(response.getTaskStatus()) || "FAILED".equals(response.getTaskStatus())) {
                        channel.queueUnbind(consumerQueueName, resultExchangeName, response.getTaskId());
                    }
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse response message", e);
                } catch (IOException e) {
                    log.error("Error processing message", e);
                }
            }
        });
    }

    public void stopConsuming() throws IOException {
        if (consumerTag != null) {
            channel.basicCancel(consumerTag);
        }
    }

    public void close() throws IOException, TimeoutException {
        stopConsuming();
        channel.close();
        connection.close();
    }

    @FunctionalInterface
    public interface TaskResponseHandler {
        void handle(TaskResponse response);
    }

}