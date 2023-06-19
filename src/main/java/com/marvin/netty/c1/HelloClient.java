package com.marvin.netty.c1;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

import java.net.InetSocketAddress;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-19 16:31
 **/
public class HelloClient {
    public static void main(String[] args) throws InterruptedException {
        // 1.启动类
        new Bootstrap()
                // 2.添加选择器
                .group(new NioEventLoopGroup())
                // 3.选择客户端的Channel实现
                .channel(NioSocketChannel.class)
                // 4.添加处理器
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override // 在连接建立后被调用
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline().addLast(new StringEncoder());
                    }
                })
                // 5.连接服务器
                .connect(new InetSocketAddress("localhost",8080))
                // 阻塞方法，直到连接建立
                // netty很多方法都是异步的，因此需要使用sync等待结果然后再执行。
                .sync()
                // 代表客户端和服务器端之间的SocketChannel
                .channel()
                // 给服务器发送数据
                .writeAndFlush("Hello World!");
    }
}
