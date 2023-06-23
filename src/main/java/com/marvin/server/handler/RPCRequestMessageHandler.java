package com.marvin.server.handler;

import com.marvin.message.RpcRequestMessage;
import com.marvin.message.RpcResponseMessage;
import com.marvin.server.service.HelloService;
import com.marvin.server.service.ServicesFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @TODO:
 * @author: dengbin
 * @create: 2023-06-23 20:22
 **/
public class RPCRequestMessageHandler extends SimpleChannelInboundHandler<RpcRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequestMessage msg) throws Exception {
        RpcResponseMessage responseMessage = new RpcResponseMessage();
        try {
            HelloService service = (HelloService) ServicesFactory.getService(Class.forName(msg.getInterfaceName()));
            Method method = service.getClass().getMethod(msg.getMethodName(), msg.getParameterTypes());
            Object invoke = method.invoke(service, msg.getParameterValue());
            responseMessage.setReturnValue(invoke);
        } catch (Exception e){
            e.printStackTrace();
            responseMessage.setExceptionValue(e);
        }
        ctx.writeAndFlush(responseMessage);
    }
}
