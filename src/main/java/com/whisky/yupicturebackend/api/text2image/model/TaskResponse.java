package com.whisky.yupicturebackend.api.text2image.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskResponse extends RabbitMQMessage {
    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("status")
    private String taskStatus; // PENDING, PROCESSING, SUCCESS, FAILED
    
    @JsonProperty("image_url")
    private String imageUrl;
    
    @JsonProperty("generation_time")
    private Double generationTime;
    
    private String error;
    
    @JsonProperty("progress")
    private Integer progress; // 0-100
}