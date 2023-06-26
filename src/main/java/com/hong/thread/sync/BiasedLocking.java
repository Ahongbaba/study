package com.hong.thread.sync;

import com.hong.thread.MyUnsafe;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * 偏向锁对象，用于判断是否可用偏向锁
 */
public class BiasedLocking {

    /**
     * 偏向锁加锁
     *
     * @param myLock myLock
     * @return 成功/失败
     */
    public boolean revokeAndRebias(MyLock myLock) {
        MarkWord markWord = myLock.getMarkWord();
        long threadId = markWord.getThreadId();
        // 获取偏向锁标记
        String biasedLock = markWord.getBiasedLock();
        // 获取锁标记
        String lockFlag = markWord.getLockFlag();

        Field markWordThreadId;
        try {
            markWordThreadId = markWord.getClass().getDeclaredField("threadId");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        Unsafe unsafe = MyUnsafe.getUnsafe();
        assert unsafe != null;
        long offset = unsafe.objectFieldOffset(markWordThreadId);
        long longVolatile = unsafe.getLongVolatile(markWord, offset);
        long currentThreadId = Thread.currentThread().getId();

        // 此时表示可偏向，但是还没有偏向任何线程。这里就是流程图里的判断null。
        if (threadId == -1 && "1".equals(biasedLock) && "01".equals(lockFlag)) {
            // 执行cas操作，将自己的线程id写入markword中
            boolean isOk = unsafe.compareAndSwapLong(markWord, offset, longVolatile, currentThreadId);
            if (isOk) {
                return true;
            }
        }

        // 此时表示可偏向，并且已经偏向某个线程
        if (threadId != -1 && "1".equals(biasedLock) && "01".equals(lockFlag)) {
            // 判断偏向的是否是当前线程
            if (threadId == currentThreadId) {
                return true;
            }
            // 已偏向的不是当前线程，撤销偏向锁
            // 这里需要判断线程是否已经执行完同步代码块，在java层面很难判断
            return revokeBiased(myLock);
        }

        return false;
    }

    /**
     * 撤销偏向锁
     * 难点：
     * 1、安全点检查（stw检查）
     * 2、是否已经离开了同步代码块（判断的是拥有偏向锁的线程是否离开）
     *
     * @return true=撤销成无锁/false=升级成轻量级锁
     */
    private boolean revokeBiased(MyLock myLock) {
        boolean isAlive = false;
        MarkWord markWord = myLock.getMarkWord();
        long threadId = markWord.getThreadId();

        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        int activeCount = threadGroup.activeCount();
        Thread[] threads = new Thread[activeCount];
        threadGroup.enumerate(threads);
        for (Thread thread : threads) {
            if (thread.getId() == threadId) {
                // 表示拥有这把锁的线程依然存活
                isAlive = true;
                break;
            }
        }

        // 判断线程是否离开同步代码块（java里很难做，简单点判断线程是否存活）
        if (isAlive) {
            // 存活，设置成无锁状态
            markWord.setBiasedLock("0");
            markWord.setLockFlag("01");
            markWord.setThreadId(-1);

            return true;
        }

        // 走轻量级锁
        return false;
    }
}
