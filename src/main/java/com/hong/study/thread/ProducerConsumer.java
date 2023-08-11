package com.hong.study.thread;

import java.util.ArrayList;
import java.util.List;

/**
 * 生产者消费者演示wait notify机制
 * 一个生产者线程，一个消费者线程，同时最多只能生产和消费1个产品
 */
public class ProducerConsumer {

    private final static Object OBJECT = new Object();
    private static boolean hasProduct = false;
    private static List<Object> productList = new ArrayList<>();

    public static void main(String[] args) {

        new Thread(() -> {
            while (true) {
                synchronized (OBJECT) {
                    if (hasProduct) {
                        try {
                            OBJECT.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        // 生产
                        Object o = new Object();
                        productList.add(o);
                        System.out.println("生产了--->" + o);
                        hasProduct = true;
                        OBJECT.notify();
                    }
                }
            }
        }, "producer").start();

        new Thread(() -> {
            while (true) {
                synchronized (OBJECT) {
                    if (hasProduct) {
                        // 消费
                        Object o = productList.get(0);
                        productList.remove(0);
                        System.out.println("消费了--->" + o);
                        hasProduct = false;
                        OBJECT.notify();
                    } else {
                        try {
                            OBJECT.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }, "consumer").start();

    }
}
