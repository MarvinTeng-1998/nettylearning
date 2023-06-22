package com.marvin.server.handler;

import com.marvin.message.LoginRequestMessage;
import com.marvin.message.LoginResponseMessage;
import com.marvin.server.service.UserServiceFactory;
import com.marvin.server.session.SessionFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-22 16:37
 **/
@ChannelHandler.Sharable
public class LoginRequestMessageHandler extends SimpleChannelInboundHandler<LoginRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginRequestMessage msg) throws Exception {
        String username = msg.getUsername();
        String password = msg.getPassword();
        LoginResponseMessage message;

        boolean login = UserServiceFactory.getUserService().login(username, password);
        if (login) {
            SessionFactory.getSession().bind(ctx.channel(), username);
            message = new LoginResponseMessage(true, "登陆成功");
        } else {
            message = new LoginResponseMessage(false, "登陆失败，用户或密码不正确");
        }
        ctx.writeAndFlush(message);
    }
}
