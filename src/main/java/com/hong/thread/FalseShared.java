package com.hong.thread;

/**
 * @Author hong
 * @Date 2023/5/26 19:44
 */
public class FalseShared {

    public static void main(String[] args) throws InterruptedException {
        final long start = System.currentTimeMillis();
        final Count count = new Count();
        final Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100000000; i++) {
                count.a++;
            }
        });

        final Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100000000; i++) {
                count.b++;
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();
        // 3332 3562 3759
        System.out.println("item:" + (System.currentTimeMillis() - start));
    }
}

class Count {
    volatile long a;
//    public long p1,p2,p3,p4,p5,p6,p7;
//    @jdk.internal.vm.annotation.Contended
    volatile long b;
}