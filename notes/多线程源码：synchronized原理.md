## 多线程源码：synchronized原理

### 1、synchronized简介

synchronized是Java的一个关键字，**是一种互斥锁**。来自官方的解释：Synchronized方法支持一种简单的策略，用于防止线程干扰和内存一致性错误：如果一个对象对多个线程可见，则对该对象变量的所有读或写操作都通过Synchronized方法完成。

**Synchronized是最基本的互斥手段**，保证同一时刻最多只有1个线程执行被Synchronized修饰的方法 / 代码，其他线程 必须等待当前线程执行完该方法 / 代码块后才能执行该方法 / 代码块。

在JDK1.5之前synchronized是一个重量级锁，相对于j.u.c.Lock，它会显得那么笨重，随着Javs SE 1.6对synchronized进行的各种优化后，synchronized并不会显得那么重了。

* **作用，特性**

  Synchronized保证同一时刻有且只有一条线程在操作共享数据，其他线程必须等待该线程处理完数据后再对共享数据进行操作。此时便产生了互斥锁，互斥锁的特性如下：

  **原子性(互斥性)：**多个操作作为一个整体，不能被分割与中断，也不能被其他线程干扰。如果被中断与干扰，则会出现数据异常、逻辑异常。在同一时刻只允许一个线程持有某个对象锁，通过这种特性来实现多线程协调机制，这样在同一时刻只有一个线程对所需要的同步的代码块（复合操作）进行访问。互斥性也成为了操作的原子性。

  **可见性：**一个线程修改的共享变量，其他线程是否能够立刻看到。对于串行程序而言，并不存在可见性问题，前一个操作修改的变量，后一个操作一定能读取到最新值。但在多线程环境下如果没有正确的同步则不一定。

  **synchronized有序性：**多线程之间串行执行。volatile有序性和synchronized有序性不同

* **volatile的有序性和synchronized不同**

  > 怎么来定义顺序呢?《深入理解Java虚拟机第三版》有提到
  > Java程序中天然的有序性可以总结为一句话：如果在本线程内观察，所有操作都是天然有序的。如果在一个线程中观察另一个线程，所有操作都是无序的。前半句是指“线程内似表现为串行的语义”，后半句是指“指令重排”现象和“工作内存与主内存同步延迟”现象。
  >
  > 「synchronized」 的有序性是持有相同锁的两个同步块只能串行的进入，即被加锁的内容要按照顺序被多个线程执行，但是其内部的同步代码还是会发生重排序，使块与块之间有序可见。
  > 「volatile」的有序性是通过插入内存屏障来保证指令按照顺序执行。不会存在后面的指令跑到前面的指令之前来执行。是保证编译器优化的时候不会让指令乱序。
  > 「synchronized 是不能保证指令重排的」。
  > synchronized可以保证原子性和可见性
  > ————————————————
  > 版权声明：本文为CSDN博主「小白划水」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
  > 原文链接：https://blog.csdn.net/qq_42218187/article/details/118558526

  synchronized的有序性是指多个线程之间的synchronized块或方法时串行的。

  volatile的有序性是通过内存屏障实现，通过加Lock前缀的方式禁止指令重排。它能保证代码不会在编译时被jit优化重排序，也能保证被修饰的变量内部的多个操作不会被重排序。DCL单例模式中用了synchronized修饰，但是在new Singleton()时还是会出现问题，因为创建对象在CPU层面分为3步（和缓存无关），创建对象分配内存空间、初始化对象、将对象指向分配的内存空间，这3步会被重排序，所以要加volatile禁止指令重排。

  **总结，synchronized可以保证有序性但是不能避免指令重排。严格来说，有序性和指令重排之间存在一些细微的区别，禁止指令重排不仅包含了CPU层面的缓存同步操作，还包含了编译和运行期间的操作指令有序。**

  *如果你看过另一篇文章：《volatile底层原理(CPU多级缓存)》，对于这一点应该就能更好的理解了*

* **使用**

  - 修饰实例方法：作用于当前实例加锁
  - 修饰静态方法：作用于当前类加锁
  - 修饰代码块：指定加锁对象，对给定对象加锁

  作用范围不同，粒度不同，用于不同的场景。

* **线程安全问题**

  变量在多线程情况下会出现安全性问题，安全性问题指的是多个线程同时访问同一个变量时，会发生混乱，如果都改变了变量的值，可能与最终想要的结果不同。

  触发线程安全问题的前置条件是：多线程，有**共享变量**，有线程安全性问题（实例变量），**局部变量**没有线程安全性问题。

  共享变量：不安全

  局部变量：安全

  ```java
  public class Sync1 {
  
      private int num = 0;
  
      public static void main(String[] args) {
          final Sync1 sync1 = new Sync1();
          new Thread(() -> {
              sync1.addNum(1);
          }, "thread1").start();
  
          new Thread(() -> {
              sync1.addNum(2);
          }, "thread2").start();
      }
  
      public void addNum(int i) {
          if (i == 1) {
              num = 100;
          } else {
              num = 200;
          }
          /**
           * 正常情况：
           * current thread->thread1,i->1,num->100
           * current thread->thread2,i->2,num->200
           * 异常情况：
           * current thread->thread1,i->1,num->100
           * current thread->thread2,i->2,num->100
           * <p> 
           * current thread->thread1,i->1,num->200
           * current thread->thread2,i->2,num->200
           */
          System.out.println("current thread->" + Thread.currentThread().getName() + ",i->" + i + ",num->" + num);
      }
  }
  ```

  以上代码中的num变量就是不安全的，两个线程同时操作时，会出现安全性问题。

  原因其实很简单，就是因为num还没被读取，就又被再次重新赋值了。

  上述代码中每一个线程都执行了两个指令：

  线程1： 1、num=100；2、read num打印。

  线程2： 3、num=200；4、read num打印。

  当指令顺序为 1 2 3 4，结果就是正常情况

  但是因为是多线程，所以指令是会交替进行的

  当指令顺序为 1 3 4 2，结果是num都为200

  当指令顺序为 3 1 2 4，结果是num都为100

  

  **上述情况中属于有序性问题，但是无法使用volatile解决，因为volatile只能保证对num的读写不会重排序，也就是说只能保证1、2不被重排序，3、4不被重排序，无法保证1、2之间不会被插入3。**并且1、2之间是有关联的，本身就不会被重排序，所以这里加volatile是没有任何作用的。

  可以使用synchronized修饰addNum方法，当进入一个synchronized获取了锁之后，另一个线程再次获取时就必须等待解锁，锁里的代码是串行化的，所以可以解决上述问题。



### 2、锁基础知识

* **什么是锁？**

  锁的本质就是一个对象，当多个线程争抢同个锁的时，同一时间内只会有一个线程获取锁，只有获取锁的线程才能执行锁内的代码，其他线程执行到锁代码时会进入阻塞状态，等待锁释放后进行争抢。**synchronized就是一种常见的锁。**

  是异步还是同步主要就是看synchronized是不是同一把锁。

  如果是同一把锁，那么就是同步执行

  如果不是同一把锁，那么就是异步执行

* **锁重入**

  synchronized是可重入锁，进入一个A锁的代码块或方法之后，又在代码块内遇到A锁，还可以继续进入A锁。每次进入A锁都会将monitorenter+1，每次释放A锁会将monitorexit-1，当monitorCount==0时，代表当前线程完全退出锁，其他线程可开始争抢锁。

  重入锁一定要注意，在锁嵌套的时候，所有嵌套的方法签名上一定要加synchronized关键字，否则其他线程在调用无synchronized关键字的方法时就无需争抢锁。

  * 继承时锁重入

    在继承关系下锁重入机制也是可行的，可以直接调用，不需要等待，这就是synchronized 父子类锁重入 。因为子类继承了父类实际上就是拥有了父类的public方法，所有父类的public 方法也属于子类对象。同一个对象拥有了同一把锁，不需要竞争。

* **synchronized修饰的方法如果发生异常锁怎么办？会释放吗？**

  发生异常后会释放锁。假设两个方法都是有synchronized修饰的，其中一个发生异常了，jvm会在发生异常之后自动释放锁，让另一个方法获取锁并执行。在JVM底层中当线程在获取锁的状态下发生了异常，jvm会自动调用monitorExit来释放锁。

* **this锁和class锁究竟在什么情况之下使用？**

  this锁实际上是当前方法所在的实例----》单例比较多

  class锁实际上是当前方法所在的类----》new N个实例

  总结：

  1）如果当前class的实例是单例，那么就用this锁

  2）如果当前class的实例不确定是否是单例，方法间需要同步，则只用class锁

  前提：多线程执行多个方法，多个方法间需要同步

* **线程的死锁问题**

  线程死锁描述的是这样一种情况：多个线程同时被阻塞，它们中的一个或者全部都在等待某个资源被释放。由于线程被无限期地阻塞，因此程序不可能正常终止。

  线程 A 持有资源 2，线程 B 持有资源 1，他们同时都想申请对方的资源，所以这两个线程就会互相等待而进入死锁状态。

  ```java
      private final Object lock1 = new Object();
      private final Object lock2 = new Object();
  
      public static void main(String[] args) {
          Sync2 sync2 = new Sync2();
          new Thread(() -> {
              while (true) {
                  sync2.method1();
              }
          }).start();
  
          new Thread(() -> {
              while (true) {
                  sync2.method2();
              }
          }).start();
      }
  
      public void method1() {
          synchronized (lock1) {
              System.out.println("this is method1...");
              try {
                  Thread.sleep(10);
              } catch (InterruptedException e) {
                  throw new RuntimeException(e);
              }
              method2();
          }
      }
  
      public void method2() {
          synchronized (lock2) {
              System.out.println("this is method2...");
              try {
                  Thread.sleep(10);
              } catch (InterruptedException e) {
                  throw new RuntimeException(e);
              }
              method1();
          }
      }
  ```

  * 产生死锁的必要条件
    1. 互斥条件：该资源任意一个时刻只由一个线程占用。
    2. 请求与保持条件：一个进程因请求资源而阻塞时，对已获得的资源保持不放。
    3. 不剥夺条件:线程已获得的资源在未使用完之前不能被其他线程强行剥夺，只有自己使用完毕后才释放资源。
    4. 循环等待条件:若干进程之间形成一种头尾相接的循环等待资源关系。

  * 如何避免死锁？

    锁的获取保持有序。线程1和线程2获取锁的顺序一致，都是lock1->lock2或lock2->lock1。

    设置超时（synchronized没有超时机制，我们自定义lock来设置超时）。

    手动做死锁检测，比如通过map等数据结构来实现，用代码来判断是否会发生死锁。

* **锁优化**

  锁是会影响性能的，我们可以针对不同情况做出一些优化，JVM也会自动做出优化

  * 降低锁粒度

    只对一小块需要同步的地方加锁。在jdk1.7之前concurrentHashMap中有一个叫做锁分段的机制，简单来说就是给map中的每个桶分别加锁，这样效率更高，不过在jdk1.8之后已经不用锁分段了，后续在juc文章中会专门讲解concurrentHashMap。

    根据业务需求，对不同的同步代码块用不同的锁。

  * 逃逸分析&锁消除

    面试题：new创建的对象是否一定分配在堆上面？

    不一定，堆上面是有GC的，如果发生full gc是很消耗性能的。JVM会做出优化，如果一个对象只在方法内部使用，会分配在线程栈中，因为此变量只在线程栈内使用，线程一旦执行完整个线程都会被一起回收。

    逃逸分析实际上就是看局部变量有没有逃出方法之外，简单来说就是方法有没有将某个变量返回。

    如果方法上加了锁，但是方法内的变量都是栈上分配，也就是说没有发生逃逸，那么就会发生锁消除，就是把锁去掉，因为方法内根本没有共享变量，不存在线程安全问题。

  * 标量替换

    这也是JVM自动做出的优化，把对象打散之后分配在栈上或者寄存器上，也是为了防止出现GC。

    ```java
    Point point = new Point(int x,int y);
    System.out.print(point.x,point.y);
    ```

    以上代码中point不会被真正创建，而是直接在栈上创建了一个x变量，一个y变量。这就是标量替换的结果，因为后续并没有实际用到point对象，只是用了里面的xy属性。

    栈上分配最主要的还是为了防止GC。

  标量替换和逃逸分析都是JVM默认开启的，可以通过手动配置VM option开关。



### 3、synchronized实现原理

Synchronized的语义底层是通过一个monitor（监视器锁）的对象来完成。线程访问加锁对象，就是去拥有一个监视器（Monitor）的过程。

　　每个对象有一个监视器锁(monitor)。每个Synchronized修饰过的代码当它的monitor被占用时就会处于锁定状态并且尝试获取monitor的所有权 ，过程：

　　1）如果monitor的进入数为0，则该线程进入monitor，然后将进入数设置为1，该线程即为monitor的所有者；

　　2）如果线程已经占有该monitor，只是重新进入，则进入monitor的进入数加1（可重入）；

　　3）如果其他线程已经占用了monitor，则该线程进入阻塞状态，直到monitor的进入数为0，再重新尝试获取monitor的所有权。

* **字节码解析**

  * **同步方法**

    方法级的同步是隐式的，无须通过字节码指令来控制，JVM可以从方法常量池的方法表结构中的ACC_SYNCHRONIZED访问标志得知一个方法是否声明为同步方法。

    当方法调用的时，调用指令会检查方法的ACC_SYNCHRONIZED访问标志是否被设置。如果设置了，执行线程就要求先持有monitor对象，然后才能执行方法，最后当方法执行完（无论是正常完成还是非正常完成）时释放monitor对象。

    在方法执行期间，执行线程持有了管程，其他线程都无法再次获取同一个管程。

    *管程是一种概念，任何语言都可以通用。在java中，管程==Monitor*

  * **同步代码块**

    同步代码块，synchronized关键字经过编译之后，会在同步代码块前后分别形成monitorenter和monitorexit字节码指令。在执行monitorenter指令的时候，首先尝试获取对象的锁。

    如果这个锁没有被锁定或者当前线程已经拥有了那个对象的锁，锁的计数器就加1。在执行monitorexit指令时会将锁的计数器减1，当减为0的时候就释放锁。如果获取对象锁一直失败，那当前线程就要阻塞等待，直到对象锁被另一个线程释放为止。

* **对象头**

  锁的本质：串行来访问共享资源。实际上同步互斥访问（多个线程来争取一个**对象**）

  new Object()---->对象最终是丢给jvm来管理----》jvm会在对象上加一些管理信息

  jvm包装完之后：

  1）**对象头（重点）**

  2）实例数据

  3）填充数据

  

  32位对象头（Markword）：

  ![image-20210416112302935](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230611205402.png)

  64位对象头（Markword）：

  ![image-20210710192940782](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230611205716.png)

  上图是jvm对象头的信息，jdk1.6之前就是用的monitor（操作系统底层的互斥锁），是重量级锁，需要在用户态和内核态之间做切换，性能较差。在jdk1.6之后，synchronized不再直接加上monitor重量级锁，而是从偏向锁->轻量级锁->重量级锁一步步加锁，提升性能。

* **Monitor**

  无论是synchronized代码块还是synchronized方法，其线程安全的语义实现最终依赖于monitor。

  在[HotSpot](https://so.csdn.net/so/search?q=HotSpot&spm=1001.2101.3001.7020)虚拟机中，monitor是由ObjectMonitor实现的。源码由C++实现，位于HotSpot虚拟机源码ObjectMonitor.hpp文件中(src/share/vm/runtime/objectMonitor.hpp)。ObjectMonitor主要数据结构如下：

  ![image-20230611210159842](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230611210200.png)

  三大队列：cxq、entryList、waitSet

  - _owner: 初始时为NULL。当有线程占有该monitor时，owner标记为该线程的唯一标识。当线程释放monitor时，owner又恢复为NULL。owner是一个临界资源，JVM是通过CAS操作来保证其线程安全。**owner指向一个线程，被指向的线程就是抢到锁**。owner字段非常繁忙，因为大家都想把自己赋值给它。
  - _cxq: 竞争队列，所有请求锁的线程首先会被放在这个队列中(单向列表)。cxq是一个临界资源，JVM通过CAS原子指令来修改cxq队列。修改前cxq的旧值填入了node的next字段，cxq指向新值(新线程)。因此cxq是一个后进先出的stack（栈）。
  - _EntryList：cxq队列中有资格成为候选资源的线程会被移动到该队列中。（重量级锁退出时会将cxq队列里的线程移动到这里）
  - _WaitSet：因为调用wait方法而被阻塞的线程会被放在该队列中。

  ![image-20230613211708984](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230613211710.png)

  

* **锁膨胀**

  ![img](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230614184142.png@f_auto)

  锁膨胀是指 synchronized 从无锁升级到偏向锁，再到轻量级锁，最后到重量级锁的过程，它叫锁膨胀也叫锁升级。

  JDK 1.6 之前，synchronized 是重量级锁，也就是说 synchronized 在释放和获取锁时都会从用户态转换成内核态，而转换的效率是比较低的。但有了锁膨胀机制之后，synchronized 的状态就多了无锁、偏向锁以及轻量级锁了，这时候在进行并发操作时，大部分的场景都不需要用户态到内核态的转换了，这样就大幅的提升了 synchronized 的性能。

* **重量级锁**

  重量级锁是jvm的最后一个锁策略，当经历了偏向锁、轻量级锁都无法使用时才会使用重量级锁。

  重量级锁的性能很差，**它性能差是由于上下文切换**。当线程挂起（进入blocked状态）时，调用内核函数挂起。当线程唤醒时也会调用内核函数，所以这两个操作就存在用户态->内核态之间的切换。jvm属于用户态，想要调用操作系统函数，就需要切换到内核态。

  

  线程挂起：park（操作系统指令）--->pthread_cond_wait()--->object.wait()

  线程唤醒：unpark（操作系统指令）--->pthread_cond_singal()--->object.notify()

  重量级锁性能差的关键就是内核态用户态的切换。所以在JVM源码中，即使已经进入了执行重量级锁的方法，也进行了多次CAS，通过自旋获取monitor，尽量避免线程挂起。

  当一系列CAS还是抢不到锁时，执行ObjectWaiter方法，进入cxq队列。cxq队列是一个先进后出的栈结构，线程会尝试入队争抢头部节点，如果争抢失败，就又进行CAS获取锁试试，没抢到就再尝试入队抢头部，循环。

  ![image-20230613212920826](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230613212921.png)

  cxq队列是一个先进后出的栈结构，所以每个线程来的时候会从头节点加入，当多个线程同时获取头节点时，头节点是非常繁忙的。所以入队有可能失败，入队 失败 --> 抢锁 失败 --> 入队 失败 --> ...

  

  进入到cxq队列之后，就是要执行阻塞挂起操作了，这时又会先CAS获取锁试试（不死心啊），如果获取不到，那就没办法了，只能调用park，挂起。挂起后就阻塞住了，需要等待调用unpark方法。

  当前拥有锁的线程释放锁之后，会将owner指向空，此时就会调用unpark方法，重新循环获取锁。

  总结：一旦进入重量级锁，线程就会进入队列，一旦进入队列，说明线程已经挂起（进入内核态），后续被唤醒又需要从内核态进入用户态。

  对象头（锁所在的对象头）---->MarkWord[ptrObjectMonitor]------>ObjectMonitor

  * **重量级锁加锁流程图**

    ![image-20230626213124465](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230626213125.png)

  * 重量级锁退出流程图

    ![image-20230627204531608](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230627204533.png)

* **轻量级锁**

  真正进入重量级锁之前会有多次CAS，CAS失败的很大部分原因是由于owner的竞争过于激烈，导致CAS失败。只要让竞争不那么激烈，就能让CAS成功的概率提升。这就是轻量级锁。

  

  ![image-20230613220705338](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230613220706.png)

  LockRecord------》当前获取到锁的线程栈上面的锁记录

  轻量级锁加锁过程：

  1）首先，在线程栈中创建一个锁记录（LockRecord）

  2）拷贝对象头的markword到当前栈帧中的锁记录中

  3）cas尝试将markword中的指针指向当前线程栈中的锁记录，所标记也要改成00，表示此时已经是轻量级锁状态

  4）如果更新失败，表示此时竞争激烈，1、需要进行锁膨胀操作 2、重入锁

  轻量级锁最终就只有两种情况：

  1、加锁成功

  2、加锁失败，膨胀成重量级锁

  * **轻量级锁加锁流程图**

    第一步is_neutral是判断是否有锁

    ![image-20230626213029057](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230626213029.png)

  * **轻量级锁膨胀流程图**

    ![image-20230614200420171](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230614200421.png)

  * 轻量级锁释放流程图

    ![image-20230626212919425](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230626212920.png)
    
    * 上图中cas将head写回对象头，为什么需要cas来撤销，而且会撤销失败？
    
      如果当前锁是轻量级锁，确实只会有一个线程操作，但是此时锁是有可能已经被膨胀为重量级锁的。
    
      线程t1获取了轻量级锁，markword指向t1所在在栈帧，此时t2也来请求锁，此时拿不到锁，那么就会升 级膨胀为重量级锁，就把markword更新为ObjectMonitor指针。此时t1线程在退出的时候准备将markword还原，那么此时就会失败。t1只能膨胀为重量级锁退出。
    
      

* **偏向锁**

  在一定时间段内，有可能只有一个线程需要进入同步代码块，当只有一个线程进入时，就会以偏向锁的形式加锁。偏向锁是不存在竞争的，一旦发生竞争就会升级成轻量级锁。

  在没有实际竞争的情况下，还能够针对部分场景继续优化。如果不仅仅没有实际竞争，自始至终，使用锁的线程都只有一个，那么，维护轻量级锁都是浪费的。**偏向锁的目标是，减少无竞争且只有一个线程使用锁的情况下，使用轻量级锁产生的性能消耗**。轻量级锁每次申请、释放锁都至少需要一次CAS，但偏向锁只有初始化时需要一次CAS。

  “偏向”的意思是，*偏向锁假定将来只有第一个申请锁的线程会使用锁*（不会有任何线程再来申请锁），因此，*只需要在Mark Word中CAS记录线程id（本质上也是更新，但初始值为空），如果记录成功，则偏向锁获取成功*，记录锁状态为偏向锁，*以后当前线程id等于记录的线程id就可以零成本的直接获得锁；否则，说明有其他线程竞争，膨胀为轻量级锁*。

  偏向锁无法使用自旋锁优化，因为一旦有其他线程申请锁，就破坏了偏向锁的假定。
  
  * 偏向锁加锁流程图
  
    ![image-20230626202028014](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230626202028.png)
  
  * 偏向锁撤销流程图
  
    ![image-20230626204043892](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230626204044.png)
    
    



### 4、java jol分析对象头
