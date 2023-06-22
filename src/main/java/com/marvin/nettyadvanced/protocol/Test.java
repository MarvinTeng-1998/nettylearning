package com.marvin.nettyadvanced.protocol;

import com.marvin.message.LoginRequestMessage;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.logging.LoggingHandler;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-23 04:02
 **/
public class Test {
    public static void main(String[] args) {
        MessageCodec  codec = new MessageCodec();
        LoggingHandler loggingHandler = new LoggingHandler();
        EmbeddedChannel channel = new EmbeddedChannel(loggingHandler,codec,loggingHandler);
        LoginRequestMessage message = new LoginRequestMessage("zhangsan", "123");
        channel.writeOutbound(message);
    }
}
