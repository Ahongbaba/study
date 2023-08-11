package com.hong.study.design.structural;

/**
 * 桥接模式示例：将抽象部分与实现部分分离，使它们可以独立变化。将不同品牌的电视和遥控器进行解耦
 * <p>
 * 模式结构：
 * - 实现化接口（TVImplementor）：定义电视品牌的接口。
 * - 具体实现化类（SonyTV、SamsungTV）：实现电视品牌的具体类。
 * - 抽象化类（RemoteControl）：定义遥控器的抽象类，包含一个对实现化的引用。
 * - 扩充抽象化类（AdvancedRemoteControl）：在抽象化的基础上进行扩展，提供额外的操作。
 * <p>
 * 工作原理：
 * 1. 定义实现化接口，通常包含电视品牌相关的方法。
 * 2. 创建具体实现化类，实现电视品牌的具体类。
 * 3. 定义抽象化类，包含对实现化的引用，并定义遥控器的基本操作。
 * 4. 创建扩充抽象化类，在抽象化的基础上扩展遥控器的功能。
 * 5. 客户端使用抽象化的接口进行操作，具体的实现由实现化的对象提供。
 * <p>
 * 适用场景：
 * - 当需要在抽象部分和实现部分之间进行更灵活的组合时，可以使用桥接模式。
 * - 当一个类需要多个维度的变化，而继承会导致类爆炸时，可以使用桥接模式来避免继承的复杂性。
 * - 当希望分离抽象和实现，使它们可以独立变化时，可以使用桥接模式。
 * <p>
 * 实际应用：
 * - 桥接模式在Java框架和库中有被广泛使用。一个很常见的实际应用就是Java AWT（Abstract Window Toolkit）图形界面库中的绘图部分。
 * 在AWT中，Graphics 类代表了一个绘图上下文，而Graphics2D 类则是对Graphics类的扩展，提供了更丰富的绘图功能。
 * Graphics2D 类的实现就使用了桥接模式。它把图形绘制的抽象部分（例如，画线、填充等）与不同平台的实际绘图实现
 * （例如，在Windows上的实现和在Linux上的实现）进行了分离。这样，开发者可以使用统一的Graphics2D接口来绘制图形，
 * 而实际的绘图实现则由底层平台特定的实现提供。
 * - 此外，Java的集合框架（例如List、Set、Map等）也使用了桥接模式。在集合框架中，List 接口与不同的实现（如ArrayList、LinkedList）
 * 进行了解耦，使得开发者可以使用统一的List接口来操作不同的实现，而无需关心底层的具体实现细节。
 *
 * @author hong
 */
public class BridgePattern {

    /**
     * 实现化接口：电视品牌
     */
    interface TVImplementor {
        void powerOn();

        void powerOff();
    }

    /**
     * 具体实现化类：Sony 电视
     */
    static class SonyTV implements TVImplementor {
        @Override
        public void powerOn() {
            System.out.println("Sony TV is turned on");
        }

        @Override
        public void powerOff() {
            System.out.println("Sony TV is turned off");
        }
    }

    /**
     * 具体实现化类：Samsung 电视
     */
    static class SamsungTV implements TVImplementor {
        @Override
        public void powerOn() {
            System.out.println("Samsung TV is turned on");
        }

        @Override
        public void powerOff() {
            System.out.println("Samsung TV is turned off");
        }
    }

    /**
     * 抽象化类：遥控器
     */
    abstract static class RemoteControl {
        protected TVImplementor tv;

        protected RemoteControl(TVImplementor tv) {
            this.tv = tv;
        }

        /**
         * 打开电视
         */
        abstract void turnOn();

        /**
         * 关闭电视
         */
        abstract void turnOff();
    }

    /**
     * 扩充抽象化类：高级遥控器
     */
    static class AdvancedRemoteControl extends RemoteControl {
        public AdvancedRemoteControl(TVImplementor tv) {
            super(tv);
        }

        /**
         * 打开电视并调整音量
         */
        @Override
        void turnOn() {
            tv.powerOn();
            System.out.println("Adjusting volume");
        }

        /**
         * 关闭电视
         */
        @Override
        void turnOff() {
            tv.powerOff();
        }
    }

    public static void main(String[] args) {
        TVImplementor sonyTV = new SonyTV();
        RemoteControl remoteControl = new AdvancedRemoteControl(sonyTV);
        remoteControl.turnOn();
        remoteControl.turnOff();

        TVImplementor samsungTV = new SamsungTV();
        remoteControl = new AdvancedRemoteControl(samsungTV);
        remoteControl.turnOn();
        remoteControl.turnOff();
    }
}
