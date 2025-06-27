package com.whisky.yupicturebackend.common;

import com.whisky.yupicturebackend.exception.ErrorCode;

public class ResultUtils {

    /**
     * 成功响应
     * @param data  数据
     * @return      响应
     * @param <T>   数据类型
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    public static BaseResponse<?> error(Integer code, String msg) {
        return new BaseResponse<>(code, null, msg);
    }

    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    public static BaseResponse<?> error(ErrorCode errorCode, String msg) {
        return new BaseResponse<>(errorCode.getCode(), null, msg);
    }
}
