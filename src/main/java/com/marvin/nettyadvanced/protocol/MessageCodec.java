package com.marvin.nettyadvanced.protocol;

import com.marvin.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.List;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-21 20:30
 **/
@Slf4j
public class MessageCodec extends ByteToMessageCodec<Message>{

    /*
     * @Description: TODO 序列化
     * @Author: dengbin
     * @Date: 21/6/23 20:39
     * @param ctx:
     * @param msg:
     * @param out:
     * @return: void
     **/
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        // 1. 4个字节的魔数
        out.writeBytes(new byte[]{1,2,3,4});
        // 2. 1字节的版本
        out.writeByte(1);
        // 3. 1字节的序列化方式 0-jdk序列化， 1-json序列化
        out.writeByte(1);
        // 4. 1字节的指令类型
        out.writeByte(msg.getMessageType());
        // 5. 4个字节：ID
        out.writeInt(msg.getSequenceId());
        // 6. 对齐填充
        out.writeByte(0xff);

        // 7. 获取内容的字节数组-> 序列化
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(msg);
        byte[] bytes = bos.toByteArray();
        // 8. 字节长度
        out.writeInt(bytes.length);
        // 9. 写入内容
        out.writeBytes(bytes);
    }

    /*
     * @Description: TODO 反序列化
     * @Author: dengbin
     * @Date: 21/6/23 20:39
     * @param ctx:
     * @param in:
     * @param out:
     * @return: void
     **/
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        int magicNum = in.readInt();
        byte version = in.readByte();
        byte serializerType = in.readByte();
        byte messageType = in.readByte();
        int sequenceId = in.readInt();
        in.readByte();
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readBytes(bytes, 0, length);

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Message message = (Message) ois.readObject();
        log.debug("{}, {}, {}, {}, {}, {}", magicNum, version, serializerType, messageType, sequenceId, length);
        log.debug("{}", message);
        out.add(message);

    }
}
