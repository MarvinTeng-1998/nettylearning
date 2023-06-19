package com.marvin.netty.futureandpromise;

import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-20 01:32
 **/
@Slf4j
public class NettyPromise {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        NioEventLoopGroup group = new NioEventLoopGroup(2);
        EventLoop eventLoop = group.next();
        // 1.可以主动创建promise,结果容器
        DefaultPromise<Integer> promise = new DefaultPromise<>(eventLoop);
        new Thread( () -> {
            // 2.任意一个线程开始计算，完毕后向promise填充结果。
            log.debug("开始计算");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            promise.setSuccess(80);
        }).start();

        // 3.设置一个接受结果的线程
        log.debug("结果是:{}",promise.get());
    }
}
