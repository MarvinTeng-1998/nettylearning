package com.marvin.server.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-21 21:25
 **/
public class UserServiceMemoryImpl implements UserService{
    private final Map<String,String> allUserMap = new ConcurrentHashMap<>();
    {
        allUserMap.put("zhangsan","123");
        allUserMap.put("lisi","123");
        allUserMap.put("wangwu","123");
        allUserMap.put("zhaoliu","123");
        allUserMap.put("qianqi","123");
    }
    @Override
    public boolean login(String username, String password) {
        String pass = allUserMap.get(username);
        if(pass == null){
            return false;
        }
        return pass.equals(password);
    }
}
