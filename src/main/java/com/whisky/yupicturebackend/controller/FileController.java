package com.whisky.yupicturebackend.controller;

import cn.hutool.core.util.RadixUtil;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.whisky.yupicturebackend.annotation.AuthCheck;
import com.whisky.yupicturebackend.common.BaseResponse;
import com.whisky.yupicturebackend.common.ResultUtils;
import com.whisky.yupicturebackend.constant.UserConstant;
import com.whisky.yupicturebackend.exception.BusinessException;
import com.whisky.yupicturebackend.exception.ErrorCode;
import com.whisky.yupicturebackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    private CosManager cosManager;

    @Autowired
    public FileController(CosManager cosManager) {
        this.cosManager = cosManager;
    }

    /**
     * 测试文件上传
     * @param multipartFile 上传文件
     * @return 响应体
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUpload(@RequestParam("file") MultipartFile multipartFile) {
        String fileName = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/upload/%s", fileName);

        File tmpFile = null;
        try {
            tmpFile = File.createTempFile(filepath, null);
            multipartFile.transferTo(tmpFile);
            cosManager.putObject(filepath, tmpFile);
            return ResultUtils.success(filepath);
        } catch (IOException e) {
            log.error("file upload error, filepath={}", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        } finally {
            if (tmpFile != null) {
                if (!tmpFile.delete()){
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = {}", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            // 释放流
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }

    }
}
