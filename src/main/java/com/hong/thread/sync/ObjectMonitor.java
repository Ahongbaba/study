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
    public void enter(MyLock myLock) {
        // 1、CAS修改owner字段为当前线程
        Thread currentThread = cmpAndChgOwner(myLock);
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
        enterI(myLock);
    }

    /**
     * 真正开始入队挂起
     * @param myLock myLock
     */
    private void enterI(MyLock myLock) {
        // 自旋抢锁
        if (tryLock(myLock) > 0) {
            return;
        }
        // 延迟处理其他逻辑
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 又自旋10次
        if (tryLock(myLock) > 0) {
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
                if (tryLock(myLock) > 0) {
                    return;
                }
            }
        }

        // 真正阻塞
        for (; ; ) {
            // 又又又自旋
            if (tryLock(myLock) > 0) {
                break;
            }
            Unsafe unsafe = MyUnsafe.getUnsafe();
            assert unsafe != null;
            // 挂起进入内核态，挂起后线程卡在此处，等待唤醒
            unsafe.park(false, 0L);

            // 唤醒后立马抢锁
            if (tryLock(myLock) > 0) {
                break;
            }
        }
    }

    /**
     * 自旋抢锁
     *
     * @param myLock myLock
     * @return 1=成功 0和-1=失败
     */
    private int tryLock(MyLock myLock) {
        for (int i = 0; i < 10; i++) {
            // 如果有线程还拥有重量级锁，直接退出
            if (owner != null) {
                return 0;
            }
            // 自旋
            Thread thread = cmpAndChgOwner(myLock);
            if (thread == null) {
                // cas获取锁成功
                return 1;
            }
        }

        return -1;
    }

    public Thread cmpAndChgOwner(MyLock myLock) {
        ObjectMonitor objectMonitor = myLock.getMarkWord().getPtrMonitor();
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
}
