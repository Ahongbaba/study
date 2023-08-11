package com.hong.study.thread;

/**
 * 生产者消费者演示wait notify机制
 * 多个生产者线程，多个消费者线程，同时可以生产和消费多个产品
 * 需求：100个生产者，100个消费者，然后同时最多只能生产10个产品
 */
public class ProducerConsumer1 {

    private final static Object OBJECT = new Object();
    private static int PRODUCT_COUNT = 0;

    public static void main(String[] args) {
        ProducerConsumer1 producerConsumer1 = new ProducerConsumer1();

        for (int i = 0; i < 100; i++) {
            // 生产者线程
            new Thread(() -> {
                while (true) {
                    synchronized (OBJECT) {
                        if (PRODUCT_COUNT < 10) {
                            producerConsumer1.producer();
                            OBJECT.notifyAll();
                            break;
                        } else {
                            try {
                                OBJECT.wait();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }).start();
        }

        for (int i = 0; i < 100; i++) {
            // 消费者线程
            new Thread(() -> {
                while (true) {
                    synchronized (OBJECT) {
                        if (PRODUCT_COUNT > 0) {
                            producerConsumer1.consumer();
                            OBJECT.notifyAll();
                            break;
                        } else {
                            try {
                                OBJECT.wait();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }).start();
        }

    }

    public void producer() {
        System.out.println(Thread.currentThread().getName() + "-生产了->" + ++PRODUCT_COUNT);
    }

    public void consumer() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(Thread.currentThread().getName() + "-消费了->" + PRODUCT_COUNT--);
    }
}
