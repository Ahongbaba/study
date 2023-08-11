package com.hong.study.thread.sync.aqs;

public class Test {

    private static int COUNT = 100;
    static final MyLock myLock = new MyLock();

    public static void main(String[] args) {
        Test test = new Test();
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                test.test1();
            }).start();
        }
    }

    public void test1() {

        while (true) {
            try {
                myLock.lock();
                if (COUNT <= 0) {
                    break;
                }

                System.out.println("current thread->" + Thread.currentThread().getName() + ",count->" + (--COUNT));
            } finally {
                myLock.unlock();
            }
        }

    }
}
