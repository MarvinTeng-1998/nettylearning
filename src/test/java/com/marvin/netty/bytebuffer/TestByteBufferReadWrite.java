package com.marvin.netty.bytebuffer;

import java.nio.ByteBuffer;

import static com.marvin.netty.bytebuffer.ByteBufferUtil.debugAll;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-14 17:13
 **/
public class TestByteBufferReadWrite {
    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.put((byte) 0x61); // 'a'
        buffer.put(new byte[]{0x62,0x63,0x64});
        debugAll(buffer);

        // System.out.println(buffer.get());// 这里读的是0，需要切换成读模式。这个时候还是写模式。
        buffer.flip();
        System.out.println((char)buffer.get());
        System.out.println((char)buffer.get());
        debugAll(buffer);

        // 这里的compact主要是用来把数据往前拉，然后position指针指向的是下一个写位置。
        // 这里的compact把buffer改成了写模式
        buffer.compact();
        debugAll(buffer);
        buffer.put(new byte[]{1,2});
        debugAll(buffer);

    }
}
