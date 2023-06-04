## 多线程源码：volatile底层原理(CPU多级缓存)

### 1、volatile机制

* **概念**

  volatile是Java提供的一种轻量级的同步机制，保证了多线程之间的可见性和有序性。在多个线程需要共享同一个变量时，此变量可用volatile关键字修饰，被修饰后的变量在修改之后所有读取它的线程都能获取到正确的值。

* **多线程特性**

  java多线程有三大特性：原子性、可见性、有序性

  **原子性：**多个操作作为一个整体，不能被分割与中断，也不能被其他线程干扰。如果被中断与干扰，则会出现数据异常、逻辑异常。

  **可见性：**一个线程修改的共享变量，其他线程是否能够立刻看到。对于串行程序而言，并不存在可见性问题，前一个操作修改的变量，后一个操作一定能读取到最新值。但在多线程环境下如果没有正确的同步则不一定。**有序性：**代码最终执行的顺序与我们看到的代码的顺序一致。串行执行。多线程下可能会发生指令重排，后面的操作反而先执行。

* **volatile的作用**

  volatile是Java虚拟机提供的轻量级同步机制

  - 保证可见性，读到的总是最新值

  - 不保证原子性

  - 禁止指令重排（保证有序性），禁止重排并不是禁止所有的重排，只有volatile写入不能向前，读取不能向后。除了这两种情况以外，其他重排是允许的。

    * 写入不能向前：对volatile变量的写入不能重排到写入之前的操作前面，从而保证别的线程能看到最新写入的值。
    * 读取不能向后：对volatile变量的读操作，不能排到后续的操作之后。

    *指令重排相关规则后续会有详细示例列举*

* **实现原理**

  volatile 可以保证线程可见性且提供了一定的有序性，但是无法保证原子性。在 JVM 底层是基于内存屏障实现的。

  **字段上面如果加上volatile修饰，那么底层汇编源码会加上lock前缀指令，从而起到全屏障的作用。**

  可见性、有序性、原子性最终发生的位置其实都在CPU底层。

  volatile的底层实现非常复杂，多数是硬件级别的操作。想要真正理解volatile的原理，我们必须了解CPU多级缓存架构、JMM(java内存模型)等知识。

  理解voaltile的正确姿势：单cpu架构->cpu多级cache结构 -> **缓存一致性协议（MESI）-> store buffer和invalidate queue引入 ->造成mesi协议不一致了**-> 内存屏障-->volatile-->mesi协议再次一致

* **可见性问题代码示例**

  假设有两个线程AB，还有一个类变量int i=0。启动线程后马上再A线程中执行i=1，过两秒再B线程中获取i，取到的值可能还是0。这就是一个典型的可见性问题。其实这就是可见性问题。

  我们先来看几段代码：

  ```java
  public class volatile1 {
  
      static int i = 0;
  
      public static void main(String[] args) {
  
          new Thread(() -> {
              while (true) {
                  if (i == 1) {
                      System.out.println(Thread.currentThread().getName() + ">>>>> i = " + i);
                      break;
                  }
              }
          }, "A").start();
  
          new Thread(() -> {
              try {
                  Thread.sleep(2000);
              } catch (InterruptedException e) {
                  throw new RuntimeException(e);
              }
              i = 1;
          }, "B").start();
          
      }
  }
  ```

  以上代码是一个常见的案例，对多线程有过一些了解的同学就能知道此时A线程中的打印是无法执行的，因为此处就存在可见性问题。这时由于每个线程对于i变量都有一个属于自己的副本，A线程获取副本时i=0，B线程修改了i的值之后A线程并没有去内存中重新获取i，没有人通知A去刷新副本，所以A里的打印不会执行。

  如果将A线程的休眠时间大于B线程，即在B修改了i的值之后，A再进入判断，则可以打印。因为B修改了值之后会将这个值写回内存，此时内存中的i已经是1了，那么当A获取内存中的i值作为副本时，自然i就等于1了。

* **可见性诡异问题**

  让我们保持上面代码的其他部分，只在A线程的循环内加入一段打印。代码如下：

  ```java
  ...
          new Thread(() -> {
              while (true) {
                  System.out.println("haha");
                  if (i == 1) {
                      System.out.println(Thread.currentThread().getName() + ">>>>> i = " + i);
                      break;
                  }
              }
          }, "A").start();
  ...
  ```

  按照我们上面的理解，此处应该还是无法打印i的值才对，但事实上却可以。这是为何呢？

  网络上有人总结了几种会出现可打印的情况：

  1. 加一行`System.out.println();`
  2. 加同步块`synchronized`
  3. 加`Thread.sleep()`休眠
  4. 加一行`File file = new File(filePath);`

  以上这几种情况都会让A线程拿到真正的i值，为什么呢？有人总结是有操作io，所以可以拿到，其实不是。

  真正的原因是这几种方法都用到了synchronized关键字，无论是打印还是线程的休眠又或是File的构造方法中，都一定有synchronized关键字出现，synchronized可以保证可见性、有序性、原子性。所以是代码遇到了synchronized的时候去刷新了副本，这时i才可以拿到最新的值。以上代码中的`System.out.println("haha");`就是通知线程去刷新副本的。

* **什么时候会自动刷新副本？**

  ```java
  public class Volatile2 {
  
      static int i = 0;
      private static final int count = 600; // 100 200 300 ... 10000 100000
  
      public static void main(String[] args) {
          new Thread(() -> {
              final List<Integer> list = new ArrayList<>();
              for (int j = 1; j <= count; j++) {
                  list.add(j);
              }
  
              while (true) {
                  if (list.contains(i)) {
                      System.out.println(Thread.currentThread().getName() + ">>>> i = " + i);
                      break;
                  }
              }
          }, "A").start();
  
          new Thread(() -> {
              try {
                  Thread.sleep(1000);
              } catch (InterruptedException e) {
                  throw new RuntimeException(e);
              }
              for (int j = 1; j <= count; j++) {
                  i = j;
              }
          }, "B").start();
      }
  }
  ```

  以上代码中在A线程中首先创建出一个list并且往里面插入了1-n的数字，然后进入循环，判断list是否包含i。B线程中休眠1秒之后（为了确保A线程已经准备好list并进入循环），也开始循环给i赋值。当i达到一定值的时候，在A线程中的副本就会被刷新。在windows系统下大概是i=600的时候，A线程中打印的i值也是600。这貌似说明了当i到达600的时候就会让A线程刷新一次副本，但是我们加大数字到100000时会发现最终打印的i值不固定。可能是10W，也可能是几千。

  这个例子中就抛出了两个问题：

  1. 这个刷新副本的阈值是如何得出的，为什么到了这个值就会刷新副本？
  2. 为什么刷新副本的值是随机的，有时10W有时几千？

  这其实是由CPU多级缓存架构造成的，我们接着来详细讲解。



### 2、JMM(java内存模型)

JMM 是Java内存模型（ Java Memory Model），简称JMM。它本身只是一个抽象的概念，并不真实存在，它描述的是一种规则或规范，是和多线程相关的一组规范。通过这组规范，定义了程序中对各个变量（包括实例字段，静态字段和构成数组对象的元素）的访问方式。需要每个JVM 的实现都要遵守这样的规范，有了JMM规范的保障，并发程序运行在不同的虚拟机上时，得到的程序结果才是安全可靠可信赖的。如果没有JMM 内存模型来规范，就可能会出现，经过不同 JVM 翻译之后，运行的结果不相同也不正确的情况。

因为在不同的硬件生产商和操作系统下，内存的访问方式各有所差异，这样就会造成相同的代码出现不一样的问题，而 **JMM 屏蔽掉了各种操作系统的内存访问差异，以实现“Write Once，Run Anywhere”的目标**。

JMM 中规定所有的变量都存储在**主内存** （Main Mem）中，包括实例变量、静态变量，但是不包括局部变量和方法参数。每条线程都有自己的**工作内存**（Work Mem），线程私有。工作内存中保存的是**线程的变量从主内存中的拷贝副本**。

这种结构的基本工作方式是：线程对变量的读和写都必须在工作内存中进行，而**线程之间变量值的传递均需要通过主内存来完成**。

* **JMM存在的必要性**

  ![1200440-20170811172858257-1790410735](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230604170743.png)

  上图中左侧是不同的CPU架构，目前x86是最常见的。不同的CPU架构访问内存的规则是不一样的，**JMM就是为了屏蔽底层的各种内存模型**。

  不同的CPU架构的对于内存屏障的严格性要求不同。JVM实现了多个方法用于应对不同操作系统和CPU架构，就是通过JMM的规范来设计，这也是java可移植性强的关键。

  

* **JMM操作指令**

  ![7](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230604170415.png)

  关于主内存与工作内存之间的交互协议，即一个变量如何从主内存拷贝到工作内存。如何从工作内存同步到主内存中的实现细节。java内存模型定义了8种操作来完成。这8种操作每一种都是原子操作。8种操作如下：

  - lock(锁定)：作用于主内存，它把一个变量标记为一条线程独占状态；
  - read(读取)：作用于主内存，它把变量值从主内存传送到线程的工作内存中，以便随后的load动作使用；
  - load(载入)：作用于工作内存，它把read操作的值放入工作内存中的变量副本中；
  - **use(使用)：作用于工作内存，它把工作内存中的值传递给执行引擎（CPU），每当虚拟机遇到一个需要使用这个变量的指令时候，将会执行这个动作；**
  - **assign(赋值)：作用于工作内存，它把从执行引擎获取（CPU）的值赋值给工作内存中的变量，每当虚拟机遇到一个给变量赋值的指令时候，执行该操作；**
  - store(存储)：作用于工作内存，它把工作内存中的一个变量传送给主内存中，以备随后的write操作使用；
  - write(写入)：作用于主内存，它把store传送值放到主内存中的变量中。
  - unlock(解锁)：作用于主内存，它将一个处于锁定状态的变量释放出来，释放后的变量才能够被其他线程锁定；

  Java内存模型还规定了执行上述8种基本操作时必须满足如下规则:

  （1）不允许read和load、store和write操作之一单独出现（即不允许一个变量从主存读取了但是工作内存不接受，或者从工作内存发起会写了但是主存不接受的情况），以上两个操作必须按顺序执行，但没有保证必须连续执行，也就是说，read与load之间、store与write之间是可插入其他指令的。

  （2）不允许一个线程丢弃它的最近的assign操作，即变量在工作内存中改变了之后必须把该变化同步回主内存。

  （3）不允许一个线程无原因地（没有发生过任何as144sign操作）把数据从线程的工作内存同步回主内存中。

  （4）一个新的变量只能从主内存中“诞生”，不允许在工作内存中直接使用一个未被初始化（load或assign）的变量，换句话说就是对一个变量实施use和store操作之前，必须先执行过了assign和load操作。

  （5）一个变量在同一个时刻只允许一条线程对其执行lock操作，但lock操作可以被同一个条线程重复执行多次，多次执行lock后，只有执行相同次数的unlock操作，变量才会被解锁。

  （6）如果对一个变量执行lock操作，将会清空工作内存中此变量的值，在执行引擎使用这个变量前，需要重新执行load或assign操作初始化变量的值。

  （7）如果一个变量实现没有被lock操作锁定，则不允许对它执行unlock操作，也不允许去unlock一个被其他线程锁定的变量。

  （8）对一个变量执行unlock操作之前，必须先把此变量同步回主内存（执行store和write操作）。



### 3、CPU结构

#### （1）CPU多级缓存

计算机在执行程序时，每条指令都是在CPU中执行的。而执行指令的过程中，势必涉及到数据的读取和写入。由于程序运行过程中的临时数据是存放在主存（物理内存）当中的，这时就存在一个问题，由于CPU执行速度很快，而从内存读取数据和向内存写入数据的过程，跟CPU执行指令的速度比起来要慢的多（硬盘 < 内存 <缓存cache < CPU）。因此如果任何时候对数据的操作都要通过和内存的交互来进行，会大大降低指令执行的速度。因此在CPU里面就有了高速缓存。也就是当程序在运行过程中，会将运算需要的数据从主存复制一份到CPU的高速缓存当中，那么CPU进行计算时，就可以直接从它的高速缓存中读取数据或向其写入数据了。当运算结束之后，再将高速缓存中的数据刷新到主存当中。

目前市面上的CPU基本都是有3级缓存的，任务管理器的性能中的L1、L2、L3缓存就是，3.00GHz代表CPU每秒可以执行30亿次。

* 为什么CPU需要三级缓存呢？

  主要是因为操作系统在处理任务时，最终是由CPU处理，但是数据是在硬盘或者内存中，内存每秒大概可执行1000W次。

  随着计算机的发展，CPU和内存之间的性能差异越来越大。

  CPU和内存的性能差距会导致系统的瓶颈被内存限制，所以随着发展CPU就开始需要缓存，开始时也只有1级缓存，发展到现在基本上CPU都是三级缓存了。

  ![1322310](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230604164559.png)

* **CPU缓存架构和JMM类比**

  ![image-20210219164910017](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230524221151.png)

  左边是CPU缓存架构图，右边是java线程架构图。

  我们会发现两个图的结构是一样的，这是因为在软件设计中其实很多都是参考硬件的架构设计的。

  上图中的其实是自上而下的关系：CPU --> cache --> 线程 --> 工作内存 --> 主内存

  两个CPU中的两个cache就存在可见性问题，比如在cache中将i修改，它需要让cache2知道，这其中有一个通知机制，即总线嗅探机制。在java中就是通过jmm实现。

  在CPU中每次修改了其中一个cache内的值后都会通知其他cache更新最新的值，但是不一定会刷到主内存。CPU在大部分情况下读取的都是缓存中的数据，这样才能保证性能。

#### （2）CPU缓存结构(cacheline)

* **cacheline(cache block)结构与原理**

  **cacheline是对cache切分的最小单位**，它的大小和操作系统、CPU有关，一般来说一个cacheline是64个字节。每次CPU读取cache时就会读取一个cacheline。之所以设计成每次读取一个cacheline而不是每次读取一个变量是因为一般来说附近的变量会和当前变量有些关联，减少读取次数提高性能。

* **缓存架构（cacheline与工作内存之间的对应关系）**

  1、Direct Mapped Cache：直接映射cache，比较早期的模型会使用此对应关系。下图中Address是一个32位的地址值，从2-11位用于计算index，12-31位用于计算tag。一个index就是一个cacheline，由于index的数量有限是1024个，当i和j两个变量计算出的index相同时，后一个值会将前一个值替代掉。tag就是当index相同的时候用于比较i和j是否同一个值，不同的话就将旧值替换成新值。

  ![v2-7682455a52c19fc023b3190bdff67e61_720w](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230525195758.jpg)

  

  2、Two-way Set Associative Cache：两路（多路）关联缓存，目前在硬件中使用最多的缓存结构。如果你了解过hashmap的存储原理，那么对于这种缓存架构一定不陌生，软件中的许多原理架构其实就是根据硬件参考设计的。hashmap中存储时首先是数组，hash值相同时，会发展成链表。在这种缓存架构中也是如此，下图中每一行叫做一个set，每一列叫做way（路），当计算出的set值相同并且way0已有值时，就将它放入到way1中，形成一个类似链表数组的结构。

  ![image-20210225104121219](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230525203548.png)

  ![2019062318525615](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230525203554.png)

  

* **cpu、cache、内存交互的过程**

  ![image-20230525205620234](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230525205621.png)

  我们可以将cache的结构理解为类似上图中的结构，每一行都是一个cacheline，每一个格子中可以存放一个变量。

  CPU获取数据时先从缓存中获取，获取不到的话再由缓存从内存中获取后存储到缓存中再交给CPU。

  假设所有的缓存都被占满了，CPU要获取的值在缓存中没有，此时缓存就需要从内存中获取数据，并且根据替换策略来替换一整行的数据，也就是一个cacheline。并且这里还涉及到一个问题，就是内存中被修改过的值何时被写入到内存中。

  * cache写机制

    *缓存命中：*

    write through 直写：修改缓存后立马将数据写回主存。这种方法性能低，早期架构会采用这种方式，现在用的很少了。

    write back 回写：修改缓存后不立即写入主存，而是等到一定时候触发一次回写。虽然没有马上写回主存，但是每次修改缓存是会通过mesi协议通知其他缓存同步修改的。这种方式是目前很常见的一种机制，性能好。

    *缓存未命中：*

    write allocate ：缓存中没有值，直接修改主存，改完之后立马获取到缓存中。这个也是比较常见的。

  * cache替换策略

    1. LFU(least frequence used) 最不经常使用
    2. LRU(least recently used) 最近不常使用
    3. 随机替换

    还有一些其他的淘汰策略，不一一列举。不难发现这些淘汰策略和redis里的几乎一样，因为这些算法都是相通通用的。

    

* **cacheline的两个局部性约束**

  从内存里读取一个cacheline的时候需要考虑处理读取本身需要的这个数据之外，还需要读取那些数据。因为一个cacheline是有一定大小的，可以读取多个变量。那么在这些变量选择上就会依据时间局部性和空间局部性来读取。

  **时间局部性**：CPU在前一个时刻访问的数据在临近的后一些时刻再被访问的概率很大。所以CPU获取数据时如果缓存中没有，需要从内存中获取，获取时也会在缓存中存储一份，方便后续获取时直接从缓存取。

  **空间局部性**：CPU访问一个数据时很可能需要此数据附近的数据。

  ![image-20230525213558055](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230525213558.png)

  上图中创建了一个二维数组，通过两种赋值方式来说明cacheline的读取。

  第一个中a\[i][j]的方式，即获取的顺序是a\[0][0]、a\[0][1]、...、a\[0][n]，他们的内存地址是连续的，在CPU获取a\[0][0]时cache读取一个cacheline，那么cacheline中就会有a\[0][0]---a\[0][cacheline.size-1]个数据，这就意味着读取第二、第三个数据时无需再从内存里拿，缓存里已经有了。

  第二种方式读取的内存地址不连续，cacheline里还是读取的连续地址，相当于开始时每个cacheline里都只有第一个值有用，当程序执行了2048次要获取a\[0][1]时，最初的cacheline很大概率早就被淘汰了，因为cache中的容量是有限的。

  

* **伪共享问题**

  伪共享是一种cacheline中降低性能的问题。

  CPU在读取数据时，是以一个缓存行为单位读取的，假设这个缓存行中有两个long类型的变量a、b，当一个线程A读取a，并修改a，线程A在未写回缓存之前，另一个线程B读取了b，读取的这个b所在的缓存是无效的（前面说的缓存失效），本来是为了提高性能是使用的缓存，现在为了提高命中率，反而被拖慢了，这就是传说中的伪共享。

  ```java
  public class FalseShared {
  
      public static void main(String[] args) throws InterruptedException {
          final long start = System.currentTimeMillis();
          final Count count = new Count();
          final Thread t1 = new Thread(() -> {
              for (int i = 0; i < 100000000; i++) {
                  count.a++;
              }
          });
  
          final Thread t2 = new Thread(() -> {
              for (int i = 0; i < 100000000; i++) {
                  count.b++;
              }
          });
  
          t1.start();
          t2.start();
          t1.join();
          t2.join();
          // 3332 3562 3759
          System.out.println("item:" + (System.currentTimeMillis() - start));
      }
  }
  
  class Count {
      volatile long a;
      volatile long b;
  }
  ```

  以上代码中就出现了伪共享问题，因为ab两个变量肯定在一个cacheline里，两个线程在操作时几乎每次都需要操作主内存。

  * 解决伪共享问题

    1、人为干预。

    我们知道一个cacheline的大小是64字节，一个long类型占用8字节。这意味着我们可以手动将ab分成两个cacheline。在ab之间再插入7个变量即可。

    ```java
    class Count {
        volatile long a;
        public long p1,p2,p3,p4,p5,p6,p7;
        volatile long b;
    }
    ```

    插入这些变量将ab分成两个cacheline之后，程序的运行速度缩短到了400多ms，性能提高了8倍之多。

    这个手段不是jvm规范中的，在jdk8之前会用这种做法。这种人为干预的做法不能保证成功。

    2、注解自动判定

    jdk8：`@sun.misc.Contended`

    jdk11：`@jdk.internal.vm.annotation.Contended`

    运行时需要在vm options中配置`-XX:-RestrictContended`用于开启自动填充，`-XX: ContendPaddingWidth`可以自定义填充的宽度。

    ```java
    class Count {
        volatile long a;
        @jdk.internal.vm.annotation.Contended
        volatile long b;
    }
    ```

    这个注解会在程序运行时在两个变量之间做填充，从而使变量被分开在两个cacheline中。默认的填充大小是128字节，可以自定义填充的字节大小。这是一种典型的空间换时间的做法。

  * 伪共享问题容易出现吗？

    不容易，实际上多线程访问不同变量的情况并不多。并且我们通常创建出来的对象地址一般是不连续的，所以两个对象在一个cacheline的几率很小。

    TLAB(thread local allocate buffer)，是jvm的一种优化。当多个线程中多个对象需要被创建时，jvm会做出这个优化。通常情况下在堆里创建一个对象时需要一个指针，那么当多个线程里有多个对象要被创建时，他们对这个指针的竞争很大，影响性能。此时jvm做出优化，将区域分成多个小区域，每个小区域内负责一个线程的对象创建。每个线程所创建的对象就集中在每个区域内。此时他们的内存地址就是连续的。

  * 需要解决伪共享问题的实际应用

    1. concurrentHashmap

    2. thread

    3. netty

    4. disruptor框架中的RingBuffer类

#### （3）CPU缓存一致性协议(MESI协议)

MESI 协议是高速缓存一致性协议，是为了解决多 cpu 、并发环境下，多个 cpu 缓存不一致问题而提出的协议。



* **MESI状态**

  mesi的就是cacheline的四个状态的首字母缩写

  * Modified: 被修改状态，cpu拥有cacheline，并且做了修改，但是修改值还没有刷新到主内存；

  * Exclusive: 独占状态，cpu拥有cacheline，但是还没有做修改；

  * Shared: 分享状态，所有cpu的cache都对某一个cacheline拥有read，但是不能写；

  * Invalid: 失效状态，当前某个cacheline数据无效，也就相当于里面没有数据；

  **M，E 重要特性：在所有cpu的cache中，只有唯一的一个cacheline是M或者E状态**。独占状态的目的就是做修改，所以ME状态一般是紧跟着的。

  

* **MESI协议消息**

  多个cache之间就通过mesi协议发送消息，主要分为以下六个消息类型

  * read：就是请求数据，读取一个物理内存地址上的数据，把消息通过总线广播，然后等待回应。先通过cache获取数据，没有的话再从主存中获取

  * read response：包括read请求的数据，这个数据可能来源于其他cpu，也可能来源于主内存

  * invalidate：包含一个物理地址，告知其他cpu，把cache中的这个地址所对应的cacheline置为无效。一般是自己要修改值，所以通知其他cache将值置为无效

  * invalidate acknowledge：置无效之后的反馈信息

  * read invalidate：read+invalidate，先请求数据，读取，查到之后会修改此数据，所以需要立马通知其他cache置为无效

  * writeback：回写数据，回写到主内存，当状态为m时会发生

    

  https://www.scss.tcd.ie/Jeremy.Jones/VivioJS/caches/MESIHelp.htm

  以上网址是一个动态模拟cpu缓存数据传输的网站，可以帮助我们更好的理解mesi协议。不过网站中的动图演示经常会刷新主内存，这样性能是比较低的，属于以前的老CPU的早期mesi协议。现在的CPU缓存进行过优化，使用的大多是mesi协议的变种，很少再和主存交互，大多是多个cache之间进行数据交换。

  ![image-20230529202429037](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230604172032.png)

  

* **MESI状态切换**

  ![image-20210304135923940-1620560564728](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230529202739.png)

  上图中是一个简化的86MESI状态切换的示例。memory列有0号地址和8号地址，V代表有效，I代表失效。CPU Cache 0-3代表4个cacheline。

  0：初始化状态，cache中无数据，内存0和8有数据。

  1：CPU0获取0号地址数据，其他缓存中没有，所以从内存中获取。只是读取，无其他操作，0号cacheline变成shared状态。

  2：CPU3获取0号地址数据，0号缓存中有，直接从0号获取。只是读取，无其他操作，3号cacheline变成shared状态。

  3：CPU0将缓存置为失效，因为CPU0此时需要读取地址8的数据，模拟的cacheline只有一个位置，所以将原先的数据置为Invalid，将新数据置为shared。

  4：CPU2执行RMW操作，RMW是read+modify+write，这是缓存中最慢的消息类型。先读取0地址，可以从3号cache中获取。再将获取到的数据状态置为Exclusive独占状态，因为马上要发生修改。

  5：CPU2继续执行写操作，store就相当于正式的写数据。写完后cache2变为modified状态，由于地址0的值已经被修改，他还需要通知其他cache和主存将地址0置为Invalid。

  6：CPU1执行原子加操作，cache1中原来没有数据，先从cache2里获取到0地址的数据，再次进行修改，此时cache1中状态是modified，其他cache和主存中的地址0都置为Invalid。

  7：CPU1执行写回操作，因为cache1即将被替换成8地址的数据，0地址的数据没地放了，所以将0地址的数据写回主存，再从cache0中获取8地址的数据。

#### （4）StoreBuffer

StoreBuffer是针对于缓存之间通信的性能优化，**通过类似异步的方式提高了程序的性能，和消息队列类似**。但同时也带来了可见性问题和指令重排问题。

StoreBuffer存在于CPU和缓存之间，它的速度比缓存更快。**StoreBuffer是导致可见性问题和指令重排的根本原因**。

* **指令重排**

  ```java
      static int a,b,x,y = 0;
  
      public static void main(String[] args) {
  
          new Thread(() -> {
              a = 1;
              x = b;
          }).start();
  
          new Thread(() -> {
              b = 1;
              y = a;
          }).start();
      }
  ```

  以上代码中最后x、y等于0或1都有可能，因为这段代码中会发生指令重排。

  我们通过CPU多级缓存机制来分析以上代码，可能出现的情况有两种：

  1、自己的cache里有a，直接执行invalid操作，将其他cache和主内存的a置为无效。

  2、自己的cache里没有a，只能去其他cache或者主内存中获取，那么就需要执行read invalidate操作，先获取到a，并且需要修改值，马上将其他缓存和内存中的a置为invalid。

  以上两种情况中都有将其它缓存和内存置为无效的操作，置为无效的操作之后紧接着就是invalidate acknowledge，返回ack操作。只有当接收到所有ack之后，当前CPU才会将a的值正式改为1并写到cache中。

  如果出现有一个或者几个CPU没有返回ack，那么当前CPU就要等待，这样会降低性能。所以引入了StoreBuffer，当代码执行a=1时，由于它要等待ack，此时这个操作就会被放入StoreBuffer，先继续向下执行其他代码。当ack都收到之后，再执行a=1。类似异步了这行代码，所以出现了先执行x=b再执行a=1的情况。

  当CPU需要修改一个变量，重新将值写回自己的缓存时是要经过StoreBuffer的，同样的在读取缓存时也是要经过StoreBuffer的，这样才能确保获取到的数据是最新的。也就是说StoreBuffer是存在于CPU和cache之间的。

  * StoreForwarding

    上下两个指令有依赖关系的时候，不会有指令重排问题。这其实就是因为读取时也是要经过StoreBuffer的。

* **可见性问题**

  **可见性问题的本质就是队列的异步化。**

  ![image-20230530201816114](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230530201818.png)

  上图中方法m1和m2分别代表CPU1和CPU2控制的两个线程，同时运行。当线程1将b改为1之后，线程2会跳出循环，断言a==1，此处会出现false的情况，也就是说b已经被改了值了，但是在线程2中获取a的值时仍然未被改变。

  这个示例中执行a=1之后需要将a=1放入StoreBuffer等待ack，执行b=1时由于b本身就在cache1中，直接执行修改并置为独占状态和通知其他cache失效。此时线程2还在不断获取b，b失效之后就通过总线去cache1拿到b，跳出循环。assert(a==1)断言为false是原因就是因为线程2在获取a的值时，需要通过总线从cache1中获取，但是此时cache1中的a=1还在StoreBuffer里，还没有被刷到cache1中，所以获取到的值还是原先的a=0。

  **每个cache都有自己的StoreBuffer，无法被其他cache获取。**可以把StoreBuffer看作一个队列，这个队列只有自己cache才能访问。当其他cache来访问时，有可能队列中的消息还没处理完，所以获取到的还是原先的值。

#### （5）invalidate queue

CPU的缓存分为L1、L2、L3三级，L1速度最快，但空间最小。StoreBuffer比L1缓存更靠近CPU，速度更快，空间更小。那么就存在StoreBuffer容量被占满的情况，就像是一个队列消费来不及，生产效率大于消费效率，队列被占满。当StoreBuffer满了之后就需要等待，影响性能。invalidata queue可以大大缩短StoreBuffer里消息的等待时长。

* 解决的问题

  提升invalidata ack的性能。

  StoreBuffer被占满的原因主要是invalidate比较耗时，当CPU接收到invalid时有可能没空马上处理，所以会先将invalid消息放入invalidate queue失效队列中，放入之后马上回复ack。这样就大大提高了ack的速度，StoreBuffer中消息的处理速度会大幅度提升。

* 出现的新问题

  引入失效队列之后又会带来新的问题。假设CPU1要将cahce里的a=0置为失效，先放入队列中，紧接着CPU1又要获取a，此时a在cache里还没有被置为失效状态，所以CPU1获取到了原来的a=0，而不是最新的值。

  解决此问题的方法其实就是加入读屏障，在使用变量之前执行指令：`smp_rmb`，CPU遇到这个指令后会强制刷新失效队列中的消息，和StoreBuffer一样，不一定是马上将队列中的消息全部消费完，而是针对需要的指令做标记。每个厂商的CPU对于实现的细节会有一些区别。

#### （6）内存屏障

StoreBuffer在提升性能的同时也会造成上述的指令重排问题和可见性问题。CPU工程师给应用工程师提供了解决方案：内存屏障。

出现问题的原因其实就是异步，把指令放入异步队列中，所以**内存屏障的本质就是串行化**。

**写屏障：**在a=1和b=1中多加一行写屏障指令：`smp_wmb`，这个命令其实是给b=1看的。a=1进入StoreBuffer之后，执行b=1时发现上方有写屏障，那么此时不会直接执行b=1，而是把b=1也放入StoreBuffer中等待执行，这样就能确保b=1一定是在a=1执行完成之后才会在缓存中生效。

*读屏障在后面讲解invalidate queue时讲解*

其实在实际应用中，出现指令重排和可见性问题的情况很少，并且StoreBuffer对CPU的性能提升很大。所以CPU工程师没有改变原先StoreBuffer的架构，而是提供内存屏障的解决方案给软件工程师，让我们在有需要的时候就手动加上内存屏障。

* JMM的四种读写屏障

  由于物理世界中的CPU屏障指令和效果各不一样，为了实现跨平台的效果，针对读操作load和写操作store，Java在JMM内存模型里提出了针对这两个操作的四种组合来覆盖读写的所有情况，即：读读LoadLoad、读写LoadStore、写写StoreStore、写读StoreLoad。

  * LoadLoad屏障：对于这样的语句Load1; LoadLoad; Load2，在Load2及后续读取操作要读取的数据被访问前，保证Load1要读取的数据被读取完毕。
  * StoreStore屏障：对于这样的语句Store1; StoreStore; Store2，在Store2及后续写入操作执行前，保证Store1的写入操作对其它处理器可见。
  * LoadStore屏障：对于这样的语句Load1; LoadStore; Store2，在Store2及后续写入操作被刷出前，保证Load1要读取的数据被读取完毕。
  * StoreLoad屏障：对于这样的语句Store1; StoreLoad; Load2，在Load2及后续所有读取操作执行前，保证Store1的写入对所有处理器可见。它的开销是四种屏障中最大的，因为写操作在前，不允许延迟写，必须在写之后才能执行读。在大多数处理器的实现中，**这个屏障是个万能屏障，兼具其它三种内存屏障的功能**。



* **x86的特殊性**

  在java中的UnSafe类中有三个获取屏障的方法，loadFence，storeFence，fullFence。这三个分别代表读屏障、写屏障、全屏障。在两个指令之间加了unSafe.storeFence()之后会发现问题确实解决了，**但但但但但但但但但但但但但是！**实际上根本就不是因为调用了这三个方法所以解决了指令重排和可见性问题。而是因为反射，unSafe类需要通过反射获取，在反射的C++源码中是会加入lock前缀的，**volatile底层也是lock前缀**，这个指令会锁cacheline并且刷新StoreBuffer和cache。

  ![image-20230530215110722](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230604172638.png)

  UnSafe类中的三个方法确实是加内存屏障，但是在x86的CPU中，只有store-load屏障。storeFence()方法是Store-Store屏障，所以调用此方法根本起不到作用。通过jvm源码可以看到java遵循jmm实现了各个操作系统和CPU架构的方法，linux_x86架构下的storeFence方法是一个空操作，因为x86中所有操作都必须放入StoreBuffer，所以无需真正加StoreStore屏障。

  fullFence就是storeLoad屏障，这个在X86下是有的。源码中也是通过lock前缀来实现内存屏障。

  ![image-20230530215704172](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230530215705.png)

  x86中没有invalidate queue --- 无读屏障

  x86中所有写操作都必须到StoreBuffer --- 无写屏障

  x86中先读再写的情况不会被重排序 --- 无读写屏障

  x86中先写后读会发生重排序 --- 有写读屏障

  

### 4、volatile禁止指令重排规则

-对volatile变量的写入不能重排到写入之前的操作之前，从而保证别的线程能够看到最新的写入的值

-对volatile变量的读操作，不能排到后续的操作之后。

禁止重排并不是禁止所有的重排，只有volatile写入不能向前，读取不能向后。除了这两种以外，其他重排可以可以允许的。

volatile int b；

volatile int a；

a=1；

b=1；



列举：**normal store，normal load，volatile store，volatile load**

重点关注后面这个指令，volatile store  

normal store，**storestore** ，*volatile store*   

volatile store ，**storestore**，*volatile store* 

normal load，**loadstore**，*volatile store* ------------numa架构

volatile load，**loadstore**，*volatile store*------------numa架构

-----------------------------------------

第一个指令是volatile写指令， 都可以重排

volatile int a；

int b；

a=1；

x=b；



volatile store，normal load--------可以重排

volatile store，normal store--------可以重排

-------------------------------------

volatile int a；

volatile int b；

int c；



x=a；

y=b；

z=c；



*volatile load*，**loadload**，volatile load

*volatile load*，**loadload**，normal load

*volatile load*，**loadstore**，normal store

-----------------------------------------

normal store，volatile load-------------可以重排

normal load，volatile load-------------可以重排

------------

最特别的一个，x86唯一的一个

volatile store，**storeload** ，volatile load

----------------

还有4个，都可以重排序

nomal store，normal load

nomal load，normal load

nomal store，normal store

nomal load，normal store



一共16个组合，记忆规则：

1. 只要看第二个指令，是不是volatile写，如果是volatile写，那么一定需要加xxstore屏障，xx根据第一个指令来，如果是读（不管是normal读还是volatile读）那么xx=load，如果是写（不管是normal写还是volatile写），那么xx=store
2. 只要看第一个指令，是不是volatile读，如果是volatile读，那么一定需要加loadxx屏障，xx根据第二个指令来，如果是读（不管是normal读还是volatile读）那么xx=load，如果是写（不管是normal写还是volatile写），那么xx=store
3. 还有一个特殊的，volatile写后面接一个volatile读。这就是全屏障，一定会加storeload屏障
4. 其余都不需要屏障，可以重排序



### 5、总结

早期的mesi协议锁总线，效率低下（强一致性）----》cpu工程师不能忍受，于是加入了storebuffer（好处：提高了访问效率，带来的缺陷是：1、可见性，2、容量问题）相对弱一点----》引入invalid queue(效率再次提升，带来的影响：一致性更加弱了)-----》请软件工程师自己解决（volatile）