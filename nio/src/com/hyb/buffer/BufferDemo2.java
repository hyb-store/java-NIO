package com.hyb.buffer;

import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class BufferDemo2 {

    static private final int start = 0;
    static private final int size = 1024;

    //内存映射文件io
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

    //直接缓冲区
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
}
