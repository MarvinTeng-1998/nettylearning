package com.marvin.netty.socketchannel;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.marvin.netty.bytebuffer.ByteBufferUtil.debugAll;
import static com.marvin.netty.bytebuffer.ByteBufferUtil.debugRead;

/**
 * @TODO: SelectionKey有4种事件：
 * 1. accept 有连接请求的时候触发
 * 2. connect 是客户端，连接建立以后触发的事件
 * 3. read 客户端发了数据，表示可读事件
 * 4. write 表示可写事件
 * <p>
 * <p>
 * SelectionKey每次被处理完成后要删除这个key，否则会报空指针异常。
 * @author: dengbin
 * @create: 2023-06-15 16:48
 **/
@Slf4j
public class ServerSelector {
    public static void split(ByteBuffer source) {
        // 变成一个读模式
        source.flip();
        for (int i = 0; i < source.limit(); i++) {
            // 找到完整信息
            if (source.get(i) == '\n') {
                int length = i + 1 - source.position();
                ByteBuffer target = ByteBuffer.allocate(length);
                // 从source去读，向targe去写
                for (int j = 0; j < length; j++) {
                    target.put(source.get());
                }
                debugAll(target);
            }
        }
        // 变成一个写模式。
        source.compact();

    }

    public static void main(String[] args) throws IOException {
        // 1. 创建Selector,用来管理多个channel
        Selector selector = Selector.open();

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);

        // 2. 建立Selector和Channel之间的联系
        // SelectionKey就是将来事件发生后，通过它知道事件是什么、哪个channel的事件 0表示不关注任何事件
        SelectionKey sscKey = ssc.register(selector, 0, null);
        // 只关注accept事件
        sscKey.interestOps(SelectionKey.OP_ACCEPT);
        log.debug("register Key:{}", sscKey);

        ssc.bind(new InetSocketAddress(8080));
        while (true) {
            // 3. Selector的select方法(),没有事件就线程堵塞，有事件就向下执行，线程恢复运行
            // 在事件未处理的时候，不会阻塞。会一直丢在集合中，然后一直让处理。如果处理了就进入阻塞，等待新的事件
            selector.select();
            // 4. 处理事件 拿到事件集合
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); // accept,read
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                log.debug("key:{}", key);
                // 5.区分事件类型
                if (key.isAcceptable()) {// 处理accept事件
                    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                    SocketChannel sc = channel.accept();
                    sc.configureBlocking(false);
                    ByteBuffer buffer = ByteBuffer.allocate(16);
                    SelectionKey scKey = sc.register(selector, 0, buffer); // 将ByteBuffer作为一个附件关联到selectionKey中
                    scKey.interestOps(SelectionKey.OP_READ);
                    log.debug("连接建立，ServerSocket是:{}", sc);
                } else if (key.isReadable()) {// 处理read事件
                    try {
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer)key.attachment(); // 拿到附件
                        // 如果是正常断开，read的返回值是-1
                        int read = channel.read(buffer);
                        if (read == -1) {
                            key.cancel();
                        } else {
                            split(buffer);
                            if(buffer.position() == buffer.limit()){
                                ByteBuffer newByteBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
                                // 变成读模式，因为要给newByteBuffer去读。
                                buffer.flip();
                                newByteBuffer.put(buffer);
                                key.attach(newByteBuffer);
                            }
                            debugAll(buffer);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        // 在客户端断开之后，通过cancel将这个key给反注册了，不让它继续运行了。
                        key.cancel();
                    }
                }
            }
        }
    }
}
