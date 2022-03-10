package com.hyb.channel;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

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