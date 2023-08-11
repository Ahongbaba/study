package com.hong.study.design.creational;

/**
 * 工厂方法示例：演示工厂方法设计模式，用于创建不同形状的图形对象。
 * <p>
 * 创建型设计模式：工厂方法模式
 * - 定义一个用于创建对象的接口，让子类决定实例化哪个类。
 * - 工厂方法使一个类的实例化延迟到其子类。
 * <p>
 * 模式结构：
 * - 抽象产品（Shape）：定义了图形对象的接口，包含绘制图形的方法。
 * - 具体产品（Circle、Rectangle）：实现了抽象产品接口，定义具体的图形对象。
 * - 抽象工厂（ShapeFactory）：声明了创建图形对象的工厂方法。
 * - 具体工厂（CircleFactory、RectangleFactory）：实现了抽象工厂接口，负责创建具体的图形对象。
 * <p>
 * 工作原理：
 * 1. 客户端通过调用抽象工厂的方法来创建具体产品对象，而无需直接实例化具体产品。
 * 2. 抽象工厂的具体子类实现了工厂方法，负责创建特定类型的产品对象。
 * 3. 客户端代码与具体工厂类交互，而具体工厂类会创建对应的具体产品。
 * <p>
 * 适用场景：
 * - 当一个类不知道它所需要的对象的类时，可以使用工厂方法模式，将对象的创建委托给子类。
 * - 当一个类希望由其子类来指定创建对象的时候，可以使用工厂方法模式。
 * - 当类中的重复代码开始增多，需要进行代码重构时，可以考虑使用工厂方法模式来优化代码结构。
 *
 * @author hong
 */
public class FactoryMethodPattern {
    /**
     * 抽象产品接口：定义了绘制图形的方法。
     */
    interface Shape {
        /**
         * 绘制图形
         */
        void draw();
    }

    /**
     * 具体产品：圆形。
     */
    static class Circle implements Shape {
        @Override
        public void draw() {
            System.out.println("绘制圆形");
        }
    }

    /**
     * 具体产品：矩形。
     */
    static class Rectangle implements Shape {
        @Override
        public void draw() {
            System.out.println("绘制矩形");
        }
    }

    /**
     * 抽象工厂接口：声明了创建图形对象的工厂方法。
     */
    interface ShapeFactory {
        /**
         * 创建图形对象
         *
         * @return 图形对象
         */
        Shape createShape();
    }

    /**
     * 具体工厂：圆形工厂。
     */
    static class CircleFactory implements ShapeFactory {
        @Override
        public Shape createShape() {
            return new Circle();
        }
    }

    /**
     * 具体工厂：矩形工厂。
     */
    static class RectangleFactory implements ShapeFactory {
        @Override
        public Shape createShape() {
            return new Rectangle();
        }
    }

    /**
     * 主函数：演示工厂方法模式的用法。
     */
    public static void main(String[] args) {
        // 使用工厂方法创建圆形
        final ShapeFactory circleFactory = new CircleFactory();
        final Shape circle = circleFactory.createShape();
        circle.draw();

        // 使用工厂方法创建矩形
        final ShapeFactory rectangleFactory = new RectangleFactory();
        final Shape rectangle = rectangleFactory.createShape();
        rectangle.draw();
    }
}
