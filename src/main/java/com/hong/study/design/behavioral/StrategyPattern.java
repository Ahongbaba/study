package com.hong.study.design.behavioral;

/**
 * 策略模式示例：定义一系列算法，并将每个算法封装成单独的类，使它们可以互相替换。
 * <p>
 * 模式结构：
 * - 策略接口（Strategy）：定义了策略的方法，不同的策略可以有不同的实现。
 * - 具体策略（Concrete Strategy）：实现了策略接口，提供了具体的算法实现。
 * - 上下文（Context）：持有一个策略对象的引用，可以根据需要切换不同的策略。
 * <p>
 * 工作原理：
 * 1. 定义一个策略接口，声明策略的方法。
 * 2. 创建具体策略类，实现策略接口，提供不同的算法实现。
 * 3. 创建上下文类，持有一个策略对象的引用。
 * 4. 客户端可以根据需要选择不同的策略，并将策略对象传递给上下文类。
 * <p>
 * 适用场景：
 * - 当有多个算法可以互相替换，且客户端需要根据运行时条件选择合适的算法时，可以使用策略模式。
 * - 当有一些相关的类仅在行为上有差异时，可以使用策略模式，避免使用条件语句或继承来实现不同的行为。
 * <p>
 * Java 实际应用：
 * - Java 中的 Comparator 接口和相关实现就是策略模式的应用。通过传递不同的比较器对象，可以在排序过程中选择不同的比较策略。
 * - Java 中的文件排序和查找算法也可以使用策略模式来实现。
 *
 * @author hong
 */
public class StrategyPattern {
    /**
     * 策略接口
     */
    interface PaymentStrategy {
        void pay(int amount);
    }

    /**
     * 具体策略
     */
    static class CreditCardPayment implements PaymentStrategy {
        private final String cardNumber;

        public CreditCardPayment(String cardNumber) {
            this.cardNumber = cardNumber;
        }

        @Override
        public void pay(int amount) {
            System.out.println("Paid $" + amount + " using credit card " + cardNumber);
        }
    }

    static class PayPalPayment implements PaymentStrategy {
        private final String email;

        public PayPalPayment(String email) {
            this.email = email;
        }

        @Override
        public void pay(int amount) {
            System.out.println("Paid $" + amount + " using PayPal account " + email);
        }
    }

    /**
     * 上下文
     */
    static class ShoppingCart {
        private PaymentStrategy paymentStrategy;

        public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
            this.paymentStrategy = paymentStrategy;
        }

        public void checkout(int amount) {
            paymentStrategy.pay(amount);
        }
    }

    public static void main(String[] args) {
        // 客户端选择不同的支付策略
        PaymentStrategy creditCardPayment = new CreditCardPayment("1234-5678-9012-3456");
        PaymentStrategy payPalPayment = new PayPalPayment("user@example.com");

        ShoppingCart cart = new ShoppingCart();
        cart.setPaymentStrategy(creditCardPayment);
        cart.checkout(100);

        cart.setPaymentStrategy(payPalPayment);
        cart.checkout(50);
    }
}
