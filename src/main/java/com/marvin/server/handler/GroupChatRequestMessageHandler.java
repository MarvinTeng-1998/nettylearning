package com.marvin.server.handler;

import com.marvin.message.GroupChatRequestMessage;
import com.marvin.message.GroupChatResponseMessage;
import com.marvin.server.session.GroupSessionFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-22 17:04
 **/
@ChannelHandler.Sharable
public class GroupChatRequestMessageHandler extends SimpleChannelInboundHandler<GroupChatRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupChatRequestMessage msg) throws Exception {
        List<Channel> membersChannel = GroupSessionFactory.getGroupSession()
                .getMembersChannel(msg.getGroupName());

        for(Channel channel : membersChannel){
            channel.writeAndFlush(new GroupChatResponseMessage(msg.getFrom(),msg.getContent()));
        }
    }
}
