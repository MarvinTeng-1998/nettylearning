package com.marvin.server.handler;

import com.marvin.server.session.SessionFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-22 21:17
 **/
@Slf4j
@ChannelHandler.Sharable
public class QuitHandler extends ChannelInboundHandlerAdapter {
    // 当连接断开的时候触发inactive事件
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        SessionFactory.getSession().unbind(ctx.channel());
        log.debug("{} 已经断开", ctx.channel());

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        SessionFactory.getSession().unbind(ctx.channel());
        log.debug("{} 已经异常断开，异常是{}",ctx.channel(), cause);

    }
}
