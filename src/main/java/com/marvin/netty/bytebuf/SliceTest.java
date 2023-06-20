package com.marvin.netty.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.extern.slf4j.Slf4j;

import static io.netty.buffer.ByteBufUtil.appendPrettyHexDump;
import static io.netty.util.internal.StringUtil.NEWLINE;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-20 15:51
 **/
@Slf4j
public class SliceTest {
    public static void main(String[] args) {

        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(10);
        buf.writeBytes(new byte[]{'a','b','c','d','e','f','g','h','i','j'});
        log(buf);

        // 在切片过程中没有发生数据复制
        ByteBuf slice1 = buf.slice(0, 5);
        log(slice1);
        // 这里releaseCount ++ 导致它不在后面被释放
        slice1.retain();
        ByteBuf slice2 = buf.slice(5, 5);
        log(slice2);

        slice1.setByte(0,'1');
        slice2.setByte(0,'2');
        log(slice1);
        log(slice2);
        log(buf);
        buf.release();
        System.out.println("释放原来的内存");
        log(slice1);
    }

    private static void log(ByteBuf buffer) {
        int length = buffer.readableBytes();
        int rows = length / 16 + (length % 15 == 0 ? 0 : 1) + 4;
        StringBuilder buf = new StringBuilder(rows * 80 * 2)
                .append("read index:").append(buffer.readerIndex())
                .append(" write index:").append(buffer.writerIndex())
                .append(" capacity:").append(buffer.capacity())
                .append(NEWLINE);
        appendPrettyHexDump(buf, buffer);
        System.out.println(buf.toString());
    }
}
