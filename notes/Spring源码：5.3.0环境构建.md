# Spring源码：5.3.0环境构建

**如果您想尽快构建好Spring源码，请按照此文章一模一样的操作，顺利的话大概只需1小时即可构建完成。这也是本人碰了两天的壁整理出来的，其他的教程或多或少都有点问题，就是一点问题导致无法构建成功。**

## 一、前言

Spring对于Java的重要性已无需多言，想要深入了解Spring则离不开源码，看源码的第一步就是构建源码环境，然而光是这一步就可以挡住很多人前进的脚步，因为构建环境的途中将会出现很多问题。网络上的构建教程大多数都有或多或少的问题，本人花了两天时间终于把源码构建完毕，决定写一篇真正简单可用的源码构建教程，避免大家还没开始看就被构建环境拒之门外。

为确保万无一失，推荐大家使用的Spring版本、Gradle版本和本文一致，否则将会出现各种各样的问题。另外请确保网络可用，有些公司的网络屏蔽了一些下载依赖的地址，需要切换网络下载。

*所需工具：IDEA 2021.3（IDEA版本不是很关键，什么版本都行）、Gradle 6.5.1、JDK11*

## 二、工具准备

#### 1、Gradle 6.5.1

这一步一般没什么问题，就是下载个构建工具，配置一下文件。不过Gradle官网有时需要梯子。

这个工具类似Maven，Spring需要使用此工具进行构建。

Gradle官网下载：https://gradle.org/releases/

![1](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716181945.png)

下载图中的binary-only即可，是一个zip压缩包，完成后直接解压到自己想要的地址即可。

随后配置环境变量：这里简单说明了，相信大家到了看源码的阶段了不至于不会配置环境变量

在系统变量中点击新建，变量名：GRADLE_HOME，变量值：你的Gradle路径，不用到bin。

![2](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716181952.png)

![3](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716181959.png)

在系统变量中找到path变量，双击打开，点击新建，值：%GRADLE_HOME%\bin

![4](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182002.png)

最后打开dos窗口，输入gradle -v测试，出现以下页面表示成功

![5](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182005.png)

#### 2、jdk11

重要！！！此处请使用jdk11，网上说的使用jdk8，实测在最后编译时会报错import jdk.jfr.Category错误。

一般来说大家电脑上已经有了jdk8了，不过再下载11也不影响的，配置环境变量时JAVA_HOME的变量名加点标识就可以了，比如：JAVA_HOME_11。

直接去Oracle官网：https://www.oracle.com/cn/index.html

先登录账号，然后在顶部菜单栏找到 产品->Java 往下拉 点击立即下载Java 继续下拉找到 Java 11 点击 Windows 下载  x64 Installer  jdk-11.0.13_windows-x64_bin.exe

完成后双击安装，然后配置环境变量。配置环境变量过程就不赘述了，和上面配置gradle基本一样。

#### 3、源码获取

Spring源码在github上可以直接获取，但github下载很慢，在gitee上有每天同步的仓库提供下载。

https://gitee.com/mirrors/Spring-Framework/tree/v5.3.0/

下载5.3.0版本，下载后是一个zip压缩包，解压出来。

## 三、开始构建

构建过程可以说是最坎坷的过程了，本文结合图片，将最有效最简单的方式分享出来，只要一步不差的跟着做，很快就可以搞定。

打开IDEA，直接Open刚刚解压出的源码。打开项目之后IDEA下方会开始自动下载依赖，先点打叉停掉。

点击【File】->【Settings】

![6](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182022.png)

设置完毕之后，打开工程下的gradle目录->wrapper目录下的，gradle-wrapper.properties文件。因为gradle每次编译都会从官网下载指定版本（gradle-6.5.1-all.zip），所以我们在它第一次下载完之后，将distributionUrl设置成本地文件，这样就不会每次编译都从官网下载了。

```properties
distributionUrl=file:///D:/gradle-6.5.1/gradle-6.5.1-bin.zip(此处是你自己zip包放的地址，我是直接放到了gradle目录下)
```

![7](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182026.png)

打开build.gradle文件（这个就相当于是maven的pom文件，在），在文件头部加上

![8](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182031.png)

```gradle
buildscript {
   repositories {
      maven {
         url "https://repo.spring.io/plugins-release"
      }
   }
}
```

还是在这个文件里，搜索repositories找到这个代码块，使用下面的这些直接替换

```gradle
repositories {
   //新增以下2个阿里云镜像
   maven { url 'https://maven.aliyun.com/nexus/content/groups/public/' }
   maven { url 'https://maven.aliyun.com/nexus/content/repositories/jcenter' }
   mavenCentral()
   maven { url "https://repo.spring.io/libs-spring-framework-build" }
   maven { url "https://repo.spring.io/milestone" } // Reactor
   // 新增spring插件库
   maven { url "https://repo.spring.io/plugins-release" }
}
```

如果此文件里有报错则说明jdk没有选择，点击【File】->【Project Settings】其中的project和Modules->Dependencies里的jdk都选择为11。

到buildSrc文件夹下的build.gradle文件，加入阿里云镜像

```gradle
repositories {
   //新增以下2个阿里云镜像
   maven { url 'https://maven.aliyun.com/nexus/content/groups/public/' }
   maven { url 'https://maven.aliyun.com/nexus/content/repositories/jcenter' }
   mavenCentral()
   gradlePluginPortal()
}
```

到settings.gradle文件，把plugins里有一行注释掉，具体看下图

![10](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182037.png)

上面的都完成之后就可以点击刷新开始构建了

![11](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182050.png)

此过程需要一些时间，请确保网络良好，大概需要十几分钟左右，如果上面的任何一步没做对都可能报错，网络问题也会报下载依赖包错误，由于对于gradle构建工具不熟悉，出现的错误可能看不懂，所以请确保以上的配置都正确完成。

构建完成后找到 spring-context下的ApplicationContext 类

![12](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182053.png)

打开后，按下Ctrl+Alt+U键，选第一个选项，如果出现下图所示类图界面说明构建成功了！(构建过程就是找依赖对象的过程)

![13](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182058.png)

此时可以查看Spring的源码了，但是我们需要在源码的基础上面进行修改，开发，调试，添加注释等等操作，所以需要将源码进行编译打包，下面就是将源码编译的过程。

## 四、编译源码

点击IDEA右侧的Gradle标签，根据图片找到spring-oxm和spring-core模块下的othre->compileTestJava双击，先编译oxm。

此处如果使用jdk8就会出现报错import jdk.jfr.Category错误，因为这个包在jdk11才有。

![14](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182101.png)

都编译完成且成功之后，开始编译整个工程（这个过程非常耗时间，可能10-20分钟！），如下图：

打开顶层spring->build->build

![15](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182105.png)

到此源码的环境构建正式完成。

## 五、测试

1、完成了上面的过程后，我们可以自己编写一个模块测试该源码构建编译过程是否真正成功完成！

步骤：【File】->【New】->【Module...】 

在Spring中添加自己的module模块，同样选择gradle构建。

![16](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182141.png)

![17](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182141-1.png)

创建好后，找到我们自己的测试模块spring-mytest，打开build.gradle文件（相当于是pom文件），默认dependencies依赖(这里的dependencies和maven里的依赖是一样的)只有一个junit，我们需要手工添加spring-context，spring-beans，spring-core，spring-aop这4个核心模块，具体如下（建议使用下面这段代码直接替换原来的）：

```gradle
dependencies {
    //添加完要刷新gradle一下，否则代码中无法引用
    compile(project(":spring-context"))
    compile(project(":spring-beans"))
    compile(project(":spring-core"))
    compile(project(":spring-aop"))
    testCompile group: 'junit', name: 'junit', version: '4.12'
}
```

![18](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182141-2.png)

刷新完毕后就可以开始编写测试代码

![19](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182141-3.png)

下面将这三个类的代码贴出

User：

```java
package com.hong.pojo;

public class User {
   private String name;
   private int age;

   public User() {

   }

   public User(String name, int age) {
      this.name = name;
      this.age = age;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public int getAge() {
      return age;
   }

   public void setAge(int age) {
      this.age = age;
   }

   @Override
   public String toString() {
      return "User{" +
            "name='" + name + '\'' +
            ", age=" + age +
            '}';
   }
}
```

JavaConfig：

```java
package com.hong.config;

import com.hong.pojo.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class JavaConfig {

   @Bean
   public User user() {
      return new User("hong", 25);
   }
}
```

test：

```java
package com.hong.test;

import com.hong.config.JavaConfig;
import com.hong.pojo.User;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class test {
   public static void main(String[] args) {
      System.out.println("hello spring");
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(JavaConfig.class);
      User user = (User) context.getBean("user");
      System.out.println(user);
   }
}
```

运行测试类，能够打印想要的内容则代表完全成功了。

每次运行源码所需时间会比较长，因为每次都要重新编译。

到此Spring源码构建过程全部完成，恭喜你成功迈入看源码的第一步。

## 六、心得（源码怎么看？）

这里写一些自己看源码总结出的一点点经验。

推荐你在看源码之前先看几个视频，先跟着视频理解源码，spring最重要的就是bean生命周期、三级缓存解决循环依赖、依赖注入的几种方式、AOP的实现原理，看过几个视频后，你会对这些问题有些大概的理解，之后再看源码，会有个明确的方向，比如这次就先看生命周期，别的先不管。

需要用debug打断点看源码，不要每个方法都进去看，先看大体流程，重要方法再进去看，看不懂就百度查一下这个方法，通常会有很多解释，并且文章中也会有相应的注释。

源码中有很多注释，但都是英文的，推荐在IDEA里下载个Translation的插件，可以直接翻译，不过这样翻译过来的解释可能还是看不懂，这时就可以百度一下，会有文章进行解释。推荐自己在看源码时也可以加入注释。

某些不重要的方法无需深挖，一定是先看整体再看细节。

## 七、注意点

#### 1、源码添加中文注释出现编译错误

在源码中打印或者添加中文注释时有可能会出现编译错误，主要是由于编码问题。

错误表现为报XXXGBKXXX，如下图：

![20](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182141-4.png)

解决方法很简单：

![21](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182141-5.png)

![22](https://gitee.com/ahongbaba/note-picture/raw/master/img/20230716182141-6.png)

```
-Dfile.encoding=utf-8
```

加入这句后重启IDEA即可

