package com.marvin.netty.bytebuffer;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @TODO:测试ByteBuffer
 * @author: dengbin
 * @create: 2023-06-14 16:37
 **/
@Slf4j
public class TestByteBuffer {

    public static void main(String[] args) {
        // FileChannel 用来读取文件
        // 1. 输入输出流可以间接获得FileChannel
        // 2. 使用RandomAccessFile 随机读取文件
        try (FileChannel channel = new FileInputStream("data.txt").getChannel()) {
            // 准备缓冲区
            ByteBuffer buffer = ByteBuffer.allocate(10); // 10个字节作为缓冲区
            while (true) {
                int len = channel.read(buffer);
                log.debug("读取到的字节:{}",len);
                if (len == -1) break;
                // 打印buffer内容
                buffer.flip();
                while (buffer.hasRemaining()) { // 检测是否还有剩余的数据
                    byte b = buffer.get();
                    log.debug("读取到的字符:{}",(char) b);
                }
                // 切换到写模式
                buffer.clear();
            }
        } catch (IOException e) {
        }

    }

}
