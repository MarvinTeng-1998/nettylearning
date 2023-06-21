package com.marvin.server.service;

/**
 * @TODO: 用户管理接口
 * @author: dengbin
 * @create: 2023-06-21 21:24
 **/
public interface UserService {
    
    /*
     * @Description: TODO 登陆
     * @Author: dengbin
     * @Date: 21/6/23 21:25
     * @param username:  用户名
     * @param password:  密码
     * @return: boolean 登陆成功返回true，失败返回false
     **/
    boolean login(String username, String password);
}
