package com.hong.thread.sync;

import com.hong.thread.MyUnsafe;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class MySynchronized {

    static MyLock myLock = new MyLock();

    static ThreadLocal<LockRecord> threadLocal = ThreadLocal.withInitial(() -> {
        // 使用内部类初始化
        MarkWord markWord = myLock.getMarkWord();
        MarkWord owner = null;
        MarkWord markWordClone = markWord.clone();

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
        if (false) {
            // 偏向锁

        } else {
            // 轻量级锁
            slowEnter();
        }
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
     * 锁膨胀过程
     *
     * @return objectMonitor
     */
    private ObjectMonitor inflate() {
        // 具体膨胀过程待实现
        ObjectMonitor objectMonitor = new ObjectMonitor();

        return objectMonitor;
    }

    /**
     * 释放锁
     */
    public void monitorExit() {

    }
}
