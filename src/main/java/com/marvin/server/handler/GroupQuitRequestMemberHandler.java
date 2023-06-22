package com.marvin.server.handler;

import com.marvin.message.GroupQuitRequestMessage;
import com.marvin.message.GroupQuitResponseMessage;
import com.marvin.server.session.Group;
import com.marvin.server.session.GroupSessionFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-22 21:29
 **/
@ChannelHandler.Sharable
public class GroupQuitRequestMemberHandler extends SimpleChannelInboundHandler<GroupQuitRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupQuitRequestMessage msg) throws Exception {
        Group group = GroupSessionFactory.getGroupSession().removeMember(msg.getGroupName(), msg.getUsername());
        if(group != null){
            ctx.writeAndFlush(new GroupQuitResponseMessage(true,"已经退出群聊" + msg.getGroupName()));
        }else{
            ctx.writeAndFlush(new GroupQuitResponseMessage(false, msg.getGroupName() + "群不存在"));
        }
    }
}
