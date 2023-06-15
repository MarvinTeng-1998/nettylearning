package com.marvin.netty.bytebuffer;

import java.nio.ByteBuffer;

import static com.marvin.netty.bytebuffer.ByteBufferUtil.debugAll;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-15 14:28
 **/
public class TestByteBufferAllocate {
    public static void main(String[] args) {
        // class java.nio.HeapByteBuffer
        System.out.println(ByteBuffer.allocate(16).getClass());
        // class java.nio.DirectByteBuffer
        System.out.println(ByteBuffer.allocateDirect(16).getClass());

        ByteBuffer byteBuffer = ByteBuffer.allocate(10);
        byteBuffer.put(new byte[]{'a','b','c','d'});
        byteBuffer.flip();

        /* byteBuffer.get(new byte[4]);
        debugAll(byteBuffer);

        byteBuffer.rewind();
        debugAll(byteBuffer);

        // mark() 做一个标记，记录position的位置
        // reset() 将position重置到mark的位置
        System.out.println((char)byteBuffer.get());
        System.out.println((char)byteBuffer.get());
        debugAll(byteBuffer);
        byteBuffer.mark(); // 加索引，标记到2的位置
        System.out.println((char)byteBuffer.get());
        System.out.println((char)byteBuffer.get());
        debugAll(byteBuffer);
        byteBuffer.reset();// 将索引重置到索引2
        debugAll(byteBuffer); */

        // get(int index) 这个方法不会改变读索引的方法
        System.out.println((char)byteBuffer.get(3));
        debugAll(byteBuffer);

    }
}
