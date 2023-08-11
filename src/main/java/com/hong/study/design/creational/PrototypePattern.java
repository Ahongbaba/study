package com.hong.study.design.creational;

/**
 * 原型模式示例：复制已有对象来创建新对象。
 * <p>
 * 模式结构：
 * - 原型接口（Prototype）：定义一个克隆自身的方法。在示例中，Prototype 接口定义了 clone 方法。
 * - 具体原型类（Concrete Prototype）：实现原型接口，实现克隆方法。在示例中，ConcretePrototype 实现了 clone 方法，用于复制自身。
 * <p>
 * 工作原理：
 * 1. 创建一个原型接口，该接口声明一个克隆自身的方法（例如：clone）。
 * 2. 创建具体原型类，实现原型接口的克隆方法，以便可以复制自身的实例。
 * 3. 在客户端中，通过调用具体原型类的克隆方法来复制已有对象，并创建新对象。
 * <p>
 * 适用场景：
 * - 当对象的创建成本较高，或者对象的创建过程较复杂时，可以使用原型模式来复制已有对象，避免重复创建。
 * - 当需要创建的对象与现有对象之间只有细微的差异时，可以通过复制已有对象来创建新对象，而不需要从头开始构建。
 * - 当希望减少类的子类数量，避免继承的复杂性，可以通过原型模式来克隆已有对象，而不是创建不同的子类。
 *
 * @author hong
 */
public class PrototypePattern {
    /**
     * 原型接口
     */
    interface Prototype {
        /**
         * 复制自身的方法。
         *
         * @return 复制后的新对象
         */
        Prototype createClone();
    }

    /**
     * 具体原型类
     */
    static class ConcretePrototype implements Prototype {
        private final String data;

        public ConcretePrototype(String data) {
            this.data = data;
        }

        @Override
        public Prototype createClone() {
            // 复制并创建新对象
            return new ConcretePrototype(data);
        }

        /**
         * 显示数据信息。
         */
        public void displayInfo() {
            System.out.println("Data: " + data);
        }
    }

    public static void main(String[] args) {
        // 创建原型对象
        ConcretePrototype prototype = new ConcretePrototype("Hello, Prototype!");

        // 复制原型对象
        ConcretePrototype clonedPrototype = (ConcretePrototype) prototype.createClone();

        // 原型对象和复制对象的数据是相同的，但它们是不同的实例
        prototype.displayInfo();
        clonedPrototype.displayInfo();
    }
}
