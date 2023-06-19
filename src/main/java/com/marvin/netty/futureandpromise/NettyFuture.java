package com.marvin.netty.futureandpromise;

import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-20 01:27
 **/
@Slf4j
public class NettyFuture {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup(2);
        EventLoop eventLoop = group.next();
        log.debug("开始计算");
        Future<Integer> future = eventLoop.submit(() -> {
            Thread.sleep(1000);
            return 70;
        });
        // log.debug("结果是{}", future.get()); // 这里还是阻塞的
        future.addListener(new GenericFutureListener<Future<? super Integer>>() {
            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                log.debug("接受结果：{}",future.getNow());
            }
        });
    }
}
