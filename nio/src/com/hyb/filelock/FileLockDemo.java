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
