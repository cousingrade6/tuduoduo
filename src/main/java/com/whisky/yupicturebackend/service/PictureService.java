package com.whisky.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.whisky.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.whisky.yupicturebackend.model.dto.picture.*;
import com.whisky.yupicturebackend.model.entity.Picture;
import com.whisky.yupicturebackend.model.entity.User;
import com.whisky.yupicturebackend.model.vo.PictureVO;
import org.springframework.scheduling.annotation.Async;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Lenovo
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-05-09 23:05:18
*/
public interface PictureService extends IService<Picture> {

    void validPicture(Picture picture);

    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    void deletePicture(long pictureId, User loginUser);

    @Async
    void clearPictureFile(Picture oldPicture);

    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);

    // 搜图功能
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);

    void checkPictureAuth(User loginUser, Picture picture);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    boolean likePicture(Long userId, Long pictureId);

    void isPictureLiked(Picture picture, Long userId);
}
