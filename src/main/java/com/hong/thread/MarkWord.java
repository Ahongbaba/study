package com.hong.thread;

import lombok.Data;

@Data
public class MarkWord {

    private String lockFlag = "01";

    /**
     * 指向轻量级锁的指针
     */
    private volatile LockRecord lockRecord;

    /**
     * 指向重量级锁monitor
     */
    private ObjectMonitor ptrMonitor = null;
}
