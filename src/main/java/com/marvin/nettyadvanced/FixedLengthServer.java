package com.marvin.nettyadvanced;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-21 15:29
 **/
@Slf4j
public class FixedLengthServer {
    public static final NioEventLoopGroup boss = new NioEventLoopGroup();
    public static final NioEventLoopGroup worker = new NioEventLoopGroup();
    public static void main(String[] args) throws InterruptedException {
        new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .group(boss,worker)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new FixedLengthFrameDecoder(30));
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                log.debug("connected: {}" , ctx.channel());
                                super.channelActive(ctx);
                            }
                        });
                    }
                }).bind("localhost",8080).sync();
    }
}
