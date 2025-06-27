package com.whisky.yupicturebackend.api.text2image.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.UUID;

/**
 * 创建扩图任务请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Text2ImageTaskRequest extends RabbitMQMessage {

    @JsonProperty("task_id")
    private String taskId = UUID.randomUUID().toString();
    private String prompt;
    private String negativePrompt;
    private int priority = 0; // 默认优先级0（普通用户），会员设为更高

    /**
     * 推理步数
     */
    private int steps = 30;

    /**
     * 图片宽度
     */
    private int width = 1024;

    /**
     * 图片高度
     */
    private int height = 1024;
}
