package com.marvin.server;

import com.marvin.nettyadvanced.protocol.MessageCodec;
import com.marvin.server.handler.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @TODO: 聊天室服务器
 * @author: dengbin
 * @create: 2023-06-21 21:35
 **/
@Slf4j
public class ChatServer {

    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        LoginRequestMessageHandler LOGIN_HANDLER = new LoginRequestMessageHandler();
        ChatRequestMessageHandler CHAT_HANDLER = new ChatRequestMessageHandler();
        GroupCreateRequestMessageHandler GROUP_CREATE = new GroupCreateRequestMessageHandler();
        GroupJoinRequestMessageHandler GROUP_JOIN = new GroupJoinRequestMessageHandler();
        GroupChatRequestMessageHandler GROUP_CHAT = new GroupChatRequestMessageHandler();
        GroupMembersRequestMessageHandler GROUP_MEMBER = new GroupMembersRequestMessageHandler();
        GroupQuitRequestMemberHandler GROUP_QUIT = new GroupQuitRequestMemberHandler();
        QuitHandler QUIT = new QuitHandler();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(boss, worker);
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    // 用来是不是 读空闲时间过长 或者是 写空闲时间过长
                    // 5s内如果没有收到channel的数据，就触发一个IdleState#READER_FILE 事件，
                    ch.pipeline().addLast(new IdleStateHandler(5,0,0));
                    // ChannelDuplexHandler可以同时作为入站处理器和出站处理器
                    ch.pipeline().addLast(new ChannelDuplexHandler(){
                        // 用来触发特殊事件
                        @Override
                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                            if (evt instanceof IdleStateEvent) {
                                IdleStateEvent event = (IdleStateEvent) evt;
                                // 触发了读空闲事件
                                if (event.state() == IdleState.READER_IDLE) {
                                    log.debug("读空闲已经超过了5s");
                                    ctx.channel().close();
                                }
                            }
                            super.userEventTriggered(ctx, evt);
                        }
                    });
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 12, 4, 0, 0));
                    ch.pipeline().addLast(LOGGING_HANDLER);
                    ch.pipeline().addLast(new MessageCodec());
                    // 这是只对LoginMessage感兴趣，目前只处理LoginMessage消息进行处理
                    ch.pipeline().addLast(LOGIN_HANDLER);
                    ch.pipeline().addLast(CHAT_HANDLER);
                    ch.pipeline().addLast(GROUP_CREATE);
                    ch.pipeline().addLast(GROUP_CHAT);
                    ch.pipeline().addLast(GROUP_JOIN);
                    ch.pipeline().addLast(GROUP_MEMBER);
                    ch.pipeline().addLast(GROUP_QUIT);
                    ch.pipeline().addLast(QUIT);
                }
            });
            Channel channel = serverBootstrap.bind(new InetSocketAddress("localhost", 8080)).sync().channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e ) {
            log.error("server error", e);
        } catch(NullPointerException e) {
            log.error("channel为空",e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

}
