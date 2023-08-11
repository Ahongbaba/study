package com.hong.study.design.creational;

/**
 * 建造者模式示例：创建一个电脑对象。
 * <p>
 * 模式结构：
 * - 产品类（Product）：定义复杂对象的组成部分。在示例中，Computer 表示要构建的电脑。
 * - 抽象建造者接口（Builder）：定义创建复杂对象的各个部分的抽象方法。
 * - 具体建造者类（Concrete Builder）：实现抽象建造者接口，构建并组装复杂对象的各个部分。
 * - 指导者类（Director）：调用具体建造者类的方法，按特定顺序组装复杂对象。
 * <p>
 * 工作原理：
 * 1. 创建一个产品类（例如：Computer）来表示复杂对象，该类包含需要构建的各个部分的属性。
 * 2. 创建一个抽象建造者接口（例如：ComputerBuilder），定义创建各个部分的抽象方法。
 * 3. 创建具体建造者类（例如：DesktopComputerBuilder），实现抽象建造者接口，构建并设置各个部分的属性。
 * 4. 创建指导者类（例如：Director），接收一个具体建造者作为参数，调用其方法按照一定顺序构建复杂对象。
 * 5. 在客户端中，创建具体建造者对象，然后将其传递给指导者对象，调用指导者的构建方法。
 * 6. 最终通过指导者返回构建完成的复杂对象。
 * <p>
 * 适用场景：
 * - 当一个复杂对象的创建过程包含多个步骤，每个步骤可以根据需求定制时。
 * - 当需要创建不同配置的同类对象，但构建过程相同，只是组装的部分不同时。
 * - 当需要构建的对象具有复杂的内部结构，而客户端只需要关注创建过程而不需要关心内部细节。
 *
 * @author hong
 */
public class BuilderPattern {

    /**
     * 电脑类（产品类）
     */
    static class Computer {
        private String cpu;
        private String memory;
        private String storage;

        public void setCpu(String cpu) {
            this.cpu = cpu;
        }

        public void setMemory(String memory) {
            this.memory = memory;
        }

        public void setStorage(String storage) {
            this.storage = storage;
        }

        public void displayInfo() {
            System.out.println("Computer Info:");
            System.out.println("CPU: " + cpu);
            System.out.println("Memory: " + memory);
            System.out.println("Storage: " + storage);
        }
    }

    /**
     * 电脑建造者接口（抽象建造者接口）
     */
    interface ComputerBuilder {
        void buildCPU();

        void buildMemory();

        void buildStorage();

        Computer getResult();
    }

    /**
     * 具体电脑建造者（具体建造者类）
     */
    static class DesktopComputerBuilder implements ComputerBuilder {
        private final Computer computer = new Computer();

        @Override
        public void buildCPU() {
            computer.setCpu("Intel Core i7");
        }

        @Override
        public void buildMemory() {
            computer.setMemory("16GB DDR4");
        }

        @Override
        public void buildStorage() {
            computer.setStorage("512GB SSD");
        }

        @Override
        public Computer getResult() {
            return computer;
        }
    }

    /**
     * 指导者类
     */
    static class Director {
        private final ComputerBuilder builder;

        public Director(ComputerBuilder builder) {
            this.builder = builder;
        }

        public Computer construct() {
            builder.buildCPU();
            builder.buildMemory();
            builder.buildStorage();
            return builder.getResult();
        }
    }

    public static void main(String[] args) {
        ComputerBuilder desktopBuilder = new DesktopComputerBuilder();
        Director director = new Director(desktopBuilder);
        Computer desktopComputer = director.construct();
        desktopComputer.displayInfo();
    }
}
