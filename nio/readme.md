

# 1、Java NIO 概述

Java NIO（New IO 或 Non Blocking IO）是从 Java 1.4 版本开始引入的一个新的IO API，可以替代标准的 Java IO API。NIO 支持面向缓冲区的、基于通道的 IO 操作。NIO 将以更加高效的方式进行文件的读写操作。

## 1.1阻塞IO

通常在进行同步 I/O 操作时，如果读取数据，代码会阻塞直至有可供读取的数据。同样，写入调用将会阻塞直至数据能够写入。传统的 Server/Client 模式会基于 TPR（Thread per Request）,服务器会为每个客户端请求建立一个线程，由该线程单独负责处理一个客户请求。这种模式带来的一个问题就是线程数量的剧增，大量的线程会增大服务器的开销。大多数的实现为了避免这个问题，都采用了线程池模型，并设置线程池线程的最大数量，这由带来了新的问题，如果线程池中有 100 个线程，而有100 个用户都在进行大文件下载，会导致第 101 个用户的请求无法及时处理，即便第101 个用户只想请求一个几 KB 大小的页面。

## 1.2非阻塞IO

NIO 中非阻塞 I/O 采用了基于 Reactor 模式的工作方式，I/O 调用不会被阻塞，相反是注册感兴趣的特定 I/O 事件，如可读数据到达，新的套接字连接等等，在发生特定事件时，系统再通知我们。NIO 中实现非阻塞 I/O 的核心对象就是 Selector，Selector 就是注册各种 I/O 事件地方，而且当我们感兴趣的事件发生时，就是这个对象告诉我们所发生的事件

<img src="imag/image-20220222193817430.png" alt="image-20220222193817430" style="zoom:60%;" />

当有读或写等任何注册的事件发生时，可以从 Selector 中获得相应的 SelectionKey，同时从 SelectionKey 中可以找到发生的事件和该事件所发生的具体的 SelectableChannel，以获得客户端发送过来的数据。

非阻塞指的是 **IO 事件本身不阻塞,但是获取 IO 事件的 select()方法是需要阻塞等待**的。区别是阻塞的 IO 会阻塞在 IO 操作上, NIO 阻塞在事件获取上,没有事件就没有 IO, 从高层次看 IO 就不阻塞了.也就是说只有 IO 已经发生那么我们才评估 IO 是否阻塞,但是select()阻塞的时候 IO 还没有发生。NIO 的本质是延迟 IO 操作到真正发生 IO 的时候,而不是以前的只要 IO 流打开了就一直等待 IO 操作。

![image-20220222194004786](imag/image-20220222194004786.png)

## 1.3 NIO 概述

Java NIO 由以下几个核心部分组成：

- Channels
  - “通道”。Channel 和 IO 中的 Stream(流)是差不多一个等级的。只不过 Stream 是单向的，譬如：InputStream, OutputStream.而Channel 是双向的，既可以用来进行读操作，又可以用来进行写操作
  - NIO 中的 Channel 的主要实现有：FileChannel、DatagramChannel、SocketChannel 和 ServerSocketChannel，这里看名字就可以猜出个所以然来：分别可以对应文件 IO、UDP 和 TCP（Server 和 Client）
- Buffers
  - NIO 中的关键 Buffer 实现有：ByteBuffer, CharBuffer, DoubleBuffer, FloatBuffer, IntBuffer, LongBuffer, ShortBuffer，分别对应基本数据类型: byte, char, double, float, int, long, short。

- Selectors
  - Selector 运行单线程处理多个 Channel，如果你的应用打开了多个通道，但每个连接的流量都很低，使用 Selector 就会很方便。例如在一个聊天服务器中，要使用Selector, 得向 Selector 注册 Channel，然后调用它的 select()方法。这个方法会一直阻塞到某个注册的通道有事件就绪。一旦这个方法返回，线程就可以处理这些事件，事件的例子有如新的连接进来、数据接收等。

# 2、Java NIO（Channel）

Channel 是一个通道，可以通过它读取和写入数据，它就像水管一样，网络数据通过Channel 读取和写入。通道与流的不同之处在于通道是双向的，流只是在**一个方向**上移动（一个流必须是 InputStream 或者 OutputStream 的子类），而且通道可以用于读、写或者同时用于读写。因为 Channel 是**全双工**的，所以它可以比流更好地映射底层操作系统的 API。

NIO 中通过 channel 封装了对数据源的操作，通过 channel 我们可以操作数据源，但又不必关心数据源的具体物理结构。这个数据源可能是多种的。比如，可以是文件，也可以是网络 socket。在大多数应用中，channel 与文件描述符或者 socket 是一一对应的。Channel 用于在字节缓冲区和位于通道另一侧的实体（通常是一个文件或套接字）之间有效地传输数据。 

```java
public interface Channel extends Closeable {

    /**Tells whether or not this channel is open.
     * @return <tt>true</tt> if, and only if, this channel is open
     */
    public boolean isOpen();

    /**Closes this channel.
     * @throws  IOException  If an I/O error occurs
     */
    public void close() throws IOException;
}
```

Channel 是一个对象，可以通过它读取和写入数据。拿 NIO 与原来的 I/O 做个比较，通道就像是流。所有数据都通过 Buffer 对象来处理。您永远不会将字节直接写入通道中，相反，您是将数据写入包含一个或者多个字节的缓冲区。同样，您不会直接从通道中读取字节，而是将数据从通道读入缓冲区，再从缓冲区获取这个字节。

Java NIO 的通道类似流，但又有些不同：

- 既可以从通道中读取数据，又可以写数据到通道。但流的读写通常是单向的。

- 通道可以异步地读写。
- 通道中的数据总是要先读到一个 Buffer，或者总是要从一个 Buffer 中写入

## 2.1 FileChannel

从文件中读写数据.可以实现常用的 read，write 以及 scatter/gather 操作，同时它也提供了很多专用于文件的新方法

![](imag/image-20220222194912051.png)

```java
public class FileChannelDemo1 {

    //在使用 FileChannel 之前，必须先打开它。但是，我们无法直接打开一个FileChannel，需要通过使用一个 InputStream、OutputStream或RandomAccessFile 来获取一个 FileChannel 实例。
    //FileChannel读取数据到buffer中
    public static void main(String[] args) throws Exception {
        //创建FileChannel
        RandomAccessFile aFile = new RandomAccessFile("E:\\IdeaProjects\\nio\\01.txt", "rw");
        FileChannel channel = aFile.getChannel();

        //创建Buffer
        ByteBuffer buf = ByteBuffer.allocate(1024);

        //读取数据到buffer中
        int bytesRead = channel.read(buf);
        while(bytesRead != -1) { //有多少字节被读到了Buffer中  如果==-1 读到末尾，结束
            System.out.println("读取了："+bytesRead);
            //在写模式下调用flip()之后，Buffer从写模式变成读模式。那么limit就设置成了position当前的值(即当前写了多少数据)，postion会被置为0，以表示读操作从缓存的头开始读，mark置为-1
            buf.flip();  //反转读写模式 
            while(buf.hasRemaining()) { //有没有剩余内容
                System.out.println((char)buf.get());
            }
            buf.clear();
            bytesRead = channel.read(buf);
        }
        aFile.close();
        System.out.println("结束了");
    }
}
---------------------------------------------------------------------------------------------------------
//FileChanne写操作
public class FileChannelDemo2 {

    public static void main(String[] args) throws Exception {
        // 打开FileChannel
        RandomAccessFile aFile = new RandomAccessFile("E:\\IdeaProjects\\nio\\src\\01w.txt","rw");
        FileChannel channel = aFile.getChannel();

        //创建buffer对象
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        String newData = "data helloworld";
        buffer.clear();

        //写入内容
        buffer.put(newData.getBytes());

        buffer.flip();

        //FileChannel完成最终实现
        //因为无法保证 write()方法一次能向 FileChannel 写入多少字节，因此需要重复调用 write()方法，直到 Buffer 中已经没有尚未写入通道的字节
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }

        //关闭
        channel.close();
    }
}
```

### 操作详解

（1）**打开 FileChannel**：在使用 FileChannel 之前，必须先打开它。但是，我们无法直接打开一个FileChannel，需要通过使用一个 InputStream、OutputStream 或RandomAccessFile 来获取一个 FileChannel 实例。

（2）**从 FileChannel 读取数据**：首先，分配一个 Buffer。从 FileChannel 中读取的数据将被读到 Buffer 中。然后，调用 FileChannel.read()方法。该方法将数据从 FileChannel 读取到 Buffer 中。read()方法返回的 int 值表示了有多少字节被读到了 Buffer 中。如果返回-1，表示到了文件末尾。

（3）**向 FileChannel 写数据**：使用 FileChannel.write()方法向 FileChannel 写数据，该方法的参数是一个 Buffer。因为无法保证 write()方法一次能向 FileChannel 写入多少字节，因此需要重复调用 write()方法，直到 Buffer 中已经没有尚未写入通道的字节

（4）**关闭 FileChannel**



### 方法

- **position 方法**：可以通过调用position()方法获取 FileChannel 的当前位置。也可以通过调用 position(long pos)方法设置 FileChannel 的当前位置。

  - ```java
    long pos = channel.position();//获取当前位置
    channel.position(pos +123);//设置当前位置
    ```

  - 如果将位置设置在文件结束符之后，然后试图从文件通道中读取数据，读方法将返回-1 （文件结束标志）。

  - 如果将位置设置在文件结束符之后，然后向通道中写数据，文件将撑大到当前位置并写入数据。这可能导致“文件空洞”，磁盘上物理文件中写入的数据间有空隙。

- **size 方法**：FileChannel 实例的 size()方法将返回该实例所关联文件的大小

  - ```java
    long fileSize = channel.size();
    ```

- **truncate 方法**:可以使用 FileChannel.truncate()方法截取一个文件。截取文件时，文件将中指定长度后面的部分将被删除

  - ```java
    channel.truncate(1024);//截取文件的前 1024 个字节
    ```

- **force 方法**:FileChannel.force()方法将通道里尚未写入磁盘的数据强制写到磁盘上。

  - 出于性能方面的考虑，操作系统会将数据缓存在内存中，所以无法保证写入到 FileChannel 里的数据一定会即时写到磁盘上。要保证这一点，需要调用 force()方法。
  - force()方法有一个 boolean 类型的参数，指明是否同时将文件元数据（权限信息等）写到磁盘上。

- **transferTo 和 transferFrom 方法**:**通道之间的数据传输**。如果两个通道中有一个是 FileChannel，那你可以直接将数据从一个 channel 传输到另外一个 channel。

  - ```java
    // 创建两个fileChannel
    RandomAccessFile aFile = new RandomAccessFile("E:\\IdeaProjects\\nio\\src\\01w.txt","rw");
    FileChannel fromChannel = aFile.getChannel();
    
    RandomAccessFile bFile = new RandomAccessFile("E:\\IdeaProjects\\nio\\src\\02.txt","rw");
    FileChannel toChannel = bFile.getChannel();
    
    //fromChannel 传输到 toChannel
    long position = 0;
    long size = fromChannel.size();
    toChannel.transferFrom(fromChannel,position,size);
    //或者fromChannel.transferTo(0,size,toChannel);
    
    aFile.close();
    bFile.close();
    System.out.println("over!");
    
    ```

## 2.2 Socket通道

（1）新的 socket 通道类可以运行非阻塞模式并且是可选择的，可以激活大程序（如网络服务器和中间件组件）巨大的可伸缩性和灵活性。所有的 socket 通道类(DatagramChannel、SocketChannel 和 ServerSocketChannel)都继承了位于 java.nio.channels.spi 包中的AbstractSelectableChannel。这意味着我们可以用一个 Selector 对象来执行socket 通道的就绪选择（readiness selection）。

（2）请注意 DatagramChannel 和 SocketChannel 实现定义读和写功能的接口而ServerSocketChannel 不实现。ServerSocketChannel 负责监听传入的连接和创建新的 SocketChannel 对象，它本身从不传输数据。

（3）通道是一个连接 I/O 服务导管并提供与该服务交互的方法。**就某个 socket 而言，它不会再次实现与之对应的 socket 通道类中的 socket 协议 API**，而 **java.net 中已经存在的 socket 通道都可以被大多数协议操作重复使用**。

全部 socket 通道类（DatagramChannel、SocketChannel 和ServerSocketChannel）在被实例化时都会创建一个对等 socket 对象。这些是我们所熟悉的来自 java.net 的类（Socket、ServerSocket 和 DatagramSocket），它们已经被更新以识别通道。对等 socket 可以通过调用 socket( )方法从一个通道上获取。此外，这三个 java.net 类现在都有 getChannel( )方法。

（4）要把一个 socket 通道置于非阻塞模式，我们要依靠所有 socket 通道类的公有超级类：SelectableChannel。就绪选择（readiness selection）是一种可以用来查询通道的机制，该查询可以判断通道是否准备好执行一个目标操作，如读或写。非阻塞 I/O 和可选择性是紧密相连的，那也正是管理阻塞模式的 API 代码要在SelectableChannel 超级类中定义的原因。

设置或重新设置一个通道的阻塞模式是很简单的，只要调用 configureBlocking( )方法即可，**传递参数值为 true 则设为阻塞模式，参数值为 false 值设为非阻塞模式**。可以通过调用 isBlocking( )方法来判断某个 socket 通道当前处于哪种模式。

```java
//AbstractSelectableChannel.java 中实现的 configureBlocking()方法如下：
 public final SelectableChannel configureBlocking(boolean block)
        throws IOException
    {
        synchronized (regLock) {
            if (!isOpen())
                throw new ClosedChannelException();
            if (blocking == block)
                return this;
            if (block && haveValidKeys())
                throw new IllegalBlockingModeException();
            implConfigureBlocking(block);
            blocking = block;
        }
        return this;
    }
```

非阻塞 socket 通常被认为是服务端使用的，因为它们使同时管理很多 socket 通道变得更容易。但是，在客户端使用一个或几个非阻塞模式的 socket 通道也是有益处的，例如，借助非阻塞 socket 通道，GUI 程序可以专注于用户请求并且同时维护与一个或多个服务器的会话。在很多程序上，非阻塞模式都是有用的。

偶尔地，我们也会需要防止 socket 通道的阻塞模式被更改。API 中有一个blockingLock( )方法，该方法会返回一个非透明的对象引用。返回的对象是通道实现修改阻塞模式时内部使用的。只有拥有此对象的锁的线程才能更改通道的阻塞模式。

### (1)ServerSocketChannel

ServerSocketChannel 可以监听新进来的 TCP 连接，像 Web 服务器那样。对每一个新进来的连接都会创建一个 SocketChannel。

ServerSocketChannel 是一个基于通道的 socket 监听器。它同我们所熟悉的java.net.ServerSocket 执行相同的任务，不过它增加了通道语义，因此能够在非阻塞模式下运行。

同 java.net.ServerSocket 一样，ServerSocketChannel 也有 accept( )方法。一旦创建了一个 ServerSocketChannel 并用对等 socket 绑定了它，然后您就可以在其中一个上调用 accept()。如果您选择在 ServerSocket 上调用 accept( )方法，那么它会同任何其他的 ServerSocket 表现一样的行为：总是阻塞并返回一个 java.net.Socket 对象。如果您选择在 ServerSocketChannel 上调用 accept( )方法则会返回SocketChannel 类型的对象，返回的对象能够在非阻塞模式下运行。

换句话说：

ServerSocketChannel 的 accept()方法会返回 SocketChannel 类型对象，SocketChannel 可以在非阻塞模式下运行。

其它 Socket 的 accept()方法会阻塞返回一个 Socket 对象。如果ServerSocketChannel 以非阻塞模式被调用，当没有传入连接在等待时，ServerSocketChannel.accept( )会立即返回 null。正是这种检查连接而不阻塞的能力实现了可伸缩性并降低了复杂性。可选择性也因此得到实现。我们可以使用一个选择器实例来注册 ServerSocketChannel 对象以实现新连接到达时自动通知的功能。

```java
public class ServerSocketChannelDemo {

    public static void main(String[] args) throws Exception {
        //端口号
        int port = 8888;

        //buffer
        ByteBuffer buffer = ByteBuffer.wrap("hello world".getBytes());

        //打开 ServerSocketChannel.
        ServerSocketChannel ssc = ServerSocketChannel.open();
        //绑定
        ssc.socket().bind(new InetSocketAddress(port));

        //设置非阻塞模式
        ssc.configureBlocking(false);

        //监听有新链接传入
        while(true) {
            System.out.println("Waiting for connections");
            //阻塞模式会在 SocketChannel sc = ssc.accept();这里阻塞住进程
            //非阻塞模式下，accept() 方法会立刻返回，如果还没有新进来的连接,返回的将是 null。 因此，需要检查返回的SocketChannel 是否是 null
            SocketChannel sc = ssc.accept();
            if(sc == null) { //没有链接传入
                System.out.println("null");
                Thread.sleep(2000);
            } else {
                System.out.println("Incoming connection from: " + sc.socket().getRemoteSocketAddress());
                buffer.rewind(); //指针0  position = 0;
                sc.write(buffer);
                sc.close();
            }
        }
    }
}
//输出---------------------
Waiting for connections
null
Waiting for connections
Incoming connection from: /127.0.0.1:59389
Waiting for connections
null
```

### (2)SocketChannel

SocketChannel能通过 TCP 读写网络中的数据

#### 1.介绍

SocketChannel 是一种面向流连接sockets 套接字的可选择通道

-  SocketChannel 是用来连接 Socket 套接字
- SocketChannel 主要用途用来处理网络 I/O 的通道
- SocketChannel 是基于 TCP 连接传输
- SocketChannel 实现了可选择通道，可以被多路复用的

#### **2.特征**

（1）对于已经存在的 socket 不能创建 SocketChannel

（2）SocketChannel 中提供的 open 接口创建的 Channel 并没有进行网络级联，需要使用 connect 接口连接到指定地址

（3）未进行连接的 SocketChannle 执行 I/O 操作时，会抛出NotYetConnectedException

（4）SocketChannel 支持两种 I/O 模式：阻塞式和非阻塞式

（5）SocketChannel 支持异步关闭。如果 SocketChannel 在一个线程上 read 阻塞，另一个线程对该 SocketChannel 调用 shutdownInput，则读阻塞的线程将返回-1 表示没有读取任何数据；如果 SocketChannel 在一个线程上 write 阻塞，另一个线程对该SocketChannel 调用 shutdownWrite，则写阻塞的线程将抛出AsynchronousCloseException

（6）SocketChannel 支持设定参数

- SO_SNDBUF 套接字发送缓冲区大小
- SO_RCVBUF 套接字接收缓冲区大小
- SO_KEEPALIVE 保活连接
- O_REUSEADDR 复用地址
- SO_LINGER 有数据传输时延缓关闭 Channel (只有在非阻塞模式下有用)
- TCP_NODELAY 禁用 Nagle 算法

#### 3.使用

**（1）创建 SocketChannel**

方式一

```java
SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("www.baidu.com", 80));
```

方式二

```java

SocketChannel socketChanne2 = SocketChannel.open();
socketChanne2.connect(new InetSocketAddress("www.baidu.com", 80));
```

直接使用有参 open api 或者使用无参 open api，但是在无参 open 只是创建了一个SocketChannel 对象，并没有进行实质的 tcp 连接。

**（2）连接校验**

```java
socketChannel.isOpen(); // 测试 SocketChannel 是否为 open 状态
socketChannel.isConnected(); //测试 SocketChannel 是否已经被连接
socketChannel.isConnectionPending(); //测试 SocketChannel 是否正在进行连接
socketChannel.finishConnect(); //校验正在进行套接字连接的 SocketChannel是否已经完成连接
```

**（3）读写模式**

```java
socketChannel.configureBlocking(false);//false 表示非阻塞，true 表示阻塞
```

**（4）读写**

```java
//读操作
ByteBuffer byteBuffer = ByteBuffer.allocate(16);
socketChannel.read(byteBuffer);
socketChannel.close();
System.out.println("read over");
```

**（5）设置和获取参数**

```java
socketChannel.getOption(StandardSocketOptions.SO_KEEPALIVE);
socketChannel.getOption(StandardSocketOptions.SO_RCVBUF);
```

通过 setOptions 方法可以设置 socket 套接字的相关参数

可以通过 getOption 获取相关参数的值。如默认的接收缓冲区大小是 8192byte。SocketChannel 还支持多路复用

### (3)DatagramChannel

DatagramChannel 能通过 UDP 读写网络中的数据。

正如 SocketChannel 对应 Socket，ServerSocketChannel 对应 ServerSocket，每一个 DatagramChannel 对象也有一个关联的 DatagramSocket 对象。正如SocketChannel 模拟连接导向的流协议（如 TCP/IP），DatagramChannel 则模拟包导向的无连接协议（如 UDP/IP）。DatagramChannel 是无连接的，每个数据报（datagram）都是一个自包含的实体，拥有它自己的目的地址及不依赖其他数据报的数据负载。与面向流的的 socket 不同，DatagramChannel 可以发送单独的数据报给不同的目的地址。同样，DatagramChannel 对象也可以接收来自任意地址的数据包。每个到达的数据报都含有关于它来自何处的信息（源地址）

**1、打开 DatagramChannel**

```java
//打开DatagramChannel
DatagramChannel receiveChannel = DatagramChannel.open();
InetSocketAddress receiveAddress = new InetSocketAddress(9999);
//绑定
receiveChannel.bind(receiveAddress);
```

**2、接收数据**

通过 receive()接收 UDP 包

```java
public void receiveDatagram() throws Exception {
        //打开DatagramChannel
        DatagramChannel receiveChannel = DatagramChannel.open();
        InetSocketAddress receiveAddress = new InetSocketAddress(9999);
        //绑定
        receiveChannel.bind(receiveAddress);

        //buffer
        ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);

        //接收
        while(true) {
            receiveBuffer.clear();
            SocketAddress socketAddress = receiveChannel.receive(receiveBuffer);
            receiveBuffer.flip();
            
            System.out.println(socketAddress.toString());
            System.out.println(Charset.forName("UTF-8").decode(receiveBuffer));
        }
    }
```

SocketAddress 可以获得发包的 ip、端口等信息，用 toString 查看

**3、发送数据**

```java
public void sendDatagram() throws Exception {
    //打开 DatagramChannel
    DatagramChannel sendChannel = DatagramChannel.open();
    InetSocketAddress sendAddress =
            new InetSocketAddress("127.0.0.1",9999);

    //发送
    while(true) {
        ByteBuffer buffer = ByteBuffer.wrap("发送hello".getBytes("UTF-8"));
        sendChannel.send(buffer,sendAddress);
        System.out.println("已经完成发送");
        Thread.sleep(1000);
    }
}
```

**4、连接**

UDP 不存在真正意义上的连接，这里的连接是向特定服务地址用 read 和 write 接收发送数据包。

```java
public void testConnect() throws Exception {
    //打开DatagramChannel
    DatagramChannel connChannel = DatagramChannel.open();
    //绑定
    connChannel.bind(new InetSocketAddress(9999));
    //连接
    connChannel.connect(new InetSocketAddress("127.0.0.1",9999));
    //write方法 
    connChannel.write(ByteBuffer.wrap("发送hello".getBytes("UTF-8")));
    //buffer
    ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    while(true) {
        readBuffer.clear();
        connChannel.read(readBuffer);//读取数据到readbuffer中
        readBuffer.flip();
        System.out.println(Charset.forName("UTF-8").decode(readBuffer));

    }
}
```

read()和 write()只有在 connect()后才能使用，不然会抛NotYetConnectedException 异常。用 read()接收时，如果没有接收到包，会抛

PortUnreachableException 异常。

##  2.3 **Scatter/Gather**

Java NIO 开始支持 scatter/gather，scatter/gather 用于描述从 Channel 中读取或者写入到 Channel 的操作。

**分散（scatter）**从 Channel 中读取是指在读操作时将读取的数据**写入多个 buffer**中。因此，Channel 将从 Channel 中读取的数据“分散（scatter）”到多个 Buffer中。

```java
ByteBuffer header = ByteBuffer.allocate(128);
ByteBuffer body = ByteBuffer.allocate(1024);
ByteBuffer[] bufferArray = { header, body };//buffer 首先被插入到数组
channel.read(bufferArray);
//read()方法按照 buffer 在数组中的顺序将从 channel 中读取的数据写入到 buffer，当一个 buffer 被写满后，channel 紧接着向另一个 buffer 中写。
//Scattering Reads 在移动下一个 buffer 前，必须填满当前的 buffer，这也意味着它不适用于动态消息(消息大小不固定)。换句话说，如果存在消息头和消息体，消息头必须完成填充（例如 128byte），Scattering Reads 才能正常工作。
```

**聚集（gather）**写入 Channel 是指在写操作时将多个 buffer 的数据**写入同一个Channel**，因此，Channel 将多个 Buffer 中的数据“聚集（gather）”后发送到Channel。

scatter / gather 经常用于需要将传输的数据分开处理的场合，例如传输一个由消息头和消息体组成的消息，你可能会将消息体和消息头分散到不同的 buffer 中，这样你可以方便的处理消息头和消息体。

```java
ByteBuffer header = ByteBuffer.allocate(128);
ByteBuffer body = ByteBuffer.allocate(1024);
//write data into buffers
ByteBuffer[] bufferArray = { header, body };
channel.write(bufferArray);
//buffers 数组是 write()方法的入参，write()方法会按照 buffer 在数组中的顺序，将数据写入到 channel，注意只有 position 和 limit 之间的数据才会被写入。因此，如果一个 buffer 的容量为 128byte，但是仅仅包含 58byte 的数据，那么这 58byte 的数据将被写入到 channel 中。因此与 Scattering Reads 相反，Gathering Writes 能较好的处理动态消息。
```



# 3、Java NIO（Buffer） 

## 3.1 介绍

Java NIO 中的 Buffer 用于和 NIO 通道进行交互。数据是从通道读入缓冲区，从缓冲区写入到通道中的。

缓冲区本质上是**一块可以写入数据，然后可以从中读取数据的内存**。这块内存被包装成 NIO Buffer 对象，并提供了一组方法，用来方便的访问该块内存。缓冲区实际上是一个容器对象，更直接的说，其实就是一个数组，在 NIO 库中，**所有数据都是用缓冲区处理的**。在读取数据时，它是直接读到缓冲区中的； 在写入数据时，它也是写入到缓冲区中的；任何时候访问 NIO 中的数据，都是将它放到缓冲区中。而在面向流 I/O系统中，所有数据都是直接写入或者直接将数据读取到 Stream 对象中。

## 3.2 用法

（1）写入数据到 Buffer

（2）调用 flip()方法

（3）从 Buffer 中读取数据

（4）调用 clear()方法或者 compact()方法

当向 buffer 写入数据时，buffer 会记录下写了多少数据。一旦要读取数据，需要通过flip()方法将 Buffer 从写模式切换到读模式。在读模式下，可以读取之前写入到 buffer的所有数据。一旦读完了所有的数据，就需要清空缓冲区，让它可以再次被写入。有两种方式能清空缓冲区：调用 clear()或 compact()方法。**clear()方法会清空整个缓冲区。compact()方法只会清除已经读过的数据**。任何未读的数据都被移到缓冲区的起始处，新写入的数据将放到缓冲区未读数据的后面。

## 3.3  **capacity、position 和 limit**

position 和 limit 的含义取决于 Buffer 处在读模式还是写模式。不管 Buffer 处在什么模式，capacity 的含义总是一样的。

**（1）capacity**

作为一个内存块，Buffer 有一个固定的大小值，也叫“capacity”.你只能往里写capacity 个 byte、long，char 等类型。一旦 Buffer 满了，需要将其清空（通过读数据或者清除数据）才能继续写数据往里写数据。

**（2）position**

（1）写数据到 Buffer 中时，position 表示写入数据的当前位置，position 的初始值为0。当一个 byte、long 等数据写到 Buffer 后， position 会向下移动到下一个可插入数据的 Buffer 单元。position 最大可为 capacity – 1（因为 position 的初始值为0）. 

（2）读数据到 Buffer 中时，position 表示读入数据的当前位置，如 position=2 时表示已开始读入了 3 个 byte，或从第 3 个 byte 开始读取。通过 ByteBuffer.flip()切换到读模式时 position 会被重置为 0，当 Buffer 从 position 读入数据后，position 会下移到下一个可读入的数据 Buffer 单元。

**（3）limit**

（1）写数据时，limit 表示可对 Buffer 最多写入多少个数据。写模式下，limit 等于Buffer 的 capacity。 

（2）读数据时，limit 表示 Buffer 里有多少可读数据（not null 的数据），因此能读到之前写入的所有数据（limit 被设置成已写数据的数量，这个值在写模式下就是position）。 

## 3.4 类型

Java NIO 有以下 Buffer 类型 ByteBuffer， MappedByteBuffer， CharBuffer， DoubleBuffer， FloatBuffer， IntBuffer， LongBuffer，ShortBuffer

这些 Buffer 类型代表了不同的数据类型。可以通过 char，short，int，long，float 或 double 类型来操作缓冲区中的字节。

## **3.5 Buffer 分配和写数据**

**1.分配**

要想获得一个 Buffer 对象首先要进行分配。 每一个 Buffer 类都有一个 allocate 方法。

```java
//分配 48 字节 capacity 的 ByteBuffer 的例子。

ByteBuffer buf = ByteBuffer.allocate(48);

//分配一个可存储 1024 个字符的 CharBuffer：

CharBuffer buf = CharBuffer.allocate(1024);
```

**2、向 Buffer 中写数据**

**写数据到 Buffer 有两种方式：**

```java
//从 Channel 写到 Buffer 
int bytesRead = inChannel.read(buf); //read into buffer.
//通过 put 方法写 Buffer
buf.put(127);
```

**3、flip()方法**

flip 方法将 Buffer 从**写模式切换到读模式**。调用 flip()方法会将 position 设回 0，并将 limit 设置成之前 position 的值。换句话说，position 现在用于标记读的位置，limit 表示之前写进了多少个 byte、char 等 （现在能读取多少个 byte、char 等）。

## **3.6 从 Buffer 中读取数据**

**从 Buffer 中读取数据有两种方式：**

```java
//从 Buffer 读取数据到 Channel 
//read from buffer into channel.
int bytesWritten = inChannel.write(buf);
//使用 get()方法从 Buffer 中读取数据/
byte aByte = buf.get();
//get 方法有很多版本，允许你以不同的方式从 Buffer 中读取数据。例如，从指定position 读取，或者从 Buffer 中读取数据到字节数组。
```

## **3.7 Buffer 几个方法**

**1、rewind()方法**

Buffer.rewind()将 position 设回 0，所以你可以重读 Buffer 中的所有数据。limit 保持不变，仍然表示能从 Buffer 中读取多少个元素（byte、char 等）。

**2、clear()与 compact()方法**

一旦读完 Buffer 中的数据，需要让 Buffer 准备好再次被写入。可以通过 clear()或compact()方法来完成。

如果调用的是 clear()方法，position 将被设回 0，limit 被设置成 capacity 的值。换句话说，Buffer 被清空了。Buffer 中的数据**并未清除**，只是这些标记告诉我们可以从哪里开始往 Buffer 里写数据。如果 Buffer 中有一些未读的数据，调用 clear()方法，数据将“被遗忘”，意味着不再有任何标记会告诉你哪些数据被读过，哪些还没有。如果 Buffer 中仍有未读的数据，且后续还需要这些数据，但是此时想要先先写些数据，那么使用 compact()方法。

compact()方法将所有未读的数据拷贝到 Buffer 起始处。然后将 position 设到最后一个未读元素正后面。limit 属性依然像 clear()方法一样，设置成 capacity。现在Buffer 准备好写数据了，但是不会覆盖未读的数据。

**3、mark()与 reset()方法**

通过调用 Buffer.mark()方法，可以标记 Buffer 中的一个特定 position。之后可以通过调用 Buffer.reset()方法恢复到这个 position。

```java
buffer.mark();
//call buffer.get() a couple of times, e.g. during parsing.
buffer.reset(); //set position back to mark.
```

## **3.8 缓冲区操作**

### **1、缓冲区分片**

在 NIO 中，除了可以分配或者包装一个缓冲区对象外，还可以根据现有的缓冲区对象来创建一个子缓冲区，即在现有缓冲区上切出一片来作为一个新的缓冲区，但现有的缓冲区与创建的子缓冲区在底层数组层面上是数据共享的，也就是说，子缓冲区相当于是现有缓冲区的一个视图窗口。调用 slice()方法可以创建一个子缓冲区。

```java
//缓冲区分片
@Test
public void b01() {
    ByteBuffer buffer = ByteBuffer.allocate(10);

    for (int i = 0; i < buffer.capacity(); i++) {
        buffer.put((byte)i);
    }

    //创建子缓冲区
    buffer.position(3);
    buffer.limit(7);
    ByteBuffer slice = buffer.slice();

    //改变子缓冲区内容
    for (int i = 0; i <slice.capacity() ; i++) {
        byte b = slice.get(i);
        b *=10;
        slice.put(i,b);
    }

    buffer.position(0);
    buffer.limit(buffer.capacity());

    while(buffer.remaining()>0) {
        System.out.println(buffer.get());
    }
}
//输出：
0  1  2  30  40  50  60  7  8  9
```

### **2、只读缓冲区**

只读缓冲区非常简单，可以读取它们，但是不能向它们写入数据。可以通过调用缓冲区的 asReadOnlyBuffer()方法，将任何常规缓冲区转 换为只读缓冲区，这个方法返回一个与原缓冲区完全相同的缓冲区，并与原缓冲区共享数据，只不过它是只读的。如果原缓冲区的内容发生了变化，只读缓冲区的内容也随之发生变化

```java
//只读缓冲区
@Test
public void b02() {
    ByteBuffer buffer = ByteBuffer.allocate(10);
    for (int i = 0; i < buffer.capacity(); i++) {
        buffer.put((byte)i);
    }
    //创建只读缓冲区
    ByteBuffer readonly = buffer.asReadOnlyBuffer();
    for (int i = 0; i < buffer.capacity(); i++) {
        byte b = buffer.get(i);
        b *=10;
        buffer.put(i,b);
    }
    readonly.position(0);
    readonly.limit(buffer.capacity());
    while (readonly.remaining()>0) {
        System.out.println(readonly.get());
    }
}
//输出
0  10  20  30  40  50  60  70  80  90
```

如果尝试修改只读缓冲区的内容，则会报 ReadOnlyBufferException 异常。只读缓冲区对于保护数据很有用。在将缓冲区传递给某个 对象的方法时，无法知道这个方法是否会修改缓冲区中的数据。创建一个只读的缓冲区可以保证该缓冲区不会被修改。只可以把常规缓冲区转换为只读缓冲区，而不能将只读的缓冲区转换为可写的缓冲区。

### **3、直接缓冲区**

直接缓冲区是为加快 I/O 速度，使用一种特殊方式为其分配内存的缓冲区，JDK 文档中的描述为：给定一个直接字节缓冲区，Java 虚拟机将尽最大努力直接对它执行本机I/O 操作。也就是说，它会在每一次调用底层操作系统的本机 I/O 操作之前(或之后)，尝试避免将缓冲区的内容拷贝到一个中间缓冲区中 或者从一个中间缓冲区中拷贝数据。要分配直接缓冲区，需要调用 allocateDirect()方法，而不是 allocate()方法，使用方式与普通缓冲区并无区别。

```java
//直接缓冲区  从01.txt复制内容到02.txt
@Test
public void b03() throws Exception {
    String infile = "E:\\IdeaProjects\\nio\\src\\01.txt";
    FileInputStream fin = new FileInputStream(infile);
    FileChannel finChannel = fin.getChannel();

    String outfile = "E:\\IdeaProjects\\nio\\src\\02.txt";
    FileOutputStream fout = new FileOutputStream(outfile);
    FileChannel foutChannel = fout.getChannel();

    //创建直接缓冲区
    ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

    while (true) {
        buffer.clear();
        int r = finChannel.read(buffer);
        if(r == -1) {
            break;
        }
        buffer.flip();
        foutChannel.write(buffer);
    }
}
```

### **4、内存映射文件 I/O**

内存映射文件 I/O 是一种读和写文件数据的方法，它可以比常规的基于流或者基于通道的 I/O 快的多。内存映射文件 I/O 是通过使文件中的数据出现为 内存数组的内容来完成的，这其初听起来似乎不过就是将整个文件读到内存中，但是事实上并不是这样。一般来说，只有文件中实际读取或者写入的部分才会映射到内存中。

```java
//内存映射文件io
static private final int start = 0;
static private final int size = 1024;
@Test
public void b04() throws Exception {
    RandomAccessFile raf = new RandomAccessFile("E:\\IdeaProjects\\nio\\src\\01.txt", "rw");
    FileChannel fc = raf.getChannel();
   /*FileChannel中的几个变量：
       MapMode mode：内存映像文件访问的方式，共三种：
        MapMode.READ_ONLY：只读，试图修改得到的缓冲区将导致抛出异常。
        MapMode.READ_WRITE：读/写，对得到的缓冲区的更改最终将写入文件；但该更改对映射到同一文件的其他程序不一定是可见的。
        MapMode.PRIVATE：私用，可读可写,但是修改的内容不会写入文件，只是buffer自身的改变，这种能力称之为”copy on write”。
       position：文件映射时的起始位置。
       allocationGranularity：Memory allocation size for mapping buffers，通过native函数initIDs初始化
      */
    MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, start, size);

    mbb.put(0, (byte) 97);
    mbb.put(1023, (byte) 122);
    raf.close();
}
```

# 4、Java NIO（Selector） 

## 4.1 简介

### **1、Selector和Channel关系**

Selector 一般称 为选择器 ，也可以翻译为 多路复用器 。它是 Java NIO 核心组件中的一个，用于检查一个或多个 NIO Channel（通道）的状态是否处于可读、可写。如此可以实现单线程管理多个 channels,也就是可以管理多个网络链接。

<img src="imag/image-20220224212420290.png" alt="image-20220224212420290" style="zoom:80%;" />

使用 Selector 的好处在于： 使用更少的线程来就可以来处理通道了， 相比使用多个线程，避免了线程上下文切换带来的开销。

### **2、可选择通道(SelectableChannel)**

（1）不是所有的 Channel 都可以被 Selector 复用的。比方说，FileChannel 就不能被选择器复用。判断一个 Channel 能被 Selector 复用，有一个前提：判断他**是否继承了一个抽象类 SelectableChannel**。如果继承了 SelectableChannel，则可以被复用，否则不能。

（2）SelectableChannel 类提供了实现通道的可选择性所需要的公共方法。它是所有支持就绪检查的通道类的父类。所有 socket 通道，都继承了 SelectableChannel 类都是可选择的，包括从管道(Pipe)对象的中获得的通道。而 FileChannel 类，没有继承 SelectableChannel，因此是不是可选通道。

（3）一个通道可以被注册到多个选择器上，但对每个选择器而言只能被注册一次。通道和选择器之间的关系，使用注册的方式完成。SelectableChannel 可以被注册到Selector 对象上，在注册的时候，需要指定通道的哪些操作，是 Selector 感兴趣的。

### **3、Channel注册到Selector**

（1）使用` Channel.register（Selector sel，int ops）`方法，将一个通道注册到一个选择器时。第一个参数，指定通道要注册的选择器。第二个参数指定选择器需要查询的通道操作。

（2）可以供选择器查询的通道操作，从类型来分，包括以下四种：

- 可读 : SelectionKey.OP_READ

- 可写 : SelectionKey.OP_WRITE

- 连接 : SelectionKey.OP_CONNECT

- 接收 : SelectionKey.OP_ACCEPT

如果 Selector 对通道的多操作类型感兴趣，可以用“位或”操作符来实现：

```java
int key = SelectionKey.OP_READ | SelectionKey.OP_WRITE ;
```

（3）选择器查询的不是通道的操作，而是通道的某个操作的一种就绪状态。比方说，一个有数据可读的通道，可以说是“读就绪”(OP_READ)。一个等待写数据的通道可以说是“写就绪”(OP_WRITE)。 

### **4、选择键(SelectionKey)**

（1）Channel 注册到后，并且一旦通道处于某种就绪的状态，就可以被选择器查询到。这个工作，使用选择器 Selector 的 select（）方法完成。select 方法的作用，对感兴趣的通道操作，**进行就绪状态的查询**。

（2）Selector 可以不断的查询 Channel 中发生的操作的就绪状态。并且挑选感兴趣的操作就绪状态。一旦通道有操作的就绪状态达成，并且是 Selector 感兴趣的操作，就会被 Selector 选中，放入**选择键集合**中。

（3）一个选择键，首先是包含了注册在 Selector 的通道操作的类型，比方说SelectionKey.OP_READ。也包含了特定的通道与特定的选择器之间的注册关系。

（4）选择键的概念，和事件的概念比较相似。一个选择键类似监听器模式里边的一个事件。由于 Selector 不是事件触发的模式，而是主动去查询的模式，所以不叫事件Event，而是叫 SelectionKey 选择键。

## 4.2 使用方法

###  1.Selector的创建

```java
//创建selector
Selector selector = Selector.open();
```

### 2.注册Channel到Selector

```java
//创建selector
Selector selector = Selector.open();
//通道
ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
//非阻塞
serverSocketChannel.configureBlocking(false);
//绑定连接
serverSocketChannel.bind(new InetSocketAddress(9999));
//将通道注册到选择器上
serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
```

（1）与 Selector 一起使用时，Channel 必须处于**非阻塞模式**下，否则将抛出异常IllegalBlockingModeException。这意味着，FileChannel 不能与 Selector 一起使用，因为 FileChannel 不能切换到非阻塞模式，而套接字相关的所有的通道都可以。

（2）一个通道，并没有一定要支持所有的四种操作。比如服务器通道ServerSocketChannel 支持 Accept 接受操作，而 SocketChannel 客户端通道则不支持。可以通过通道上的 validOps()方法，来获取特定通道下所有支持的操作集合。

### 3.轮询查询就绪操作

- select():阻塞到至少有一个通道在你注册的事件上就绪了。

- select(long timeout)：和 select()一样，但最长阻塞事件为 timeout 毫秒。

- selectNow():非阻塞，只要有通道就绪就立刻返回。

select()方法返回的 int 值，表示有多少通道已经就绪，更准确的说，是自前一次 select方法以来到这一次 select 方法之间的时间段上，有多少通道变成就绪状态。

一旦调用 select()方法，并且返回值不为 0 时，在 Selector 中有一个 selectedKeys()方法，用来访问已选择键集合，迭代集合的每一个选择键元素

```java
//查询已经就绪通道操作
Set<SelectionKey> selectionKeys = selector.selectedKeys();
//遍历集合
Iterator<SelectionKey> iterator = selectionKeys.iterator();
while(iterator.hasNext()) {
    SelectionKey key = iterator.next();
    //判断key就绪状态操作
    if(key.isAcceptable()) {
        // a connection was accepted by a ServerSocketChannel.
    } else if (key.isConnectable()) {
        // a connection was established with a remote server.
    } else if (key.isReadable()) {
        // a channel is ready for reading
    } else if (key.isWritable()) {
        // a channel is ready for writing
    }
    iterator.remove();
}
```

### 4.停止选择的方法

选择器执行选择的过程，系统底层会依次询问每个通道是否已经就绪，这个过程可能会造成调用线程进入阻塞状态,那么我们有以下三种方式可以唤醒在 select（）方法中阻塞的线程。

**wakeup()方法** ：通过调用 Selector 对象的 wakeup（）方法让处在阻塞状态的select()方法立刻返回该方法使得选择器上的第一个还没有返回的选择操作立即返回。如果当前没有进行中的选择操作，那么下一次对 select()方法的一次调用将立即返回。

**close()方法** ：通过 close（）方法关闭 Selector，该方法使得任何一个在选择操作中阻塞的线程都被唤醒（类似 wakeup（）），同时使得注册到该 Selector 的所有 Channel 被注销，所有的键将被取消，但是 Channel本身并不会关闭。

## 4.3 示例代码

```java
public class SelectorDemo2 {

    //服务端代码
    @Test
    public void serverDemo() throws Exception {
        //1 获取服务端通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //2 切换非阻塞模式
        serverSocketChannel.configureBlocking(false);
        //3 创建buffer
        ByteBuffer serverByteBuffer = ByteBuffer.allocate(1024);
        //4 绑定端口号
        serverSocketChannel.bind(new InetSocketAddress(8080));
        //5 获取selector选择器
        Selector selector = Selector.open();
        //6 通道注册到选择器，进行监听
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        //7 选择器进行轮询，进行后续操作
        while(selector.select()>0) {
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            //遍历
            Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();
            while(selectionKeyIterator.hasNext()) {
                //获取就绪操作
                SelectionKey next = selectionKeyIterator.next();
                //判断什么操作
                if(next.isAcceptable()) {
                    //获取连接  每一个新进来的连接都会创建一个 SocketChannel
                    SocketChannel accept = serverSocketChannel.accept();
                    //切换非阻塞模式
                    accept.configureBlocking(false);
                    //注册
                    accept.register(selector, SelectionKey.OP_READ);
                } else if(next.isReadable()) {
                    SocketChannel channel = (SocketChannel) next.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    //读取数据
                    int length = 0;
                    while((length = channel.read(byteBuffer))>0) {
                        byteBuffer.flip();
                        System.out.println(new String(byteBuffer.array(),0,length));
                        byteBuffer.clear();
                    }
                }
                selectionKeyIterator.remove();
            }
        }
    }

    //客户端代码
    @Test
    public void clientDemo() throws Exception {
        //1 获取通道，绑定主机和端口号
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1",8080));
        //2 切换到非阻塞模式
        socketChannel.configureBlocking(false);
        //3 创建buffer
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        //4 写入buffer数据
        byteBuffer.put(new Date().toString().getBytes());
        //5 模式切换
        byteBuffer.flip();
        //6 写入通道
        socketChannel.write(byteBuffer);
        //7 关闭
        byteBuffer.clear();
    }

    public static void main(String[] args) throws IOException {
        //1 获取通道，绑定主机和端口号
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1",8080));
        //2 切换到非阻塞模式
        socketChannel.configureBlocking(false);
        //3 创建buffer
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        Scanner scanner = new Scanner(System.in);
        while(scanner.hasNext()) {
            String str = scanner.next();
            //4 写入buffer数据
            byteBuffer.put((new Date().toString()+"--->"+str).getBytes());
            //5 模式切换
            byteBuffer.flip();
            //6 写入通道
            socketChannel.write(byteBuffer);
            //7 关闭
            byteBuffer.clear();
        }
    }
}
//客户端输入
hello
hi
abcd
//服务端输出
Fri Feb 25 21:27:35 CST 2022--->hello
Fri Feb 25 21:27:37 CST 2022--->hi
Fri Feb 25 21:27:41 CST 2022--->abcd
```

## 4.4 步骤

第一步：创建 Selector 选择器

第二步：创建 ServerSocketChannel 通道，并绑定监听端口

第三步：设置 Channel 通道是非阻塞模式

第四步：把 Channel 注册到 Socketor 选择器上，监听连接事件

第五步：调用 Selector 的 select 方法（循环调用），监测通道的就绪状况

第六步：调用 selectKeys 方法获取就绪 channel 集合

第七步：遍历就绪 channel 集合，判断就绪事件类型，实现具体的业务操作

第八步：根据业务，决定是否需要再次注册监听事件，重复执行第三步操作

# 5、Java NIO（Pipe 和 FileLock） 

## 5.1 pipe

Java NIO 管道是 2 个线程之间的单向数据连接。Pipe 有一个 source 通道和一个sink 通道。数据会被写到 sink 通道，从 source 通道读取。

![image-20220226175638830](imag/image-20220226175638830.png)

### **1、创建管道**

通过 Pipe.open()方法打开管道。

```java
Pipe pipe = Pipe.open();
```

### **2、写入管道**

要向管道写数据，需要访问 sink 通道。：

```java
Pipe.SinkChannel sinkChannel = pipe.sink();
```

通过调用 SinkChannel 的 write()方法，将数据写入 SinkChannel：

```java
 sinkChannel.write(byteBuffer);
```

### **3、从管道读取数据**

从读取管道的数据，需要访问 source 通道

```java
Pipe.SourceChannel sourceChannel = pipe.source();
```

调用 source 通道的 read()方法来读取数据：

```java
int length = sourceChannel.read(byteBuffer2);//read()方法返回的 int 值会告诉我们多少字节被读进了缓冲区。
```

### 示例

```java
public class PipeDemo {
    public static void main(String[] args) throws IOException {
        //1 获取管道
        Pipe pipe = Pipe.open();

        //2 获取sink通道,用来传送数据
        Pipe.SinkChannel sinkChannel = pipe.sink();

        //3 创建缓冲区
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.put("hello".getBytes());
        byteBuffer.flip();

        //4 sink 发送数据
        sinkChannel.write(byteBuffer);

        //5 创建接收 pipe 数据的 source 管道
        Pipe.SourceChannel sourceChannel = pipe.source();

        //6 接收数据，并保存到缓冲区中
        ByteBuffer byteBuffer2 = ByteBuffer.allocate(1024);
        int length = sourceChannel.read(byteBuffer2);
        System.out.println(new String(byteBuffer2.array(),0,length));

        //7 关闭通道
        sourceChannel.close();
        sinkChannel.close();
    }
}
```

## 5.2 FileLock

### **1、FileLock 简介**

文件锁在 OS 中很常见，如果多个程序同时访问、修改同一个文件，很容易因为文件数据不同步而出现问题。给文件加一个锁，同一时间，只能有一个程序修改此文件，或者程序都只能读此文件，这就解决了同步问题。

**文件锁是进程级别的，不是线程级别的**。文件锁可以解决多个进程并发访问、修改同一个文件的问题，但不能解决多线程并发访问、修改同一文件的问题。使用文件锁时，同一进程内的多个线程，可以同时访问、修改此文件。

文件锁是当前程序所属的 JVM 实例持有的，一旦获取到文件锁（对文件加锁），要调用 release()，或者关闭对应的 FileChannel 对象，或者当前 JVM 退出，才会释放这个锁。

一旦某个进程（比如说 JVM 实例）对某个文件加锁，则在释放这个锁之前，此进程不能再对此文件加锁，就是说 JVM 实例在同一文件上的文件锁是不重叠的（进程级别不能重复在同一文件上获取锁）。

### **2、文件锁分类：**

**排它锁**：又叫独占锁。对文件加排它锁后，该进程可以对此文件进行读写，该进程独占此文件，其他进程不能读写此文件，直到该进程释放文件锁。

**共享锁**：某个进程对文件加共享锁，其他进程也可以访问此文件，但这些进程都只能读此文件，不能写。线程是安全的。只要还有一个进程持有共享锁，此文件就只能读，不能写。

### **3、获取文件锁方法**

**有 4 种获取文件锁的方法：**

```java
lock() //对整个文件加锁，默认为排它锁。
lock(long position, long size, booean shared) //自定义加锁方式。前 2 个参数指定要加锁的部分（可以只对此文件的部分内容加锁），第三个参数值指定是否是共享锁。
tryLock() //对整个文件加锁，默认为排它锁。
tryLock(long position, long size, booean shared) //自定义加锁方式。如果指定为共享锁，则其它进程可读此文件，所有进程均不能写此文件，如果某进程试图对此文件进行写操作，会抛出异常。
```

lock 与 tryLock 的区别：

- lock 是阻塞式的，如果未获取到文件锁，会一直阻塞当前线程，直到获取文件锁
- tryLock 和 lock 的作用相同，只不过 tryLock 是非阻塞式的，tryLock 是尝试获取文件锁，获取成功就返回锁对象，否则返回 null，不会阻塞当前线程。

### **4、FileLock 两个方法：**

```java
boolean isShared() //此文件锁是否是共享锁
boolean isValid() //此文件锁是否还有效在某些 OS 上，对某个文件加锁后，不能对此文件使用通道映射。
```

### 5、示例

```java
package com.hyb.filelock;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileLockDemo {

    public static void main(String[] args) throws Exception {
        String input = "hello";
        System.out.println("input:"+input);

        ByteBuffer buffer = ByteBuffer.wrap(input.getBytes());

        String filePath = "E:\\IdeaProjects\\nio\\src\\01.txt";
        Path path = Paths.get(filePath);

        FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE,StandardOpenOption.APPEND);
        channel.position(channel.size()-1);

        //加锁
        //FileLock lock = channel.lock(0L,Long.MAX_VALUE,true);//有参 lock()为共享锁，有写操作会报异常
        FileLock lock = channel.lock();//独占锁
        System.out.println("是否共享锁："+lock.isShared());

        channel.write(buffer);
        channel.close();

        //读文件
        readFile(filePath);
    }

    private static void readFile(String filePath) throws Exception {
        FileReader fileReader = new FileReader(filePath);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String tr = bufferedReader.readLine();
        System.out.println("读取出内容：");
        while(tr != null) {
            System.out.println(" "+tr);
            tr = bufferedReader.readLine();
        }
        fileReader.close();
        bufferedReader.close();
    }
}
//输出
input:hello
是否共享锁：false
读取出内容：
hello
```

# 6、Java NIO（其他）

## 6.1 Path 

### **1、Path 简介**

Java Path 接口是 Java NIO 更新的一部分，同 Java NIO 一起已经包括在 Java6 和Java7 中。Java Path 接口是在 Java7 中添加到 Java NIO 的。Path 接口位于java.nio.file 包中，所以 Path 接口的完全限定名称为 java.nio.file.Path。

Java Path 实例表示**文件系统中的路径**。一个路径可以指向一个文件或一个目录。路径可以是绝对路径，也可以是相对路径。绝对路径包含从文件系统的根目录到它指向的文件或目录的完整路径。相对路径包含相对于其他路径的文件或目录的路径。

在许多方面，java.nio.file.Path 接口类似于 java.io.File 类，但是有一些差别。不过，在许多情况下，可以使用 Path 接口来替换 File 类的使用。

### **2、创建 Path 实例**

使用 java.nio.file.Path 实例必须创建一个 Path 实例。可以使用 Paths 类(java.nio.file.Paths)中的静态方法 Paths.get()来创建路径实例。

可以理解为，Paths.get()方法是 Path 实例的工厂方法

### **3、创建绝对路径**

（1）创建绝对路径，通过调用 Paths.get()方法，给定绝对路径文件作为参数来完成。

### **4、Path.normalize()**

Path 接口的 normalize()方法可以使路径标准化。标准化意味着它将移除所有在路径

字符串的中间的.和..代码，并解析路径字符串所引用的路径。

```java
public class PathDemo {

    public static void main(String[] args) {
        //创建path实例
        Path path = Paths.get("E:\\IdeaProjects\\nio\\src\\01.txt");

        //创建相对路径
        //代码1
        Path projects = Paths.get("E:\\IdeaProjects\\nio\\src", "projects");
        System.out.println(projects);

        //代码2
        Path file = Paths.get("E:\\IdeaProjects\\nio\\src", "projects\\02.txt");
        System.out.println(file);

        String originalPath = "E:\\IdeaProjects\\nio\\src\\projects\\..\\yygh-project";

        Path path1 = Paths.get(originalPath);
        System.out.println("path1 = " + path1);

        Path path2 = path1.normalize();
        System.out.println("path2 = " + path2);

    }
}
//输出
E:\IdeaProjects\nio\src\projects
E:\IdeaProjects\nio\src\projects\02.txt
path1 = E:\IdeaProjects\nio\src\projects\..\yygh-project
path2 = E:\IdeaProjects\nio\src\yygh-project
```

## 6.2 Files

### **1、Files.createDirectory()**

Files.createDirectory()方法，用于根据 Path 实例创建一个新目录

```java
Path path = Paths.get("d:\\****\\****");
try {
 Path newDir = Files.createDirectory(path);
} catch(FileAlreadyExistsException e){
 // 目录已经存在
} catch (IOException e) {
 // 其他发生的异常
 e.printStackTrace();
}
```

第一行创建表示要创建的目录的 Path 实例。在 try-catch 块中，用路径作为参数调用Files.createDirectory()方法。如果创建目录成功，将返回一个 Path 实例，该实例指向新创建的路径。

如果该目录已经存在，则是抛出一个 java.nio.file.FileAlreadyExistsException。如果出现其他错误，可能会抛出 IOException。例如，如果想要的新目录的父目录不存在，则可能会抛出 IOException。

### **2、Files.copy()**

**（1）Files.copy()方法从一个路径拷贝一个文件到另外一个目录**

```java
Path sourcePath = Paths.get("d:\\***\\1.txt");
Path destinationPath = Paths.get("d:\\***\\2.txt");
try {
 Files.copy(sourcePath, destinationPath);
} catch(FileAlreadyExistsException e) {
 // 目录已经存在
} catch (IOException e) {
 // 其他发生的异常
 e.printStackTrace();
}
```

 Files.copy()，将两个 Path实例作为参数传递。这可以让源路径引用的文件被复制到目标路径引用的文件中。

如果目标文件已经存在，则抛出一个 java.nio.file.FileAlreadyExistsException 异常。如果有其他错误，则会抛出一个 IOException。例如，如果将该文件复制到不存在的目录，则会抛出 IOException。

**（2）覆盖已存在的文件**

```java
Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
```

Files.copy()方法的第三个参数。如果目标文件已经存在，这个参数指示 copy()方法**覆盖现有的文件**。

### **3、Files.move()**

Files.move()用于将文件从一个路径移动到另一个路径。移动文件与重命名相同，但是移动文件既可以移动到不同的目录，也可以在相同的操作中更改它的名称。

```java
Files.move(sourcePath, destinationPath,StandardCopyOption.REPLACE_EXISTING);//Files.move()的第三个参数。这个参数告诉 Files.move()方法来覆盖目标路径上的任何现有文件。
```

### **4、Files.delete()**

Files.delete()方法可以删除一个文件或者目录。

```java
Files.delete(path);//创建指向要删除的文件的 Path。然后调用 Files.delete()方法。如果 Files.delete()不能删除文件(例如，文件或目录不存在)，会抛出一个 IOException。
```

### **5、Files.walkFileTree()**

（1）Files.walkFileTree()方法包含递归遍历目录树功能，将 Path 实例和 FileVisitor作为参数。Path 实例指向要遍历的目录，FileVisitor 在遍历期间被调用。

（2）FileVisitor 是一个接口，必须自己实现 FileVisitor 接口，并将实现的实例传递给walkFileTree()方法。在目录遍历过程中，您的 FileVisitor 实现的每个方法都将被调用。如果不需要实现所有这些方法，那么可以扩展 SimpleFileVisitor 类，它包含FileVisitor 接口中所有方法的默认实现。

（3）FileVisitor 接口的方法中，每个都返回一个 FileVisitResult 枚举实例。FileVisitResult 枚举包含以下四个选项:

- CONTINUE 继续
- TERMINATE 终止
- SKIP_SIBLING 跳过同级
- SKIP_SUBTREE 跳过子级

```java
public class FileDemo {

    public static void main(String[] args) {
        Path rootPath = Paths.get("E:\\IdeaProjects\\nio\\src");
        String fileToFind = File.separator + "01.txt";
        System.out.println(fileToFind);// \01.txt
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileString = file.toAbsolutePath().toString();
                    //System.out.println("pathString = " + fileString);
                    if(fileString.endsWith(fileToFind)){//以 \01.txt 结尾的
                        System.out.println("file found at path: " + file.toAbsolutePath());
                        return FileVisitResult.TERMINATE;//终止
                    }
                    return FileVisitResult.CONTINUE;//继续
                }
            });
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
```

## 6.3 AsynchronousFileChannel

在 Java 7 中，Java NIO 中添加了 AsynchronousFileChannel，也就是是异步地将数据写入文件。

### **1、创建 AsynchronousFileChannel**

通过静态方法 open()创建

```java
AsynchronousFileChannel.open(path, StandardOpenOption.READ);
```

open()方法的第一个参数指向与 AsynchronousFileChannel 相关联文件的 Path 实例。第二个参数是一个或多个打开选项，它告诉 AsynchronousFileChannel 在文件上执行什么操作。在本例中，我们使用了 StandardOpenOption.READ 选项，表示该文件将被打开阅读。

### **2、通过 Future 读取数据**

可以通过两种方式从 AsynchronousFileChannel 读取数据。第一种方式是调用返回

Future 的 read()方法

```java
Future<Integer> future = fileChannel.read(buffer, 0);
```

```java
 @Test
    public void readAsyncFileChannelFuture() throws Exception {
        //1 创建AsynchronousFileChannel
        Path path = Paths.get("E:\\IdeaProjects\\nio\\src\\01.txt");
        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);
        //2 创建Buffer
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        //3 调用channel的read方法得到Future 
        Future<Integer> future = fileChannel.read(buffer, 0);
        //4 判断是否完成 isDone,返回true
        while(!future.isDone());
        //5 读取数据到buffer里面
        buffer.flip();
//        while(buffer.remaining()>0) {
//            System.out.println(buffer.get());
//        }
        byte[] data = new byte[buffer.limit()];
        buffer.get(data);
        System.out.println(new String(data));
        buffer.clear();

    }
```

### **3、通过 CompletionHandler 读取数据**

第二种方法是调用 read()方法，该方法将一个 CompletionHandler 作为参数

```java
@Test
public void readAsyncFileChannelComplate() throws Exception {
    //1 创建AsynchronousFileChannel
    Path path = Paths.get("E:\\IdeaProjects\\nio\\src\\01.txt");
    AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);

    //2 创建Buffer
    ByteBuffer buffer = ByteBuffer.allocate(1024);

    //3 调用channel的read方法得到Future
    fileChannel.read(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            System.out.println("result: "+result);
            attachment.flip();
            byte[] data = new byte[attachment.limit()];
            attachment.get(data);
            System.out.println(new String(data));
            attachment.clear();
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {

        }
    });
}
//输出
result: 20
hellohellohellohello
```

（1）读取操作完成，将调用 CompletionHandler 的 completed()方法。

（2）对于 completed()方法的参数传递一个整数，它告诉我们读取了多少字节，以及传递给 read()方法的“附件”。“附件”是 read()方法的第三个参数。在本代码中，它是 ByteBuffer，数据也被读取。

（3）如果读取操作失败，则将调用 CompletionHandler 的 failed()方法。

### **4、通过 Future 写数据**

和读取一样，可以通过两种方式将数据写入一个 AsynchronousFileChannel

```java
@Test
public void writeAsyncFileFuture() throws IOException {
    //1 创建AsynchronousFileChannel
    Path path = Paths.get("E:\\IdeaProjects\\nio\\src\\01.txt");
    AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.WRITE);

    //2 创建Buffer
    ByteBuffer buffer = ByteBuffer.allocate(1024);

    //3 write方法
    buffer.put("hello ".getBytes());
    buffer.flip();
    Future<Integer> future = fileChannel.write(buffer, 0);

    while(!future.isDone());

    buffer.clear();
    System.out.println("write over");
}
```

首先，AsynchronousFileChannel 以写模式打开。然后创建一个 ByteBuffer，并将一些数据写入其中。然后，ByteBuffer 中的数据被写入到文件中。最后，示例检查返回的 Future，以查看写操作完成时的情况。

注意，文件必须已经存在。如果该文件不存在，那么 write()方法将抛出一个java.nio.file.NoSuchFileException。 

### **5、通过 CompletionHandler 写数据**

```java
@Test
public void writeAsyncFileComplate() throws IOException {
    //1 创建AsynchronousFileChannel
    Path path = Paths.get("E:\\IdeaProjects\\nio\\src\\01.txt");
    AsynchronousFileChannel fileChannel =
            AsynchronousFileChannel.open(path, StandardOpenOption.WRITE);

    //2 创建Buffer
    ByteBuffer buffer = ByteBuffer.allocate(1024);

    //3 write方法
    buffer.put("hellohello".getBytes());
    buffer.flip();

    fileChannel.write(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            System.out.println("bytes written: " + result);
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {

        }
    });

    System.out.println("write over");
}
```

当写操作完成时，将会调用 CompletionHandler 的 completed()方法。如果写失败，则会调用 failed()方法。

## **6.4** **字符集（**Charset）

java 中使用 Charset 来表示字符集编码对象

### **Charset 常用静态方法**

```java
public static Charset forName(String charsetName)//通过编码类型获得 Charset 对 象
public static SortedMap<String,Charset> availableCharsets()//获得系统支持的所有编码方式
public static Charset defaultCharset()//获得虚拟机默认的编码方式
public static boolean isSupported(String charsetName)//判断是否支持该编码类型
```

### **Charset** **常用普通方法**

```java
public final String name()//获得 Charset 对象的编码类型(String)
public abstract CharsetEncoder newEncoder()//获得编码器对象
public abstract CharsetDecoder newDecoder()//获得解码器对象
```

```java
public class CharsetDemo {

    public static void main(String[] args) throws CharacterCodingException {
        //1 获取charset对象
        Charset charset = Charset.forName("UTF-8");

        //2 获得编码器对象
        CharsetEncoder charsetEncoder = charset.newEncoder();

        //3 创建缓冲区
        CharBuffer charBuffer = CharBuffer.allocate(1024);
        charBuffer.put("hello你好");
        charBuffer.flip();

        //4 编码
        ByteBuffer byteBuffer = charsetEncoder.encode(charBuffer);
        System.out.println("编码之后结果：");
        for (int i = 0; i < byteBuffer.limit(); i++) {
            System.out.println(byteBuffer.get());
        }

        //5 获取解码器对象
        byteBuffer.flip();
        CharsetDecoder charsetDecoder = charset.newDecoder();

        //6 解码
        CharBuffer charBuffer1 = charsetDecoder.decode(byteBuffer);
        System.out.println("解码之后结果：");    //hello你好
        System.out.println(charBuffer1.toString());

        //7 使用GBK解码
        Charset charset1 = Charset.forName("GBK");
        byteBuffer.flip();
        CharBuffer charBuffer2 = charset1.decode(byteBuffer);
        System.out.println("使用其他编码进行解码：");      //hello浣犲ソ
        System.out.println(charBuffer2.toString());

        //8.获取Charset所支持的字符编码
        Map<String ,Charset> map= Charset.availableCharsets();
        Set<Map.Entry<String,Charset>> set=map.entrySet();
        for (Map.Entry<String, Charset> entry: set) {
            System.out.println(entry.getKey()+"="+entry.getValue().toString());
        }

    }
}
```