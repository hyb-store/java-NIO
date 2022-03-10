package com.hyb.channel;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

//通道之间数据传输
public class FileChannelDemo3 {

    //transferFrom()
    public static void main(String[] args) throws Exception {
        // 创建两个fileChannel
        RandomAccessFile aFile = new RandomAccessFile("E:\\IdeaProjects\\nio\\src\\01w.txt","rw");
        FileChannel fromChannel = aFile.getChannel();

        RandomAccessFile bFile = new RandomAccessFile("E:\\IdeaProjects\\nio\\src\\02.txt","rw");
        FileChannel toChannel = bFile.getChannel();

        //fromChannel 传输到 toChannel
        long position = 0;
        long size = fromChannel.size();
        toChannel.transferFrom(fromChannel,position,size);

        aFile.close();
        bFile.close();
        System.out.println("over!");
    }
}