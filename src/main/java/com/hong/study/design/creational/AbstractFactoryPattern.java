package com.hong.study.design.creational;

/**
 * 抽象工厂演示：演示抽象工厂设计模式，用于创建不同类型的电子产品系列。
 * <p>
 * 创建型设计模式：抽象工厂模式
 * - 提供一个创建一系列相关或相互依赖对象的接口，而无需指定它们的具体类。
 * <p>
 * 模式结构：
 * - 抽象工厂（ElectronicFactory）：声明了一组创建电子产品的方法，返回抽象电子产品。
 * - 具体工厂（PhoneFactory、LaptopFactory）：实现抽象工厂接口，返回具体电子产品。
 * - 抽象电子产品（ElectronicProduct）：声明了电子产品的接口。
 * - 具体电子产品（SmartPhone、Laptop）：实现抽象电子产品接口。
 * <p>
 * 使用场景：
 * - 当需要创建一组相关或相互依赖的对象时，可以使用抽象工厂模式。
 * - 当系统需要独立于产品的创建、组合和表示时，可以使用抽象工厂模式。
 *
 * @author hong
 */
public class AbstractFactoryPattern {

    /**
     * 抽象电子产品接口
     */
    interface ElectronicProduct {
        /**
         * 显示电子产品信息。
         */
        void displayInfo();
    }

    /**
     * 具体手机产品
     */
    static class SmartPhone implements ElectronicProduct {
        @Override
        public void displayInfo() {
            System.out.println("This is a smart phone.");
        }
    }

    /**
     * 具体笔记本产品
     */
    static class Laptop implements ElectronicProduct {
        @Override
        public void displayInfo() {
            System.out.println("This is a laptop.");
        }
    }

    /**
     * 抽象工厂接口
     */
    interface ElectronicFactory {
        /**
         * 创建手机产品。
         *
         * @return 手机产品
         */
        ElectronicProduct createPhone();

        /**
         * 创建笔记本产品。
         *
         * @return 笔记本产品
         */
        ElectronicProduct createLaptop();
    }

    /**
     * 具体手机工厂
     */
    static class PhoneFactory implements ElectronicFactory {
        @Override
        public ElectronicProduct createPhone() {
            return new SmartPhone();
        }

        @Override
        public ElectronicProduct createLaptop() {
            return new Laptop();
        }
    }

    /**
     * 具体笔记本工厂
     */
    static class LaptopFactory implements ElectronicFactory {
        @Override
        public ElectronicProduct createPhone() {
            return new SmartPhone();
        }

        @Override
        public ElectronicProduct createLaptop() {
            return new Laptop();
        }
    }

    /**
     * 主函数：演示抽象工厂模式的用法
     */
    public static void main(String[] args) {
        // 使用手机工厂创建手机和笔记本
        ElectronicFactory phoneFactory = new PhoneFactory();
        ElectronicProduct phone = phoneFactory.createPhone();
        ElectronicProduct laptopFromPhoneFactory = phoneFactory.createLaptop();
        phone.displayInfo();
        laptopFromPhoneFactory.displayInfo();

        // 使用笔记本工厂创建手机和笔记本
        ElectronicFactory laptopFactory = new LaptopFactory();
        ElectronicProduct phoneFromLaptopFactory = laptopFactory.createPhone();
        ElectronicProduct laptop = laptopFactory.createLaptop();
        phoneFromLaptopFactory.displayInfo();
        laptop.displayInfo();
    }
}
