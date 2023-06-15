package com.marvin.netty.filechannel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-15 16:09
 **/
public class TestFileChannelTransferTo {
    public static void main(String[] args) {
        try (FileChannel from = new FileInputStream("data.txt").getChannel();
             FileChannel to = new FileOutputStream("to.txt").getChannel();
        ) {

            // 效率高，jdk会使用操作系统的零拷贝来进行优化，这个是有上限的，一次最多传2g的数据
            // size代表还剩下多少字节没有传输
            long size = from.size();
            for (long left = size; left > 0; ) {
                left -= from.transferTo(size - left, left, to);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
