package com.hong.study.design.creational;

/**
 * 单例模式演示：演示单例设计模式，确保一个类只有一个实例，并提供全局访问点。
 * <p>
 * 创建型设计模式：单例模式
 * - 保证一个类仅有一个实例，并提供一个访问它的全局访问点。
 * <p>
 * 模式结构：
 * - 单例类（Singleton）：定义一个静态方法来获取实例，确保类只有一个实例。
 * <p>
 * 使用场景：
 * - 当只需要一个实例来协调行为，比如配置管理器、线程池、数据库连接池等。
 * - 当控制实例数目，节省系统资源，避免多个实例竞争时出现问题。
 *
 * @author hong
 */
public class SingletonPattern {

    /**
     * 私有静态实例
     */
    private static SingletonPattern instance;

    /**
     * 私有构造函数，防止外部实例化
     */
    private SingletonPattern() {
    }

    /**
     * 获取单例实例。
     *
     * @return 单例实例
     */
    public static SingletonPattern getInstance() {
        if (instance == null) {
            instance = new SingletonPattern();
        }
        return instance;
    }

    /**
     * 主函数：演示单例模式的用法。
     */
    public static void main(String[] args) {
        // 获取单例实例
        SingletonPattern singletonPattern1 = SingletonPattern.getInstance();
        SingletonPattern singletonPattern2 = SingletonPattern.getInstance();

        // 检查是否是同一个实例
        if (singletonPattern1 == singletonPattern2) {
            System.out.println("singleton1 和 singleton2 是同一个实例。");
        } else {
            System.out.println("singleton1 和 singleton2 不是同一个实例。");
        }
    }
}