package com.sunshine.usercenterbackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sunshine.usercenterbackend.common.BaseResponse;
import com.sunshine.usercenterbackend.common.ErrorCode;
import com.sunshine.usercenterbackend.common.ResultUtils;
import com.sunshine.usercenterbackend.exception.BusinessException;
import com.sunshine.usercenterbackend.model.domain.User;
import com.sunshine.usercenterbackend.model.domain.request.UserLoginRequest;
import com.sunshine.usercenterbackend.model.domain.request.UserRegisterRequest;
import com.sunshine.usercenterbackend.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.sunshine.usercenterbackend.constant.UserConstant.ADMIN_ROLE;
import static com.sunshine.usercenterbackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount,userPassword,checkPassword,planetCode)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long result = userService.userRegister(userAccount, userPassword, checkPassword,planetCode);
        return ResultUtils.success(result);

    }

    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);

    }

    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout( HttpServletRequest request){
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        int result = userService.userLogout(request);
        return ResultUtils.success(result);

    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request){
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User)userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long userId = currentUser.getId();
        //todo: 检验用户是否合法
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request){
        //添加校验，仅管理员可查询
        if (!isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username",username);
        }

        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);

    }


    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUsers(@RequestBody long id, HttpServletRequest request){
        //添加校验，仅管理员可删除
        if (!isAdmin(request)){
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        if (id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }


    /**
     *  是否为管理员
     * @param request
     * @return
     */
    private boolean isAdmin(HttpServletRequest request){
        //添加校验，仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }


}
