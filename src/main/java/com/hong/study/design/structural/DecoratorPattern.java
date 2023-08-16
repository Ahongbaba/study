package com.hong.study.design.structural;

/**
 * 装饰器模式示例：动态地为对象添加新功能，不修改现有代码。
 * <p>
 * 模式结构：
 * - 组件接口（Component）：定义了被装饰者和装饰者的共同接口，通常包含基本的操作方法。
 * - 具体组件（ConcreteComponent）：实现了组件接口，是被装饰者，包含了基本的功能。
 * - 装饰器（Decorator）：持有一个组件对象的引用，实现了组件接口，可以附加新的责任（功能）。
 * - 具体装饰器（ConcreteDecorator）：扩展了装饰器，添加了具体的装饰功能。
 * <p>
 * 工作原理：
 * 1. 定义一个组件接口，包含基本的操作方法。
 * 2. 创建具体组件，实现组件接口，提供基本的功能。
 * 3. 创建装饰器，持有一个组件对象的引用，并实现组件接口。
 * 4. 创建具体装饰器，扩展装饰器，添加额外的功能。
 * 5. 客户端使用具体装饰器包装具体组件，从而动态地添加功能。
 * <p>
 * 适用场景：
 * - 当需要动态地为对象添加功能，且不改变现有代码时，可以使用装饰器模式。
 * - 当不希望使用继承来扩展功能，或者类爆炸问题较为严重时，可以使用装饰器模式。
 * - 当需要按照不同的方式组合对象的功能时，可以使用装饰器模式。
 * <p>
 * Java 实际应用：
 * - Java I/O 库中的输入流和输出流类就使用了装饰器模式。不同的装饰器可以添加不同的功能，如缓冲、压缩等。
 * - Java Swing GUI 库中的图形组件也使用了装饰器模式。例如，JScrollPane 可以包装 JTextArea，添加滚动功能。
 *
 * @author hong
 */
public class DecoratorPattern {
    /**
     * 组件接口
     */
    interface Coffee {
        String getDescription();
        double cost();
    }

    /**
     * 具体组件
     */
    static class SimpleCoffee implements Coffee {
        @Override
        public String getDescription() {
            return "Simple Coffee";
        }

        @Override
        public double cost() {
            return 2.0;
        }
    }

    /**
     * 装饰器
     */
    abstract static class CoffeeDecorator implements Coffee {
        private final Coffee decoratedCoffee;

        public CoffeeDecorator(Coffee coffee) {
            this.decoratedCoffee = coffee;
        }

        @Override
        public String getDescription() {
            return decoratedCoffee.getDescription();
        }

        @Override
        public double cost() {
            return decoratedCoffee.cost();
        }
    }

    /**
     * 具体装饰器
     */
    static class MilkDecorator extends CoffeeDecorator {
        public MilkDecorator(Coffee coffee) {
            super(coffee);
        }

        @Override
        public String getDescription() {
            return super.getDescription() + ", Milk";
        }

        @Override
        public double cost() {
            return super.cost() + 1.0;
        }
    }

    public static void main(String[] args) {
        // 创建一个简单咖啡
        Coffee simpleCoffee = new SimpleCoffee();
        System.out.println("Simple Coffee: " + simpleCoffee.getDescription() + ", Cost: $" + simpleCoffee.cost());

        // 使用装饰器给咖啡添加牛奶
        Coffee milkCoffee = new MilkDecorator(simpleCoffee);
        System.out.println("Milk Coffee: " + milkCoffee.getDescription() + ", Cost: $" + milkCoffee.cost());
    }
}
