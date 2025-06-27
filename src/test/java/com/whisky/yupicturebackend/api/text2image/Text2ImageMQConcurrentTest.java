package com.whisky.yupicturebackend.api.text2image;

import com.whisky.yupicturebackend.api.text2image.model.Text2ImageTaskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.*;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

@SpringBootTest
public class Text2ImageMQConcurrentTest {
    @Resource
    private SDXLRabbitMQClientRabbitTemplate client;

    // 并发线程数
    private static final int THREAD_COUNT = 5;
    // 每个线程发送的请求数
    private static final int REQUESTS_PER_THREAD = 2;

    @Test
    public void testConcurrentRequests() throws InterruptedException {
        // 线程池
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        // 总计数器（确保所有请求完成）
        CountDownLatch allRequestsLatch = new CountDownLatch(THREAD_COUNT * REQUESTS_PER_THREAD);
        // 结果收集（线程安全）
        ConcurrentHashMap<String, String> successResults = new ConcurrentHashMap<>();

        // 先启动消费者（全局监听结果队列）
        client.startConsuming(response -> {
            if ("SUCCESS".equals(response.getTaskStatus())) {
                successResults.put(response.getTaskId(), response.getImageUrl());
            }
            allRequestsLatch.countDown(); // 每个响应减少计数器
        });

        // 并发发送请求
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
                    try {
                        Text2ImageTaskRequest request = new Text2ImageTaskRequest();
                        request.setPrompt(String.format("Thread-%d Request-%d: A landscape", threadId, j));
                        // 发送请求（无需等待结果）
                        client.submitTextToImageTask(request);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        // 等待所有请求完成（超时时间根据实际情况调整）
        boolean allDone = allRequestsLatch.await(120, TimeUnit.SECONDS);
        executor.shutdown();

        // 验证结果
        assertTrue("Not all requests completed in time", allDone);
        assertEquals(
            "Should receive all success responses",
            THREAD_COUNT * REQUESTS_PER_THREAD,
            successResults.size()
        );
        successResults.forEach((taskId, url) -> {
            System.out.printf("Task %s -> %s\n", taskId, url);
        });
    }
}