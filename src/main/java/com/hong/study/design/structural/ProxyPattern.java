package com.hong.study.design.structural;

/**
 * 代理模式示例：为其他对象提供代理，以控制对这个对象的访问。
 * <p>
 * 模式结构：
 * - 主题接口（Subject）：定义了代理和实际对象的共同接口，可以是抽象类或接口。
 * - 实际主题（Real Subject）：实现了主题接口，是需要被代理的真实对象。
 * - 代理（Proxy）：持有一个实际主题的引用，实现了主题接口，可以在调用实际主题前后执行额外操作。
 * <p>
 * 工作原理：
 * 1. 定义一个主题接口，包含需要被代理对象和代理对象都需要实现的方法。
 * 2. 创建实际主题，实现主题接口，提供真正的功能。
 * 3. 创建代理，实现主题接口，持有实际主题的引用，可以在调用实际主题前后执行额外操作。
 * 4. 客户端通过代理对象来访问实际主题。
 * <p>
 * 适用场景：
 * - 当需要控制对对象的访问时，例如实现访问权限控制，可以使用代理模式。
 * - 当需要为对象添加额外的操作，例如实现懒加载、性能优化等，可以使用代理模式。
 * <p>
 * Java 实际应用：
 * - Java 中的远程代理（Remote Proxy）和虚拟代理（Virtual Proxy）可以用于实现分布式系统中的对象访问和延迟加载。
 * - Java 中的安全代理（Protection Proxy）可以用于控制对对象的访问权限。
 *
 * @author hong
 */
public class ProxyPattern {
    /**
     * 主题接口
     */
    interface Image {
        void display();
    }

    /**
     * 实际主题
     */
    static class RealImage implements Image {
        private final String filename;

        public RealImage(String filename) {
            this.filename = filename;
            loadFromDisk();
        }

        private void loadFromDisk() {
            System.out.println("Loading image: " + filename);
        }

        @Override
        public void display() {
            System.out.println("Displaying image: " + filename);
        }
    }

    /**
     * 代理
     */
    static class ProxyImage implements Image {
        private RealImage realImage;
        private final String filename;

        public ProxyImage(String filename) {
            this.filename = filename;
        }

        @Override
        public void display() {
            if (realImage == null) {
                realImage = new RealImage(filename);
            }
            realImage.display();
        }
    }

    public static void main(String[] args) {
        // 客户端使用代理对象来访问实际主题
        Image image = new ProxyImage("example.jpg");

        // 实际图片加载发生在调用 display 方法时
        image.display();
    }
}
