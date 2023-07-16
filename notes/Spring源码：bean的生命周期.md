# Spring源码：bean的生命周期

## 一、前言

Spring bean的生命周期可以说是Spring中非常核心的知识点，本文简要分析了 ApplicationContext中Bean的生命周期，重点解析几个核心方法，帮助您熟悉bean生命周期的整体流程。不深入每个方法，只分析大体流程。

## 二、生命周期流程图

![1](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182331.png)

本文将会以上图为基础开始讲解，重点在于实例化和赋值过程、初始化过程。

当我们自己创建对象时十分简单，只需要new即可。然而Spring为我们创建对象时就比较复杂，因为Spring作为一个框架，必须具备通用性、扩展性等重要特性，这也导致它对于bean的创建有很多逻辑判断和优先级等考虑。

Spring实现扩展性的两个非常重要的类：BeanFactoryPostProcessor)、BeanPostProcessor，简称BFPP和BPP，在《Spring源码：容器启动初期》文章中已经详细讲解BFPP的使用过程和BPP的注册过程，然而真正使用BPP就在初始化bean的前后。

## 三、正式流程

#### 1、容器启动初期

首先我们来回顾一下容器启动初期做了什么，容器启动流程图：

![2](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182331-1.png)

容器启动时主要完成了：1、根据注解或配置文件生成BeanDefintion对象；2、注册调用BFPP；3、注册BPP

总的来说就是一个容器的初始化过程，要注意的是在这时候其实就已经有bean完成了实例化和初始化操作了，只不过这时加载的bean一般是容器自带的，我们自己定义的bean一般需要在容器初始化之后进入上图第7点时加载。为了方便理解我们也从此开始分析bean的生命周期。

#### 2、开始加载bean

初始化所有剩余非惰性bean对应的方法是refresh()方法中的finishBeanFactoryInitialization(beanFactory);，这个方法是非常重要的一个方法，是spring的核心方法。

该方法会实例化所有剩余的非懒加载单例 bean。除了一些内部的 bean、实现了 BeanFactoryPostProcessor 接口的 bean、实现了 BeanPostProcessor 接口的 bean，其他的非懒加载单例 bean 都会在这个方法中被实例化，并且 BeanPostProcessor 的触发也是在这个方法中。

进入此方法：

```java
protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
   // Initialize conversion service for this context. 
   // 1.初始化此上下文的转换服务
   if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
         beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
      beanFactory.setConversionService(
            beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
   }

   // Register a default embedded value resolver if no bean post-processor 
   // (such as a PropertyPlaceholderConfigurer bean) registered any before: 
   // at this point, primarily for resolution in annotation attribute values. 
   // 2.如果beanFactory之前没有注册嵌入值解析器，则注册默认的嵌入值解析器：主要用于注解属性值的解析
   if (!beanFactory.hasEmbeddedValueResolver()) {
      beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
   }

   // Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early. 
   // 3.初始化LoadTimeWeaverAware Bean实例对象
   String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
   for (String weaverAwareName : weaverAwareNames) {
      getBean(weaverAwareName);
   }

   // Stop using the temporary ClassLoader for type matching. 
   beanFactory.setTempClassLoader(null);

   // Allow for caching all bean definition metadata, not expecting further changes. 
   // 4.冻结所有bean定义，注册的bean定义不会被修改或进一步后处理，因为马上要创建 Bean 实例对象了
   beanFactory.freezeConfiguration();

   // Instantiate all remaining (non-lazy-init) singletons. 
   // 5.实例化所有剩余（非懒加载）单例对象
   beanFactory.preInstantiateSingletons();
}
```

可以看到这个方法内的1-4是实例化bean之前的一些准备工作，第5点是真正开始实例化，接着进入此方法：

```java
@Override
public void preInstantiateSingletons() throws BeansException {
   if (logger.isTraceEnabled()) {
      logger.trace("Pre-instantiating singletons in " + this);
   }

   // Iterate over a copy to allow for init methods which in turn register new bean definitions. 
   // While this may not be part of the regular factory bootstrap, it does otherwise work fine. 
   // 1.创建beanDefinitionNames的副本beanNames用于后续的遍历，以允许init等方法注册新的bean定义
   List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

   // Trigger initialization of all non-lazy singleton beans... 
   // 2.遍历beanNames，触发所有非懒加载单例bean的初始化
   for (String beanName : beanNames) {
      // 3.获取beanName对应的MergedBeanDefinition
      RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
      // 4.bd对应的Bean实例：不是抽象类 && 是单例 && 不是懒加载
      if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
         // 5.判断beanName对应的bean是否为FactoryBean
         if (isFactoryBean(beanName)) {
            // 5.1 通过beanName获取FactoryBean实例
            // 通过getBean(&beanName)拿到的是FactoryBean本身；通过getBean(beanName)拿到的是FactoryBean创建的Bean实例
            Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
            if (bean instanceof FactoryBean) {
               FactoryBean<?> factory = (FactoryBean<?>) bean;
               // 5.2 判断这个FactoryBean是否希望急切的初始化
               boolean isEagerInit;
               if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
                  isEagerInit = AccessController.doPrivileged(
                        (PrivilegedAction<Boolean>) ((SmartFactoryBean<?>) factory)::isEagerInit,
                        getAccessControlContext());
               }
               else {
                  isEagerInit = (factory instanceof SmartFactoryBean &&
                        ((SmartFactoryBean<?>) factory).isEagerInit());
               }
               // 5.3 如果希望急切的初始化，则通过beanName获取bean实例
               if (isEagerInit) {
                  getBean(beanName);
               }
            }
         }
         else {
            // 6.如果beanName对应的bean不是FactoryBean，只是普通Bean，通过beanName获取bean实例
            getBean(beanName);
         }
      }
   }

   // Trigger post-initialization callback for all applicable beans...
   // 7.遍历beanNames，触发所有SmartInitializingSingleton的后初始化回调，这是 Spring 提供的一个扩展点，在所有非懒加载单例实例化结束后调用。
   for (String beanName : beanNames) {
      // 7.1 拿到beanName对应的bean实例
      Object singletonInstance = getSingleton(beanName);
      // 7.2 判断singletonInstance是否实现了SmartInitializingSingleton接口
      if (singletonInstance instanceof SmartInitializingSingleton) {
         StartupStep smartInitialize = this.getApplicationStartup().start("spring.beans.smart-initialize")
               .tag("beanName", beanName);
         SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
         // 7.3 触发SmartInitializingSingleton实现类的afterSingletonsInstantiated方法
         if (System.getSecurityManager() != null) {
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
               smartSingleton.afterSingletonsInstantiated();
               return null;
            }, getAccessControlContext());
         }
         else {
            smartSingleton.afterSingletonsInstantiated();
         }
         smartInitialize.end();
      }
   }
}
```

下面我们来分析一些此方法中比较重要的方法和概念：

##### MergedLocalBeanDefinition

可以进入第3点的 getMergedLocalBeanDefinition(beanName);方法，从名字中就可以得知这个方法是获取bean的定义信息，但是这里还有个merged，合并的意思，其实就是合并父子类定义

```java
protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
   // Quick check on the concurrent map first, with minimal locking. 首先快速检查并发映射，锁定最少。
   // 1.检查beanName对应的MergedBeanDefinition是否存在于缓存中
   RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
   if (mbd != null && !mbd.stale) {
      // 2.如果存在于缓存中则直接返回
      return mbd;
   }
   // 3.如果不存在于缓存中
   // 3.1 getBeanDefinition(beanName)： 获取beanName对应的BeanDefinition，从beanDefinitionMap缓存中获取
   // 3.2 getMergedBeanDefinition: 根据beanName和对应的BeanDefinition，获取MergedBeanDefinition
   return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
}
```

在Spring的生命周期中， 如果要实例化一个bean则需要先将bean进行合并，这样拿到的BeanDefinition才是信息完整的。

对于BeanDefinition的合并，Spring都会创建一个新的RootBeanDefinition来进行接收, 而不是用原来的BeanDefinition，如果原始BeanDefinition没有父BeanDefinition了，那么就直接创建一个RootBeanDefinition，并将原始BeanDefinition作为参数传入构造方法中， 如果原始BeanDefinition存在BeanDefinition，Spring除了会做上述的操作外，还会调用overrideFrom方法进行深入的合并, 其实就是一系列的setXXX方法的调用而已,在合并完成后，对于合并后的BeanDefinition如果没有作用域， 则设置为单例，并且将合并的BeanDefinition放入到mergedBeanDefinitions这个map中缓存起来。

此处的1、2点就是从缓存map中获取，如果没有则进入3，真正去创建获取mbd并放入缓存，这个方法内其实就是将子类和父类的定义信息合并在一起，这里面会涉及到一些递归操作，因为父类可能还有父类。

##### isFactoryBean

FactoryBena：一般情况下，Spring 通过反射机制利用 bean 的  class 属性指定实现类来实例化 bean。而 FactoryBean 是一种特殊的 bean，它是个工厂 bean，可以自己创建 bean 实例，如果一个类实现了 FactoryBean 接口，则该类可以自己定义创建实例对象的方法，只需要实现它的 getObject() 方法。很多中间件都利用FactoryBean进行扩展。

进入第5点的isFactoryBean(beanName)方法中：

```java
@Override
public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
   // 1.拿到真正的beanName（去掉&前缀、解析别名）
   String beanName = transformedBeanName(name);
   // 2.尝试从缓存获取Bean实例对象
   Object beanInstance = getSingleton(beanName, false);
   if (beanInstance != null) {
      // 3.beanInstance存在，则直接判断类型是否为FactoryBean
      return (beanInstance instanceof FactoryBean);
   }
   // No singleton instance found -> check bean definition.
   if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
      // No bean definition found in this factory -> delegate to parent.
      // 5.如果缓存中不存在此beanName && 父beanFactory是ConfigurableBeanFactory，则调用父BeanFactory判断是否为FactoryBean
      return ((ConfigurableBeanFactory) getParentBeanFactory()).isFactoryBean(name);
   }
   // 6.通过MergedBeanDefinition来检查beanName对应的Bean是否为FactoryBean
   return isFactoryBean(beanName, getMergedLocalBeanDefinition(beanName));
}
```

可以看到此处就是通过beanName去缓存中尝试获取bean对象，使用的方法是getSingleton（此处先了解）：

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

这个方法内的多层判断就是三级缓存解决循环依赖的方案，这是spring非常重要的一块知识点，后续将有完整的一篇文章进行具体分析，此处先了解一下即可。

#### 3、getBean() 重要

从上面的preInstantiateSingletons()源码中可以发现很多地方都使用了getBean()这个方法，它是spring中最核心的方法，它的作用就是获取bean实例，如果没有则会创建。

```java
@Override
public Object getBean(String name) throws BeansException {
   // 获取name对应的bean实例，如果不存在，则创建一个
   return doGetBean(name, null, null, false);
}
```

看到这里其实大家可以发现spring源码中以do为前缀的往往是真正做事的方法，没有do的经常有一些准备工作。继续进入doGetBean：

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

   else {
      // Fail if we're already creating this bean instance:
      // We're assumably within a circular reference.
      // 4.scope为prototype的循环依赖校验：如果beanName已经正在创建Bean实例中，而此时我们又要再一次创建beanName的实例，则代表出现了循环依赖，需要抛出异常。
      // 因为 Spring 只解决单例模式下得循环依赖，在原型模式下如果存在循环依赖则会抛出异常
      if (isPrototypeCurrentlyInCreation(beanName)) {
         throw new BeanCurrentlyInCreationException(beanName);
      }

      // Check if bean definition exists in this factory.
      // 5.获取parentBeanFactory
      BeanFactory parentBeanFactory = getParentBeanFactory();
      // 5.1 如果parentBeanFactory存在，并且beanName在当前BeanFactory不存在Bean定义，则尝试从parentBeanFactory中获取bean实例
      if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
         // Not found -> check parent.
         // 5.2 将别名解析成真正的beanName
         String nameToLookup = originalBeanName(name);
         // 5.3 尝试在parentBeanFactory中获取bean对象实例
         if (parentBeanFactory instanceof AbstractBeanFactory) {
            return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
                  nameToLookup, requiredType, args, typeCheckOnly);
         }
         else if (args != null) {
            // Delegation to parent with explicit args.
            return (T) parentBeanFactory.getBean(nameToLookup, args);
         }
         else if (requiredType != null) {
            // No args -> delegate to standard getBean method.
            return parentBeanFactory.getBean(nameToLookup, requiredType);
         }
         else {
            return (T) parentBeanFactory.getBean(nameToLookup);
         }
      }

      if (!typeCheckOnly) {
         // 6.如果不是仅仅做类型检测，而是创建bean实例，这里要将beanName放到alreadyCreated缓存
         markBeanAsCreated(beanName);
      }

      StartupStep beanCreation = this.applicationStartup.start("spring.beans.instantiate")
            .tag("beanName", name);
      try {
         if (requiredType != null) {
            beanCreation.tag("beanType", requiredType::toString);
         }
         // 7.根据beanName重新获取MergedBeanDefinition（步骤6将MergedBeanDefinition删除了，这边获取一个新的）
         RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
         // 7.1 检查MergedBeanDefinition
         checkMergedBeanDefinition(mbd, beanName, args);

         // Guarantee initialization of beans that the current bean depends on.
         // 8.拿到当前bean依赖的bean名称集合，在实例化自己之前，需要先实例化自己依赖的bean
         String[] dependsOn = mbd.getDependsOn();
         if (dependsOn != null) {
            // 8.1 遍历当前bean依赖的bean名称集合
            for (String dep : dependsOn) {
               // 8.2 检查dep是否依赖于beanName，即检查是否存在循环依赖
               if (isDependent(beanName, dep)) {
                  // 8.3 如果是循环依赖则抛异常
                  throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                        "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
               }
               // 8.4 将dep和beanName的依赖关系注册到缓存中
               registerDependentBean(dep, beanName);
               try {
                  // 8.5 获取dep对应的bean实例，如果dep还没有创建bean实例，则创建dep的bean实例
                  getBean(dep);
               }
               catch (NoSuchBeanDefinitionException ex) {
                  throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                        "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
               }
            }
         }

         // Create bean instance.
         // 9.针对不同的scope进行bean的创建
         if (mbd.isSingleton()) {
            // 9.1 scope为singleton的bean创建（新建了一个ObjectFactory，并且重写了getObject方法
            sharedInstance = getSingleton(beanName, () -> {
               try {
                  return createBean(beanName, mbd, args);
               }
               catch (BeansException ex) {
                  // Explicitly remove instance from singleton cache: It might have been put there
                  // eagerly by the creation process, to allow for circular reference resolution.
                  // Also remove any beans that received a temporary reference to the bean.
                  destroySingleton(beanName);
                  throw ex;
               }
            });
            // 9.1.2 返回beanName对应的实例对象
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
         }

         else if (mbd.isPrototype()) {
            // It's a prototype -> create a new instance.
            // 9.2 scope为prototype的bean创建
            Object prototypeInstance = null;
            try {
               // 9.2.1 创建实例前的操作（将beanName保存到prototypesCurrentlyInCreation缓存中）
               beforePrototypeCreation(beanName);
               // 9.2.2 创建Bean实例
               prototypeInstance = createBean(beanName, mbd, args);
            }
            finally {
               // 9.2.3 创建实例后的操作（将创建完的beanName从prototypesCurrentlyInCreation缓存中移除）
               afterPrototypeCreation(beanName);
            }
            // 9.2.4 返回beanName对应的实例对象
            bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
         }

         else {
            // 9.3 其他scope的bean创建，可能是request之类的
            // 9.3.1 根据scopeName，从缓存拿到scope实例
            String scopeName = mbd.getScope();
            if (!StringUtils.hasLength(scopeName)) {
               throw new IllegalStateException("No scope name defined for bean ´" + beanName + "'");
            }
            Scope scope = this.scopes.get(scopeName);
            if (scope == null) {
               throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
            }
            try {
               // 9.3.2 其他scope的bean创建（新建了一个ObjectFactory，并且重写了getObject方法）
               Object scopedInstance = scope.get(beanName, () -> {
                  // 9.3.3 创建实例前的操作（将beanName保存到prototypesCurrentlyInCreation缓存中）
                  beforePrototypeCreation(beanName);
                  try {
                     // 9.3.4 创建bean实例
                     return createBean(beanName, mbd, args);
                  }
                  finally {
                     // 9.3.5 创建实例后的操作（将创建完的beanName从prototypesCurrentlyInCreation缓存中移除）
                     afterPrototypeCreation(beanName);
                  }
               });
               // 9.3.6 返回beanName对应的实例对象
               bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
            }
            catch (IllegalStateException ex) {
               throw new ScopeNotActiveException(beanName, scopeName, ex);
            }
         }
      }
      catch (BeansException ex) {
         beanCreation.tag("exception", ex.getClass().toString());
         beanCreation.tag("message", String.valueOf(ex.getMessage()));
         // 如果创建bean实例过程中出现异常，则将beanName从alreadyCreated缓存中移除
         cleanupAfterBeanCreationFailure(beanName);
         throw ex;
      }
      finally {
         beanCreation.end();
      }
   }

   // Check if required type matches the type of the actual bean instance. 检查所需类型是否与实际 bean 实例的类型匹配。
   // 10.检查所需类型是否与实际的bean对象的类型匹配
   if (requiredType != null && !requiredType.isInstance(bean)) {
      try {
         // 10.1 类型不对，则尝试转换bean类型
         T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
         if (convertedBean == null) {
            throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
         }
         return convertedBean;
      }
      catch (TypeMismatchException ex) {
         if (logger.isTraceEnabled()) {
            logger.trace("Failed to convert bean '" + name + "' to required type '" +
                  ClassUtils.getQualifiedName(requiredType) + "'", ex);
         }
         throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
      }
   }
   // 11.返回创建出来的bean实例对象
   return (T) bean;
}
```

虽然这个方法代码很长，但是总结来说这个方法做的事其实很简单，可以看到第2点，调用了getSingleton方法，先去缓存中获取bean，如果缓存中没有则创建。只不过在创建之前又进行了一系列的判断，包括类型检测、循环依赖检查（此处的循环依赖不是我们平时说的三级缓存解决的循环依赖）、scope判断等。

总而言之，最终如果没有获取到bean则需要进行创建，进入creatBean方法

#### 4、creatBean 重要 

同样的，进入creatBean方法中的doCreateBean：

```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
      throws BeanCreationException {

   // Instantiate the bean.
   // 1.新建Bean包装类
   BeanWrapper instanceWrapper = null;
   if (mbd.isSingleton()) {
      // 2.如果是FactoryBean，则需要先移除未完成的FactoryBean实例的缓存
      instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
   }
   if (instanceWrapper == null) {
      // 创建bean实例，属性还没
      // 3.根据beanName、mbd、args，使用对应的策略创建Bean实例，并返回包装类BeanWrapper
      instanceWrapper = createBeanInstance(beanName, mbd, args);
   }
   // 4.拿到创建好的Bean实例
   Object bean = instanceWrapper.getWrappedInstance();
   // 5.拿到Bean实例的类型
   Class<?> beanType = instanceWrapper.getWrappedClass();
   if (beanType != NullBean.class) {
      mbd.resolvedTargetType = beanType;
   }

   // Allow post-processors to modify the merged bean definition.
   synchronized (mbd.postProcessingLock) {
      if (!mbd.postProcessed) {
         try {
            // 6.应用后置处理器MergedBeanDefinitionPostProcessor，允许修改MergedBeanDefinition，
            // Autowired注解正是通过此方法实现注入类型的预解析
            applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
         }
         catch (Throwable ex) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                  "Post-processing of merged bean definition failed", ex);
         }
         mbd.postProcessed = true;
      }
   }

   // Eagerly cache singletons to be able to resolve circular references
   // even when triggered by lifecycle interfaces like BeanFactoryAware.
   // 7.判断是否需要提早曝光实例：单例 && 允许循环依赖 && 当前bean正在创建中
   boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
         isSingletonCurrentlyInCreation(beanName));
   if (earlySingletonExposure) {
      if (logger.isTraceEnabled()) {
         logger.trace("Eagerly caching bean '" + beanName +
               "' to allow for resolving potential circular references");
      }
      // 8.提前曝光beanName的ObjectFactory，用于解决循环引用
      // 8.1 应用后置处理器SmartInstantiationAwareBeanPostProcessor，允许返回指定bean的早期引用，若没有则直接返回bean
      addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
   }

   // Initialize the bean instance. 初始化bean实例。
   Object exposedObject = bean;
   try {
      // 给bean里的普通属性赋值
      // 9.对bean进行属性填充；其中，可能存在依赖于其他bean的属性，则会递归初始化依赖的bean实例
      populateBean(beanName, mbd, instanceWrapper);
      // 10.对bean进行初始化
      exposedObject = initializeBean(beanName, exposedObject, mbd);
   }
   catch (Throwable ex) {
      if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
         throw (BeanCreationException) ex;
      }
      else {
         throw new BeanCreationException(
               mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
      }
   }

   if (earlySingletonExposure) {
      // 11.如果允许提前曝光实例，则进行循环依赖检查
      Object earlySingletonReference = getSingleton(beanName, false);
      // 11.1 earlySingletonReference只有在当前解析的bean存在循环依赖的情况下才会不为空
      if (earlySingletonReference != null) {
         if (exposedObject == bean) {
            // 11.2 如果exposedObject没有在initializeBean方法中被增强，则不影响之前的循环引用
            exposedObject = earlySingletonReference;
         }
         else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
            // 11.3 如果exposedObject在initializeBean方法中被增强 && 不允许在循环引用的情况下使用注入原始bean实例
            // && 当前bean有被其他bean依赖
            // 11.4 拿到依赖当前bean的所有bean的beanName数组
            String[] dependentBeans = getDependentBeans(beanName);
            Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
            for (String dependentBean : dependentBeans) {
               // 11.5 尝试移除这些bean的实例，因为这些bean依赖的bean已经被增强了，他们依赖的bean相当于脏数据
               if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                  // 11.6 移除失败的添加到 actualDependentBeans
                  actualDependentBeans.add(dependentBean);
               }
            }
            if (!actualDependentBeans.isEmpty()) {
               // 11.7 如果存在移除失败的，则抛出异常，因为存在bean依赖了“脏数据”
               throw new BeanCurrentlyInCreationException(beanName,
                     "Bean with name '" + beanName + "' has been injected into other beans [" +
                     StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
                     "] in its raw version as part of a circular reference, but has eventually been " +
                     "wrapped. This means that said other beans do not use the final version of the " +
                     "bean. This is often the result of over-eager type matching - consider using " +
                     "'getBeanNamesForType' with the 'allowEagerInit' flag turned off, for example.");
            }
         }
      }
   }

   // Register bean as disposable.
   try {
      // 12.注册用于销毁的bean，执行销毁操作的有三种：自定义destroy方法、DisposableBean接口、DestructionAwareBeanPostProcessor
      registerDisposableBeanIfNecessary(beanName, bean, mbd);
   }
   catch (BeanDefinitionValidationException ex) {
      throw new BeanCreationException(
            mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
   }
   // 13.完成创建并返回
   return exposedObject;
}
```

此方法中有几个非常重要的方法，分别为：

第3点 实例化方法：createBeanInstance

第9点 赋值方法：populateBean

第10点 初始化方法：initializeBean

#### 5、创建bean实例 重要

进入createBeanInstance方法

```java
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
   // Make sure bean class is actually resolved at this point.
   // 解析bean的类型信息
   Class<?> beanClass = resolveBeanClass(mbd, beanName);

   if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
      // beanClass不为空 && beanClass不是公开类（不是public修饰） && 该bean不允许访问非公共构造函数和方法，则抛异常
      throw new BeanCreationException(mbd.getResourceDescription(), beanName,
            "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
   }
   // 在注册bean定义时，可以设置一个Supplier<?>类型的函数式接口。其实就是用户可以提供一段创建bean实例的代码，这样Spring就使用它来创建bean实例。
   Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
   if (instanceSupplier != null) {
      return obtainFromSupplier(instanceSupplier, beanName);
   }
   // 1.如果存在工厂方法则使用工厂方法实例化bean对象
   if (mbd.getFactoryMethodName() != null) {
      return instantiateUsingFactoryMethod(beanName, mbd, args);
   }

   // Shortcut when re-creating the same bean...
   // resolved: 构造函数或工厂方法是否已经解析过
   boolean resolved = false;
   // autowireNecessary: 是否需要自动注入（即是否需要解析构造函数参数)
   boolean autowireNecessary = false;
   if (args == null) {
      // 2.加锁
      synchronized (mbd.constructorArgumentLock) {
         if (mbd.resolvedConstructorOrFactoryMethod != null) {
            // 2.1 如果resolvedConstructorOrFactoryMethod缓存不为空，则将resolved标记为已解析
            resolved = true;
            // 2.2 根据constructorArgumentsResolved判断是否需要自动注入
            autowireNecessary = mbd.constructorArgumentsResolved;
         }
      }
   }
   if (resolved) {
      // 3.如果已经解析过，则使用resolvedConstructorOrFactoryMethod缓存里解析好的构造函数方法
      if (autowireNecessary) {
         // 3.1 需要自动注入，则执行构造函数自动注入
         return autowireConstructor(beanName, mbd, null, null);
      }
      else {
         // 3.2 否则使用默认的构造函数进行bean的实例化
         return instantiateBean(beanName, mbd);
      }
   }

   // Candidate constructors for autowiring?
   // 4.应用后置处理器SmartInstantiationAwareBeanPostProcessor，拿到bean的候选构造函数
   Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
   if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
         mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
      return autowireConstructor(beanName, mbd, ctors, args);
   }

   // Preferred constructors for default construction?
   ctors = mbd.getPreferredConstructors();
   if (ctors != null) {
      // 5.如果ctors不为空 || mbd的注入方式为AUTOWIRE_CONSTRUCTOR || mdb定义了构造函数的参数值 || args不为空，则执行构造函数自动注入
      return autowireConstructor(beanName, mbd, ctors, null);
   }

   // No special handling: simply use no-arg constructor.
   // 6.没有特殊处理，则使用默认的构造函数进行bean的实例化
   return instantiateBean(beanName, mbd);
}
```

创建实例的方法通常有以下几种：工厂方法、构造函数自动装配（通常指带有参数的构造函数）、简单实例化（默认的构造函数）。其中工厂方法现在基本不使用了，不再解析；构造函数自动装配方法由于篇幅有限，在后续文章spring bean依赖注入方法中具体分析；简单实例化对应的方法就是instantiateBean，在这个方法内部判断了需要实例化的对象是普通bean还是代理bean，普通bean则直接通过反射的方式创建对象并返回，代理对象则生成 CGLIB 创建的子类对象。

这里要注意的是此处创建返回的对象属性都是默认值，是一个半成品对象，需要进行下一步populateBean赋值操作。

#### 6、属性赋值 重要

回顾我们的bean生命周期流程图可以看到5、6两点都是赋值操作，一个是给普通对象赋值，另一个是给实现了Aware接口的对象赋值。所谓实现了Aware接口的对象其实就是spring容器本身的内部对象，比如BeanFactory、ApplicationContext等，如果在某个对象中有这种类型的对象，那么普通赋值就无法进行，需要调用给Aware对象赋值的方法。

populateBean对@Autowired、@Resource、@Value注解标注的属性进行填充；以及对beanDefinition.getPropertyValues获取到的属性进行处理, 如果我们手动设置了一个BeanDefinition的注入模型为byName或者byType的情况下, 也会在这里进行处理，手动设置就是在xml文件中<bean>标签里加入autowire属性，指定根据什么来注入，现在很少这么做了。

```java
protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
		/**
		 * 	这一段代码是Spring用来提供给程序员扩展使用的, 如果我们不希望一个bean参与到属性注入, 自动装配的流
		 *  程中, 那么就可以创建一个InstantiationAwareBeanPostProcessor后置处理器的实现类, 重写其
		 *  postProcessAfterInstantiation方法, 如果该方法返回false, 那么continueWithPropertyPopulation
		 *  这个变量会被置为false, 而这个变量被置为false, 在下面我们可以看到直接就return了, 从而Spring就不
		 *  会对属性进行注入
		 */
		// 2.bw为空时的处理
		if (bw == null) {
			if (mbd.hasPropertyValues()) {
				// 2.1 如果bw为空，属性不为空，抛异常，无法将属性值应用于null实例
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
			}
			else {
				// Skip property population phase for null instance.
				// 2.2 如果bw为空，属性也为空，则跳过
				return;
			}
		}

		// Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
		// state of the bean before properties are set. This can be used, for example,
		// to support styles of field injection.
		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			// 3.1 如果mbd不是合成的 && 存在InstantiationAwareBeanPostProcessor，则遍历处理InstantiationAwareBeanPostProcessor
			for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
				// 3.2 在bean实例化后，属性填充之前被调用，允许修改bean的属性，如果返回false，则跳过之后的属性填充
				if (!bp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
					// 3.3 如果返回false，直接返回
					return;
				}
			}
		}
		// =============================================================================================

		/**
		 * 这一段代码是当我们手动设置了注入模型为byType/byName的时候, Spring就会利用Java的内省机制拿到所有的
		 * set方法, 如果一个set方法有参数, Spring就会将其封装成一个PropertyValue, 然后放入到新创建的newPvs
		 * 中, 最终用这个newPvs来替换原来的pvs, 这里有一个注意点, 在获取pvs的时候, 如果程序员没有提供, pvs
		 * 被设置成了null, 因为 mbd.getPropertyValues()这段代码始终是能拿到一个集合对象的, 只是这个集合对象
		 * 中没有PropertyValue而已
		 */
		PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);

		int resolvedAutowireMode = mbd.getResolvedAutowireMode();
		// 4.解析自动装配模式为AUTOWIRE_BY_NAME和AUTOWIRE_BY_TYPE（现在几乎不用）
		if (resolvedAutowireMode == AUTOWIRE_BY_NAME || resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
			MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
			// Add property values based on autowire by name if applicable.
			// 4.1 解析autowireByName的注入
			if (resolvedAutowireMode == AUTOWIRE_BY_NAME) {
				autowireByName(beanName, mbd, bw, newPvs);
			}
			// Add property values based on autowire by type if applicable.
			// 4.2 解析autowireByType的注入
			if (resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
				autowireByType(beanName, mbd, bw, newPvs);
			}
			pvs = newPvs;
		}
		// =============================================================================================

		/**
		 * 在这段代码中, 如果pvs == null, Spring就获取beanDefinition中的集合对象了, 如果pvs == null,
		 * 我们也可以推断出, 程序员没有提供PropertyValue, 同时, 该beanDefinition也不是byName/byType的, 之后
		 * Spring会调用InstantiationAwareBeanPostProcessor.postProcessProperties方法, 在之前我们分析
		 * applyMergedBeanDefinitionPostProcessor的时候, 有讲解到, Spring会将所有需要被注入的属性/方法封装
		 * 成一个InjectedElement, 然后放入到InjectionMetadata中, 而这个InjectionMetada是位于后置处理器中的,
		 * 这是一个策略模式的应用, 不同的后置处理器处理不同的注入类型, 而在当前这一步, 就是遍历这些不同的后置
		 * 处理器, 开始将它们中的InjectionMetadata拿出来, 取出一个个InjectedElement完成注入
		 */
		// 5.BeanFactory是否注册过InstantiationAwareBeanPostProcessors
		boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
		// 6.是否需要依赖检查
		boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
		// 实现了Aware接口的类
		PropertyDescriptor[] filteredPds = null;
		// 7.注册过InstantiationAwareBeanPostProcessors
		if (hasInstAwareBpps) {
			if (pvs == null) {
				pvs = mbd.getPropertyValues();
			}
			// 7.1 应用后置处理器InstantiationAwareBeanPostProcessor
			for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
				PropertyValues pvsToUse = bp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
				if (pvsToUse == null) {
					if (filteredPds == null) {
						filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
					}
					// 7.1.1 应用后置处理器InstantiationAwareBeanPostProcessor的方法postProcessPropertyValues，
					// 进行属性填充前的再次处理。例子：现在最常用的@Autowire属性注入就是这边注入依赖的bean实例对象
					pvsToUse = bp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
					if (pvsToUse == null) {
						return;
					}
				}
				pvs = pvsToUse;
			}
		}
		// 需要依赖检查
		if (needsDepCheck) {
			if (filteredPds == null) {
				filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
			}
			// 7.2 依赖检查，对应depends-on属性
			checkDependencies(beanName, mbd, filteredPds, pvs);
		}
		// =============================================================================================

		/**
		 * 如果pvs不为null, Spring就会开始遍历里面的一个个PropertyValue, 通过反射调用setXXX方法来完成注入,
		 * 所以这就很好理解为什么当注入模型为byName/byType的时候, Spring能完成自动注入了
		 */
		// 8.将所有PropertyValues中的属性填充到bean中
		if (pvs != null) {
			applyPropertyValues(beanName, mbd, bw, pvs);
		}
	}
```

通过对populateBean方法的整体分析, 可以看出赋值的过程其实就是根据注解进行依赖注入的过程，可以简单分成几个步骤：判断是否需要属性赋值-->判断<bean>标签的autowire属性-->根据注解注入bean-->将前面保持好的pvs填充到bean。其中Aware接口的类也是同步判断赋值的，在第7点。

不难看出此处比较重要的环节就是根据注解注入bean，也是我们最常用的自动依赖注入方式，@Autowired注解，关于依赖注入的细节由于篇幅原因就放在之后的文章中详细讲解。目前只要了解到属性赋值的大致流程即可。

#### 7、初始化 重要

到这一步时候，对象的实例化和赋值都已完成，其实对象已经是完成了创建了，但是spring还多了一步初始化操作，这一步操作其实是提供了一些扩展，之前提到过的BeanPostProcessor也在这里使用。

首先让我们看一下源码，进入doCreateBean的第10点 initializeBean方法：

```java
protected Object initializeBean(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {
   // 1.激活Aware方法
   if (System.getSecurityManager() != null) {
      AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
         invokeAwareMethods(beanName, bean);
         return null;
      }, getAccessControlContext());
   }
   else {
      invokeAwareMethods(beanName, bean);
   }

   Object wrappedBean = bean;
   if (mbd == null || !mbd.isSynthetic()) {
      // 2.在初始化前应用BeanPostProcessor的postProcessBeforeInitialization方法，允许对bean实例进行包装
      wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
   }

   try {
      // 3.调用初始化方法
      invokeInitMethods(beanName, wrappedBean, mbd);
   }
   catch (Throwable ex) {
      throw new BeanCreationException(
            (mbd != null ? mbd.getResourceDescription() : null),
            beanName, "Invocation of init method failed", ex);
   }
   if (mbd == null || !mbd.isSynthetic()) {
      // 4.在初始化后应用BeanPostProcessor的postProcessAfterInitialization方法，允许对bean实例进行包装
      wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
   }
   // 5.返回wrappedBean
   return wrappedBean;
}
```

这里比较重要的3个步骤就是2、3、4

3是调用初始化方法，我们在定义bean的时候可以给一个init-method，就是在此处进行调用。

2和4就是调用BPP前置和后置方法，是spring提供的一个扩展点。

##### BPP前置方法

第2点 applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);

```java
@Override
public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
      throws BeansException {

   Object result = existingBean;
   // 1.遍历所有注册的BeanPostProcessor实现类，调用postProcessBeforeInitialization方法
   for (BeanPostProcessor processor : getBeanPostProcessors()) {
      // 2.在bean初始化方法执行前，调用postProcessBeforeInitialization方法
      Object current = processor.postProcessBeforeInitialization(result, beanName);
      if (current == null) {
         return result;
      }
      result = current;
   }
   return result;
}
```

进入方法可以看到这里需要遍历调用postProcessBeforeInitialization方法，这里直接进入的话可以看到是一个叫做BeanPostProcessor的接口类，其中就只有两个方法，一个before，一个after，很多spring内部类实现了这个接口。比较重要的：ApplicationContextAwareProcessor，我们经常通过实现 ApplicationContextAware 接口来拿到 ApplicationContext，我们之所以能拿到 ApplicationContext，就是在这边被赋值的。

```java
public interface BeanPostProcessor {
   @Nullable
   default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      return bean;
   }

   @Nullable
   default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
      return bean;
   }
}
```

##### 初始化方法

invokeInitMethods

```java
protected void invokeInitMethods(String beanName, Object bean, @Nullable RootBeanDefinition mbd)
      throws Throwable {
   // 1.首先检查bean是否实现了InitializingBean接口，如果是的话调用afterPropertiesSet方法
   boolean isInitializingBean = (bean instanceof InitializingBean);
   if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
      if (logger.isTraceEnabled()) {
         logger.trace("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
      }
      // 2.调用afterPropertiesSet方法
      if (System.getSecurityManager() != null) {
         try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
               ((InitializingBean) bean).afterPropertiesSet();
               return null;
            }, getAccessControlContext());
         }
         catch (PrivilegedActionException pae) {
            throw pae.getException();
         }
      }
      else {
         ((InitializingBean) bean).afterPropertiesSet();
      }
   }

   if (mbd != null && bean.getClass() != NullBean.class) {
      String initMethodName = mbd.getInitMethodName();
      if (StringUtils.hasLength(initMethodName) &&
            !(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
            !mbd.isExternallyManagedInitMethod(initMethodName)) {
         // 3.调用自定义初始化方法
         invokeCustomInitMethod(beanName, bean, mbd);
      }
   }
}
```

初始化方法非常简单，就两种情况，一种是实现了InitializingBean接口，另一种是有自定义的初始化方法，也就是我们自己写的init-method。

##### BPP后置方法

```java
@Override
public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
      throws BeansException {

   Object result = existingBean;
   // 1.遍历所有注册的BeanPostProcessor实现类，调用postProcessAfterInitialization方法
   for (BeanPostProcessor processor : getBeanPostProcessors()) {
      // 2.在bean初始化方法执行后，调用postProcessAfterInitialization方法
      Object current = processor.postProcessAfterInitialization(result, beanName);
      if (current == null) {
         return result;
      }
      result = current;
   }
   return result;
}
```

和前面的前置方法一样，只不过这个方法是在初始化之后执行，AOP就是在BPP后置方法中完成代理的。

## 四、总结

到此bean的实例化、初始化就正式完成了，接下来就是使用和销毁阶段，销毁其实就是调用destroy方法，同样是可以自定义的，在对象销毁之前执行某些操作。

在初始化所有剩余非惰性bean过程中，主要完成了以下步骤： -->finishBeanFactoryInitialization(beanFactory);

将之前解析的 BeanDefinition 进一步处理，将有父 BeanDefinition 的进行合并，获得 MergedBeanDefinition -->getMergedLocalBeanDefinition

尝试从缓存获取 bean 实例 -->getSingleton

创建 bean 实例 -->getBean、createBean、createBeanInstance

循环引用的处理 -->三级缓存解决循环依赖（后续文章详细讲解）

bean 实例属性填充 -->populateBean

bean 实例的初始化 -->initializeBean

BeanPostProcessor 的各种扩展应用 -->postProcessBeforeInitialization、postProcessAfterInitialization

由于篇幅有限，文章中还有一些重要细节没有具体讲解，比如getSingleton中的三级缓存解决循环依赖问题、注解自动依赖注入的详细介绍。这些将在后续的文章中具体分析。

