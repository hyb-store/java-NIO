package com.hyb.pipe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

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
