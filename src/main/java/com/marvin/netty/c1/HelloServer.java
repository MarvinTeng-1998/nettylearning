package com.marvin.netty.c1;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-19 16:08
 **/
public class HelloServer {
    public static void main(String[] args) {
        // ServerBootstrap: 服务器端的启动器，负责组装netty组件，启动服务器
        new ServerBootstrap()
                // 添加NioEventLoopGroup，BossEventLoop(Selector)用来创建SocketChannel通道,WorkerEventLoop(Selector,thread)用来监听读写事件。
                // group组，加入了多个EventLoop
                .group(new NioEventLoopGroup())
                // 选择一个ServerSocketChannel的实现：NIO、OIO(BIO)、Linux -> epoll
                .channel(NioServerSocketChannel.class)
                // boss负责处理连接的，worker负责处理读写的，决定了将来的worker来干哪些事情。
                .childHandler(
                        // channel代表和客户端进行读写的通道Initializer初始化，负责添加别的handler
                        new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        // 添加具体的handler，StringDecoder用来bytebuf转换成字符串，解码器
                        nioSocketChannel.pipeline().addLast(new StringDecoder());
                        nioSocketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter(){ // 自定义Handler
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                System.out.println(msg);
                            }
                        });
                    }
                })
                // 绑定监听端口
                .bind(8080);
    }
}
