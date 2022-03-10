package com.hyb.channel;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

//FileChanne读操作
public class FileChannelDemo1 {

    //在使用 FileChannel 之前，必须先打开它。但是，我们无法直接打开一个FileChannel，需要通过使用一个 InputStream、OutputStream或RandomAccessFile 来获取一个 FileChannel 实例。
    //FileChannel读取数据到buffer中
    public static void main(String[] args) throws Exception {
        //创建FileChannel
        RandomAccessFile aFile = new RandomAccessFile("E:\\IdeaProjects\\nio\\src\\01.txt", "rw");
        FileChannel channel = aFile.getChannel();

        //创建Buffer
        ByteBuffer buf = ByteBuffer.allocate(1024);

        //读取数据到buffer中
        int bytesRead = channel.read(buf);
        while(bytesRead != -1) { //有多少字节被读到了Buffer中  如果==-1 读到末尾，结束
            System.out.println("读取了："+bytesRead);
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
