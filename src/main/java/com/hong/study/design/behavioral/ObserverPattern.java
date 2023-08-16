package com.hong.study.design.behavioral;

import java.util.ArrayList;
import java.util.List;

/**
 * 观察者模式示例：定义对象之间的一种依赖关系，当一个对象的状态发生变化时，通知依赖它的其他对象。
 * <p>
 * 模式结构：
 * - 主题（Subject）：维护一个观察者列表，提供方法来注册、注销和通知观察者。
 * - 具体主题（Concrete Subject）：实现了主题接口，维护具体的状态，并在状态变化时通知观察者。
 * - 观察者（Observer）：定义了更新方法，用于接收主题状态的变化通知。
 * - 具体观察者（Concrete Observer）：实现了观察者接口，具体处理主题状态变化的通知。
 * <p>
 * 工作原理：
 * 1. 定义一个主题接口，包含注册、注销和通知观察者的方法。
 * 2. 创建具体主题类，实现主题接口，维护观察者列表，当状态变化时通知观察者。
 * 3. 定义观察者接口，包含更新方法。
 * 4. 创建具体观察者类，实现观察者接口，实现在主题状态变化时的更新逻辑。
 * 5. 客户端创建主题对象和观察者对象，并将观察者注册到主题中。
 * <p>
 * 适用场景：
 * - 当一个对象的状态变化需要通知其他对象，并且不希望耦合太紧密时，可以使用观察者模式。
 * - 当一个对象需要将变化通知给多个对象，而这些对象可能具有不同的处理逻辑时，可以使用观察者模式。
 *
 * @author hong
 */
public class ObserverPattern {
    /**
     * 主题接口
     */
    interface Subject {
        void registerObserver(Observer observer);
        void removeObserver(Observer observer);
        void notifyObservers(String message);
    }

    /**
     * 具体主题
     */
    static class ConcreteSubject implements Subject {
        private final List<Observer> observers = new ArrayList<>();
        private String state;

        @Override
        public void registerObserver(Observer observer) {
            observers.add(observer);
        }

        @Override
        public void removeObserver(Observer observer) {
            observers.remove(observer);
        }

        @Override
        public void notifyObservers(String message) {
            for (Observer observer : observers) {
                observer.update(message);
            }
        }

        public void setState(String state) {
            this.state = state;
            notifyObservers("State changed to: " + state);
        }
    }

    /**
     * 观察者接口
     */
    interface Observer {
        void update(String message);
    }

    /**
     * 具体观察者
     */
    static class ConcreteObserver implements Observer {
        private String name;

        public ConcreteObserver(String name) {
            this.name = name;
        }

        @Override
        public void update(String message) {
            System.out.println(name + " received notification: " + message);
        }
    }

    public static void main(String[] args) {
        ConcreteSubject subject = new ConcreteSubject();
        Observer observer1 = new ConcreteObserver("Observer 1");
        Observer observer2 = new ConcreteObserver("Observer 2");

        subject.registerObserver(observer1);
        subject.registerObserver(observer2);

        subject.setState("State 1");
        subject.setState("State 2");

        subject.removeObserver(observer1);

        subject.setState("State 3");
    }
}
