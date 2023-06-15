package com.marvin.netty.socketchannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-15 16:55
 **/
public class Client {
    public static void main(String[] args) throws IOException {
        SocketChannel sc= SocketChannel.open();
        sc.connect(new InetSocketAddress("localhost",8080));
        System.out.println("waiting.....");
    }
}
