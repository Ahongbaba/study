package com.hong.thread.sync.aqs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * aqs实现锁
 */
public class MyLock implements Lock {

    private MySync mySync = null;

    public MyLock() {
        mySync = new MySync();
    }

    /**
     * aqs = 队列 + state
     */
    private static class MySync extends AbstractQueuedSynchronizer {
        /**
         * 加锁逻辑
         */
        public void lock() {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
            } else {
                // 入队
                acquire(1);
            }
        }

        /**
         * cas尝试获取锁
         *
         * @param arg the acquire argument. This value is always the one
         *            passed to an acquire method, or is the value saved on entry
         *            to a condition wait.  The value is otherwise uninterpreted
         *            and can represent anything you like.
         * @return 成功 失败
         */
        @Override
        protected boolean tryAcquire(int arg) {
            Thread thread = Thread.currentThread();
            int state = getState();
            if (state == 0) {
                if (compareAndSetState(0, arg)) {
                    setExclusiveOwnerThread(thread);
                    return true;
                }
            } else if (thread == getExclusiveOwnerThread()) {
                // 重入
                int next = state + arg;
                if (next < 0) {
                    throw new Error("锁重入错误");
                }
                setState(next);
                return true;
            }

            return false;
        }

        /**
         * cas尝试释放锁
         *
         * @param arg the release argument. This value is always the one
         *            passed to a release method, or the current state value upon
         *            entry to a condition wait.  The value is otherwise
         *            uninterpreted and can represent anything you like.
         * @return 成功 失败
         */
        @Override
        protected boolean tryRelease(int arg) {
            int c = getState() - arg;
            if (Thread.currentThread() != getExclusiveOwnerThread()) {
                // 不是锁的持有者
                throw new IllegalMonitorStateException();
            }

            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }

            setState(c);

            return free;
        }
    }

    /**
     * 加锁入口
     */
    @Override
    public void lock() {
        mySync.lock();
    }

    /**
     * 解锁入口
     */
    @Override
    public void unlock() {

    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
