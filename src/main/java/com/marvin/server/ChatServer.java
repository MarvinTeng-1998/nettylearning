package com.marvin.server;

import com.marvin.message.LoginRequestMessage;
import com.marvin.message.LoginResponseMessage;
import com.marvin.nettyadvanced.protocol.MessageCodec;
import com.marvin.server.service.UserServiceFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
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
        try{
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(boss,worker);
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024,12,4,0,0));
                    ch.pipeline().addLast(LOGGING_HANDLER);
                    ch.pipeline().addLast(new MessageCodec());
                    // 这是只对LoginMessage感兴趣，目前只处理LoginMessage消息进行处理
                    ch.pipeline().addLast(new SimpleChannelInboundHandler<LoginRequestMessage>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, LoginRequestMessage msg) throws Exception {
                            String username = msg.getUsername();
                            String password = msg.getPassword();
                            LoginResponseMessage message;

                            boolean login = UserServiceFactory.getUserService().login(username, password);
                            if(login){
                                 message = new LoginResponseMessage(true, "登陆成功");
                            }else{
                                 message = new LoginResponseMessage(false, "登陆失败，用户或密码不正确");
                            }
                            ctx.writeAndFlush(message);
                        }
                    });
                }
            });
            Channel channel = serverBootstrap.bind(new InetSocketAddress("localhost", 8080)).sync().channel();
            // ...
            channel.closeFuture().sync();
        }catch (InterruptedException e) {
            log.error("server error",e);
        }finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
