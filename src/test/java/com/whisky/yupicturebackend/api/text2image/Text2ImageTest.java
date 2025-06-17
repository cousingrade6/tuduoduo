package com.whisky.yupicturebackend.api.text2image;

import com.whisky.yupicturebackend.api.text2image.model.Text2ImageTaskRequest;
import org.junit.jupiter.api.Test;

public class Text2ImageTest {

    @Test
    public void test() {
        // 创建客户端
        SdxlClient client = new SdxlClient("https://9d5b-58-251-166-18.ngrok-free.app/");

        // 创建请求
        Text2ImageTaskRequest request = new Text2ImageTaskRequest();
        request.setPrompt("A beautiful sunset over mountains, 4K ultra detailed");
        request.setNegativePrompt("blurry, low quality");
        request.setSteps(30);

        // 3. 同步调用（适合阻塞式场景）
        try {
            String imageUrl = client.generateImageSync(request, 10); // 最多重试10次
            System.out.println("生成成功！图片URL: " + imageUrl);
        } catch (Exception e) {
            System.err.println("生成失败: " + e.getMessage());
        }
    }
}
