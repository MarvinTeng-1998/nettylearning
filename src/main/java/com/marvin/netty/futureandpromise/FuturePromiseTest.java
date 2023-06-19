package com.marvin.netty.futureandpromise;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-20 01:21
 **/
@Slf4j
public class FuturePromiseTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 线程池
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        log.debug("任务开始");
        // 提交任务
        Future<Integer> future = executorService.submit(() -> {
            Thread.sleep(1000);
            return 50;
        });
        log.debug("{}",future.get());
    }
}
