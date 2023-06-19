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
