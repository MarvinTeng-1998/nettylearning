package com.marvin.server.handler;

import com.marvin.message.ChatRequestMessage;
import com.marvin.message.ChatResponseMessage;
import com.marvin.server.session.SessionFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-22 16:38
 **/
@ChannelHandler.Sharable
public class ChatRequestMessageHandler extends SimpleChannelInboundHandler<ChatRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatRequestMessage msg) throws Exception {
        String to = msg.getTo();

        Channel channel = SessionFactory.getSession().getChannel(to);
        if(channel != null){ // online
            channel.writeAndFlush(new ChatResponseMessage(msg.getFrom(),msg.getContent()));
        }else{ // offline
            ctx.writeAndFlush(new ChatResponseMessage(false,"对方不存在或者不在线"));
        }
    }
}
