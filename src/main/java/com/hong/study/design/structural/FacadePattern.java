package com.hong.study.design.structural;

/**
 * 外观模式示例：提供一个简单接口，隐藏底层子系统的复杂性，使客户端与子系统之间交互更加简单。
 * <p>
 * 模式结构：
 * - 外观（Facade）：提供了一个统一的接口，知道如何将客户端的请求委派给适当的子系统对象。
 * - 子系统（Subsystem）：包含了实际的功能和操作。客户端可以通过外观类来访问子系统的功能。
 * <p>
 * 工作原理：
 * 1. 定义一个外观类，封装了对多个子系统的调用。
 * 2. 在外观类中提供简单的方法，使客户端可以通过这些方法来访问底层子系统功能。
 * 3. 客户端只需要与外观类进行交互，不需要了解底层子系统的复杂性。
 * <p>
 * 适用场景：
 * - 当一个复杂子系统拥有多个不同的接口，而客户端需要与这些接口进行交互时，可以使用外观模式来简化交互过程。
 * - 当希望将子系统的内部结构隐藏起来，以减少客户端与子系统之间的依赖性时，可以使用外观模式。
 * <p>
 * Java 实际应用：
 * - Java 的 Servlet API 就是外观模式的例子。Servlet 容器提供了一个统一的接口，使得开发者可以创建和管理 Servlet，而不需要了解底层的网络和线程管理细节。
 * - Java 中的 GUI 库，如 Swing，提供了外观模式来隐藏底层的操作，使得开发者可以通过简单的接口来创建复杂的图形界面。
 *
 * @author hong
 */
public class FacadePattern {
    /**
     * 子系统1
     */
    static class Subsystem1 {
        public void operation1() {
            System.out.println("Subsystem1 Operation 1");
        }
    }

    /**
     * 子系统2
     */
    static class Subsystem2 {
        public void operation2() {
            System.out.println("Subsystem2 Operation 2");
        }
    }

    // 外观类
    static class Facade {
        private final Subsystem1 subsystem1;
        private final Subsystem2 subsystem2;

        public Facade() {
            subsystem1 = new Subsystem1();
            subsystem2 = new Subsystem2();
        }

        // 提供简单的接口来封装多个子系统的操作
        public void performOperations() {
            subsystem1.operation1();
            subsystem2.operation2();
        }
    }

    public static void main(String[] args) {
        // 客户端使用外观类，通过简单的接口来访问子系统功能
        Facade facade = new Facade();
        facade.performOperations();
    }
}
