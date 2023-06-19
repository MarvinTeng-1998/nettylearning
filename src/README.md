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

