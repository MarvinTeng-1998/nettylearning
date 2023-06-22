package com.marvin.server.handler;

import com.marvin.message.GroupCreateRequestMessage;
import com.marvin.message.GroupCreateResponseMessage;
import com.marvin.server.session.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Set;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-22 17:05
 **/
@ChannelHandler.Sharable
public class GroupCreateRequestMessageHandler extends SimpleChannelInboundHandler<GroupCreateRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupCreateRequestMessage msg) throws Exception {
        String groupName = msg.getGroupName();
        Set<String> members = msg.getMembers();

        GroupSession groupSession = GroupSessionFactory.getGroupSession();
        Group group = groupSession.createGroup(groupName, members);

        if (group == null) { // success
            // 发送拉群消息
            for (String s : members) {
                for (Channel channel : groupSession.getMembersChannel(groupName)) {
                    System.out.println(111);
                    channel.writeAndFlush(new GroupCreateResponseMessage(true, "您已经被拉入" + groupName));
                }
            }
            // 发送成功消息
            ctx.writeAndFlush(new GroupCreateResponseMessage(true, groupName + "创建成功"));
        } else {
            ctx.writeAndFlush(new GroupCreateResponseMessage(false, groupName + "群已经存在！"));
        }

    }
}
