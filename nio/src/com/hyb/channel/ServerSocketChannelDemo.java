package com.hyb.channel;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

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
