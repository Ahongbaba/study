package com.hong.thread.sync;

import com.hong.thread.MyUnsafe;
import lombok.Data;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.LinkedBlockingQueue;

@Data
public class ObjectMonitor {
    // 线程重入次数
    private int recursions = 0;

    // 当前获取到锁的线程，加volatile是为了拿到最新值，因为这个值是被多线程修改的
    private volatile Thread owner = null;

    // 多线程竞争锁进入时的单向链表，实际上是一个栈，先进后出
    private LinkedBlockingQueue<ObjectWaiter> cxq = new LinkedBlockingQueue<>();
    // 所有在等待获取锁的线程的对象，也就是说如果有线程处于等待获取锁的状态的时候，将被挂入这个队列。
    private LinkedBlockingQueue<ObjectWaiter> entryList;
    // 主要存放所有wait的线程的对象，也就是说如果有线程处于wait状态，将被挂入这个队列
    private LinkedBlockingQueue<ObjectWaiter> waitSet;

    /**
     * 重量级锁入口
     */
    public void enter(CustomLock customLock) {
        // 1、CAS修改owner字段为当前线程
        Thread currentThread = cmpAndChgOwner(customLock);
        if (currentThread == null) {
            return;
        }

        // 2、如果之前的owner指向当前线程。那么就表示是重入，recursions++
        if (currentThread == Thread.currentThread()) {
            recursions++;
            return;
        }

        // 3、从轻量级锁膨胀
        LockRecord lockRecord = MySynchronized.threadLocal.get();
        MarkWord head = lockRecord.getMarkWord();
        if (head != null) {
            recursions = 1;
            owner = Thread.currentThread();
            return;
        }

        // 4、预备入队挂起
        enterI(customLock);
    }

    /**
     * 真正开始入队挂起
     *
     * @param customLock myLock
     */
    private void enterI(CustomLock customLock) {
        // 自旋抢锁
        if (tryLock(customLock) > 0) {
            return;
        }
        // 延迟处理其他逻辑
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 又自旋10次
        if (tryLock(customLock) > 0) {
            return;
        }

        // 自旋全部失败，入队挂起
        ObjectWaiter objectWaiter = new ObjectWaiter(Thread.currentThread());
        for (; ; ) {
            try {
                cxq.put(objectWaiter);
                break;
            } catch (InterruptedException e) {
                // 又又自旋
                if (tryLock(customLock) > 0) {
                    return;
                }
            }
        }

        // 真正阻塞
        for (; ; ) {
            // 又又又自旋
            if (tryLock(customLock) > 0) {
                break;
            }
            Unsafe unsafe = MyUnsafe.getUnsafe();
            assert unsafe != null;
            // 挂起进入内核态，挂起后线程卡在此处，等待唤醒
            unsafe.park(false, 0L);

            // 唤醒后立马抢锁
            if (tryLock(customLock) > 0) {
                break;
            }
        }
    }

    /**
     * 自旋抢锁
     *
     * @param customLock myLock
     * @return 1=成功 0和-1=失败
     */
    private int tryLock(CustomLock customLock) {
        for (int i = 0; i < 10; i++) {
            // 如果有线程还拥有重量级锁，直接退出
            if (owner != null) {
                return 0;
            }
            // 自旋
            Thread thread = cmpAndChgOwner(customLock);
            if (thread == null) {
                // cas获取锁成功
                return 1;
            }
        }

        return -1;
    }

    public Thread cmpAndChgOwner(CustomLock customLock) {
        ObjectMonitor objectMonitor = customLock.getMarkWord().getPtrMonitor();
        Field owner;
        try {
            owner = objectMonitor.getClass().getDeclaredField("owner");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        Unsafe unsafe = MyUnsafe.getUnsafe();
        assert unsafe != null;
        long offset = unsafe.objectFieldOffset(owner);

        Thread currentThread = Thread.currentThread();
        boolean isOk = unsafe.compareAndSwapObject(objectMonitor, offset, null, currentThread);

        if (isOk) {
            return null;
        }

        return objectMonitor.getOwner();
    }

    /**
     * 重量级锁退出
     *
     * @param customLock myLock
     */
    public void exit(CustomLock customLock) {
        // 流程图里没画这步，判断当前线程和owner是否相等，不等的情况下如果是轻量级锁升级来的则设置owner，否则就是非法释放
        Thread currentThread = Thread.currentThread();
        if (owner != currentThread) {
            LockRecord lockRecord = MySynchronized.threadLocal.get();
            MarkWord head = lockRecord.getMarkWord();
            if (head != null) {
                // 从轻量级锁升级而来
                owner = currentThread;
                recursions = 0;
            } else {
                // 非法释放
                throw new RuntimeException("不是锁的拥有者，无权释放该锁");
            }
        }

        // 如果recursions不为0，说明重入了，自减
        if (recursions != 0) {
            recursions--;
            return;
        }

        // 开始选择唤醒模式，此处只写QMode==2
        // 触发屏障，让各个线程工作内存可见
        MyUnsafe.getUnsafe();
        // 从队列里获取一个线程准备唤醒
        ObjectWaiter objectWaiter = cxq.poll();
        if (objectWaiter != null) {
            exitEpilog(customLock, objectWaiter);
        }
    }

    /**
     * 唤醒线程
     *
     * @param customLock       myLock
     * @param objectWaiter objectWaiter
     */
    private void exitEpilog(CustomLock customLock, ObjectWaiter objectWaiter) {
        // 丢弃锁，将owner置为null
        MarkWord markWord = customLock.getMarkWord();
        markWord.getPtrMonitor().setOwner(null);
        // 获取线程唤醒
        Thread thread = objectWaiter.getThread();
        Unsafe unsafe = MyUnsafe.getUnsafe();
        assert unsafe != null;
        unsafe.unpark(thread);
        markWord.setPtrMonitor(null);
    }
}
