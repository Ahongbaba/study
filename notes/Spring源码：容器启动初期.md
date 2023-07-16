# Spring源码：容器启动初期

## 一、前言

Spring可以简单理解为一个bean容器，它负责创建、管理、销毁bean。容器在创建和注入我们定义的bean之前会进行一系列的准备工作，包括初始化容器、从配置文件或者注解获取需要被加载的bean生成beanDefinition、初始化BFPP、初始化BPP等等。

本文章主要介绍Spring在执行finishBeanFactoryInitialization(beanFactory);方法**之前**所做的事，这个方法是实例化所有剩余的（非惰性初始化）单例，bean的生命周期也从此方法正式开始，但在这之前我们需要先了解Spring启动时先做了什么。

## 二、容器启动流程图

![1](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182454.png)

本文中主要讲解1-6流程，7流程将在后续bean生命周期的文章中详细讲解。

## 三、正式流程

#### 1、初始化bean读取器和扫描器

首先进入AnnotationConfigApplicationContext类的构造方法

```java
/**
    * Create a new AnnotationConfigApplicationContext, deriving bean definitions
    * from the given component classes and automatically refreshing the context.
    * @param componentClasses one or more component classes &mdash; for example,
    * {@link Configuration @Configuration} classes
    * 创建一个新的 AnnotationConfigApplicationContext，从给定的组件类派生 bean 定义并自动刷新上下文。
    * @param componentClasses 一个或多个组件类——例如，{@link Configuration @Configuration} 类
*/
public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
   // 1. 初始化bean读取器和扫描器;
   // 调用父类GenericApplicationContext无参构造函数，初始化一个BeanFactory:
   // DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory()
   this();
   // 2.注册bean配置类
   register(componentClasses);
   // 3.刷新上下文
   refresh();
}
```

进行的第一步就是this()，进入此方法

```java
public AnnotationConfigApplicationContext() {
   StartupStep createAnnotatedBeanDefReader = this.getApplicationStartup().start("spring.context.annotated-bean-reader.create");
   // 在IOC容器中初始化一个 注解bean读取器AnnotatedBeanDefinitionReader
   this.reader = new AnnotatedBeanDefinitionReader(this);
   createAnnotatedBeanDefReader.end();
   // 在IOC容器中初始化一个 按类路径扫描注解bean的 扫描器
   this.scanner = new ClassPathBeanDefinitionScanner(this);
}
```

此方法内就执行了两步操作，一个是生成读取注解的注解读取器，另一个是生成扫描器。

#### 2、注册bean配置类

register()方法，进入后可以看到是循环注册bean

```java
public void register(Class<?>... componentClasses) {
   for (Class<?> componentClass : componentClasses) {
      registerBean(componentClass);
   }
}
```

继续进入到registerBean方法里的doRegisterBean方法

```java
private <T> void doRegisterBean(Class<T> beanClass, @Nullable String name,
      @Nullable Class<? extends Annotation>[] qualifiers, @Nullable Supplier<T> supplier,
      @Nullable BeanDefinitionCustomizer[] customizers) {
   // 将Bean配置类信息转成容器中AnnotatedGenericBeanDefinition数据结构, AnnotatedGenericBeanDefinition继承自
   // BeanDefinition作用是定义一个bean的数据结构，下面的getMetadata可以获取到该bean上的注解信息
   AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);
   if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
      return;
   }
   // 设置回调
   abd.setInstanceSupplier(supplier);
   // 解析bean作用域(单例或者原型)，如果有@Scope注解，则解析@Scope，没有则默认为singleton
   ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
   // 作用域写回BeanDefinition数据结构, abd中缺损的情况下为空，将默认值singleton重新赋值到abd
   abd.setScope(scopeMetadata.getScopeName());
   // 生成bean配置类beanName
   String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));
   // 通用注解解析到abd结构中，主要是处理Lazy, primary DependsOn, Role ,Description这五个注解
   AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);
   // @Qualifier特殊限定符处理
   if (qualifiers != null) {
      for (Class<? extends Annotation> qualifier : qualifiers) {
         if (Primary.class == qualifier) {
            // 如果配置@Primary注解，则设置当前Bean为自动装配autowire时首选bean
            abd.setPrimary(true);
         }
         else if (Lazy.class == qualifier) {
            // 设置当前bean为延迟加载
            abd.setLazyInit(true);
         }
         else {
            // 其他注解，则添加到abd结构中
            abd.addQualifier(new AutowireCandidateQualifier(qualifier));
         }
      }
   }
   // 自定义bean注册，通常用在applicationContext创建后，手动向容器中以lambda表达式的方式注册bean,
   // 比如：applicationContext.registerBean(UserService.class, () -> new UserService());
   if (customizers != null) {
      for (BeanDefinitionCustomizer customizer : customizers) {
         // 自定义bean添加到BeanDefinition
         customizer.customize(abd);
      }
   }
   // 根据beanName和bean定义信息封装一个beanHold,beanHold其实就是一个 beanName和BeanDefinition的映射
   BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
   // 创建代理对象
   definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
   // BeanDefinitionReaderUtils.registerBeanDefinition 内部通过
   // DefaultListableBeanFactory.registerBeanDefinition(String beanName, BeanDefinition beanDefinition)按名称将bean定义信息注册到容器中，
   // 实际上DefaultListableBeanFactory内部维护一个Map<String, BeanDefinition>类型变量beanDefinitionMap，
   // 用于保存注bean定义信息（beanName 和 beanDefinition映射）
   BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
}
```

register方法重点完成了bean配置类本身的解析和注册，处理过程可以分为以下几个步骤：

1. 根据bean配置类，使用BeanDefinition解析Bean的定义信息，主要是一些注解信息
2. Bean作用域的处理，默认缺少@Scope注解，解析成单例
3. 借助AnnotationConfigUtils工具类解析通用注解
4. 将bean定义信息已beanname，beandifine键值对的形式注册到ioc容器中

**BeanDefinition是bean的定义信息，spring先获取到所有BeanDefintion，再根据它来创建bean，这个概念比较重要需要记住。**

#### 3、根据配置文件初始化

进入refresh()方法，此方法是非常重要的方法，我们分成几个部分来看。

```java
@Override
public void refresh() throws BeansException, IllegalStateException {
   synchronized (this.startupShutdownMonitor) {
      StartupStep contextRefresh = this.applicationStartup.start("spring.context.refresh");
      // Prepare this context for refreshing. 准备工作 初始化spring状态 使spring处于运行状态
      // 1.刷新上下文前的预处理
      prepareRefresh();
      // Tell the subclass to refresh the internal bean factory.
      // 2.获取刷新后的内部Bean工厂
      // 这一步主要作用是将配置文件定义解析成beanDefine 注册到beanFactory中，但是这里的bean并没有初始化出来
      // 只是提取配置信息 定义相关的属性，将这些信息保存到beanDefineMap中（beanName->beanDefine）对应的map
      ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
      // Prepare the bean factory for use in this context. beanFactory的准备工作，对各种属性进行填充
      // 3.BeanFactory的预准备工作
      prepareBeanFactory(beanFactory);
      try {
         // Allows post-processing of the bean factory in context subclasses.
         // 允许在上下文子类中对 bean 工厂进行后处理。
         // 4.空方法，用于在容器的子类中扩展
         postProcessBeanFactory(beanFactory);
         StartupStep beanPostProcess = this.applicationStartup.start("spring.context.beans.post-process");
		// ==============================================================================================
   }
}
```

###### 1.刷新上下文前的预处理

prepareRefresh()此方法做了一些开始刷新前的准备工作，例如：记录容器的启动时间、设置启动标识、检验属性是否合法等。

###### 2.获取刷新后的内部Bean工厂

obtainFreshBeanFactory()此方法主要是创建beanFactory，也就是容器。

```java
protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
   // 创建beanFactory、指定序列化Id、定制beanFactory、加载bean定义
   refreshBeanFactory();
   return getBeanFactory();
}
```

这里的refreshBeanFactory();方法有两个子类实现，如果是以xml方式配置bean，会使用AbstractRefreshableApplicationContext容器中的实现，该容器中实现xml配置文件定位，并通过BeanDefinition载入和解析xml配置文件。而如果是注解的方式，则并没有解析项目包下的注解，而是通过在refresh()方法中执行ConfigurationClassPostProcessor后置处理器完成对bean的加载。

###### 3.BeanFactory的预准备工作

prepareBeanFactory(beanFactory);主要完成beanFactory的一些属性设置，例如：设置类加载器、注册组件等。

###### 4.空方法，用于在容器的子类中扩展

postProcessBeanFactory(beanFactory);此方法内部是空的，用于给用户重写扩展的。

#### 4、BeanFactoryPostProcessor（重要）

```java
@Override
public void refresh() throws BeansException, IllegalStateException {
   synchronized (this.startupShutdownMonitor) {
	// ==============================================================================================
       // 5. 执行BeanFactoryPostProcessor的方法，BeanFactory的后置处理器，在BeanFactory标准初始化之后执行的
       invokeBeanFactoryPostProcessors(beanFactory);
    // ==============================================================================================
   }
}
```

**BeanFactoryPostProcessor 是针对 BeanFactory 的扩展，主要用在 bean 实例化之前，读取 bean 的定义，并可以修改它。**例如我们配置数据源时，需要配置username，我们经常使用${username}占位符的方式来实现，其实此处的占位符就是通过实现BeanFactoryPostProcessor的子类完成的功能，原本BeanDefinition里的属性值还是${username}，在经过BFPP后就完成了替换。

本方法会实例化和调用所有 BeanFactoryPostProcessor（包括其子类 BeanDefinitionRegistryPostProcessor）。

BeanDefinitionRegistryPostProcessor 继承自 BeanFactoryPostProcessor，比 BeanFactoryPostProcesso具有更高的优先级，主要用来在常规的 BeanFactoryPostProcessor 检测开始之前注册其他 bean 定义。特别是，你可以通过BeanDefinitionRegistryPostProcessor 来注册一些常规的 BeanFactoryPostProcessor，因为此时所有常规的 BeanFactoryPostProcessor 都还没开始被处理。

invokeBeanFactoryPostProcessors方法内主要做的事：先根据优先级加载BeanFactoryPostProcessor，再根据优先级执行BeanFactoryPostProcessor 。

以下是invokeBeanFactoryPostProcessors的具体代码，每个方法都加上了注释，有兴趣可以阅读一下。

```java
public static void invokeBeanFactoryPostProcessors(
      ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

   // Invoke BeanDefinitionRegistryPostProcessors first, if any.
   Set<String> processedBeans = new HashSet<>();

   // 1.判断beanFactory是否为BeanDefinitionRegistry，beanFactory为DefaultListableBeanFactory,
   // 而DefaultListableBeanFactory实现了BeanDefinitionRegistry接口，因此这边为true
   if (beanFactory instanceof BeanDefinitionRegistry) {
      BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
      List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
      List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

      // 2.首先处理入参中的beanFactoryPostProcessors
      // 遍历所有的beanFactoryPostProcessors, 将BeanDefinitionRegistryPostProcessor和普通BeanFactoryPostProcessor区分开
      for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
         if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
            // 2.1 如果是BeanDefinitionRegistryPostProcessor
            BeanDefinitionRegistryPostProcessor registryProcessor =
                  (BeanDefinitionRegistryPostProcessor) postProcessor;
            // 2.1.1 直接执行BeanDefinitionRegistryPostProcessor接口的postProcessBeanDefinitionRegistry方法
            registryProcessor.postProcessBeanDefinitionRegistry(registry);
            // 2.1.2 添加到registryProcessors(用于最后执行postProcessBeanFactory方法)
            registryProcessors.add(registryProcessor);
         }
         else {
            // 2.2 否则，只是普通的BeanFactoryPostProcessor
            // 2.2.1 添加到regularPostProcessors(用于最后执行postProcessBeanFactory方法)
            regularPostProcessors.add(postProcessor);
         }
      }

      // Do not initialize FactoryBeans here: We need to leave all regular beans
      // uninitialized to let the bean factory post-processors apply to them!
      // Separate between BeanDefinitionRegistryPostProcessors that implement
      // PriorityOrdered, Ordered, and the rest.
      // 用于保存本次要执行的BeanDefinitionRegistryPostProcessor
      List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

      // First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
      // 3.调用所有实现PriorityOrdered接口的BeanDefinitionRegistryPostProcessor实现类
      // 3.1 找出所有实现BeanDefinitionRegistryPostProcessor接口的Bean的beanName
      String[] postProcessorNames =
            beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
      // 3.2 遍历postProcessorNames
      for (String ppName : postProcessorNames) {
         // 3.3 校验是否实现了PriorityOrdered接口
         if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
            // 3.4 获取ppName对应的bean实例, 添加到currentRegistryProcessors中,
            // beanFactory.getBean: 这边getBean方法会触发创建ppName对应的bean对象, 目前暂不深入解析
            currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
            // 3.5 将要被执行的加入processedBeans，避免后续重复执行
            processedBeans.add(ppName);
         }
      }
      // 3.6 进行排序(根据是否实现PriorityOrdered、Ordered接口和order值来排序)
      sortPostProcessors(currentRegistryProcessors, beanFactory);
      // 3.7 添加到registryProcessors(用于最后执行postProcessBeanFactory方法)
      registryProcessors.addAll(currentRegistryProcessors);
      // 3.8 遍历currentRegistryProcessors, 执行postProcessBeanDefinitionRegistry方法
      invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
      // 3.9 执行完毕后, 清空currentRegistryProcessors
      currentRegistryProcessors.clear();

      // Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
      // 4.调用所有实现了Ordered接口的BeanDefinitionRegistryPostProcessor实现类（过程跟上面的步骤3基本一样）
      // 4.1 找出所有实现BeanDefinitionRegistryPostProcessor接口的类, 这边重复查找是因为执行完上面的BeanDefinitionRegistryPostProcessor,
      // 可能会新增了其他的BeanDefinitionRegistryPostProcessor, 因此需要重新查找
      postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
      for (String ppName : postProcessorNames) {
         if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
            currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
            processedBeans.add(ppName);
         }
      }
      sortPostProcessors(currentRegistryProcessors, beanFactory);
      registryProcessors.addAll(currentRegistryProcessors);
      // 4.2 遍历currentRegistryProcessors, 执行postProcessBeanDefinitionRegistry方法
      invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
      currentRegistryProcessors.clear();

      // Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
      // 5.最后, 调用所有剩下的BeanDefinitionRegistryPostProcessors
      boolean reiterate = true;
      while (reiterate) {
         reiterate = false;
         // 5.1 找出所有实现BeanDefinitionRegistryPostProcessor接口的类
         postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
         for (String ppName : postProcessorNames) {
            // 5.2 跳过已经执行过的
            if (!processedBeans.contains(ppName)) {
               currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
               processedBeans.add(ppName);
               // 5.3 如果有BeanDefinitionRegistryPostProcessor被执行, 则有可能会产生新的BeanDefinitionRegistryPostProcessor,
               // 因此这边将reiterate赋值为true, 代表需要再循环查找一次
               reiterate = true;
            }
         }
         sortPostProcessors(currentRegistryProcessors, beanFactory);
         registryProcessors.addAll(currentRegistryProcessors);
         // 5.4 遍历currentRegistryProcessors, 执行postProcessBeanDefinitionRegistry方法
         invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
         currentRegistryProcessors.clear();
      }

      // Now, invoke the postProcessBeanFactory callback of all processors handled so far.
      // 6.调用所有BeanDefinitionRegistryPostProcessor的postProcessBeanFactory方法(BeanDefinitionRegistryPostProcessor继承自BeanFactoryPostProcessor)
      invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
      // 7.最后, 调用入参beanFactoryPostProcessors中的普通BeanFactoryPostProcessor的postProcessBeanFactory方法
      invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
   }

   else {
      // Invoke factory processors registered with the context instance.
      invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
   }

   // 到这里 , 入参beanFactoryPostProcessors和容器中的所有BeanDefinitionRegistryPostProcessor已经全部处理完毕,
   // 下面开始处理容器中的所有BeanFactoryPostProcessor

   // Do not initialize FactoryBeans here: We need to leave all regular beans
   // uninitialized to let the bean factory post-processors apply to them!
   // 8.找出所有实现BeanFactoryPostProcessor接口的类
   String[] postProcessorNames =
         beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

   // Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
   // Ordered, and the rest.
   // 用于存放实现了PriorityOrdered接口的BeanFactoryPostProcessor
   List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
   // 用于存放实现了Ordered接口的BeanFactoryPostProcessor的beanName
   List<String> orderedPostProcessorNames = new ArrayList<>();
   // 用于存放普通BeanFactoryPostProcessor的beanName
   List<String> nonOrderedPostProcessorNames = new ArrayList<>();
   // 8.1 遍历postProcessorNames, 将BeanFactoryPostProcessor按实现PriorityOrdered、实现Ordered接口、普通三种区分开
   for (String ppName : postProcessorNames) {
      // 8.2 跳过已经执行过的
      if (processedBeans.contains(ppName)) {
         // skip - already processed in first phase above
      }
      else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
         // 8.3 添加实现了PriorityOrdered接口的BeanFactoryPostProcessor
         priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
      }
      else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
         // 8.4 添加实现了Ordered接口的BeanFactoryPostProcessor的beanName
         orderedPostProcessorNames.add(ppName);
      }
      else {
         // 8.5 添加剩下的普通BeanFactoryPostProcessor的beanName
         nonOrderedPostProcessorNames.add(ppName);
      }
   }

   // First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
   // 9.调用所有实现PriorityOrdered接口的BeanFactoryPostProcessor
   // 9.1 对priorityOrderedPostProcessors排序
   sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
   // 9.2 遍历priorityOrderedPostProcessors, 执行postProcessBeanFactory方法
   invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

   // Next, invoke the BeanFactoryPostProcessors that implement Ordered.
   // 10.调用所有实现Ordered接口的BeanFactoryPostProcessor
   List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
   for (String postProcessorName : orderedPostProcessorNames) {
      // 10.1 获取postProcessorName对应的bean实例, 添加到orderedPostProcessors, 准备执行
      orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
   }
   // 10.2 对orderedPostProcessors排序
   sortPostProcessors(orderedPostProcessors, beanFactory);
   // 10.3 遍历orderedPostProcessors, 执行postProcessBeanFactory方法
   invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

   // Finally, invoke all other BeanFactoryPostProcessors.
   // 11.调用所有剩下的BeanFactoryPostProcessor
   List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
   for (String postProcessorName : nonOrderedPostProcessorNames) {
      // 11.1 获取postProcessorName对应的bean实例, 添加到nonOrderedPostProcessors, 准备执行
      nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
   }
   // 11.2 遍历nonOrderedPostProcessors, 执行postProcessBeanFactory方法
   invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

   // Clear cached merged bean definitions since the post-processors might have
   // modified the original metadata, e.g. replacing placeholders in values...
   // 12.清除元数据缓存（mergedBeanDefinitions、allBeanNamesByType、singletonBeanNamesByType），
   // 因为后处理器可能已经修改了原始元数据，例如， 替换值中的占位符...
   beanFactory.clearMetadataCache();
}
```

#### 5、BeanPostProcessor（重要）

```java
@Override
public void refresh() throws BeansException, IllegalStateException {
   synchronized (this.startupShutdownMonitor) {
	// ==============================================================================================
    	// 6. 注册BeanPostProcessor（Bean的后置处理器）,用于拦截bean创建过程
		registerBeanPostProcessors(beanFactory);
    // ==============================================================================================
   }
}
```

**BeanPostProcessor 是针对 bean 的扩展，主要用在 bean 实例化之后，执行初始化方法前后，允许开发者对 bean 实例进行修改。**初始化前后分别对应两个方法：postProcessBeforeInitialization和postProcessAfterInitialization，AOP的功能就在after方法中实现。

BeanPostProcessor 接口是 Spring 初始化 bean 时对外暴露的扩展点，Spring IoC 容器允许 BeanPostProcessor在容器初始化 bean 的前后，添加自己的逻辑处理。在 registerBeanPostProcessors 方法只是注册到 BeanFactory 中，具体调用是在 bean 初始化的时候。

具体的：在所有 bean 实例化时，执行初始化方法前会调用所有 BeanPostProcessor 的postProcessBeforeInitialization 方法，在执行初始化方法后会调用所有BeanPostProcessor 的 postProcessAfterInitialization 方法。

本方法会注册所有的 BeanPostProcessor，将所有实现了 BeanPostProcessor 接口的类加载到 BeanFactory 中。

registerBeanPostProcessors方法内主要做的事：和BFPP类似，根据优先级加载BeanPostProcessor。

以下是registerBeanPostProcessors的具体代码，每个方法都加上了注释，有兴趣可以阅读一下。

```java
public static void registerBeanPostProcessors(
      ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
   // 1.找出所有实现BeanPostProcessor接口的类
   String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

   // Register BeanPostProcessorChecker that logs an info message when
   // a bean is created during BeanPostProcessor instantiation, i.e. when
   // a bean is not eligible for getting processed by all BeanPostProcessors.
   // BeanPostProcessor的目标计数
   int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
   // 2.添加BeanPostProcessorChecker(主要用于记录信息)到beanFactory中
   beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

   // Separate between BeanPostProcessors that implement PriorityOrdered,
   // Ordered, and the rest.
   // 3.定义不同的变量用于区分: 实现PriorityOrdered接口的BeanPostProcessor、实现Ordered接口的BeanPostProcessor、普通BeanPostProcessor
   // 3.1 priorityOrderedPostProcessors: 用于存放实现PriorityOrdered接口的BeanPostProcessor
   List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
   // 3.2 internalPostProcessors: 用于存放Spring内部的BeanPostProcessor
   List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
   // 3.3 orderedPostProcessorNames: 用于存放实现Ordered接口的BeanPostProcessor的beanName
   List<String> orderedPostProcessorNames = new ArrayList<>();
   // 3.4 nonOrderedPostProcessorNames: 用于存放普通BeanPostProcessor的beanName
   List<String> nonOrderedPostProcessorNames = new ArrayList<>();
   // 4.遍历postProcessorNames, 将BeanPostProcessors按3.1 - 3.4定义的变量区分开
   for (String ppName : postProcessorNames) {
      if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
         // 4.1 如果ppName对应的Bean实例实现了PriorityOrdered接口, 则拿到ppName对应的Bean实例并添加到priorityOrderedPostProcessors
         BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
         priorityOrderedPostProcessors.add(pp);
         if (pp instanceof MergedBeanDefinitionPostProcessor) {
            // 4.2 如果ppName对应的Bean实例也实现了MergedBeanDefinitionPostProcessor接口,
            // 则将ppName对应的Bean实例添加到internalPostProcessors
            internalPostProcessors.add(pp);
         }
      }
      else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
         // 4.3 如果ppName对应的Bean实例没有实现PriorityOrdered接口, 但是实现了Ordered接口, 则将ppName添加到orderedPostProcessorNames
         orderedPostProcessorNames.add(ppName);
      }
      else {
         // 4.4 否则, 将ppName添加到nonOrderedPostProcessorNames
         nonOrderedPostProcessorNames.add(ppName);
      }
   }

   // First, register the BeanPostProcessors that implement PriorityOrdered.
   // 5.首先, 注册实现PriorityOrdered接口的BeanPostProcessors
   // 5.1 对priorityOrderedPostProcessors进行排序
   sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
   // 5.2 注册priorityOrderedPostProcessors
   registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

   // Next, register the BeanPostProcessors that implement Ordered.
   // 6.接下来, 注册实现Ordered接口的BeanPostProcessors
   List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
   for (String ppName : orderedPostProcessorNames) {
      // 6.1 拿到ppName对应的BeanPostProcessor实例对象
      BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
      // 6.2 将ppName对应的BeanPostProcessor实例对象添加到orderedPostProcessors, 准备执行注册
      orderedPostProcessors.add(pp);
      if (pp instanceof MergedBeanDefinitionPostProcessor) {
         // 6.3 如果ppName对应的Bean实例也实现了MergedBeanDefinitionPostProcessor接口,
         // 则将ppName对应的Bean实例添加到internalPostProcessors
         internalPostProcessors.add(pp);
      }
   }
   // 6.4 对orderedPostProcessors进行排序
   sortPostProcessors(orderedPostProcessors, beanFactory);
   // 6.5 注册orderedPostProcessors
   registerBeanPostProcessors(beanFactory, orderedPostProcessors);

   // Now, register all regular BeanPostProcessors.
   // 7.注册所有常规的BeanPostProcessors（过程与6类似）
   List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
   for (String ppName : nonOrderedPostProcessorNames) {
      BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
      nonOrderedPostProcessors.add(pp);
      if (pp instanceof MergedBeanDefinitionPostProcessor) {
         internalPostProcessors.add(pp);
      }
   }
   registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

   // Finally, re-register all internal BeanPostProcessors.
   // 8.最后, 重新注册所有内部BeanPostProcessors（相当于内部的BeanPostProcessor会被移到处理器链的末尾）
   // 8.1 对internalPostProcessors进行排序
   sortPostProcessors(internalPostProcessors, beanFactory);
   // 8.2注册internalPostProcessors
   registerBeanPostProcessors(beanFactory, internalPostProcessors);

   // Re-register post-processor for detecting inner beans as ApplicationListeners,
   // moving it to the end of the processor chain (for picking up proxies etc).
   // 9.重新注册ApplicationListenerDetector（跟8类似，主要是为了移动到处理器链的末尾）
   beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
}
```

#### 6、初始化各种组件、监听器

```java
@Override
public void refresh() throws BeansException, IllegalStateException {
   synchronized (this.startupShutdownMonitor) {
	// ==============================================================================================
       // Initialize message source for this context. 为此上下文初始化消息源。
       // 7. 初始化MessageSource组件（做国际化功能；消息绑定，消息解析）
       initMessageSource();
       // Initialize event multicaster for this context. 为此上下文初始化事件多播器。
       // 8. 初始化事件派发器
       initApplicationEventMulticaster();
       // Initialize other special beans in specific context subclasses.
       // 初始化特定上下文子类中的其他特殊 bean。 默认空实现
       // 9.空方法，可以用于子类实现在容器刷新时自定义逻辑
       onRefresh();
       // Check for listener beans and register them. 检查侦听器 bean 并注册它们。
       // 10. 注册时间监听器，将所有项目里面的ApplicationListener注册到容器中来
       registerListeners();
    // ==============================================================================================
   }
}
```

这几个方法就是进行一些初始化的操作，不是重点，了解即可。

#### 7、初始化剩下的实例

```java
@Override
public void refresh() throws BeansException, IllegalStateException {
   synchronized (this.startupShutdownMonitor) {
	// ==============================================================================================
       // Instantiate all remaining (non-lazy-init) singletons. 实例化所有剩余的（非惰性初始化）单例。
       // 重要 此方法实例化我们自己定义的非懒加载的bean
       // 11. 初始化所有剩下的单实例bean,单例bean在初始化容器时创建，原型bean在获取时（getbean）时创建
       finishBeanFactoryInitialization(beanFactory);
    // ==============================================================================================
   }
}
```

此方法正式实例化我们自己通过注解或者配置文件的bean，通常讲解bean生命周期也是从这里开始，非常重要，由于篇幅有限，将会在下一篇文章中详细讲解。

#### 8、容器创建完成

```java
@Override
public void refresh() throws BeansException, IllegalStateException {
   synchronized (this.startupShutdownMonitor) {
	// ==============================================================================================
       // Last step: publish corresponding event. 最后一步：发布相应的事件。
       // 12. 完成BeanFactory的初始化创建工作，IOC容器就创建完成；
       finishRefresh();
    // ==============================================================================================
   }
}
```

执行到这步容器就完全创建完毕了。不用深入了解，就是清空资源，改变状态等。

## 四、总结

以上基本分析了AnnotationConfigApplicationContext容器的初始化过程， Spring容器在启动过程中，会先保存所有注册进来的Bean的定义信息；Spring容器根据条件创建Bean实例，区分单例，还是原型，后置处理器等（后置处理器会在容器创建过程中通过getBean创建，并执行相应的逻辑）；Spring容器在创建bean实例后，会使用多种后置处理器来增加bean的功能，比如处理自动注入，AOP，异步，这种后置处理器机制也丰富了bean的功能。

下一章将进入finishBeanFactoryInitialization(beanFactory);方法，开始bean的生命周期分析，重点了解bean的实例化和初始化过程。