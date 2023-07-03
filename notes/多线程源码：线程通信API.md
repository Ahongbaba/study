## 多线程源码：线程通信API

### 1、wait/notify机制

> wait()方法是Object 类的方法，它的作用是使当前执行wait()方法的线程等待，在wait()所在的代码行处暂停执行，并释放锁，直到接到通知或中断。

> notify()方法用来通知那些可能等待该锁的其他线程，如果有多个线程等待，则按照执行wait方法的顺序发出一次性通知（一次只能通知一个！），使得等待排在第一顺序的线程获得锁。需要说明的是，执行notify方法后，当前线程并不会立即释放锁，要等到程序执行完，即退出synchronized同步区域后。
>

**总结：wait 方法使线程暂停运行，而notify 方法通知暂停的线程继续运行。**

要想正确使用wait/notify，一定要注意：
**wait/notify在调用前一定要获得相同的锁**，如果在调用前没有获得锁，程序会抛出异常，也就调用不了wait/notify；另外，如果获得的不是同一把锁，notify不起作用。

* **不使用wait/notify**

  生产者消费者的模式下，如果不使用这个机制，就需要一直循环。因为缺少通知机制，消费者线程不知道何时才能读取到所需的资源，直到生产者线程生产了资源，浪费资源。

* **生产者/消费者案例**

  ```java
  /**
   * 生产者消费者演示wait notify机制
   * 一个生产者线程，一个消费者线程，同时最多只能生产和消费1个产品
   */
  public class ProducerConsumer {
  
      private final static Object OBJECT = new Object();
      private static boolean hasProduct = false;
      private static List<Object> productList = new ArrayList<>();
  
      public static void main(String[] args) {
  
          new Thread(() -> {
              while (true) {
                  synchronized (OBJECT) {
                      if (hasProduct) {
                          try {
                              OBJECT.wait();
                          } catch (InterruptedException e) {
                              throw new RuntimeException(e);
                          }
                      } else {
                          // 生产
                          Object o = new Object();
                          productList.add(o);
                          System.out.println("生产了--->" + o);
                          hasProduct = true;
                          OBJECT.notify();
                      }
                  }
              }
          }, "producer").start();
  
          new Thread(() -> {
              while (true) {
                  synchronized (OBJECT) {
                      if (hasProduct) {
                          // 消费
                          Object o = productList.get(0);
                          productList.remove(0);
                          System.out.println("消费了--->" + o);
                          hasProduct = false;
                          OBJECT.notify();
                      } else {
                          try {
                              OBJECT.wait();
                          } catch (InterruptedException e) {
                              throw new RuntimeException(e);
                          }
                      }
                  }
              }
          }, "consumer").start();
  
      }
  }
  ```

  ```java
  /**
   * 生产者消费者演示wait notify机制
   * 多个生产者线程，多个消费者线程，同时可以生产和消费多个产品
   * 需求：100个生产者，100个消费者，然后同时最多只能生产10个产品
   */
  public class ProducerConsumer1 {
  
      private final static Object OBJECT = new Object();
      private static int PRODUCT_COUNT = 0;
  
      public static void main(String[] args) {
          ProducerConsumer1 producerConsumer1 = new ProducerConsumer1();
          // 生产者线程
          new Thread(() -> {
              while (true) {
                  synchronized (OBJECT) {
                      if (PRODUCT_COUNT < 10) {
                          producerConsumer1.producer();
                          OBJECT.notifyAll();
                          break;
                      } else {
                          try {
                              OBJECT.wait();
                          } catch (InterruptedException e) {
                              throw new RuntimeException(e);
                          }
                      }
                  }
              }
          }, "producer").start();
  
          // 消费者线程
          new Thread(() -> {
              while (true) {
                  synchronized (OBJECT) {
                      if (PRODUCT_COUNT > 0) {
                          producerConsumer1.consumer();
                          OBJECT.notifyAll();
                          break;
                      } else {
                          try {
                              OBJECT.wait();
                          } catch (InterruptedException e) {
                              throw new RuntimeException(e);
                          }
                      }
                  }
              }
          }, "consumer").start();
  
      }
  
      public void producer() {
          System.out.println(Thread.currentThread().getName() + "-生产了->" + ++PRODUCT_COUNT);
      }
  
      public void consumer() {
          System.out.println(Thread.currentThread().getName() + "-消费了->" + PRODUCT_COUNT--);
      }
  }
  ```

  