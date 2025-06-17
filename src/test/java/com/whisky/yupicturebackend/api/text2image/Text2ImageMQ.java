package com.whisky.yupicturebackend.api.text2image;

import com.whisky.yupicturebackend.api.text2image.model.Text2ImageTaskRequest;
import org.junit.jupiter.api.Test;


public class Text2ImageMQ {
    @Test
    public void test() {
        try {
            SDXLRabbitMQClient client = new SDXLRabbitMQClient();

            Text2ImageTaskRequest request = new Text2ImageTaskRequest();
            request.setPrompt("A beautiful landscape");

            String taskId = client.submitTextToImageTask(request);
            System.out.println("Task ID: " + taskId);

            client.startConsuming(response -> {
                System.out.println("Received update at " + response.getTimestamp());
                System.out.println("Status: " + response.getTaskStatus());

                if ("SUCCESS".equals(response.getTaskStatus())) {
                    System.out.println("Image URL: " + response.getImageUrl());
                    try {
                        client.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            Thread.sleep(60000); // 等待1分钟
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
