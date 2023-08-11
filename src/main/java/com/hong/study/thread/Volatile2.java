package com.hong.study.thread;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author hong
 * @Date 2023/5/23 22:50
 */
public class Volatile2 {

    static int i = 0;
    private static final int count = 600; // 100 200 300 ... 10000 100000

    public static void main(String[] args) {
        new Thread(() -> {
            final List<Integer> list = new ArrayList<>();
            for (int j = 1; j <= count; j++) {
                list.add(j);
            }

            while (true) {
                if (list.contains(i)) {
                    System.out.println(Thread.currentThread().getName() + ">>>> i = " + i);
                    break;
                }
            }
        }, "A").start();

        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (int j = 1; j <= count; j++) {
                i = j;
            }
        }, "B").start();
    }
}
