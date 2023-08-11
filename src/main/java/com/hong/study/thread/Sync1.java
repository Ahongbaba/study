package com.hong.study.thread;

public class Sync1 {

    private int num = 0;

    public static void main(String[] args) {
        final Sync1 sync1 = new Sync1();
        new Thread(() -> {
            sync1.addNum(1);
        }, "thread1").start();

        new Thread(() -> {
            sync1.addNum(2);
        }, "thread2").start();
    }

    public void addNum(int i) {
        if (i == 1) {
            num = 100;
        } else {
            num = 200;
        }
        /**
         * 正常情况：
         * current thread->thread1,i->1,num->100
         * current thread->thread2,i->2,num->200
         * 异常情况：
         * current thread->thread1,i->1,num->100
         * current thread->thread2,i->2,num->100
         * <p>
         * current thread->thread1,i->1,num->200
         * current thread->thread2,i->2,num->200
         */
        System.out.println("current thread->" + Thread.currentThread().getName() + ",i->" + i + ",num->" + num);
    }
}
