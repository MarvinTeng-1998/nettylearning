package com.marvin.netty.c2;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.NettyRuntime;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-19 17:15
 **/
@Slf4j
public class TestEventLoop {
    public static void main(String[] args) {
        // 1.创建事件循环组 可以指定线程数
        EventLoopGroup group = new NioEventLoopGroup(2); // 处理IO事件，也能提交普通任务，也能提交定时任务
        EventLoopGroup group1 = new DefaultEventLoopGroup(); // 普通任务，定时任务，不能处理IO事件

        // 2.获取下一个事件循环对象
        System.out.println(group.next());
        System.out.println(group.next());
        System.out.println(group.next());

        // 3.执行普通任务
        group.next().submit(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.debug("ok");
        });

        // 4.执行定时任务
        group.next().scheduleAtFixedRate(() -> {
            log.debug("定时任务");
        },0,1, TimeUnit.SECONDS);

        // 5.

        log.debug("main");
    }
}
