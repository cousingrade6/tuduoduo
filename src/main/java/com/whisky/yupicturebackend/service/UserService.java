package com.whisky.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.whisky.yupicturebackend.model.dto.user.UserQueryRequest;
import com.whisky.yupicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.whisky.yupicturebackend.model.vo.LoginUserVO;
import com.whisky.yupicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Lenovo
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-05-07 21:37:22
*/
public interface UserService extends IService<User> {

    /**
     *
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    boolean userLogout(HttpServletRequest request);

    /**
     * 密码加密
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    User getLoginUser(HttpServletRequest request);

    /**
     * 获得脱敏后的用户信息
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    boolean isAdmin(User user);

    boolean exchangeVip(User user, String vipCode);
}
