package com.hong.thread;

import lombok.Data;

import java.util.concurrent.LinkedBlockingQueue;

@Data
public class ObjectMonitor {
    private int recursions = 0;

    private Thread owner = null;

    private LinkedBlockingQueue<Thread> cxq;
    private LinkedBlockingQueue<Thread> entryList;
    private LinkedBlockingQueue<Thread> waitSet;

    /**
     * 重量级锁入口
     */
    public void enter() {
    }
}
