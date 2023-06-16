package com.marvin.netty.socketchannel;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.marvin.netty.bytebuffer.ByteBufferUtil.debugAll;

/**
 * @TODO:多线程下的多路复用服务器
 * 多个线程下存在几个问题：
 * 1.worker的新建线程之后，还需要register到对应的SocketChannel中。但是存在一个问题：就是new完之后，worker线程在select的地方进行了阻塞。但是主线程又需要register
 * 2.这里我们可以设计一个消息队列，这个消息队列我们可以把要执行的注册操作放到worker线程中进行。也就是放入到队列中。
 * 3.放入完队列后我们使用wakeup去唤醒这个selector，从而这个selector去队列中取出来任务，从而去执行register
 * 4.这样就彻底绑定SocketServer了。
 * @author: dengbin
 * @create: 2023-06-16 15:25
 **/
@Slf4j
public class MultiThreadServer {
    public static void main(String[] args) throws IOException {
        Thread.currentThread().setName("boss");
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        Selector boss = Selector.open();
        SelectionKey bossKey = ssc.register(boss, 0, null);
        bossKey.interestOps(SelectionKey.OP_ACCEPT);
        ssc.bind(new InetSocketAddress(8080));
        Worker[] workers = new Worker[Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker("worker-" + i);
        }
        AtomicInteger index = new AtomicInteger();
        while (true) {
            boss.select();
            Iterator<SelectionKey> iterator = boss.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isAcceptable()) {
                    SocketChannel sc = ssc.accept();
                    log.debug("建立了连接，连接channel是:{}", sc);
                    sc.configureBlocking(false);
                    log.debug("Reading-worker注册前，连接是：{}", sc.getRemoteAddress());
                    // polling 轮询
                    workers[index.getAndIncrement() % workers.length].register(sc);
                    log.debug("Reading-worker注册后，连接是：{}", sc.getRemoteAddress());
                }
            }
        }
    }

    static class Worker implements Runnable {
        private Thread thread;
        private Selector selector;
        public String name;
        private volatile boolean start = false; // 还没初始化
        private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

        public Worker(String name) {
            this.name = name;
        }

        /*
         * @Description: TODO 初始化线程和selector
         * @Author: dengbin
         * @Date: 16/6/23 15:39
         * @return: void
         **/
        public void register(SocketChannel sc) throws IOException {
            if (!start) {
                thread = new Thread(this);
                thread.start();
                selector = Selector.open();
                start = true;
            }
            // 向队列添加了任务，但是任务没有立刻执行
            queue.add(() -> {
                try {
                    sc.register(selector, SelectionKey.OP_READ, null);
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                }
            });
            selector.wakeup(); // 唤醒selector
        }

        @Override
        public void run() {
            while (true) {
                try {
                    selector.select();  // 线程阻塞住了，
                    Runnable task = queue.poll();
                    if (task != null) task.run(); // 执行了register事件，是worker线程进行注册的。
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isReadable()) {
                            ByteBuffer buffer = ByteBuffer.allocate(16);
                            SocketChannel sc = (SocketChannel) key.channel();
                            log.debug("Read...有数据进来了");
                            int read = sc.read(buffer);
                            buffer.flip();
                            debugAll(buffer);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}


