package com.marvin.netty.channel;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-19 18:21
 **/
@Slf4j
public class ChannelClient {
    public static void main(String[] args) throws InterruptedException {
        // 带有Future或是Promise的类型都是和异步方法配套使用的，用来处理结果。
        ChannelFuture channelFuture = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new StringEncoder());
                    }
                })
                // 1. 连接到服务器
                // connect的方法是异步非阻塞的，调用connect的线程指派给另一个线程去做，不管你的执行情况。然后继续向下运行不需要等结果
                .connect(new InetSocketAddress("localhost", 8080));

        // 如果不调用sync的话，那连接还没完成，主线程还在执行，则下面的指令会直接获取到channel，然后进行发数据，但是这个时候channel还没有建立出来。
        // 2.1 调用sync，方法同步处理结果。阻塞住当前线程，直到nio连接建立完毕。
        // channelFuture.sync();
        // Channel channel = channelFuture.channel();
        // channel.writeAndFlush("hello,world!");

        // 2.2 使用addListener(回调对象)可以异步处理结果
        channelFuture.addListener(new ChannelFutureListener() {
            // 在NIO线程连接建立好之后，会调用这个operationComplete方法，这个时候去拿channel和处理逻辑
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                Channel channel = future.channel();
                log.debug("{}", channel);
                channel.writeAndFlush("hello async!");
            }
        });
    }
}
