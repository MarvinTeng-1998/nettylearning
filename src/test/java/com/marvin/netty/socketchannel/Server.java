package com.marvin.netty.socketchannel;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import static com.marvin.netty.bytebuffer.ByteBufferUtil.debugRead;

/**
 * @TODO: 非阻塞：线程还会继续运行，如果没有连接建立，sc返回的是null
 * 阻塞：线程在accept方法、read方法中都会阻塞，只能等这个事情处理完了再去处理别的
 * @author: dengbin
 * @create: 2023-06-15 16:48
 **/
@Slf4j
public class Server {
    public static void main(String[] args) throws IOException {
        // 使用NIO来理解阻塞模式,单线程
        ByteBuffer buffer = ByteBuffer.allocate(16);
        // 1.创建了服务器
        ServerSocketChannel ssc = ServerSocketChannel.open();
        // ServerSocketChannel改成非阻塞模式的
        ssc.configureBlocking(false);
        // 2.绑定一个监听端口
        ssc.bind(new InetSocketAddress(8080));
        // 3.连接集合
        List<SocketChannel> channels = new ArrayList<>();
        while (true) {
            // 4.建立客户端连接,其中SocketChannel用来跟客户端之间通信
            SocketChannel sc = ssc.accept();
            if (sc != null) {
                log.debug("connected...{}", sc);
                sc.configureBlocking(false);// 将socketChannel设置为非阻塞的
                channels.add(sc);
            }
            // 5.接受客户端发送的数据
            for (SocketChannel channel : channels) {
                /*
                    阻塞模式下：
                        线程在这里停止，这也是一个阻塞方法，会让线程停止运行。
                        等待客户端发送数据。
                    非阻塞模式下：
                        如果没有得到数据，返回为0
                 */
                int read = channel.read(buffer);
                if (read > 0) {
                    buffer.flip();
                    debugRead(buffer);
                    buffer.clear();
                    log.debug("after read...{}", channel);
                }
            }

        }
    }
}
