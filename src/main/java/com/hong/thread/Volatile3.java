package com.hong.thread;

/**
 * @Author hong
 * @Date 2023/5/29 20:54
 */
public class Volatile3 {

    static int a,b,x,y = 0;

    public static void main(String[] args) {

        new Thread(() -> {
            a = 1;
            x = b;
        }).start();

        new Thread(() -> {
            b = 1;
            y = a;
        }).start();
    }

}
