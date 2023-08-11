package com.hong.study.thread.sync;

import lombok.Data;

/**
 * 对象头
 */
@Data
public class MarkWord implements Cloneable {

    /**
     * 锁标记位
     * 01：无锁或者偏向锁
     * 00：轻量级锁
     * 10：重量级锁
     * 11：GC标记
     */
    private String lockFlag = "01";

    /**
     * 是否偏向
     */
    private String biasedLock = "1";

    private String epoch;

    private volatile long threadId = -1;

    /**
     * 指向轻量级锁的指针
     */
    private volatile LockRecord ptrLockRecord;

    /**
     * 分代年龄
     */
    private String age;


    /**
     * 指向轻量级锁的指针
     */
    private volatile LockRecord lockRecord;

    /**
     * 指向重量级锁monitor
     */
    private ObjectMonitor ptrMonitor = null;

    /**
     * 锁膨胀状态
     */
    private volatile String status = null;

    @Override
    public MarkWord clone() {
        try {
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return (MarkWord) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
