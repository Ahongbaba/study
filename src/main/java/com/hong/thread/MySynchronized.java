package com.hong.thread;

public class MySynchronized {

    static MyLock myLock = new MyLock();

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


    private void slowEnter() {
        // 这里有很多逻辑，先不写

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
