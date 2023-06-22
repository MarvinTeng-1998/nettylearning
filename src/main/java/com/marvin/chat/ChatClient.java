package com.marvin.chat;

import com.marvin.message.*;
import com.marvin.nettyadvanced.protocol.MessageCodec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
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
        // LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        // 线程之间通信，如果返回登陆成功则减1
        CountDownLatch WAIT_FOR_LOGGING = new CountDownLatch(1);
        // 如果登陆成功则为true
        AtomicBoolean LOGIN = new AtomicBoolean(false);
        AtomicBoolean EXIT = new AtomicBoolean(false);
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024,12,4,0,0));
                    ch.pipeline().addLast(new MessageCodec());
                    // 3s内如果没有向服务端发数据，则触发写空闲事件。
                    ch.pipeline().addLast(new IdleStateHandler(0,3,0));
                    ch.pipeline().addLast(new ChannelDuplexHandler(){
                        @Override
                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                            IdleStateEvent event = (IdleStateEvent) evt;
                            if(event.state() == IdleState.WRITER_IDLE){
                                log.debug("已经3s没有写数据了，发送一个心跳包");
                                ctx.writeAndFlush(new PingMessage());
                            }
                        }
                    });
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
                                if(EXIT.get()){
                                    return;
                                }
                                System.out.println("请输入密码:");
                                String password = scanner.nextLine();
                                if(EXIT.get()){
                                    return;
                                }

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
                                    String command = null;
                                    try {
                                        command = scanner.nextLine();
                                    } catch (Exception e) {
                                        break;
                                    }
                                    if(EXIT.get()){
                                        return;
                                    }
                                    String[] s = command.split(" ");
                                    switch (s[0]){
                                        case "send":
                                            ChatRequestMessage chatRequestMessage = new ChatRequestMessage(username, s[1], s[2]);
                                            ctx.writeAndFlush(chatRequestMessage);
                                            break;
                                        case "gsend":
                                            GroupChatRequestMessage groupChatRequestMessage = new GroupChatRequestMessage(username, s[1], s[2]);
                                            ctx.writeAndFlush(groupChatRequestMessage);
                                            break;
                                        case "gcreate":
                                            String[] split = s[2].split(",");
                                            Set<String> set = new HashSet<>(Arrays.asList(split));
                                            set.add(username); // 加入自己
                                            GroupCreateRequestMessage groupCreateRequestMessage = new GroupCreateRequestMessage(s[1], set);
                                            ctx.writeAndFlush(groupCreateRequestMessage);
                                            break;
                                        case "gmembers":
                                            ctx.writeAndFlush(new GroupMembersRequestMessage(s[1]));
                                            break;
                                        case "gjoin":
                                            ctx.writeAndFlush(new GroupJoinRequestMessage(username,s[1]));
                                            break;
                                        case "gquit":
                                            ctx.writeAndFlush(new GroupQuitRequestMessage(username,s[1]));
                                            break;
                                        case "quit":
                                            ctx.channel().close();
                                            return;
                                    }
                                }


                            },"System in").start();
                            super.channelActive(ctx);
                        }

                        // 在连接断开时触发
                        @Override
                        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                            log.debug("连接已经断开，按任意键退出..");
                            EXIT.set(true);
                        }

                        // 在出现异常时触发
                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                            log.debug("连接已经断开，按任意键退出..{}", cause.getMessage());
                            EXIT.set(true);
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
