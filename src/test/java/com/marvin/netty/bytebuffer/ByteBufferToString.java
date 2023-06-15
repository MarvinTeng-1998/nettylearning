package com.marvin.netty.bytebuffer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static com.marvin.netty.bytebuffer.ByteBufferUtil.debugAll;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-15 14:46
 **/
public class ByteBufferToString {
    public static void main(String[] args) {
        // 1.字符串转ByteBuffer
        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.put("hello".getBytes());
        debugAll(buffer);

        // 2.字符串转ByteBuffer --> CharSet 默认将buffer从写模式转为读模式
        ByteBuffer buffer1 = StandardCharsets.UTF_8.encode("hello");
        debugAll(buffer1);

        // 3.wrap
        ByteBuffer wrap = ByteBuffer.wrap("hello".getBytes());
        debugAll(wrap);

        // 1.bytebuffer转string
        String string = StandardCharsets.UTF_8.decode(buffer1).toString();
        System.out.println(string);

    }
}
