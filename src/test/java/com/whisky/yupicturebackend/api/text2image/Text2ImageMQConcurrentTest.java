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
    // 将会员线程设为第4号线程
    private static final int VIP_THREAD_INDEX = 4;

    @Test
    public void testConcurrentRequests() throws InterruptedException {
        // 线程池
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        // 总计数器（确保所有请求完成）
        CountDownLatch allRequestsLatch = new CountDownLatch(THREAD_COUNT * REQUESTS_PER_THREAD);
        // 结果收集（线程安全）
        ConcurrentLinkedQueue<String> executionOrder = new ConcurrentLinkedQueue<>();

        // 先启动消费者（全局监听结果队列）
        client.startConsuming(response -> {
            if ("SUCCESS".equals(response.getTaskStatus())) {
                executionOrder.add(response.getTaskId() + ":" + response.getImageUrl());
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
                        String prompt = "A wonderful landscape.";
                        request.setPrompt(prompt);

                        // 会员线程设置高优先级（9），其他线程保持默认（0）
                        if (threadId == VIP_THREAD_INDEX) {
                            request.setPriority(9); // 最高优先级
                            Thread.sleep(200);
                        }
                        // 发送请求（无需等待结果）
                        client.submitTextToImageTask(request);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        // 等待所有请求完成（超时时间根据实际情况调整）
        boolean allDone = allRequestsLatch.await(1200, TimeUnit.SECONDS);
        executor.shutdown();

        // 验证结果
        assertTrue("Not all requests completed in time", allDone);

        // 打印执行顺序（验证会员请求优先）
        System.out.println("=== 执行顺序 ===");
        executionOrder.forEach(System.out::println);
    }
}