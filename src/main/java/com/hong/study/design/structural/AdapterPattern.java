package com.hong.study.design.structural;

/**
 * 适配器模式示例：将一个类的接口转换成另一个客户端所期望的接口。
 * <p>
 * 模式结构：
 * - 目标接口（Target）：定义客户端期望使用的接口。在示例中，Target 接口定义了 request 方法。
 * - 适配者类（Adaptee）：已存在的类或接口，其方法与目标接口不兼容。在示例中，Adaptee 类提供了 specificRequest 方法。
 * - 适配器类（Adapter）：实现目标接口，同时包含对适配者类的引用，将原始接口的方法适配成目标接口的方法。在示例中，Adapter 类实现了 Target 接口，并在其 request 方法中调用了 Adaptee 的 specificRequest 方法。
 * <p>
 * 工作原理：
 * 1. 定义目标接口，客户端期望使用的方法。
 * 2. 创建适配者类，提供原始接口的具体实现。
 * 3. 创建适配器类，实现目标接口，同时包含对适配者类的引用。在适配器类的方法中，调用适配者类的方法，并将其转换为目标接口的方法。
 * 4. 客户端使用适配器对象，调用目标接口的方法。实际上是在调用适配器对象中的方法，适配器对象再调用适配者对象的方法。
 * <p>
 * 适用场景：
 * - 当需要使用一个已存在的类，但其接口与你的需求不匹配时，可以使用适配器模式进行接口转换。
 * - 当需要复用一些现有的类，但其接口与其他代码不兼容时，可以使用适配器模式进行接口适配。
 * - 当希望创建一个可以复用的类，其接口与其他类不兼容时，可以使用适配器模式将其适配为其他类可以使用的接口。
 *
 * @author hong
 */
public class AdapterPattern {
    /**
     * 目标接口
     */
    interface Target {
        /**
         * 客户端期望使用的方法。
         */
        void request();
    }

    /**
     * 适配者类
     */
    static class Adaptee {
        void specificRequest() {
            System.out.println("Adaptee's specific request");
        }
    }

    /**
     * 适配器类
     */
    static class Adapter implements Target {
        private final Adaptee adaptee;

        public Adapter(Adaptee adaptee) {
            this.adaptee = adaptee;
        }

        @Override
        public void request() {
            // 转换调用适配者类的方法
            adaptee.specificRequest();
        }
    }

    public static void main(String[] args) {
        Adaptee adaptee = new Adaptee();
        Target target = new Adapter(adaptee);
        // 调用适配器对象的方法，实际上是调用了适配者对象的方法
        target.request();
    }
}
