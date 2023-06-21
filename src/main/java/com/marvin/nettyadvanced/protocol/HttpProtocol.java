package com.marvin.nettyadvanced.protocol;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-21 16:05
 **/
@Slf4j
public class HttpProtocol {
    public static final NioEventLoopGroup boss = new NioEventLoopGroup();
    public static final NioEventLoopGroup worker = new NioEventLoopGroup();

    public static void main(String[] args) {
        try {
            ChannelFuture localhost = new ServerBootstrap()
                    .channel(NioServerSocketChannel.class)
                    .group(boss, worker)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                            ch.pipeline().addLast(new HttpServerCodec());
                            // SimpleChannelInboundHandler 根据消息的类型来进行处理消息
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<HttpRequest>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {
                                    log.debug(msg.uri());

                                    // 返回响应
                                    DefaultFullHttpResponse response =
                                            new DefaultFullHttpResponse(msg.protocolVersion(),HttpResponseStatus.OK);
                                    response.content().writeBytes("<h1>Hello,World!</h1>".getBytes());
                                    response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH , "<h1>Hello,World!</h1>".length());
                                    ctx.writeAndFlush(response);
                                    ctx.fireChannelRead(msg);
                                }
                            });
                            /* ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                    log.debug("{}", msg.getClass());
                                    if (msg instanceof HttpRequest) {

                                    } else if (msg instanceof HttpContent) {

                                    }
                                }
                            }); */
                        }
                    }).bind("localhost", 8080).sync();
            localhost.channel().closeFuture().sync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
