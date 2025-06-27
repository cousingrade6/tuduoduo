package com.whisky.yupicturebackend.api.text2image;

import com.whisky.yupicturebackend.api.text2image.model.Text2ImageTaskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

@SpringBootTest
public class Text2ImageMQTemplate {
    @Resource
    SDXLRabbitMQClientRabbitTemplate client;

    @Test
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> imageUrl = new AtomicReference<>();

        Text2ImageTaskRequest request = new Text2ImageTaskRequest();
        request.setPrompt("A beautiful sunset over mountains");

        client.startConsuming(response -> {
            System.out.println("Status update: " + response.getTaskStatus());

            if ("SUCCESS".equals(response.getTaskStatus())) {
                imageUrl.set(response.getImageUrl());
                latch.countDown();
            } else if ("FAILED".equals(response.getTaskStatus())) {
                latch.countDown();
            }
        });

        String taskId = client.submitTextToImageTask(request);

        // 等待最多60秒
        boolean completed = latch.await(60, TimeUnit.SECONDS);

        assertTrue("Task did not complete in time", completed);
        assertNotNull("Image URL should not be null", imageUrl.get());

        System.out.println("Generated image URL: " + imageUrl.get());
    }
}
