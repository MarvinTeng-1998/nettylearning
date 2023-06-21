package com.marvin.nettyadvanced.protocol;

import com.marvin.message.LoginRequestMessage;
import com.marvin.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-21 20:39
 **/
public class MessageCodecTest {
    public static void main(String[] args) throws Exception {

        EmbeddedChannel channel = new EmbeddedChannel(
                new LoggingHandler(),
                new LengthFieldBasedFrameDecoder(1024,12,4,0,0),
                new MessageCodec()
        );

        // encode
        LoginRequestMessage message = new LoginRequestMessage("zhangsan","123");
        channel.writeOutbound(message);

        // decode
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();

        new MessageCodec().encode(null,message,buf);

        // 尽管这里做了分割，但是还是能序列化出来，主要是用了LengthFieldBasedFrameDecoder技术来做这个事情。
        ByteBuf s1 = buf.slice(0,100);
        ByteBuf s2 = buf.slice(100,buf.readableBytes() - 100);
        s1.retain();
        channel.writeInbound(s1);
        channel.writeInbound(s2);

    }


}
