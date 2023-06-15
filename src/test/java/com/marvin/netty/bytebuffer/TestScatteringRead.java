package com.marvin.netty.bytebuffer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static com.marvin.netty.bytebuffer.ByteBufferUtil.debugAll;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-15 15:33
 **/
public class TestScatteringRead {
    public static void main(String[] args) {
        try(FileChannel channel = new RandomAccessFile("threeparts.txt","rw").getChannel()) {
            ByteBuffer a = ByteBuffer.allocate(3);
            ByteBuffer b = ByteBuffer.allocate(3);
            ByteBuffer c = ByteBuffer.allocate(5);
            channel.read(new ByteBuffer[]{a,b,c});
            a.flip();
            b.flip();
            c.flip();
            debugAll(a);
            debugAll(b);
            debugAll(c);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
