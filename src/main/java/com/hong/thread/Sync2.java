package com.hong.thread;

public class Sync2 {

    private final Object lock1 = new Object();
    private final Object lock2 = new Object();

    public static void main(String[] args) {
        Sync2 sync2 = new Sync2();
        new Thread(() -> {
            while (true) {
                sync2.method1();
            }
        }).start();

        new Thread(() -> {
            while (true) {
                sync2.method2();
            }
        }).start();
    }

    public void method1() {
        synchronized (lock1) {
            System.out.println("this is method1...");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            method2();
        }
    }

    public void method2() {
        synchronized (lock2) {
            System.out.println("this is method2...");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            method1();
        }
    }
}
