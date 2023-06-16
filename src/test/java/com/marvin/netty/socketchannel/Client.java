package com.marvin.netty.socketchannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-15 16:55
 **/
public class Client {
    public static void main(String[] args) throws IOException {
        SocketChannel sc = SocketChannel.open();
        sc.connect(new InetSocketAddress("localhost", 8080));
        SocketAddress address = sc.getLocalAddress();
        sc.write(Charset.defaultCharset().encode("0123456789he"));
        System.in.read();
    }
}
