package com.marvin.server.session;

import io.netty.channel.Channel;

/**
 * @program: nettylearning
 * @description: 会话接口
 * @author:Marvin
 * @create: 2023-06-21 21:27
 **/
public interface Session {

    
    /*
     * @Description: TODO 绑定会话
     * @Author: dengbin
     * @Date: 21/6/23 21:28
     * @param channel: 哪个Channel要绑定会话
     * @param username: 会话绑定用户
     * @return: void
     **/
    void bind(Channel channel, String username);

    /*
     * @Description: TODO 解绑会话
     * @Author: dengbin
     * @Date: 21/6/23 21:29
     * @param channel: 哪个会话要解绑
     * @return: void
     **/
    void unbind(Channel channel);

    /*
     * @Description: TODO 获取属性
     * @Author: dengbin
     * @Date: 21/6/23 21:29
     * @param channel:
     * @param username:
     * @return: java.lang.Object
     **/
    Object getAttribute(Channel channel,String username);

    /*
     * @Description: TODO 设置属性
     * @Author: dengbin
     * @Date: 21/6/23 21:30
     * @param channel: 
     * @param username: 
     * @param value: 
     * @return: void
     **/
    void setAttribute(Channel channel, String username,Object value);

    /*
     * @Description: TODO 根据用户名获取Channel
     * @Author: dengbin
     * @Date: 21/6/23 21:30
     * @param username:
     * @return: io.netty.channel.Channel
     **/
    Channel getChannel(String username);
}
