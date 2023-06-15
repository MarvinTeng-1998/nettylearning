package com.marvin.netty.bytebuffer;

import java.nio.ByteBuffer;

import static com.marvin.netty.bytebuffer.ByteBufferUtil.debugAll;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-15 15:41
 **/
public class TestByteBufferExam {
    /*
    * 网络上有多种数据发送给服务端，数据之间用\n进行分割
    * 但是由于某种原因这些数据在接受的时，被进行了重新组合，例如原始数据有3条为：
    * hello,world\n
    * i'm zhangsan\n
    * how are you?\n
    * 变成了下面两个buffer (粘包、半包)
    * hello,world\nI'm zhangsan\nho
    * w are you?\n
    * 现在要求写程序，将错乱的数据恢复成原始的
    *
    * 消息合在一起-》粘包，消息被切断了-》半包
    * 粘包的产生原因：
    *           发送效率高，导致多条数据合在一起。
    * 半包的产生的原因：
    *           服务器缓冲区大小限制导致半包的问题。
    *  */
    public static void main(String[] args) {
        ByteBuffer source = ByteBuffer.allocate(32);
        source.put("Hello World\nI'm zhangsan\nHo".getBytes());
        split(source);
        debugAll(source);
        source.put("w are you?\n".getBytes());
        split(source);
        debugAll(source);
    }

    public static void split(ByteBuffer source){
        source.flip();
        for (int i = 0; i < source.limit(); i++) {
            // 找到完整信息
            if (source.get(i) == '\n') {
                int length = i + 1 - source.position();
                ByteBuffer target = ByteBuffer.allocate(length);
                // 从source去读，向targe去写
                for (int j = 0; j < length; j++) {
                    target.put(source.get());
                }
                // debugAll(target);
            }
        }
        source.compact();

    }



}
