# Spring源码：解决循环依赖

## 一、前言

spring解决循环依赖问题可以说是一个经典问题了，有了解过的同学都能回答出spring是使用三级缓存来解决循环依赖问题的，但是光是回答这一句肯定是不够的，想要了解具体的实现方式则需要深入到spring的源码中。

要注意的是spring并不能解决所有的循环依赖问题，就像我们的代码不可能避免所有错误一样，它只能解决部分循环依赖问题，具体需要什么条件我们下面会具体分析。

## 二、解决循环依赖流程图

![1](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352.png)

## 三、前置问题

#### 1、什么是循环依赖？

根据上述流程图，我们创建两个对象，A和B，A中有个B类型的属性，B中有个A类型的属性。如果没有三级缓存解决就会形成一个环，我们可以把上图的map缓存去掉来看看。

![2](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-1.png)

可以看到图中形成了一个环，永远没法创建出完整的A或B对象，对象中的属性无法得到赋值，其实spring引入三级缓存就是把环打破，在最后B又去容器中获取A的时候就不会再去创建A，因为此时从缓存中可以取到A的半成品或者早期对象用于给B赋值。

我们再通过代码来看一下循环依赖到底是什么，通过代码演示就可以很清晰的了解了。

A类：

```java
public class A {

   private B b;

   public B getB() {
      return b;
   }

   public void setB(B b) {
      System.out.println("======================");
      System.out.println("A里的setB方法被执行");
      System.out.println("======================");
      this.b = b;
   }
}
```

B类：

```java
public class B {

   private A a;

   public A getA() {
      return a;
   }

   public void setA(A a) {
      System.out.println("======================");
      System.out.println("B里的setA方法被执行");
      System.out.println("======================");
      this.a = a;
   }
}
```

配置类：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

   <bean id="a" class="com.hong.pojo.A">
      <property name="b" ref="b"/>
   </bean>

   <bean id="b" class="com.hong.pojo.B">
      <property name="a" ref="a"/>
   </bean>
</beans>
```

最终执行的效果：

![3](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-2.png)

成功执行，说明循环依赖问题被解决。从这里其实也可以看出spring创建对象时是会调用setXXX方法的。

#### 2、怎样的循环依赖可以被解决？

我们前面还说过不是所有循环依赖都能拿解决，只能解决部分循环依赖，让我们来看看解决循环依赖所需的条件：

- ##### 出现循环依赖的Bean必须是单例Bean

  IOC容器默认的单例Singleton的场景，并且使用setter方法注入，是支持循环依赖的，不会报错。

  只有单例的bean会通过三级缓存提前暴露来解决循环依赖问题，而非单例的bean，每次从容器中获取都是一个新的对象，都会重新创建，所以非单例的bean是没有缓存的，不会将其放到三级缓存中。

  从getBean的源码中isPrototypeCurrentlyInCreation(beanName)也可以看出，此处就是检查是否为原型模式创建bean，如果是原型模式并且依赖循环则直接报错。

  ```java
  // 4.scope为prototype的循环依赖校验：如果beanName已经正在创建Bean实例中，而此时我们又要再一次创建beanName的实例，则代表出现了循环依赖，需要抛出异常。
  // 因为 Spring 只解决单例模式下得循环依赖，在原型模式下如果存在循环依赖则会抛出异常
  if (isPrototypeCurrentlyInCreation(beanName)) {
     throw new BeanCurrentlyInCreationException(beanName);
  }
  ```

- ##### 依赖注入的方式不能全是构造器注入

  如果A中注入B的方式是通过构造器，B中注入A的方式也是通过构造器，这个时候循环依赖是无法被解决的。同样也会报错！

## 四、正式流程

#### 1、三级缓存

三级缓存究竟是什么？在源码中其实就是三个map：

```java
	/** Cache of singleton objects: bean name to bean instance. */
	// 一级缓存 单例对象的缓存：bean 名称到 bean 实例
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

	/** Cache of singleton factories: bean name to ObjectFactory. */
	// 三级缓存 单例工厂的缓存：对象工厂的 bean 名称
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

	/** Cache of early singleton objects: bean name to bean instance. */
	// 二级缓存 早期单例对象的缓存：bean 名称到 bean 实例
	private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);
```

注意此处是按源码的排列顺序复制出来的，第二个是三级缓存，第三个是二级缓存。一二三级缓存其实是我们自己取的名字，对应的就是上述代码中的单例对象缓存、早期单例对象缓存、单例工厂缓存。

重点看三个map的泛型。

一级缓存：singletonObjects，value是一个Object，用来存储成品对象，即已经实例化并且属性赋值都完成的对象。

二级缓存：earlySingletonObjects，value也是Object，用来存储半成品对象，即已经实例化但属性赋值未完成的对象。例如A对象，b属性=null。

三级缓存：singletonFactories，value是ObjectFactory<?>，它存储的是一个lambda表达式，是一串代码，只有当调用内部的getObject时才会被执行。存放可以生成Bean的工厂。

#### 2、getBean

首先让我们回顾一下bean的生命周期，对象的生命周期是从getBean方法正式开始的，我们通过代码可以发现，一进入doGetBean方法就调用了getSingleton方法（省略无关代码）：

```java
protected <T> T doGetBean(
      String name, @Nullable Class<T> requiredType, @Nullable Object[] args, boolean typeCheckOnly)
      throws BeansException {
   // 1.解析beanName，主要是解析别名、去掉FactoryBean的前缀“&”
   String beanName = transformedBeanName(name);
   Object bean;

   // Eagerly check singleton cache for manually registered singletons. 急切地检查单例缓存以获取手动注册的单例。
   // 2.尝试从缓存中获取beanName对应的实例
   Object sharedInstance = getSingleton(beanName);
   if (sharedInstance != null && args == null) {
      // 3.如果beanName的实例存在于缓存中
      if (logger.isTraceEnabled()) {
         if (isSingletonCurrentlyInCreation(beanName)) {
            logger.trace("Returning eagerly cached instance of singleton bean '" + beanName +
                  "' that is not fully initialized yet - a consequence of a circular reference");
         }
         else {
            logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
         }
      }
      // 3.1 返回beanName对应的实例对象（主要用于FactoryBean的特殊处理，普通Bean会直接返回sharedInstance本身）
      bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
   }
	// ...................
   return (T) bean;
}
```

从这里我们可以知道获取三级缓存中的数据的时机是在刚进入getBean方法的时候，也就是说spring创建对象之前会先到三级缓存中找，然后再根据结果判断需不需要创建。

#### 3、getSingleton

阅读过之前文章《Spring源码：bean的生命周期》的同学应该还记得有个方法叫做getSingleton，这个方法就是解决循环依赖的关键方法，从流程图中也可以看出需要获取缓存时就是通过此方法。

```java
@Nullable
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
   /**
    * 重要，这段代码判断了一二三级缓存中是否存在bean，用于解决循环依赖问题
    * 一级缓存：singletonObjects   二级缓存：earlySingletonObjects 三级缓存：singletonFactories
    */
   // Quick check for existing instance without full singleton lock
   // 1.从单例对象缓存中获取beanName对应的单例对象
   Object singletonObject = this.singletonObjects.get(beanName);
   // 2.如果单例对象缓存中没有，并且该beanName对应的单例bean正在创建中
   if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
      singletonObject = this.earlySingletonObjects.get(beanName);
      if (singletonObject == null && allowEarlyReference) {
         synchronized (this.singletonObjects) {
            // 4.从早期单例对象缓存中获取单例对象（之所称成为早期单例对象，是因为earlySingletonObjects里
            // 的对象的都是通过提前曝光的ObjectFactory创建出来的，还未进行属性填充等操作）半成品对象
            // Consistent creation of early reference within full singleton lock
            singletonObject = this.singletonObjects.get(beanName);
            // 5.如果在早期单例对象缓存中也没有，并且允许创建早期单例对象引用
            if (singletonObject == null) {
               singletonObject = this.earlySingletonObjects.get(beanName);
               if (singletonObject == null) {
                  // 6.从单例工厂缓存中获取beanName的单例工厂
                  ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                  if (singletonFactory != null) {
                     // 7.如果存在单例对象工厂，则通过工厂创建一个单例对象
                     singletonObject = singletonFactory.getObject();
                     // 8.将通过单例对象工厂创建的单例对象，放到早期单例对象缓存中
                     this.earlySingletonObjects.put(beanName, singletonObject);
                     // 9.移除该beanName对应的单例对象工厂，因为该单例工厂已经创建了一个实例对象，并且放到earlySingletonObjects缓存了，
                     // 因此，后续获取beanName的单例对象，可以通过earlySingletonObjects缓存拿到，不需要在用到该单例工厂
                     this.singletonFactories.remove(beanName);
                  }
               }
            }
         }
      }
   }
   return singletonObject;
}
```

getSingleton里的代码需要仔细理解，其实很简单的逻辑，就是先找一级缓存，如果一级缓存找到了，则直接返回，如果没找到再找二级，二级没有再找三级，如果三级有，则调用getObject()方法，通过工厂创建一个单例对象，并且放到二级缓存中，此时由于三级缓存已经没用，所以将这个Bean从三级缓存中删除。

#### 4、整体流程debug

![1](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352.png)

看了上面两个重点方法之后我们需要跟着流程图来走一遍代码，在这过程中将会重复的调用getBean方法，推荐大家看完后也自己debug走一遍了解其中的原理。

这里的代码就是前面介绍什么是循环依赖的时候的代码 A类中有B属性，B类中有A属性

##### （1）创建A对象

首先在实例化所有剩余单例方法处打断点运行

![4](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-3.png)

进入此方法继续往下走，可以看到要进入getBean方法了，并且这时的beanName是a

![5](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-4.png)

进入后发现马上就调用了getSingleton方法，但是此处返回了null，因为是刚开始创建，缓存里肯定没有

![6](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-5.png)

继续在doGetBean方法往下走，会发现又出现了getSingleton方法，这个方法和上面的参数不同，传递了一个String类型和一个ObjectFactory<?>类型，此处传递了beanName和一个lambda表达式，从代码中可以看出lambda表达式中是执行了createBean，但要注意的是代码运行到这里并不会马上执行createBean，而是当调用getObject时才会执行

![7](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-6.png)

F7进入此方法，以下是此方法的源码加中文注释解释：

```java
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
   // 如果beanName为null，抛出异常
   Assert.notNull(beanName, "Bean name must not be null");
   // 使用单例对象的高速缓存Map作为锁，保证线程同步
   synchronized (this.singletonObjects) {
      // 从单例对象的高速缓存Map中获取beanName对应的单例对象
      Object singletonObject = this.singletonObjects.get(beanName);
      // 如果单例对象获取不到
      if (singletonObject == null) {
         // 如果当前在destorySingletons中
         if (this.singletonsCurrentlyInDestruction) {
            // 抛出不允许创建Bean异常：在工厂的单例销毁时不允许创建单例bean(请勿在destory方法中向BeanFactory请求Bean)
            throw new BeanCreationNotAllowedException(beanName,
                  "Singleton bean creation not allowed while singletons of this factory are in destruction " +
                  "(Do not request a bean from a BeanFactory in a destroy method implementation!)");
         }
         // 如果当前日志级别时调试
         if (logger.isDebugEnabled()) {
            // 打印调试级别日志：创建单例bean的共享实例
            logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
         }
         // 创建单例之前的回调,默认实现将单例注册为当前正在创建中
         beforeSingletonCreation(beanName);
         // 表示生成了新的单例对象的标记，默认为false，表示没有生成新的单例对象
         boolean newSingleton = false;
         // 有抑制异常记录标记,没有时为true,否则为false
         boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
         // 如果没有抑制异常记录
         if (recordSuppressedExceptions) {
            // 对抑制的异常列表进行实例化(LinkedHashSet)
            this.suppressedExceptions = new LinkedHashSet<>();
         }
         try {
            // 从单例工厂中获取对象
            singletonObject = singletonFactory.getObject();
            // 生成了新的单例对象的标记为true，表示生成了新的单例对象
            newSingleton = true;
         }
         // 捕捉非法状态异常
         catch (IllegalStateException ex) {
            // Has the singleton object implicitly appeared in the meantime ->
            // if yes, proceed with it since the exception indicates that state.
            // 同时，单例对象是否隐式出现 -> 如果是，请继续操作，因为异常表明该状态

            // 因为singletonFactory.getObject()的目的就是为将beanName的
            // 单例对象注册到单例对象的高速缓存Map中，忽略掉注册后抛出的非法状态异常，可以保证
            // beanFactory不会因为该bean注册后的后续处理而导致beanFactoury的生命周期结束

            // 默认情况下，sinagletoObjects是拿不到该beanName的，但Spring的作者考虑到自定义BeanFactory的
            // 情况，但不建议在singleFactory#getObject()的方法中就注册到singletonObjects中，因为spring
            // 后面已经帮你将singleObject注册到singleObjects了。

            // 尝试从 单例对象的高速缓存Map 中获取beanName的单例对象
            singletonObject = this.singletonObjects.get(beanName);
            if (singletonObject == null) {
               throw ex;
            }
         }
         // 捕捉Bean创建异常
         catch (BeanCreationException ex) {
            if (recordSuppressedExceptions) {
               for (Exception suppressedException : this.suppressedExceptions) {
                  ex.addRelatedCause(suppressedException);
               }
            }
            throw ex;
         }
         finally {
            if (recordSuppressedExceptions) {
               this.suppressedExceptions = null;
            }
            // 创建单例后的回调,默认实现将单例标记为不在创建中
            afterSingletonCreation(beanName);
         }
         // 生成了新的单例对象
         if (newSingleton) {
            // 将beanName和singletonObject的映射关系添加到该工厂的单例缓存中:
            addSingleton(beanName, singletonObject);
         }
      }
      return singletonObject;
   }
}
```

进入此方法后第一步仍然是从缓存中获取，这里的singletonObjects是一级缓存，可以看到获取的结果为null，这个缓存的size=6，这个是正常的，因为在加载我们定义的对象之前也会有spring自身需要的对象被加载存入缓存。

![8](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-7.png)

继续向下发现此处调用了getObject方法，也就说明要进行createBean操作了，F7进入此方法

![9](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-8.png)

![10](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-9.png)

进入到createBean方法中的doCreateBean，这两个类的源码在bean生命周期文章中都有体现了，就不重复粘帖了

##### （2）实例化A对象

经过createBeanInstence方法之后A对象就被实例化出来了，但是它的b属性还是null

![11](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-10.png)

![12](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-11.png)

继续向下可以看到addSingletonFactory这个方法

![13](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-12.png)

进入此方法，可以看到此处将A对象添加到singletonFactories三级缓存中了，并且加入的value是一个lambda表达式。这里还有一个操作时移除二级缓存中key为a的元素，因为三个缓存中只要一个里有就可以了，不要重复浪费资源。

![14](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-13.png)

从下面的debug参数图也可以看到目前只有三级缓存加了元素，一二级还是原来的。

![15](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-14.png)

##### （3）给A对象赋值

![16](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-15.png)

继续F7进入populateBean方法，这个方法前面都是做了一些判断，都是false，直接到最后一个赋值方法

![17](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-16.png)

进入此方法，可以看到就是遍历为解析的属性，做一些实际的赋值操作，重点就在resolveValueIfNecessary这个方法

![18](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-17.png)

继续F7进入到resolveReference方法

![19](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-18.png)

可以看到在又resolveReference方法中又调用了getBean方法

![20](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-19.png)

##### （4）从缓存中获取B

到此我们梳理一下目前流程到了什么时候了，一开始我们在创建A对象，然后实例化A对象，都是正常的，给A对象赋值的操作中spring遍历找到A对象中的B属性，然后进入到了解析B属性的方法，在此方法里又调用了getBean方法，这里的调用其实就是去容器中获取bean对象，同样的也是先getSingleton，从缓存中获取B，没有的话就创建B对象，和上面的流程一样。

![21](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-20.png)

##### （5）创建B对象

同A对象创建

##### （6）实例化B对象

同A对象实例化，当实例化B对象之后第三级缓存中也加入了b对象和它的工厂方法

![22](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-21.png)

##### （7）给B对象赋值

之前的流程都和A相同，进入到resolveReference方法，再次调用了getBean，不过这次的getBean是获取a，所以我们又会回到之前的代码，但是这次与之前都不同的是这次a已经在三级缓存中了，所以getSingleton将会有值

![23](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-22.png)

##### （8）从缓存中获取A

这次的getSingleton将会进入这个判断块中，进入之后调用了getObject方法生成了a的半成品对象，因为a里的b属性还没被赋值，所以这里的A对象是不完整的，同时将a对象加入到二级缓存，再三级缓存中的a移除，因为三级缓存里的a已经没用了。

![24](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-23.png)

![25](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-24.png)

##### （9）找到A，给B对象赋值

那么这次就已经找到了A，可以开始返回操作了。返回的流程其实就是倒着走一遍回去，看流程图更好理解，一开始我们是从创建A对象开始的，现在一路返回，将会先把B对象创建完毕，再把A对象创建完毕。

![26](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-25.png)

这次找A我们是为了给B对象赋值，那么找到了，就可以给B对象赋值了。

![27](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-26.png)

可以看到B对象目前的状态就是a属性已经被赋值上了，只不过这个a属性还不完整，但是不妨碍B对象已经是一个成品对象。

##### （10）更新缓存，返回B对象

目前B对象已经成功完成了实例化和赋值操作，后面的初始化过程和循环依赖无关，直接进行即可，然后开始返回B对象。从流程图可以看出我们创建B对象的目的就是给A对象里的B属性赋值，所以这里自然是返回给A的，但是这中间还要注意因为B的状态更新了，同样也需要更新到缓存中，返回之前会调用addSingleton方法更新缓存。

这里的B直接加入到了一级缓存中，因为它已经完全创建完毕了，同样也将其他两个缓存中的b移除。接下来就是返回B，就回到了我们给A对象赋值的时候了。

![28](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-27.png)

![29](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-28.png)

##### （11）返回B给A，回到A赋值

![30](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-29.png)

A既然已经获取到B属性了，也可以接着往下走进行初始化操作了。

##### （12）更新缓存，返回A对象

初始化之后，同前面的更新B对象缓存，也将A对象更新到一级缓存。

整个流程到此结束，最终效果：

![31](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182352-30.png)

不难看出AB对象其实是第一次循环就一起创建出来了，我们一开始进入流程时只是创建A对象，B对象应该是下一次循环的，那么这次循环结束后两个都创建出来了，下一次循环创建B时一样的进入到getBean方法中，一开始就调用getSingleton，从一级缓存中就可以直接获得到B返回了。

## 五、思考

#### 1、三级缓存的查找顺序？

先找一级，再二级，最后三级。

#### 2、一定要三级缓存才能解决循环依赖吗？二级缓存可以吗？一级缓存可以吗？

首先说一级缓存，答案肯定是不行的，通过上面的分析我们知道对象的状态有成品和半成品，如果只有一级缓存就不好在一个map里区分两种状态，半成品状态的对象是不能直接暴露给其他对象引用的。可以用标识符来区分，但是这样就更麻烦了，不如直接用两个map。

二级缓存可以解决部分情况下的循环依赖问题，比如我们文章中的案例，其实不用三级缓存也可以解决。

当所有的对象都是原始对象时，不使用三级缓存是没问题的，但是当有代理对象出现时，没有三级缓存就不行。AOP就是典型代表。

当需要创建代理对象的时候，原始对象和代理对象同时存在，那么对外暴露哪个？我们希望暴露的肯定是代理对象，但是程序是写死的，它不知道什么时候暴露原始对象，什么时候暴露代理对象。

三级缓存中map的value是ObjectFactory类型，可以传递一个lambda进去，对外暴露对象的时机是没法确定的，所以只有在对象被引用的时候才会去判断返回的是代理对象还是原始对象，所以通过lambda相当于一种回调机制，在刚开始时没有执行，在需要用的时候才执行。

## 六、总结

如果仔细看过以上文章应该不难发现，spirng解决循环依赖的时候就是重复调用了getBean方法，每次调用的不同点就在于getSingleton方法中的三级缓存里有没有数据。

第一次调用时都没数据，则会开始创建这个bean A，实例化后会将A和它生成对象的工厂方法放入到三级缓存中。

第二次调用是在给A里的B属性赋值时发生的，因为需要给B属性赋值也必须去容器中获取bean，同样从缓存中无法找到bean，实例化B，加入三级缓存。

第三次调用实在给B里的A属性赋值时，这次调用getBean里的getSingleton方法结果就不同了，因为三级缓存已经有了a的数据，这时就会调用工厂方法的getObject生成A对象，把A更新到二级缓存，赋值给B，并且在初始化后，B已经是成品对象，更新到一级缓存。

经过这三次之后B对象创建完毕，就可以返回B对象给A了，A继续赋值后的初始化等操作也就创建完毕了。

强烈推荐大家通过debug模式自己从头操作走一遍流程，会帮助你非常清晰的了解三级循环解决循环依赖的问题。