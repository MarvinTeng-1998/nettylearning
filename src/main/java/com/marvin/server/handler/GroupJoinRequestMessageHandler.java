package com.marvin.server.handler;

import com.marvin.message.GroupJoinRequestMessage;
import com.marvin.message.GroupJoinResponseMessage;
import com.marvin.server.session.Group;
import com.marvin.server.session.GroupSessionFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-22 21:21
 **/
@ChannelHandler.Sharable
public class GroupJoinRequestMessageHandler extends SimpleChannelInboundHandler<GroupJoinRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupJoinRequestMessage msg) throws Exception {
        Group group = GroupSessionFactory.getGroupSession().joinMember(msg.getGroupName(), msg.getUsername());
        if(group != null){
            ctx.writeAndFlush(new GroupJoinResponseMessage(true,msg.getGroupName() + "群加入成功！"));
        }else{
            ctx.writeAndFlush(new GroupJoinResponseMessage(false, msg.getGroupName() + "群加入失败！"));
        }
    }
}
