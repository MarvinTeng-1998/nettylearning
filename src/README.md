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
## 3.6 ByteBuf

是对字节数据的封装。

**直接内存vs堆内存**

```java
ByteBuf buffer = ByteBufAllocator.DEFAULT.heapBuffer(10);
ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer(10);
```

- 直接内存创建和销毁的代价昂贵，但是读写性能比较高(少一次内存复制)，适合配合池化功能使用
- 直接内存对GC的压力小，因为这部分不受JVM垃圾回收的管理，但是也要主动释放。

**池化VS非池化**

池化的最大意义在于可以重用ByteBuf，优点有：

- 没有池化，则每次都得创建心的ByteBuf实例，这个操作对直接内存代价昂贵，就算是堆内存，也会增加GC压力。
- 有了池化，则可以重用ByteBuf实例，并且采用了jemalloc类似的内存分配算法提升效率。
- 高并发时，池化功能更加节省资源，减少内存溢出的可能。

**组成**

ByteBuf由4个部分组成：

![22.png](/img/22.png)

最开始读写指针都在头部。

**写入**

网络编程，默认使用的是大端写入。也就是说0x250 -> 00 00 02 50。小端写入：0x250 -> 50 02 00 00

**扩容**

- 如果写入后数据大小未超过512，则选择下一个16的整数倍，例如写入后大小为12，则扩容的capacity为16
- 如果写入后数据大小超过了512，则选择下一个2^n，例如写入后大小为513，则扩容后capacity是2^10=1024
- 扩容不能超过max capacity，会报错。

**读取**

读过的内容属于废弃部分，再读只能读取尚未读取的部分。

**内存回收，内存释放**

由于 Netty 中有堆外内存的 ByteBuf 实现，堆外内存最好是手动来释放，而不是等 GC 垃圾回收。

* **UnpooledHeapByteBuf 使用的是 JVM 内存，只需等 GC 回收内存即可**
* UnpooledDirectByteBuf 使用的就是直接内存了，需要特殊的方法来回收内存
* PooledByteBuf 和它的子类使用了池化机制，需要更复杂的规则来回收内存

> 回收内存的源码实现，protected abstract void deallocate()

Netty 这里采用了引用计数法来控制回收内存，每个 ByteBuf 都实现了 ReferenceCounted 接口

* 每个 ByteBuf 对象的初始计数为 1
* 调用 release 方法计数减 1，如果计数为 0，ByteBuf 内存被回收
* 调用 retain 方法计数加 1，表示调用者没用完之前，其它 handler 即使调用了 release 也不会造成回收
* 当计数为 0 时，底层内存会被回收，这时即使 ByteBuf 对象还在，其各个方法均无法正常使用

因为 pipeline 的存在，一般需要将 ByteBuf 传递给下一个 ChannelHandler，如果在 finally 中 release 了，就失去了传递性（当然，如果在这个 ChannelHandler 内这个 ByteBuf 已完成了它的使命，那么便无须再传递）

基本规则是，**谁是ByteBuf的最后使用者，谁负责 release**，详细分析如下

* 起点，对于 NIO 实现来讲，在 io.netty.channel.nio.AbstractNioByteChannel.NioByteUnsafe#read 方法中首次创建 ByteBuf 放入 pipeline（line 163 pipeline.fireChannelRead(byteBuf)）
* 入站 ByteBuf 处理原则
  * 对原始 ByteBuf 不做处理，调用 ctx.fireChannelRead(msg) 向后传递，这时无须 release
  * 将原始 ByteBuf 转换为其它类型的 Java 对象，这时 ByteBuf 就没用了，必须 release
  * 如果不调用 ctx.fireChannelRead(msg) 向后传递，那么也必须 release
  * 注意各种异常，如果 ByteBuf 没有成功传递到下一个 ChannelHandler，必须 release
  * 假设消息一直向后传，那么 TailContext 会负责释放未处理消息（原始的 ByteBuf）
* 出站 ByteBuf 处理原则
  * 出站消息最终都会转为 ByteBuf 输出，一直向前传，由 HeadContext flush 后 release
* 异常处理原则
  * 有时候不清楚 ByteBuf 被引用了多少次，但又必须彻底释放，可以循环调用 release 直到返回 true

**slice**

【零拷贝的体现之一】，对原始的ByteBuf进行切片成多个ByteBuf，切片后的ByteBuf并没有发生内存复制，还是使用原始ByteBuf内存，切片后的ByteBuf维护独立的write、read指针。

**duplicate**

【零拷贝的实现之一】，就好比截取了原始ByteBuf所有内容，并且没有max capacity的限制，也就是与原始ByteBuf使用同一块底层内存，只是读写指针是独立的。

**copy**

会将底层数据结构进行深拷贝，因此无论读写都跟原始内存无关。

**composite**

【零拷贝】的体现之一，可以将多个 ByteBuf 合并为一个逻辑上的 ByteBuf，避免拷贝

**compositeByteBuf.addComponents(true,buf1,buf2);第一个参数是increaseWriteIndex，如果不是true，则不会增长写指针。**

CompositeByteBuf 是一个组合的 ByteBuf，它内部维护了一个 Component 数组，每个 Component 管理一个 ByteBuf，记录了这个 ByteBuf 相对于整体偏移量等信息，代表着整体中某一段的数据。

* 优点，对外是一个虚拟视图，组合这些 ByteBuf 不会产生内存复制
* 缺点，复杂了很多，多次操作会带来性能的损耗


 **💡 ByteBuf 优势**

* 池化 - 可以重用池中 ByteBuf 实例，更节约内存，减少内存溢出的可能
* 读写指针分离，不需要像 ByteBuffer 一样切换读写模式
* 可以自动扩容
* 支持链式调用，使用更流畅
* 很多地方体现零拷贝，例如 slice、duplicate、CompositeByteBuf

# 1.粘包半包问题

## 1.1 粘包半包现象分析

**粘包**

- 现象：发送abc def，接受abcdefg
- 原因：
  - 接收方ByteBuf设置太大。netty默认下是1024
  - 滑动窗口：假设发送方256bytes表示一个完整的报文，但是由于接收方不及时处理而且窗口足够大，这256bytes字节就会缓冲在接收方的滑动窗口中，当滑动窗口中缓存多个报文就会粘包。
  - Naggle算法：会产生粘包问题。

**半包**

- 现象：发送abcdefg 接受 abcd efg
- 原因：
  - 应用层：接收方的ByteBuf小于发送方发出来的数据
  - 滑动窗口，当窗口小于发送方的数据就会先发送滑动窗口的数据，然后再等待ack后才发送剩余部分
  - MSS限制：当发送的数据超过MSS之后，就会将数据切分发送，就会产生半包。

**本质是TCP是流式协议，消息无边界**

> 滑动窗口：
>
> - TCP以一个数据段segment作为数据单位，每发送一个段就会进行一次确认应答（ack），但是如果这么做，缺点是包的往返时间越长性能越差。
> - ![23.png](/img/23.png)
> - 为了解决这个问题，引入滑动窗口，窗口大小决定了无需等待应答就可以继续发送的最大值。
>
> - ![24.png](/img/24.png)
>
> - 窗口实际上起到了一个缓冲区的作用，同时也能对流量进行控制。
    >   - 窗口内的数据允许被发送，当没收到ack时，窗口停止滑动。
>   - 如果1001～2000字段的ack返回，则窗口向前滑动
>   - 接收方也会维护一个窗口，只有落在窗口中的数据才能被接受。



> MSS限制：
>
> - 链路层对一次能够最大发送的数据具备限制，这个称谓MTU，不同链路设备的MTU值不同
> - TCP在传递大量数据时，会按照MSS大小将数据进行分割发送
> - MSS的值在三次握手时会通知对方自己的MSS值，然后在两者之间选一个小的MSS



> Naggle算法：
>
> - 发送一个字节需要加入tcp头和ip头，字节会用41bytes，不经济，为了提升利用率，尽可能发送足够大的数据。
> - 如果数据达到MSS，则发送
> - 如果数据中含有Fin则将剩余数据发送再关闭
> - 如果TCP_NODELAY=true则发送
> - 已发送的数据都收到ack时则发送
> - 上述条件不满足，但是超时则发送
> - 除上述情况都延迟发送

## 1.2 粘包半包问题解决办法

1. 短连接，发一个包建立一次连接，这样连接建立到连接断开之间就是消息的边界。**（效率低）**

  - **半包用这种办法还是不好解决，因为接收方的缓冲区大小是有限的**

2. 每一条消息采用固定长度。**（浪费空间）**

  - 缺点是，数据包的大小不好把握

    * 长度定的太大，浪费

    * 长度定的太小，对某些数据包又显得不够

3. 每一条消息采用分隔符。**（效率低）**

  - 缺点，处理字符数据比较合适，但如果内容本身包含了分隔符（字节数据常常会有此情况），那么就会解析错误

4. 每一条消息分为body和head，head中包含body的长度

  - // 最大长度，长度偏移，长度占用字节，长度调整，剥离字节数

  - ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 0, 1, 0, 1));


## 1.2 为什么需要协议？

TCP/IP 中消息传输基于流的方式，没有边界。

协议的目的就是划定消息的边界，制定通信双方要共同遵守的通信规则

例如：在网络上传输

```
下雨天留客天留我不留
```

是中文一句著名的无标点符号句子，在没有标点符号情况下，这句话有数种拆解方式，而意思却是完全不同，所以常被用作讲述标点符号的重要性

一种解读

```
下雨天留客，天留，我不留
```

另一种解读

```
下雨天，留客天，留我不？留
```



如何设计协议呢？其实就是给网络传输的信息加上“标点符号”。但通过分隔符来断句不是很好，因为分隔符本身如果用于传输，那么必须加以区分。因此，下面一种协议较为常用

``` 
定长字节表示内容长度 + 实际内容
```

例如，假设一个中文字符长度为 3，按照上述协议的规则，发送信息方式如下，就不会被接收方弄错意思了

```
0f下雨天留客06天留09我不留
```

## 1.3 自定义协议要素

* 魔数，用来在第一时间判定是否是无效数据包
* 版本号，可以支持协议的升级
* 序列化算法，消息正文到底采用哪种序列化反序列化方式，可以由此扩展，例如：json、protobuf、hessian、jdk
* 指令类型，是登录、注册、单聊、群聊... 跟业务相关
* 请求序号，为了双工通信，提供异步能力
* 正文长度
* 消息正文

#### 💡 什么时候可以加 @Sharable

* 当 handler 不保存状态时，就可以安全地在多线程下被共享
* 但要注意对于编解码器类，不能继承 ByteToMessageCodec 或 CombinedChannelDuplexHandler 父类，他们的构造方法对 @Sharable 有限制
* 如果能确保编解码器不会保存状态，可以继承 MessageToMessageCodec 父类

## 1.4 连接假死

原因

* 网络设备出现故障，例如网卡，机房等，底层的 TCP 连接已经断开了，但应用程序没有感知到，仍然占用着资源。
* 公网网络不稳定，出现丢包。如果连续出现丢包，这时现象就是客户端数据发不出去，服务端也一直收不到数据，就这么一直耗着
* 应用程序线程阻塞，无法进行数据读写

问题

* 假死的连接占用的资源不能自动释放
* 向假死的连接发送数据，得到的反馈是发送超时

服务器端解决

* 怎么判断客户端连接是否假死呢？如果能收到客户端数据，说明没有假死。因此策略就可以定为，每隔一段时间就检查这段时间内是否接收到客户端数据，没有就可以判定为连接假死

客户端定时心跳

* 客户端可以定时向服务器端发送数据，只要这个时间间隔小于服务器定义的空闲检测的时间间隔，那么就能防止前面提到的误判，客户端可以定义如下心跳处理器


