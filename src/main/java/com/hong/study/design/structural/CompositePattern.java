package com.hong.study.design.structural;

import java.util.ArrayList;
import java.util.List;

/**
 * 组合模式示例：将对象组织成树形结构，使得客户端可以一致地处理单个对象和组合对象。
 * <p>
 * 模式结构：
 * - 组件接口（Component）：定义组合对象和叶节点的公共接口，通常包含操作方法，如 display。
 * - 叶节点（Leaf）：实现组件接口的叶节点类，代表单个对象。
 * - 复合对象（Composite）：实现组件接口的复合对象类，可以包含其他叶节点和复合对象，形成递归结构。
 * <p>
 * 工作原理：
 * 1. 定义组件接口，包含组合对象和叶节点的共同操作方法，如 display。
 * 2. 创建叶节点类，实现组件接口，代表单个对象，无法包含其他子节点。
 * 3. 创建复合对象类，实现组件接口，可以包含其他叶节点和复合对象，形成树形结构。
 * 4. 客户端可以使用组件接口来操作单个对象和组合对象，无需区分它们的类型。
 * <p>
 * 适用场景：
 * - 当希望将单个对象和组合对象一视同仁地处理，可以使用组合模式。
 * - 当对象有整体-部分关系，且希望以相同的方式处理整体和部分时，可以使用组合模式。
 * - 当需要构建树形结构，并以统一的方式处理其中的节点时，可以使用组合模式。
 * <p>
 * 实际应用：
 * - 操作系统文件系统：文件系统中的文件和文件夹可以看作是组合模式的应用。文件夹可以包含其他文件夹和文件，从而构建了一个树状的文件系统结构。
 * - 树形数据结构：处理树状数据结构时，组合模式也非常有用。例如，XML和JSON解析库可以将解析的数据以树形结构表示，方便处理和操作。
 *
 * @author hong
 */
public class CompositePattern {

    /**
     * 组件接口
     */
    interface Component {
        void display();
    }

    /**
     * 叶节点
     */
    static class Leaf implements Component {
        private final String name;

        public Leaf(String name) {
            this.name = name;
        }

        @Override
        public void display() {
            System.out.println("Leaf: " + name);
        }
    }

    /**
     * 复合对象
     */
    static class Composite implements Component {
        private final List<Component> components = new ArrayList<>();

        public void add(Component component) {
            components.add(component);
        }

        @Override
        public void display() {
            for (Component component : components) {
                component.display();
            }
        }
    }

    public static void main(String[] args) {
        Component leaf1 = new Leaf("Leaf 1");
        Component leaf2 = new Leaf("Leaf 2");
        Component leaf3 = new Leaf("Leaf 3");

        Composite composite1 = new Composite();
        composite1.add(leaf1);
        composite1.add(leaf2);

        Composite composite2 = new Composite();
        composite2.add(leaf3);
        composite2.add(composite1);

        System.out.println("Composite 2:");
        composite2.display();
    }
}
