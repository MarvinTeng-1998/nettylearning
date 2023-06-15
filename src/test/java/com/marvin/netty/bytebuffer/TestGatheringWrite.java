package com.marvin.netty.bytebuffer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-15 15:37
 **/
public class TestGatheringWrite {
    public static void main(String[] args) {

        ByteBuffer a = StandardCharsets.UTF_8.encode("hello");
        ByteBuffer b = StandardCharsets.UTF_8.encode("world");
        ByteBuffer c = StandardCharsets.UTF_8.encode("你好");


        try( RandomAccessFile rw = new RandomAccessFile("threeparts2.txt", "rw")) {
            FileChannel channel = rw.getChannel();
            channel.write(new ByteBuffer[]{a,b,c});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
