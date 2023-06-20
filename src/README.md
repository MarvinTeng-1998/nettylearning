# 1. Netty概念

## 1.1 Netty是什么？

netty是一个**异步的(多线程异步)**、**基于事件驱动**的网络应用框架，用于快速开发可维护、高性能的网络服务器和客户端。

## 1.2 Netty的优势

### Netty vs NIO

- 需要自己构建协议
- 解决TCP传输问题，如粘包、半包
- epoll空轮询导致CPU 100%
- 对API进行增强，使之更加易用，如FastThreadLocal -> ThreadLocal,ByteBuf -> ByteBuffer

# 2. Hello World

## 2.1 目标

开发一个简单的服务器端和客户端

- 客户端向服务器端发送hello，world
- 服务端仅接受，不返回

### 💡 提示

* 把 channel 理解为数据的通道
* 把 msg 理解为流动的数据，最开始输入是 ByteBuf，但经过 pipeline 的加工，会变成其它类型对象，最后输出又变成 ByteBuf
* 把 handler 理解为数据的处理工序
  * 工序有多道，合在一起就是 pipeline，pipeline 负责发布事件（读、读取完成...）传播给每个 handler， handler 对自己感兴趣的事件进行处理（重写了相应事件处理方法）
  * handler 分 Inbound 和 Outbound 两类
* 把 eventLoop 理解为处理数据的工人
  * 工人可以管理多个 channel 的 io 操作，并且一旦工人负责了某个 channel，就要负责到底（绑定）
  * 工人既可以执行 io 操作，也可以进行任务处理，每位工人有任务队列，队列里可以堆放多个 channel 的待处理任务，任务分为普通任务、定时任务
  * 工人按照 pipeline 顺序，依次按照 handler 的规划（代码）处理数据，可以为每道工序指定不同的工人

## 3. 组件

## 3.1 EventLoop

**EventLoop**本质是一个单线程执行器(同时维护了一个Selector)，里面有run方法处理Channel上源源不断的io事件。

它的继承方法比较复杂

- 一条线是继承了自j.u.c.ScheduledExecutorService因此包含线程池中的所有方法
- 另一条线是继承了parent方法来看看自己属于哪一个EventLoopGroup

**EventLoopGroup**是一组EventGroup，Channel一般会调用EventLoopGroup的register方法来绑定其中EventLoop。后续这个Channel上的io事件都由这个EventLoop来处理(保证了IO时的线程安全)

- 继承自netty自己的EventExecutorGroup
  - 实现了Iterable接口提供便利EventLoop能力
  - 另外有next方法获取集合下一个EventLoop

#### 💡 优雅关闭

优雅关闭 `shutdownGracefully` 方法。该方法会首先切换 `EventLoopGroup` 到关闭状态从而拒绝新的任务的加入，然后在任务队列的任务都处理完成后，停止线程的运行。从而确保整体应用是在正常有序的状态下退出的。

![20.png](/img/20.png)

可以看到这时两个EventLoop(工人)在轮流处理channel，但是channel和工人之间进行了绑定。

![21.png](/img/21.png)
当我们有耗时比较长的任务需执行时，我们可以新定义一个DefaultEventLoopGroup，它用来处理这种时间长的任务。

#### 💡 handler 执行中如何换人？

关键代码 `io.netty.channel.AbstractChannelHandlerContext#invokeChannelRead()`

```java
static void invokeChannelRead(final AbstractChannelHandlerContext next, Object msg) {
    final Object m = next.pipeline.touch(ObjectUtil.checkNotNull(msg, "msg"), next);
    // 下一个 handler 的事件循环是否与当前的事件循环是同一个线程
    // next表示的是下一个handler，next.executor返回的是这个handler所对应的EventLoop
    EventExecutor executor = next.executor();
  
    // 是，直接调用
    if (executor.inEventLoop()) {
        next.invokeChannelRead(m);
    } 
    // 不是，将要执行的代码作为任务提交给下一个事件循环处理（换人）
    else {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                next.invokeChannelRead(m);
            }
        });
    }
}
```

* 如果两个 handler 绑定的是同一个线程，那么就直接调用
* **否则，把要调用的代码封装为一个任务对象，由下一个 handler 的线程来调用**

## 3.2 Channel

channel的主要作用：

- close()可以用来关闭channel
- closeFuture()用来处理channel的关闭
  - sync方法是同步等待channel的关闭
  - addListener方法是异步等待channel关闭
- pipeline()是添加处理器
- write()方法是用来把数据写入
- writeAndFlush()将数据写入并刷出

## 3.3 ChannelFuture

这时刚才的客户端代码

```java
new Bootstrap()
    .group(new NioEventLoopGroup())
    .channel(NioSocketChannel.class)
    .handler(new ChannelInitializer<Channel>() {
        @Override
        protected void initChannel(Channel ch) {
            ch.pipeline().addLast(new StringEncoder());
        }
    })
    .connect("127.0.0.1", 8080)
    .sync()
    .channel()
    .writeAndFlush(new Date() + ": hello world!");
```

现在把它拆开来看

```java
ChannelFuture channelFuture = new Bootstrap()
    .group(new NioEventLoopGroup())
    .channel(NioSocketChannel.class)
    .handler(new ChannelInitializer<Channel>() {
        @Override
        protected void initChannel(Channel ch) {
            ch.pipeline().addLast(new StringEncoder());
        }
    })
    .connect("127.0.0.1", 8080); // 1

channelFuture.sync().channel().writeAndFlush(new Date() + ": hello world!");
```

* 1 处返回的是 ChannelFuture 对象，它的作用是利用 channel() 方法来获取 Channel 对象

**注意** connect 方法是异步的，意味着不等连接建立，方法执行就返回了。因此 channelFuture 对象中不能【立刻】获得到正确的 Channel 对象

实验如下：

```java
ChannelFuture channelFuture = new Bootstrap()
    .group(new NioEventLoopGroup())
    .channel(NioSocketChannel.class)
    .handler(new ChannelInitializer<Channel>() {
        @Override
        protected void initChannel(Channel ch) {
            ch.pipeline().addLast(new StringEncoder());
        }
    })
    .connect("127.0.0.1", 8080);

System.out.println(channelFuture.channel()); // 1
channelFuture.sync(); // 2
System.out.println(channelFuture.channel()); // 3
```

* 执行到 1 时，连接未建立，打印 `[id: 0x2e1884dd]`
* 执行到 2 时，sync 方法是同步等待连接建立完成
* 执行到 3 时，连接肯定建立了，打印 `[id: 0x2e1884dd, L:/127.0.0.1:57191 - R:/127.0.0.1:8080]`

除了用 sync 方法可以让异步操作同步以外，还可以使用回调的方式：

```java
ChannelFuture channelFuture = new Bootstrap()
    .group(new NioEventLoopGroup())
    .channel(NioSocketChannel.class)
    .handler(new ChannelInitializer<Channel>() {
        @Override
        protected void initChannel(Channel ch) {
            ch.pipeline().addLast(new StringEncoder());
        }
    })
    .connect("127.0.0.1", 8080);
System.out.println(channelFuture.channel()); // 1
channelFuture.addListener((ChannelFutureListener) future -> {
    System.out.println(future.channel()); // 2
});
```

* 执行到 1 时，连接未建立，打印 `[id: 0x749124ba]`
* ChannelFutureListener 会在连接建立时被调用（其中 operationComplete 方法），因此执行到 2 时，连接肯定建立了，打印 `[id: 0x749124ba, L:/127.0.0.1:57351 - R:/127.0.0.1:8080]`

#### CloseFuture

```java
@Slf4j
public class CloseFutureClient {
    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup group new NioEventLoopGroup();
        ChannelFuture channelFuture = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override // 在连接建立后被调用
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        ch.pipeline().addLast(new StringEncoder());
                    }
                })
                .connect(new InetSocketAddress("localhost", 8080));
        Channel channel = channelFuture.sync().channel();
        log.debug("{}", channel);
        new Thread(()->{
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String line = scanner.nextLine();
                if ("q".equals(line)) {
                    channel.close(); // close 异步操作 1s 之后
//                    log.debug("处理关闭之后的操作"); // 不能在这里善后
                    break;
                }
                channel.writeAndFlush(line);
            }
        }, "input").start();

        // 获取 CloseFuture 对象， 1) 同步处理关闭， 2) 异步处理关闭
        ChannelFuture closeFuture = channel.closeFuture();
        /*log.debug("waiting close...");
        closeFuture.sync();
        log.debug("处理关闭之后的操作");*/
        closeFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.debug("处理关闭之后的操作");
                group.shutdownGracefully();
            }
        });
    }
}
```

**Netty用异步的要点：**

* 单线程没法异步提高效率，必须配合多线程、多核 cpu 才能发挥异步的优势！
* 异步并没有缩短响应时间，反而有所增加；**但是这样做能提高吞吐量，单位时间内处理请求的个数！**
* **合理进行任务拆分，也是利用异步的关键！**

## 3.4 Future & Promise

异步处理时，需要使用到这两个接口。netty中的Future继承jdk中的future，netty中的promise是对netty中的Future做的拓展。

- jdk Future只能同步等待任务成功、失败
- netty 中的Future可以同步也可以异步，但都要等待任务结束
- netty 中的Promise不仅有Future的功能，而且脱离任务独立存在，只作为两个线程间传递结果的容器。


| 功能/名称    | jdk Future                     | netty Future                                                    | Promise      |
| ------------ | ------------------------------ | --------------------------------------------------------------- | ------------ |
| cancel       | 取消任务                       | -                                                               | -            |
| isCanceled   | 任务是否取消                   | -                                                               | -            |
| isDone       | 任务是否完成，不能区分成功失败 | -                                                               | -            |
| get          | 获取任务结果，阻塞等待         | -                                                               | -            |
| getNow       | -                              | 获取任务结果，非阻塞，还未产生结果时返回 null                   | -            |
| await        | -                              | 等待任务结束，如果任务失败，不会抛异常，而是通过 isSuccess 判断 | -            |
| sync         | -                              | 等待任务结束，如果任务失败，抛出异常                            | -            |
| isSuccess    | -                              | 判断任务是否成功                                                | -            |
| cause        | -                              | 获取失败信息，非阻塞，如果没有失败，返回null                    | -            |
| addLinstener | -                              | 添加回调，异步接收结果                                          | -            |
| setSuccess   | -                              | -                                                               | 设置成功结果 |
| setFailure   | -                              | -                                                               | 设置失败结果 |

## 3.5 HANDLER & PIPELINE

**ChannelHandler用来处理Channel上的各种事件，分为入站、出站两种。所有ChannelHandler被练成一串就是Pipeline**

* **入站处理器通常是ChannelInboundHandlerAdapter的子类，主要用来读取客户端数据，写回结果**
* **出站处理器通常是ChannelOutboundHandlerAdapter的子类，主要用来写回结果进行加工。**

**ChannelInboundHandlerAdapter 是按照 addLast 的顺序执行的，而 ChannelOutboundHandlerAdapter 是按照 addLast 的逆序执行的。ChannelPipeline 的实现是一个 ChannelHandlerContext（包装了 ChannelHandler） 组成的双向链表**

**也就是说，当我们入站我们会从head->到h1 -> 到h2 -> 到h3 -> h4这种过程。当我们出站我们会从tail -> h6 -> h5这种过程**
