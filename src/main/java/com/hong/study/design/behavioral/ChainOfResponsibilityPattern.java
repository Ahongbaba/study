package com.hong.study.design.behavioral;

/**
 * 责任链模式示例：将多个处理者连成一条链，依次处理请求，直到请求被处理或到达链的末端。
 * <p>
 * 模式结构：
 * - 处理者接口（Handler）：定义了处理请求的方法和对下一个处理者的引用。所有的处理者都实现这个接口。
 * - 具体处理者（Concrete Handler）：实现了处理者接口，具体判断是否能够处理请求，如果能则处理之，否则将请求传递给下一个处理者。
 * <p>
 * 工作原理：
 * 1. 创建一个处理者接口，定义处理请求的方法和对下一个处理者的引用。
 * 2. 创建具体处理者，实现处理者接口，实现自己的处理逻辑和判断能力。
 * 3. 将处理者按照一定的顺序链接成一条链。
 * 4. 客户端将请求发送给链的第一个处理者，责任链会依次判断并处理请求。
 * <p>
 * 适用场景：
 * - 当有多个对象可以处理同一个请求，但客户端不需要知道哪个对象处理，可以使用责任链模式。
 * - 当需要在多个对象之间传递请求，形成一条处理链，可以使用责任链模式。
 * <p>
 * Java 实际应用：
 * - Java 中的异常处理机制就是责任链模式的例子。不同层次的异常处理代码可以依次捕获并处理异常，如果无法处理则传递给更上层的异常处理机制。
 * - Java 中的过滤器链也可以用责任链模式来实现。在 Java Web 应用中，可以通过一系列过滤器来处理请求，每个过滤器可以决定是否继续传递给下一个过滤器。
 *
 * @author hong
 */
public class ChainOfResponsibilityPattern {
    /**
     * 处理者接口
     */
    interface Handler {
        void setNextHandler(Handler nextHandler);
        void handleRequest(String request);
    }

    /**
     * 具体处理者
     */
    static class ConcreteHandler1 implements Handler {
        private Handler nextHandler;

        @Override
        public void setNextHandler(Handler nextHandler) {
            this.nextHandler = nextHandler;
        }

        @Override
        public void handleRequest(String request) {
            if (request.equalsIgnoreCase("RequestType1")) {
                System.out.println("ConcreteHandler1 handles the request.");
            } else if (nextHandler != null) {
                nextHandler.handleRequest(request);
            } else {
                System.out.println("No handler can process the request.");
            }
        }
    }

    static class ConcreteHandler2 implements Handler {
        private Handler nextHandler;

        @Override
        public void setNextHandler(Handler nextHandler) {
            this.nextHandler = nextHandler;
        }

        @Override
        public void handleRequest(String request) {
            if (request.equalsIgnoreCase("RequestType2")) {
                System.out.println("ConcreteHandler2 handles the request.");
            } else if (nextHandler != null) {
                nextHandler.handleRequest(request);
            } else {
                System.out.println("No handler can process the request.");
            }
        }
    }

    public static void main(String[] args) {
        // 创建责任链
        Handler handler1 = new ConcreteHandler1();
        Handler handler2 = new ConcreteHandler2();
        handler1.setNextHandler(handler2);

        // 客户端发送请求
        handler1.handleRequest("RequestType1");
        handler1.handleRequest("RequestType2");
        handler1.handleRequest("RequestType3");
    }
}
