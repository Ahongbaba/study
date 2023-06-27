package com.hong.thread.sync;

import com.hong.thread.MyUnsafe;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class MySynchronized {

    static MyLock myLock = new MyLock();

    /**
     * 是否开启偏向
     */
    private boolean useBiasedLocking = true;

    /**
     * 偏向锁对象
     */
    static BiasedLocking biasedLocking = new BiasedLocking();

    static ThreadLocal<LockRecord> threadLocal = ThreadLocal.withInitial(() -> {
        // 使用内部类初始化
        MarkWord owner = null;
        MarkWord markWordClone = null;

        return new LockRecord(markWordClone, owner);
    });

    /**
     * 加锁入口
     */
    public void monitorEnter() {
        /**
         * 锁升级后续实现
         * 无锁->偏向锁->轻量级锁->重量级锁
         */
        if (useBiasedLocking) {
            // 偏向锁
            fastEnter();
        } else {
            // 轻量级锁
            slowEnter();
        }
    }

    /**
     * 释放锁入口
     */
    public void monitorExit() {
        MarkWord markWord = myLock.getMarkWord();
        String biasedLock = markWord.getBiasedLock();
        String lockFlag = markWord.getLockFlag();
        long threadId = markWord.getThreadId();
        Thread currentThread = Thread.currentThread();

        if ("1".equals(biasedLock) && "01".equals(lockFlag)) {
            // 偏向锁状态
            if (threadId != currentThread.getId()) {
                // 释放锁的不是拥有锁的线程，报错
                throw new RuntimeException("非法释放锁");
            }
        } else {
            // 轻量级锁和重量级锁的释放
            slowExit(markWord);
        }
    }

    /**
     * 轻量级锁和重量级锁的释放
     */
    private void slowExit(MarkWord markWord) {
        fastExit(markWord);
    }

    /**
     * 轻量级锁的释放
     */
    private void fastExit(MarkWord markWord) {
        // 需要将markWord还原：markWord替换（cas替换），lockFlag改成01
        LockRecord lockRecord = threadLocal.get();
        // 当前栈帧中的markWord(head),还原到对象头中
        MarkWord head = lockRecord.getMarkWord();
        if (head != null) {
            // cas变更markWord
            Unsafe unsafe = MyUnsafe.getUnsafe();
            Field markWordField;
            try {
                markWordField = myLock.getClass().getDeclaredField("markWord");
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
            assert unsafe != null;
            long offset = unsafe.objectFieldOffset(markWordField);
            Object objectVolatile = unsafe.getObjectVolatile(myLock, offset);
            boolean isOk = unsafe.compareAndSwapObject(myLock, offset, objectVolatile, head);
            if (isOk) {
                // head = null
                lockRecord.setMarkWord(null);
                lockRecord.setOwner(null);
                markWord.setLockFlag("01");
                return;
            }
            // cas修改失败，轻量级锁膨胀
            inflateExit();
        }
    }

    /**
     * 偏向锁加锁
     */
    private void fastEnter() {
        if (useBiasedLocking) {
            boolean isOk = biasedLocking.revokeAndRebias(myLock);
            if (isOk) {
                // 偏向锁加锁成功，可以执行代码块了
                return;
            }
        }

        // 偏向锁加锁失败，走轻量级锁
        slowEnter();
    }


    /**
     * 轻量级锁加锁
     */
    private void slowEnter() {
        // 这里有很多逻辑，先不写

        // 如果是偏向锁或者无锁状态 lockFlag=01
        MarkWord markWord = myLock.getMarkWord();
        String lockFlag = markWord.getLockFlag();
        if ("01".equals(lockFlag)) {
            markWord.setThreadId(-1);
            markWord.setBiasedLock(null);
            // cas变更lockRecord指针
            Unsafe unsafe = MyUnsafe.getUnsafe();
            Field ptrLockRecord;
            try {
                assert unsafe != null;
                ptrLockRecord = unsafe.getClass().getDeclaredField("ptrLockRecord");
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
            long offset = unsafe.objectFieldOffset(ptrLockRecord);
            Object currentLockRecord = unsafe.getObjectVolatile(markWord, offset);
            // 获取当前线程的lockRecord
            LockRecord lockRecord = threadLocal.get();
            boolean isOk = unsafe.compareAndSwapObject(markWord, offset, currentLockRecord, lockRecord);
            if (isOk) {
                // 成功，设置成轻量级锁
                MarkWord markWordClone = markWord.clone();
                lockRecord.setMarkWord(markWordClone);
                markWord.setLockFlag("00");
                lockRecord.setOwner(markWord);
                return;
            }
        } else if ("00".equals(lockFlag)) {
            // 已经是轻量级状态，需要进一步判断是重入还是膨胀
            markWord.setThreadId(-1);
            markWord.setBiasedLock(null);

            // 先获取是当前线程中的lockRecord
            LockRecord lockRecord = threadLocal.get();

            // 再获取markWord中的lockRecord指针
            LockRecord ptrLockRecord = markWord.getPtrLockRecord();

            // 判断当前线程是否已经拥有锁，重入
            if (ptrLockRecord != null && (lockRecord == ptrLockRecord)) {
                return;
            }
        }

        // 轻量级锁失败，开始膨胀
        inflateEnter();
    }

    /**
     * 锁膨胀
     */
    private void inflateEnter() {
        ObjectMonitor objectMonitor = inflate();
        objectMonitor.enter(new MyLock());
    }

    /**
     * 退出时膨胀
     */
    private void inflateExit() {
        ObjectMonitor objectMonitor = inflate();
        // 重量级锁退出
        objectMonitor.exit(myLock);
    }

    /**
     * 锁膨胀过程
     *
     * @return objectMonitor
     */
    private ObjectMonitor inflate() {
        for (; ; ) {
            MarkWord markWord = myLock.getMarkWord();
            ObjectMonitor ptrMonitor = markWord.getPtrMonitor();
            // 1、如果已经膨胀完毕（已经生成了内置锁：ObjectMonitor）
            if (ptrMonitor != null) {
                return ptrMonitor;
            }

            // 2、正在膨胀中
            String status = markWord.getStatus();
            if ("inflating".equals(status)) {
                continue;
            }

            // 3、当前是轻量级锁
            LockRecord ptrLockRecord = markWord.getPtrLockRecord();
            String lockFlag = markWord.getLockFlag();
            if ("00".equals(lockFlag) && ptrLockRecord != null) {
                // cas自旋更改markWord状态
                Unsafe unsafe = MyUnsafe.getUnsafe();
                Field statusField;
                try {
                    statusField = markWord.getClass().getDeclaredField("status");
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
                assert unsafe != null;
                long offset = unsafe.objectFieldOffset(statusField);
                Object objectVolatile = unsafe.getObjectVolatile(markWord, offset);
                boolean isOk = unsafe.compareAndSwapObject(markWord, offset, objectVolatile, "inflating");
                if (isOk) {
                    // 更新成功
                    ObjectMonitor objectMonitor = new ObjectMonitor();
                    markWord.setPtrMonitor(objectMonitor);
                    markWord.setLockFlag("10");
                    markWord.setLockRecord(null);

                    return objectMonitor;
                }
            }
        }
    }

}
