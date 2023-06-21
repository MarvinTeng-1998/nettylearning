package com.marvin.chat;

import com.marvin.message.LoginRequestMessage;
import com.marvin.message.LoginResponseMessage;
import com.marvin.nettyadvanced.protocol.MessageCodec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-21 21:40
 **/
@Slf4j
public class ChatClient {
    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        // 线程之间通信，如果返回登陆成功则减1
        CountDownLatch WAIT_FOR_LOGGING = new CountDownLatch(1);
        // 如果登陆成功则为true
        AtomicBoolean LOGIN = new AtomicBoolean(false);
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024,12,4,0,0));
                    ch.pipeline().addLast(LOGGING_HANDLER);
                    ch.pipeline().addLast(new MessageCodec());
                    ch.pipeline().addLast("channel handler",new ChannelInboundHandlerAdapter(){

                        // 接受响应事件
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            log.debug("msg:{}",msg);
                            if(msg instanceof LoginResponseMessage){
                                LoginResponseMessage responseMessage = (LoginResponseMessage) msg;
                                if (responseMessage.isSuccess()) {
                                    // 如果登陆成功
                                    LOGIN.set(true);
                                }
                                // 唤醒System.in线程
                                WAIT_FOR_LOGGING.countDown();
                            }
                        }

                        // 在连接后会触发Active事件
                        @Override
                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                            // 负责接受用户在控制台的输入，负责向服务器发送消息
                            new Thread(() -> {
                                Scanner scanner = new Scanner(System.in);
                                System.out.println("请输入用户名字:");
                                String username = scanner.nextLine();
                                System.out.println("请输入密码:");
                                String password = scanner.nextLine();

                                // 构建消息对象
                                LoginRequestMessage loginRequestMessage = new LoginRequestMessage(username, password);
                                ctx.writeAndFlush(loginRequestMessage);

                                System.out.println("等待");
                                try {
                                    WAIT_FOR_LOGGING.await();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if(!LOGIN.get()){
                                    ctx.channel().close();
                                    return;
                                }
                                while(true){
                                    System.out.println("==================================");
                                    System.out.println("send [username] [content]");
                                    System.out.println("gsend [group name] [content]");
                                    System.out.println("gcreate [group name] [m1,m2,m3...]");
                                    System.out.println("gmembers [group name]");
                                    System.out.println("gjoin [group name]");
                                    System.out.println("gquit [group name]");
                                    System.out.println("quit");
                                    System.out.println("==================================");
                                }

                            },"System in").start();
                            super.channelActive(ctx);
                        }
                    });
                }
            });
            Channel localhost = bootstrap.connect("localhost", 8080).sync().channel();
            localhost.closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("client error",e);
        }finally {
            group.shutdownGracefully();
        }
    }
}
