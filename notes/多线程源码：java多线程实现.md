## 多线程源码：java多线程实现

### 1、进程、线程、协程概念和区别

#### （1）什么是进程和线程？

进程是应用程序的启动实例，进程拥有代码和打开的文件资源、数据资源、独立的内存空间。

线程从属于进程，是程序的实际执行者，一个进程至少包含一个主线程，也可以有更多的子线程，线程拥有自己的栈空间。

![image-20230508202926912](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230508203517.png)



cmd查看java进程命令：

```
jps -l
```



* **操作系统中的进程和线程**

对操作系统而言，线程是最小的执行单元，进程是最小的资源管理单元。无论是进程还是线程都是由操作系统所管理的。

* **线程的状态**

线程具有物种状态：初始化、可运行、运行中、阻塞、销毁

![image-20230508205123089](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230508205124.png)

进程和线程区别

1、进程是CPU资源分配的基本单位，线程是独立运行和独立调度的基本单位（CPU上真正运行的是线程）。

2、进程拥有自己的资源空间，一个进程包含若干个线程，线程与CPU资源分配无关，多个线程共享同一进程内的资源。

3、线程的调度与切换比进程快很多

**CPU密集型代码（各种循环处理、计算等等）：使用多进程。很少和硬盘或网络传输，io少，阻塞少**

**IO密集型代码（文件处理、网络爬虫等）：使用多线程。硬盘和网络传输多，硬盘io多，网络io多，在阻塞期间可以切换线程先处理其他任务，提高效率**



* **进程之间也有共享内存的情况**

通常进程之间的内存是相互隔离的，进程共享内存的情况比较少，技术上也比较复杂。比如迅雷看看和迅雷下载，还没下载完成的电影就可以使用迅雷看看进行提前播放，这期间两个进程之间就共享了内存。

下图中迅雷下载和迅雷播放器就共享了已下载的百分20部分的内存。

![image-20230508204906784](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230508204907.png)



#### （2）进程、线程调度（并发、并行）

* **并发：单核CPU处理多个进程**

每个进程都会有自己的时间片，CPU在同一时间内只会执行一个进程的任务，当执行时间片消耗完后，调度程序会根据优先级和饥饿度选择下一个被执行的程序。单核CPU下通过这种不断轮询调度的方式实现程序同步运行。这种单CPU处理多个进程的方式叫做**并发**。

* **并行：多核CPU处理多个进程**

多个CPU可以在同一时间内处理多个不同的进程任务，比如两个CPU处理两个任务，那么这是两个CPU是**并行**的。**但是并行的情况往往和并发同时发生**，因为实际上进程肯定不止两个。比如有三个任务，A时间片300ms，B时间片20ms，C时间片10ms，CPU1处理A任务，CPU2处理B任务，当CPU2处理完B单次的时间片后CPU1还没处理完A的单次时间片，此时CPU2就需要通过调度程序从BC任务中选择一个执行，这时的处理方式还是并发。

![image-20230508202758888](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230508203529.png)



线程和进程同理，线程在进程之中，一个进程往往会有多个线程来执行多个任务，其中也存在并发和并行的概念，执行调度任务的原理其实和多进程差不多。区别在于多个线程是共享内存的，存在线程安全问题，可能需要用安全类或者锁的方式来解决。

![image-20230508204457638](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230508204458.png)

* **线程之间的协作**

最经典的例子是生产者/消费者模式，即若干个生产者线程向队列中生产数据，若干个消费者线程从队列中消费数据。

![image-20230508205838198](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230508205839.png)

生产者/消费者模式的性能问题是什么？

1、涉及到同步锁

2、涉及到线程阻塞状态和可运行状态之间的切换

3、涉及到线程上下文的切换

#### （3）什么是协程？

协程是一种比线程更轻量级的存在，正如一个进程可以有多个线程一样，一个线程可以拥有多个协程。java官方对于协程的支持还不完善，但是已经有第三方库可以使用。官方库：loom，第三方库：quasar

![image-20230508210258430](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230508210259.png)

* **操作系统中的协程**

协程不是被操作系统内核所管理的，完全由程序所控制，只有线程知道协程存在。这样的好处是性能大幅度提升，因为不会像线程那样切换消耗资源。

协程不是进程也不是线程，而是一个特殊函数，这个函数可以在某个地方挂起，并且可以重新在挂起处继续运行。所以说协程与进程、线程相比并不是同一个维度的概念。

一个线程可以由多个协程运行，但是一个线程内的多个协程的运行是串行的。



* **协程为什么效率高？**

协程的原理是java-agent，执行代码时需要在VM options加上一些配置，-javaagent:本地maven库中quasar-core的地址

协程类：Fiber  线程类：Thread



传统的J2EE系统都是基于每个请求占用一个线程去完成完整的业务逻辑（包括事务）。所以系统的吞吐能力取决于每个线程的操作耗时。如果遇到很耗时的I/O行为，则整个系统的吞吐立刻下降，比如JDBC是同步阻塞的，这也是为什么很多人都说数据库是瓶颈的原因。这里的耗时其实是让CPU一直在等待I/O返回，说白了线程根本没有利用CPU去做运算，而是处于空转状态。暴殄天物啊。另外过多的线程，也会带来更多的ContextSwitch开销。

Java的JDK里有封装很好的ThreadPool，可以用来管理大量的线程生命周期，但是本质上还是不能很好的解决线程数量的问题，以及线程空转占用CPU资源的问题。

协程的本质上其实还是和上面的方法一样，只不过他的核心点在于调度那块由他来负责解决，遇到阻塞操作，立刻yield掉，并且记录当前栈上的数据，阻塞完后立刻再找一个线程恢复栈并把阻塞的结果放到这个线程上去跑，这样看上去好像跟写同步代码没有任何差别，这整个流程可以称为`coroutine`，而跑在由coroutine负责调度的线程称为`Fiber`。比如Golang里的 `go`关键字其实就是负责开启一个`Fiber`，让`func`逻辑跑在上面。而这一切都是发生的用户态上，没有发生在内核态上，也就是说没有ContextSwitch上的开销。



#### （4）进程、线程、协程的对比

- 协程既不是进程也不是线程，协程仅仅是一个特殊的函数，协程与进程和线程不是一个维度的。
- 一个进程可以包含多个线程，一个线程可以包含多个协程。
- 一个线程内的多个协程虽然可以切换，但是多个协程是串行执行的，只能在一个线程内运行，没法利用CPU多核能力。
- 协程与进程一样，切换是存在上下文切换问题的。

* **上下文切换**

- 进程的切换者是操作系统，切换时机是根据操作系统自己的切换策略，用户是无感知的。进程的切换内容包括页全局目录、内核栈、硬件上下文，切换内容保存在内存中。进程切换过程是由“用户态到内核态到用户态”的方式，切换效率低。

- 线程的切换者是操作系统，切换时机是根据操作系统自己的切换策略，用户无感知。线程的切换内容包括内核栈和硬件上下文。线程切换内容保存在内核栈中。线程切换过程是由“用户态到内核态到用户态”， 切换效率中等。

- 协程的切换者是用户（编程者或应用程序），切换时机是用户自己的程序所决定的。协程的切换内容是硬件上下文，切换内存保存在用户自己的变量（用户栈或堆）中。协程的切换过程只有用户态，即没有陷入内核态，因此切换效率高。

  

#### （5）线程vs协程性能对比

需求：10w的线程和10w的协程，同时做运算，处理200w次运算处理

结果：

1W线程：4秒左右 1W协程：2.5秒左右。

10W线程：30秒左右  10W协程：2.5秒左右。

**线程数越大，后台调度的成本越高，线程调度需要到内核态，但是协程不要。**

创建了10W个协程实际上是被分配到若干个线程里（3个左右，具体由操作系统决定），CPU调度时只有3个线程之间在切换，所以比10W个线程快很多。



* **java线程案例**

~~~java
package com.mx.lang.Object;

public class JavaThread {


    /**
     * 10w个线程，每个线程处理2百万次运算
     * @param argus
     * @throws InterruptedException
     */
    public static void main(String[] argus) throws InterruptedException {
        long begin = System.currentTimeMillis();
        int threadLength = 100000;//10w
        Thread[] threads = new Thread[threadLength];
        for (int i = 0; i < threadLength; i++) {
            threads[i] = new Thread(() -> {
                calc();
            });
        }


        for (int i = 0; i < threadLength; i++) {
            threads[i].start();
        }
        for (int i = 0; i < threadLength; i++) {
            threads[i].join();
        }
        System.out.println(System.currentTimeMillis() - begin);
    }


    //200w次计算
    static void calc() {
        int result = 0;
        for (int i = 0; i < 10000; i++) {
            for (int j = 0; j < 200; j++) {
                result += i;
            }
        }
    }
}
~~~

* **java协程案例**

~~~java
package com.mx.lang.Object;

import co.paralleluniverse.fibers.Fiber;

import java.util.concurrent.ExecutionException;

public class JavaFiber {
    /**
     * 10w个协程，每个协程处理2百万次运算
     * @param argus
     * @throws InterruptedException
     */
    public static void main(String[] argus) throws ExecutionException, InterruptedException {
        long begin = System.currentTimeMillis();
        int fiberLength = 100000;//10w
        Fiber<Void>[] fibers = new Fiber[fiberLength];
        for (int i = 0; i < fiberLength; i++) {
            fibers[i] = new Fiber(() -> {
                calc();
            });
        }


        for (int i = 0; i < fiberLength; i++) {
            fibers[i].start();
        }
        for (int i = 0; i < fiberLength; i++) {
            fibers[i].join();
        }
        System.out.println(System.currentTimeMillis() - begin);
    }

    //200w次计算
    static void calc() {
        int result = 0;
        for (int i = 0; i < 10000; i++) {
            for (int j = 0; j < 200; j++) {
                result += i;
            }
        }
    }
}
~~~

查看jvm此时有多少线程？

10w协程分配给3个线程

***结论：25w个协程共用一个线程（一个线程中跑多个协程，协程不需要调度，对内核透明），4个线程一个100w个协程。***



* **多线程一定快吗？**

***上下文切换是有成本的。因此，思考一个问题：多线程一定快吗？***

代码演示：

~~~java
package com.mx.lang.Object;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * 验证多线程是否就一定快
 */
public class ConcurrencyTest {
    private static final long count = 1000000000l;

    public static void main(String[] args) throws InterruptedException {
        concurrency();
        serial();
    }

    private static void concurrency() throws InterruptedException {
        long start = System.currentTimeMillis();
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                int a = 0;
//                for (long i = 0; i < count; i++) {
//                    a += 5;
//                }
//            }
//        });

        FutureTask task=new FutureTask<Integer>(new Callable<Integer>(){
            @Override
            public Integer call() throws Exception {
                int a = 0;
                for (long i = 0; i < count; i++) {
                    a += 5;
                }
                return a;
            }
        });

        Thread thread2 = new Thread(task);
        thread2.start();
        int b = 0;
        for (long i = 0; i < count; i++) {
            b--;
        }
        //两种方式计算才准：1、用join 2、task.get() 区别是计算总时间放的位置
//        thread2.join();
//        long time = System.currentTimeMillis() - start;
        try {
            System.out.println("b=" + b + ",a=" + task.get() );
            long time = System.currentTimeMillis() - start;
            System.out.println("concurrency :" + time + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void serial() {
        long start = System.currentTimeMillis();
        int a = 0;
        for (long i = 0; i < count; i++) {
            a += 5;
        }
        int b = 0;
        for (long i = 0; i < count; i++) {
            b--;
        }
        long time = System.currentTimeMillis() - start;
        System.out.println("serial:" + time + "ms,b=" + b + ",a=" + a);
    }
}
~~~



### 2、java线程详解

#### （1）java默认线程

![image-20230509205532214](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230509205533.png)

* **main线程**

  运行一个java程序时jvm创建的主线程，源码是c++里创建的

* **Attach Listener线程**

  类似jvm的监听器，负责执行一些外部命令，比如在cmd执行java -version时，就是由这个线程首先执行。此线程在第一次执行jvm命令时就已经启动了。

* **Finalizer线程**

  垃圾回收线程，在main线程启动之后就启动了，是守护线程，main线程结束也会一起结束。

* **Reference线程**

  引用对象本身垃圾回收线程，比如软引用、弱引用、虚引用对象的垃圾回收。在main线程启动之后启动，也是守护线程。

* **Signal Dispatcher线程**

  监听Attach Listener线程，将接收到的命令进行分发处理，分发到不同模块处理。

* **Monitor Ctrl-Break线程（run执行）**

  在执行程序时如果是debug是会出现上述5个线程，如果是直接执行run还会多出一个线程Monitor Ctrl-Break。此线程用于监控死锁，在系统发生死锁时可以把线程的信息输出。当我们使用jdk自带的工具进行线程监控时，就需要此线程输出线程Dump，比如用jvisualvm查看线程。debug调试模式不需要此线程。



`Thread.getAllStackTraces();`可以获取所有的线程。

![image-20230509211754320](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230509211755.png)

上述线程在代码启动时可以直接从编辑器中看到，除了这些线程之外，使用jvisualvm还可以看到更多的线程，比如RMI线程，JMX线程，RMI是远程方法调用协议，这些线程用于远程连接程序。



#### （2）main线程C++源码跟踪

main线程由jvm创建。main方法是程序的入口，由jvm调用。

Thread类中有很多native方法，这种方法的实现在C++，这些方法很多都是线程的核心方法，java层面无法实现这些方法。线程是从操作系统层面来的，jvm不是真正的操作系统。线程调度时是通过CPU直接调度的，CPU不会直接操作jvm，CPU只会操作系统层面的线程，所以可以分析出java（hotspot）中的线程是和操作系统中的线程对应的。



由于Thread的实现方法有部分是C++代码，需要编译jdk才能跟踪到源码。



* **总体流程图**

![java中main方法执行流程](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230510210610.png)



* **main函数入口**

main入口其实是在C++里，最终返回到java层面。进入main函数首先判断操作系统，根据不同操作系统执行不同方法，我们以linux为例，进入的是JLI_Launch方法。

**main**

![image-20230510200422510](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230510200424.png)





* **JLI_Launch方法**

此方法会先进行检查jdk版本、jre环境、加载虚拟机、解析参数等操作。此处的LoadJavaVM方法是加载虚拟机方法，在此方法中最主要的是验证三个JNI方法的可用性。

JLI_Launch方法最后调用的是JVMInit方法。JVMInit中调用ContinueInNewThread0，在此方法中真正创建线程，此方法里最先创建出主线程，主线程的pid和进程pid一样。

pthread_create是操作系统函数，操作系统执行创建线程。此时操作系统的原生线程创建完毕，调用JavaMain方法。

JavaMain方法是创建main线程的核心，在此处调用initializeJVM初始化虚拟机，内部创建虚拟机，通过new JavaThread()创建java线程。

创建java线程完毕之后还需要将它和原生线程关联起来。通过OS Thread将他们关联起来。

**Java Thread（C++） --对应-->OS Thread --对应-->原生线程（hotsport）**



创建并关联完成java线程之后，在initializeJVM方法中还会继续初始化ObjectMonitor(锁相关)、全局模块初始化、创建VMThread(垃圾回收线程)。

目前为止这些线程还都在JVM虚拟机中，还在C++层面，接下来还需要将刚才初始化的这些线程映射到Java层面。

依然在initializeJVM方法中，继续往后会进入映射的方法create_initial_thread，在此方法中调用`JavaCalls::call_special`真正创建java线程。





**JLI_Launch**

![image-20230510200755923](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230510201539.png)

![image-20230510201115123](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230510201617.png)



**LoadJavaVM**

![image-20230510201412472](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230510201413.png)



**JVMInit**

![image-20230510202110659](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230510202111.png)



**pthread_create（创建操作系统原生线程）**

![image-20230510203710701](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230510203711.png)



**JavaMain（创建JAVA线程）**

![image-20230510203919074](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230510203920.png)

![image-20230510204040277](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230510204041.png)

![image-20230510204214897](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230510204215.png)

![image-20230510204254978](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230510204255.png)



**原生线程和java线程(C++)对应**

![image-20230510204358411](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230510204359.png)

![image-20230510204425463](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230510204426.png)



**create_initial_thread（创建真正java线程）**

![image-20230510210100261](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230510210101.png)

![image-20230510210116187](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230510210117.png)



#### （3）java实现线程

java层面的线程和操作系统原生线程还是有一定区别，原生线程的方法会更多，还有epoll，poll等方法。

* **Runnable接口**

  java的thread类实现了Runnable接口，这是一个函数式接口，这个接口中只有一个抽象方法run()，我们需要执行的代码就写在run方法中。

  Thread中也有一个run方法，此方法是给操作系统反调用的，通过实现runnable接口`new Thread(runnable).start()`时，就会产生这个操作系统反调用。操作系统只认识Thread下的run方法。下图中就是在Thread调用`new Thread(runnable).start()`时Thread类里的run方法，target是传入的runnable实例。

  ![image-20230512210836477](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230512210838.png)

* **registerNatives方法**

  此方法在Thread的static块中被调用，用于注册native方法，让jvm通过JNI技术调用底层实现。



* **java中实现实现线程有几种方式？**

  1、继承Thread类

  2、实现Runnable接口

  3、实现Callable接口（拿回返回值，FutuTask）

  4、线程池

​	

**方式一：继承Thread类的方式**

1. 创建一个继承于Thread类的子类
2. 重写Thread类中的run()：将此线程要执行的操作声明在run()
3. 创建Thread的子类的对象
4. 调用此对象的start():①启动线程 ②调用当前线程的run()方法

**方式二：实现Runnable接口的方式**

1. 创建一个实现Runnable接口的类
2. 实现Runnable接口中的抽象方法：run():将创建的线程要执行的操作声明在此方法中
3. 创建Runnable接口实现类的对象
4. 将此对象作为参数传递到Thread类的构造器中，创建Thread类的对象
5. 调用Thread类中的start():① 启动线程 ② 调用线程的run() --->调用Runnable接口实现类的run()

以下两种方式是jdk1.5新增的！

**方式三：实现Callable接口**

说明：

1. 与使用Runnable相比， Callable功能更强大些
2. 实现的call()方法相比run()方法，可以返回值
3. 方法可以抛出异常
4. 支持泛型的返回值
5. 需要借助FutureTask类，比如获取返回结果

- Future接口可以对具体Runnable、Callable任务的执行结果进行取消、查询是否完成、获取结果等。
- FutureTask是Futrue接口的唯一的实现类
- FutureTask 同时实现了Runnable, Future接口。它既可以作为Runnable被线程执行，又可以作为Future得到Callable的返回值



方式3和方式2的区别就是有些场景需要有返回值，此时方式3派上用场



**方式四：使用线程池**

说明：

- 提前创建好多个线程，放入线程池中，使用时直接获取，使用完放回池中。可以避免频繁创建销毁、实现重复利用。类似生活中的公共交通工具。

好处：

1.  提高响应速度（减少了创建新线程的时间）
2.  降低资源消耗（重复利用线程池中线程，不需要每次都创建）
3.  便于线程管理

自定义线程池时可以给定一些参数，例如核心线程数、拒绝策略等等，此处暂不详细讲解，后续会有专门的专题。

**在springboot项目中一般通过配置bean的方式初始化线程池**



* **代码演示**

~~~java
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

//方式一
class ThreadTest extends Thread {
	@Override
	public void run() {
		for (int i = 0; i < 10; i++) {
			System.out.println(Thread.currentThread().getName() + ":" + i);
		}
	}
}

// 方式二
class RunnableTest implements Runnable {
	@Override
	public void run() {
		for (int i = 0; i < 10; i++) {
			System.out.println(Thread.currentThread().getName() + ":" + i);
		}
	}
}

// 方式三
class CallableTest implements Callable<Integer> {

	@Override
	public Integer call() throws Exception {
		int sum = 0;
		for (int i = 0; i < 10; i++) {
			System.out.println(Thread.currentThread().getName() + ":" + i);
			sum += i;
		}
		return sum;
	}

}

// 方式四
class ThreadPool implements Runnable {

	@Override
	public void run() {
		for (int i = 0; i < 10; i++) {
			System.out.println(Thread.currentThread().getName() + ":" + i);
		}
	}

}

public class Test {
	public static void main(String[] args) {
		// 继承Thread
		ThreadTest thread = new ThreadTest();
		thread.setName("方式一");
		thread.start();

		// 实现Runnable
		RunnableTest runnableTest = new RunnableTest();
		Thread thread2 = new Thread(runnableTest, "方式二");
		thread2.start();

		// 实现Callable<> 有返回值
		CallableTest callableTest = new CallableTest();
		FutureTask<Integer> futureTask = new FutureTask<>(callableTest);
		new Thread(futureTask, "方式三").start();
		// 返回值
		try {
			Integer integer = futureTask.get();
			System.out.println("返回值（sum）：" + integer);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// 线程池
		ExecutorService pool = Executors.newFixedThreadPool(10);

		ThreadPoolExecutor executor = (ThreadPoolExecutor) pool;
		/*
		 * 可以做一些操作:
		 * corePoolSize：核心池的大小 
		 * maximumPoolSize：最大线程数
		 * keepAliveTime：线程没任务时最多保持多长时间后会终止
		 */
		executor.setCorePoolSize(5);

		// 开启线程
		executor.execute(new ThreadPool());
		executor.execute(new ThreadPool());
		executor.execute(new ThreadPool());
		executor.execute(new ThreadPool());

	}

}
~~~



#### （4）线程安全性

多个线程在操作同一资源时，有可能会出现数据错误的问题，这种问题被称为线程安全性问题。比如两个线程同时循环对一个int类型的值进行++操作，这个值到100时停下，最终的值有可能会超过100。假设值到达99了，线程A和线程B这时同时在执行，他们判断 flag<maxSize时都成立，所以都进行了++操作，那么最终的值就是101了。

通常解决线程安全性可以用两种方法：

1、加锁，比如synchronized

2、原子类，比如juc包下的AtomicInteger

锁和原子类详细实现和方式后续章节再讲解。



#### （5）线程状态转换详解

在java Thread类中有一个State的枚举类，此类中定义了6种状态分别对应了线程的不同阶段，也就是线程的生命周期。thread.getState()方法可以获取当前线程的状态。

![image-20230514150228428](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514150230.png)

Java线程的生命周期分为:**NEW（初始化状态）、RUNNABLE（可运行状态/运行状态）、BLOCKED（阻塞状态）、WAITING（等待状态）、TIMED_WAITING（有时限的等待）、TERMINATED（终止状态）**

![image-20201203132842569](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514152930.png)

* **NEW 初始状态**

  new Thread()，未调用start()方法之前

* **RUNNABLE 可运行/运行状态**

  可运行状态包括运行中状态（RUNNING）和就绪状态(READY)。

  * **运行中状态**（RUNNING) 表示处于该状态的的线程正在运行， 即相应线程对象的run方法所对应的指令正在由处理器执行 。当：

    1. 操作系统执行yield（）方法

    2. 时间片用完

    3. 来了更高优先级而被抢断时

       就会变为就绪状态。

  * **就绪状态**(READY) 表示正在执行run（）方法，可以通过系统调度来变为可运行状态。

* **BLOCKED 阻塞状态**

  处于这个状态的线程需要等待其他线程释放锁或者等待进入synchronized。线程的sleep()方法和join()方法也会进入阻塞状态。

* **WAITING 等待状态**

  处于这个状态的线程需要等待其他线程对其进行通知或中断等操作，从而进入到下一个状态。此状态一般需要手动调用方法才会出现，必须要调用唤醒方法才能解除，正常的业务代码中一般很少出现这个状态。**从WAITING状态唤醒的线程不会马上执行，要等待CPU分配时间片时才会执行。**

  * 调用如下3个方法会使线程进入等待状态：
    Object.wait()：使当前线程进入等待状态，直到它被其他线程通过notify()或者notifyAll唤醒。该方法只能在同步方法中调用。如果当前线程不是锁的持有者，该方法抛出一个IllegalMonitorStateException异常。
    Thread.join()：等待线程执行完毕，底层调用的是Object实例的wait方法；
    LockSupport.park()：除非获得调用许可，否则禁用当前线程进行线程调度。

* **TIMED_WAITING 等待状态**

  和WAITING状态类似，但是可以多定义一个超时时间，当超过此时间时线程被唤醒。

* **TWEMINATED 终止状态**

  线程执行完毕



**阻塞状态和等待状态的区别**

线程阻塞状态是线程本身不可计划的，而线程等待状态是线程本身计划之内的。

线程进入阻塞状态是被动的, 而线程进入等待状态是主动的。
阻塞状态的被动：线程在同步代码外，获取对象锁失败时，线程进入阻塞状态；何时获取对象锁失败不可知，即线程阻塞状态是线程本身不可计划的。
等待状态的主动：线程在同步代码内，等待其他线程操作时，线程接入等待状态；何时等待其他线程操作可知，即线程等待状态是线程本身计划之内的。

* **sleep()方法和wait()方法的区别**

  最主要的区别就是**释放锁(monitor的所有权)**与否。

  * sleep()

    sleep()方法是Thread类的方法，通过其定义可知是个native方法，在指定的时间内阻塞线程的执行。而且从其注释中可知，并不会失去对任何监视器(monitors)的所有权，也就是说**不会释放锁，仅仅会让出cpu的执行权**。

  * wait()

    wait()方式是基类Object的方法，其实也是个native方法。

    该方法会调用后不仅会让出cpu的执行权，还**会释放锁(即monitor的所有权)**，并且进入wait set中，知道其他线程调用notify()或者notifyall()方法，或者指定的timeout到了，才会从wait set中出来，并重新竞争锁。

  

#### （6）start0()方法native源码实现

java代码中的thread.start()方法里只是做了一个线程状态的校验，通知了线程组。之后就调用了start0()方法，此方法是native方法。说明启动线程时主要代码也是通过C++调用操作系统线程来实现的。在java层面做的事情非常有限。



**猜想：**在jvm层面启动一个原生线程，然后原生线程置为RUNNABLE状态，然后回调java中的run方法

**结论（关键步骤）：**

1. 判断是否重复调用start方法。
2. `new JavaThread()`创建线程，传入`thread_entry`方法，此方法中定义了回调的run方法，最终就是通过这个调用来实现回调Java接口。（JavaThread方法里）
3. 进入`JavaThread`后首先就会设置回调入口，也就是上一步的`thread_entry`，后续在原生线程初始化完成后会用到此回调。（JavaThread方法里）
4. 调用`os::create_thread`开始创建osThread，首先就是与JavaThread关联。（JavaThread方法里）
5. 调用`pcreate_thread`是真正执行创建线程的方法，在调用时传入了`Java_start`方法，这代表着创建的同时也需要将线程启动。（JavaThread方法里）
6. `Java_start`方法内的INITALIZED对应着java的NEW状态，此处有个无限循环，必须当线程状态不为此状态时才会继续往下走。我们是调用start方法启动线程，此时线程的状态会切换成RUNNABLE状态再继续执行。这里的循环实际上是在等待线程启动，改变状态的逻辑在外层 `new JavaThread()`下方的代码。（JavaThread方法里）
7. 从JavaThread的内部逻辑跳出，继续执行，`Thread_start`被执行后原生线程的状态被改为RUNNABLE，JavaThread内部的循环跳出，根据之前给定的`thread_entry`回调java run方法。

![java中Thread的start0方法源码流程](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514164817.jpg)



**判断是否重复调用start()方法**

![image-20230514160346395](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514160347.png)



**创建对应的内核线程**![image-20230514160636306](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514160637.png)



**thread_entry 最终的反调java run方法**

![image-20230514160740394](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514160741.png)



**JavaThread方法**

设置回调入口，创建原生线程，在前面的main线程源码时一样的，底层调用pthread_create创建原生线程。

![image-20230514161127147](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514161128.png)



**create_thread**

在此方法中先将java线程和osThread关联，再调用pcreate_thread真正创建线程。并且在创建线程时传入了**java_start**方法，直接启动线程。

![image-20230514161740204](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514161741.png)

![image-20230514161755877](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514161756.png)



**java_start**

此方法内的INITALIZED对应着java的NEW状态，此处有个无限循环，必须当线程状态不为此状态时才会继续往下走。我们是调用start方法启动线程，此时线程的状态会切换成RUNNABLE状态再继续执行。这里的循环实际上是在等待线程启动，改变状态的逻辑在外层 new JavaThread()下方的代码。

![image-20230514162031787](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514162032.png)



**Thread::Start()**

设置线程状态，启动原生线程真正执行改变线程状态的操作。

![image-20230514162446399](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514162447.png)

![image-20230514162802789](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514162803.png)

![image-20230514162719189](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514162720.png)



**回到JavaThread中，调用run方法**

![image-20230514163002036](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514163003.png)

**entry_point**

在刚进入**JavaThread方法**时设置了一个回调入口点，就是此时使用，回调run方法。

![image-20230514163159290](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514163200.png)



#### （7）java方法与C++方法对应

![image-20230514155832057](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230514155833.png)



### 3、java线程与C++实现

#### （1）手写自定义线程实现回调run方法

通过我们前面学习到的内容，知道了Java线程是通过c++回调java的run方法。我们可以手动写一个简单的run方法回调，有助于更清晰理解整体流程。

手写一个简单的线程回调实现其实大体上就分为三步：

1. C++创建原生线程
2. Java调用C++方法
3. C++回调run方法



* **C++创建原生线程**

  创建原生线程的方法就是`pthread_create(&tid,&attr,java_start,thread)`，调用此方法时需要传入四个参数。

  tid就是创建后线程的id，直接定义一个就可以。第二个是属性参数，不需要属性可以直接设为null。第三个参数是java_start，是回调入口，需要定义好回调入口传入。第四个参数是java线程，用来做java线程和操作系统映射的；我们这里简单实现，不做映射，直接用操作系统的线程回调java的run方法即可，直接设置为null。

  此处的重点是传入的`java_start`，此方法中我们需要做run方法的回调。

  ![image-20230515203830983](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230515203831.png)

  

* **Java调用C++方法**

  java中通过调用`start0()`实现线程启动，我们也可以在java层面写一个start0()的native方法，在C++层面实现，具体实现的逻辑其实就是创建操作系统线程并回调java run方法。需要用JNI技术调用C++代码，写好代码后通过`javah 全路径类名`命令生成.h文件，.h文件就像C++里的接口，用于和C++代码互相调用。

  以下java代码中还有个run方法，用于回调。

![image-20230515204123034](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230515204123.png)

![image-20230515204407315](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230515204408.png)



* **C++回调run方法**

  回调run方法的逻辑都在`java_start`里，以下代码实现了几个关键逻辑：

  1. `AttachCurrentThread`获取当前线程，也就是操作系统创建出来的线程。
  2. 通过虚拟机找到java的类，也就是我们写在java代码里的线程类。
  3. 找到java类后通过反射获取无参构造函数并实例化。
  4. 实例化后找到run方法。
  5. 回调run方法。

![image-20230515204538092](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230515204539.png)

![image-20230515205802827](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230515205803.png)



#### （2）native方法类比总结（java跨平台的实现）

* **数据库驱动**

  当应用程序需要调用数据库进行操作时，我们不会通过代码直接调用数据库，而是由数据库提供对应的数据库驱动，代码通过驱动来操作数据库。比如java的jdbc，数据库驱动提供的全都是接口，没有具体实现，因为在应用层面不用关心数据库具体是如何实现的。mysql、oracle等数据库只需要提供对应语言的驱动即可实现应用对数据库的操作。

* **native方法的设计原理**

  native方法的实现正好和数据库驱动的**相反**。理论上来说在java中调用线程时，是在应用层面调用操作系统，那么我们也不需要关心操作系统的实现，所以java也可以通过linux驱动（实际上没有这个驱动，只是类比数据库的模式来讲解），来实现对操作系统的操作。

  如果也按照这样正向的调用存在一个问题，操作系统需要提供驱动给java等语言使用。但是操作系统很早就已经有了，不会因为某个的出现重新改写并且提供驱动给对应的语言。既然操作系统不能提供，那么就由java自己提供。

  java为了适配各个系统，jvm被一分为二，分为java应用部分和c++部分，在c++部分就是native方法的实现，相当于native的方法就是各个操作系统的驱动。在实现native方法时就会根据不同的系统做不同的实现，这也是java能够**跨平台**的关键。



![native方法类比总结](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230515210302.png)



### 4、java线程API详解

#### **（1）线程构造方法源码解读**

* **Thread()**

  无参的构造方法内部直接调用了`init(ThreadGronp g, Runnable target, String name, long stackSize)`；一二参数都是null，name参数用默认的"Thread-计数数字"，stackSize是线程栈的深度，传0。(threadGroup和stackSize参数后续会详细讲解)

  第一次调用的init方法还会再调用一个重载的init方法，多传一个AccessControlContext，防止第三方系统再线程中搞破坏，后续讲解。

  最终init方法内部实现就是进行一些初始化操作

  1. 将当前线程作为该线程的父线程，如果是main调用，那么此线程的父线程就是main线程。
  2. 线程组相关判断，如果线程组为null则用父线程的线程组。线程组概念后续讲解。
  3. 将父级线程置为守护线程，使用父线程的优先级。

  如果new Thread()直接调用start方法，会直接结束，因为Thread里的run方法会判断runnable是否为空，为空就直接结束了。

  ![image-20230516200319049](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230516200320.png)

* **Thread(String)**

  指定线程名字。

* **Thread(Runnable)**

  传入runnable，这时调用start方法则会运行runnable内部的逻辑。

* **Thread(Runnable, AccessControlContext)**

  不能直接new，默认是protected方法，是在线程类内部使用的。用来防止外部系统对线程造成影响。

* **Thread(Runnable, String)**

  传入runnable并指定线程名字。

* **Thread(ThreadGroup, Runnable)**

  线程组相关，对应的有4个构造方法，就是和前4个一样的，多传一个线程组参数。



在创建线程时，由于runnable是函数式接口（接口中只有一个抽象方法，@FunctionalInterface注解修饰），所以可以用lamda表达式进行简写

```java
new Thread(() -> {                       
    System.out.println(Thread.currentThread().getName() + " " + b);
},"线程1").start();
```



#### （2）线程组（ThreadGroup）

**线程组是为了方便管理相同类型的线程**，比如生产者消费者模式，生产者都是做A逻辑，消费者都是做B逻辑，那么使用线程组就可以方便管理，批量操作A或B线程组。比如我们需要中断线程时，如果没有线程组则需要通过循环来一个个中断，通过线程组就可以直接进行批量中断操作。

线程组是一个树状结构，最上层的根线程组system是在C++层面创建的，在jvm启动时就创建好了，线程组下可以有线程，也可以有线程组。

main线程的父级线程组是system根线程组，main线程的线程组的名字就叫做main，所以当我们在main线程中创建线程不手动指定线程组时，那么新创建的线程组就是main。和main线程共用一个线程组。

![image-20230516203043933](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230516203044.png)

线程组操作方法：

* **threadGroup.activeCount()**

  获取当前线程组内活跃的线程数量，必须是启动状态的线程才会被计入活跃数量，调用线程的start方法之后才会把线程加入到线程组中，启动的线程计数+1，未启动的线程计数-1。

* **threadGroup.enumerate(Thread[])**

  获取线程组内的所有线程，包括内部的线程组里的线程。需要新建一个数组，传入后会给这个数组赋值，把当前线程组下的所有的线程放到此数组中。同理还可以传入ThreadGroup[]来获取当前线程组下所有的线程组。

* **threadGroup.interupt()**

  批量中断线程组内的线程。其实线程组的中断就是通过循环来把组内的所有线程中断，是方便我们操作的。线程中断的方法后续会做详细讲解。



构造方法：

* **ThreadGroup()**

  不能直接new空参构造方法，是private方法，此方法是用来给c++反调的，用来创建根线程组，起名system。

* **ThreadGroup(String)**

  可以new，指定线程组名字。

* **ThreadGroup(ThreadGroup, String)**

  线程组内放线程组，命名。



#### （3）线程中的代码安全性保护

线程代码中可能有一些破坏性的代码，比如把系统文件删除、在无预期的情况把虚拟机关闭、打开系统内的其他文件等。线程需要防止这些情况的发生，防止第三方系统对我们的线程进行一些危险操作代码，保护系统的安全。

当外部系统调用类似`Runtime.getRuntime()`和`System.xxx`的代码就需要拒绝访问，防止系统被篡改。**在架构设计时需要格外注意这些安全性问题。**

线程中内部调用`Thread(Runnable, AccessControlContext)`，其中的AccessControlContext就是用来做安全保护的。

* **AccessControlContext**

  以下代码中的`protectMethod()`方法就可以提供给第三方系统实现，用`CodeSource`和`Permissions`组成一个`ProtectionDomain`保护域。保护域用来创建`AccessControlContext`，最终在创建线程时将`AccessControlContext`传入即可。

  ![image-20230517200915111](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230517201303.png)

  ![image-20230517201050998](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230517201310.png)

  新建类实现protectMethod()方法，此时调用System.exit会失败，报错access denied

  ![image-20230517201233735](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230517201421.png)

  

#### （4）线程和stackSize的关系

* **虚拟机栈**

  栈可以理解为一个桶，桶里装着方法，每个方法都会占满桶的一层，而且执行时需要从最上面的方法开始执行，只有当最上层的执行完毕了才能继续向下层执行。当我们执行一个方法A后，方法A会被放入到栈里。如果此方法A内又调用了另一个方法B，那么方法B也会被放入栈里。此时栈的顶层是B，底层是A，那么一定是先执行B，B完成后再继续执行A。

* **栈容量**

  栈容量指一个栈内能存储的变量信息大小，栈的容量不是无限的，而且不会自动扩容。linux系统的栈容量默认值为1M，设置默认值的代码在c++层面。在java中创建线程并启动后，进入到前面提到过的C++创建线程入口`JVM_ENTRY`方法中，是在`new JavaThread()`之前获取好并传入的。

  * **stackSize默认值设置**

    通过观察java创建线程时调用的init方法，可以发现当我们不指定stackSize时init方法默认传的是0，在C++中会判断值为0并且是Java线程时会设置1M的容量。

    栈容量有个最小值，linux系统下最小容量为236k，最终设置的栈容量是两者取最大值。如果我们在linux启动java项目时指定虚拟机启动使用的内存 -Xss235k，那么启动时就会报错。

  * **手动设置stackSize**

    线程中有构造方法可以传入stackSize参数的，接受的是一个long类型的值，比如需要指定2M容量，就传入1024 x 1024 x 2。

    当手动设置stackSize了又指定-Xss235k时，会忽略-Xss235k，按照设置的stackSize启动项目。

* **栈深度**

  栈深度指一个栈内能存储的方法个数，根据每个栈帧的大小和栈容量来决定栈深度。栈的深度不会固定的值，每次启动都会随有些浮动。我们可以用递归的方式来测试栈的深度。



在main线程和新开子线程中分别测试栈的最大深度会发现，子线程中的深度往往会比main方法更深，这是因为main线程里还会存在一些其他的变量，那么它的每个方法需要的容量就会更大一些，所以main的深度一般来说会更浅一些。



#### （5）线程优先级

顾名思义就是各个线程之间执行的优先级，优先级的值在1-10之间，并且单个线程的优先级不能高于它所在的线程组的优先级。线程的默认优先级是5。

java代码中只是做了一些最基础的判断，真正的实现在c++层面。本质上就是调的操作系统函数：setpriority。`setpriority, pthread_create, pthread_join`。实际上在设置时还是和start()方法一样，通过C++调用操作系统的设置优先级方法。

**高优先级的线程不代表一定先执行，只不过是在分配CPU资源时会更容易被分配。优先级只是让操作系统和cpu尽量优先分配资源，但并不能用来控制执行顺序。**

* **线程优先级具有继承性**

  在一个线程内创建另一个新线程时，新线程默认的优先级和父线程的优先级相同。

![image-20230521151854853](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230521151856.png)



* **setPriority0() c++代码**

![image-20230521152209430](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230521152210.png)

![image-20230521152236013](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230521152236.png)

![image-20230521152250747](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230521152251.png)



#### （6）守护线程

在 Java 语言中，线程分为两类：用户线程和守护线程，默认情况下我们创建的线程或线程池都是用户线程，所以用户线程也被称之为普通线程。 想要查看线程到底是用户线程还是守护线程，可以通过 `Thread.isDaemon()` 方法来判断，如果返回的结果是 true 则为守护线程，反之则为用户线程。 想要将一个线程设置为守护线程可以通过`Thread.setDaemon()`方法设置。
![image-20230521163257908](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230521163331.png)

* **守护线程和用户线程**

  守护线程（Daemon Thread）也被称之为后台线程或服务线程，守护线程是为用户线程服务的，当程序中的用户线程全部执行结束之后，守护线程也会跟随结束。 守护线程的角色就像“服务员”，而用户线程的角色就像“顾客”，当“顾客”全部走了之后（全部执行结束），那“服务员”（守护线程）也就没有了存在的意义，所以当一个程序中的全部用户线程都结束执行之后，那么**无论守护线程是否还在工作都会随着用户线程一块结束，整个程序也会随之结束运行。**

* **java中的守护线程**

  最典型的守护线程就是垃圾回收线程。java垃圾回收线程的作用是监控并清理java程序运行时产生的无用对象，如果java程序都结束了，那么自然也不需要再继续进行垃圾回收，所以垃圾回收线程是守护线程，随最后一个用户线程结束而结束。

* **守护线程的应用场景**

  系统监控：采集系统性能参数、CPU、磁盘等信息。

* **main可以被设置成守护线程吗**

  main线程也是用户线程，`Thread.setDaemon()`方法是设置守护线程的方法，这里有一个判断是否活跃线程。如果线程已经启动，那么就不可再设置守护线程。必须要在线程调用start方法之前设置。所以main线程无法被设置为守护线程。

  ![image-20230521153526625](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230521153527.png)

* ##### 结束程序：DestroyJavaVM线程

  `DestroyJavaVM`销毁虚拟机线程，此线程是结束虚拟机的线程，**它自身也是一个用户线程，是程序结束之前的最后一个用户线程**。它会监控应用中如果用户线程大于1，执行wait操作。`wait`方法需要用`notify`方法唤醒，在每个用户线程结束后都会判断当前应用中还剩下几个用户线程，如果只剩下1个后（最后一个用户线程就是DestroyJavaVM线程本身），就调用`notify`唤醒`DestroyJavaVM`线程。

  在main线程启动时的最后一步就是创建`DestroyJavaVM`线程，并且让此线程wait()。

  用户线程在系统中实际上是以链表形式存在的，某个线程结束完成后就会从链表中移除，让它的前一个node指向后一个node。所以每个线程都可以判断是不是最终只剩下一个用户线程。

* **线程结束**

  当一个线程结束时会由C++回调java的`Thread.exit()`方法，java层面的此方法会将线程的target、threadLocals等参数赋值为空，方便垃圾回收线程进行回收。

  对应C++层面其实也是执行exit()方法，此方法中最主要的有三点：

  1. 移除链表节点
  2. 判断用户线程数量
  3. 用户线程数量=1，唤醒DestroyJavaVM线程

  *回调java exit*

  ![image-20230521160453023](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230521160454.png)

  *移除链表节点*

  ![image-20230521160552750](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230521160553.png)

  *判断线程数量，唤醒DestroyJavaVM线程*

  ![image-20230521160624304](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230521160625.png)

  *DestroyJavaVM的wait*

  ![image-20230521161027980](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230521161028.png)



* **能不能救守护线程一命？**

  能，但没必要。原理就是让DestroyJavaVM延迟执行，使用hook实现。但是这样已经失去了守护线程的意义。

  ![image-20230521164237456](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230521164238.png)



#### （7）hook（钩子）线程

如果添加了Hook线程，则JVM程序即将退出的时候（收到了中断信号），Hook 线程在JVM主线程彻底退出之前会被执行。**直接杀死进程是无法执行hook线程的**。

用法：`Runtime.getRuntime().addShutdownHook(new Thread(()->{...}))`。在方法内放入一个线程，但无需启动，后续c++回调时在`ApplicationShutdownHooks.runHooks()`中会启动此线程。

* **使用场景**

  比如有一个任务，一直在运行，如果说发生异常（main线程，main结束），

  1. 此时需要上报异常，邮件通知相关人员
  2. 此时需要释放资源，网络，数据库连接资源等等

  很多中间件和框架中都会用到hook，比如结束时把数据写入磁盘等等。

* **钩子线程可以设置多个吗？**

  可以。钩子线程实际上会被放到一个map中。在`ApplicationShutdownHooks`类中，static块里就会初始化此map。

  ![image-20230521170323351](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230521170324.png)

* **钩子线程在哪个阶段被执行**

  `DestroyJavaVM`线程被notify之后就会调用`hook`线程，回调java中`Shutdown`类中的方法，触发`addShutdownHook`里的代码。

  ![image-20230521164913366](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230521164914.png)

  ![image-20230521165128377](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230521165129.png)



#### （8）线程中的异常处理

如果在Main线程中创建了子线程，常规情况下我们用try catch无法在main线程里捕获子线程内的异常。因为run方法没有往外抛出异常，并且我们在run方法中手动抛出异常也无法被父线程捕获，因为抛出的异常是被虚拟机捕获的。

如果我们无法在父线程中处理子线程内的异常，就存在一定问题，比如hook线程抛出的需要上报的异常，就无法被捕获。

* **如何在线程外部捕获异常？**

  `t.setUncaughtExceptionHandler(UncaughtExceptionHandler eh)`，此方法就是用来捕获catch不到的异常，需要传入的handler是函数式接口，可以用lambda表达式。入参为线程和异常。

  要注意的是如果子线程内已经把异常catch了，通过此方法就无法再次捕获了，除非在catch块中再次抛出异常。所以使用了此方法捕获异常就无需在线程内再做捕获，可以做统一的异常处理。

  ![image-20230522183542367](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230522183544.png)

* **统一处理所有线程异常**

  直接调用`Thread.setUncaughtExceptionHandler`，不通过某个线程调用而是直接Thread类调用，即可统一处理所有未设置异常处理的线程异常。也就是说线程自身设置的异常处理比统一设置的优先级更高。

* **线程异常处理源码跟踪**

  在Thread类中的`dispatchUncaughtException`，此方法由C++代码回调。此方法会优先获取线程的`UncaughtExceptionHandler`，如果没有的话，会调用线程组的handler，会进入到ThreadGroup里调用uncaughtException找父线程的handler，还没有的话就找全局的handler，如果还没有就在控制台进行打印异常。

  C++中有一个线程退出的方法，无论是正常还是异常退出都会调用此方法，在此方法中就会回调java异常处理方法。

  

  *Thread.dispatchUncaughtException*

  ![image-20230522192029447](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230522192142.png)

  *Thread.getUncaughtExceptionHandler*

  ![image-20230522192656101](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230522192657.png)

  *ThreadGroup.uncaughtException*

  ![image-20230522193007564](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230522193008.png)

  

* **线程异常处理优先级总结**

  1. 线程自身设置的异常处理
  2. 线程所在线程组设置的异常处理
  3. 父级线程设置的优先级

  常规情况下的优先级就是根据以上三种来决定的，但是我们还可以通过重写Thread类的`getUncaughtExceptionHandler`方法来设置一个最优先的异常处理。

