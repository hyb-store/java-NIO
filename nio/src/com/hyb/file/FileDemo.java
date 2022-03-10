package com.hyb.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

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
