package com.whisky.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whisky.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.whisky.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.whisky.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.whisky.yupicturebackend.model.entity.Space;
import com.whisky.yupicturebackend.model.entity.User;
import com.whisky.yupicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author Lenovo
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-05-15 21:56:06
*/
public interface SpaceService extends IService<Space> {

    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    void validSpace(Space space, boolean add);

    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    void fillSpaceBySpaceLevel(Space space);

    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    void checkSpaceAuth(User loginUser, Space space);
}
