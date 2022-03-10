package com.hyb.path;

import java.nio.file.Path;
import java.nio.file.Paths;

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
