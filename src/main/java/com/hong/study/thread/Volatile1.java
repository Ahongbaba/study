package com.hong.study.thread;

/**
 * @Author hong
 * @Date 2023/5/23 21:57
 */
public class Volatile1 {

    static int i = 0;

    public static void main(String[] args) {

        new Thread(() -> {
            while (true) {
//                System.out.println("haha");
//                File file = new File("");
                if (i == 1) {
                    System.out.println(Thread.currentThread().getName() + ">>>>> i = " + i);
                    break;
                }
//                file.canExecute();
//                synchronized (Volatile1.class) {
//                }
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
            }
        }, "A").start();

        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            i = 1;
        }, "B").start();

    }
}
