package com.whisky.yupicturebackend.controller;

import com.whisky.yupicturebackend.common.BaseResponse;
import com.whisky.yupicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {

    /**
     * 健康检查接口
     *
     * @return
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("OK");
    }
}
