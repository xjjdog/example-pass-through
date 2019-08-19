#  你的也是我的。3例ko多线程，局部变量透传
>原创：小姐姐味道（微信公众号ID：xjjdog），欢迎分享，转载请保留出处。

java中的threadlocal，是绑定在线程上的。你在一个线程中set的值，在另外一个线程是拿不到的。如果在threadlocal的平行线程中，创建了新的子线程，那么这里面的值是无法传递、共享的（先想清楚为什么再往下看）。这就是透传问题。

值在线程之间的透传，你可以认为是一个bug，这些问题一般会比较隐蔽，但问题暴露的时候脾气却比较火爆，让人手忙脚乱，怀疑人生。

作为代码的掌舵者，我们必然不能忍受这种问题的蹂躏。本篇文章适合细看，我们拿出3个例子，通过编码手段说明解决此类bug的通用方式，希望能达到举一反三的效果。对于搞基础架构的同学，是必备知识点。

1、普通线程的ThreadLocal透传问题  
2、sl4j MDC组件中ThreadLocal透传问题  
3、Hystrix组件的透传问题

由于涉及代码比较多，xjjdog将这三个例子的代码，放在了github上，想深入研究，可以下载下来debug一下。                       
```
https://github.com/xjjdog/example-pass-through
```

# 一、问题简单演示

为了有个比较直观的认识，下面展示一段异常代码。
![](media/15661756105776/15661977979729.jpg)
以上代码在主线程设置了一个简单的threadlocal变量，然后在自线程中想要取出它的值。执行后发现，程序的输出是：`null`。

程序的输出和我们的期望产生了明显的差异。其实，将**ThreadLocal** 换成**InheritableThreadLocal** 就ok了。不要高兴太早，对于使用线程池的情况，由于会缓存线程，线程是缓存起来反复使用的。这时父子线程关系的上下文传递，已经没有意义。

# 二、解决线程池透传问题

所以，线程池InheritableThreadLocal进行提交，获取的值，有可能是前一个任务执行后留下的，是错误的。使用只有在任务执行的时候进行传递，才是正常的功能。

上面的问题，transmittable-thread-local项目，已经很好的解决，并提供了java-agent的方式支持。

我们这里从最小集合的源码层面，来看一下其中的内容。首先，我们看一下ThreadLocal的结构。
![](media/15661756105776/15662066178206.jpg)
ThreadLocal其实是作为一个Map中的key而存在的，这个Map就是ThreadLocalMap，它以私有变量的形式，存在于Thread类中。拿上图为例，如果我创建了一个ThreadLocal，然后调用set方法，它会首先找到当前的thread，然后找到threadLocals，最后把自己作为key，存放在这个map里。
```
hread t = Thread.currentThread();
ThreadLocalMap map = getMap(t);
map.set(this, value);
```

要能够完成多线程的协调工作，必须提供全套的多线程工具。包括但不限于：

**1、定义注解，以及被注解修饰的ThreadLocal类**  
![](media/15661756105776/15662008169736.jpg)
定义新的ThreadLocal类，以便在赋值的时候，能够根据注解进行拦截和过滤。这就要求，在定义ThreadLocal的时候，要使用我们提供的ThreadLocal类，而不是jdk提供的那两个。

**2、进行父子线程之间的数据拷贝**  
在线程池提交任务之前，我们需要有个地方，将父进程的ThreadLocal内容，暂存一下。
![](media/15661756105776/15662073449760.jpg)
由于很多变量都是private的，需要根据反射进行操作。根据上面提供的ThreadLocal类的结构，我们需要直接操作其中的变量table（这也是为什么jdk不能随便改变变量名的原因）。

将父线程相关的变量暂存之后，就可以在使用的时候，通过主动设值和清理，完成变量拷贝。

**3、提供专用的Callable或者Runnable**  
那么这些数据是如何组装起来的呢？还是靠我们的任务载体类。
线程池提交线程，一般是通过Callable或者Runnable，以Runnable为例，我们看一下这个调用关系。

以下类采用了委托模式。
![](media/15661756105776/15662081632496.jpg)

这样，只要在提交任务的时候，使用了我们自定义的Runnable；同时，使用了自定义的ThreadLocal，就能够正常完成透传。

# 三、解决MDC透传问题

 sl4j MDC机制非常好，通常用于保存线程本地的“诊断数据”然后有日志组件打印，其内部时基于threadLocal实现；不过这就有一些问题，主线程中设置的MDC数据，在其子线程（多线程池）中是无法获取的，下面就来介绍如何解决这个问题。
 
 >MDC ( Mapped Diagnostic Contexts )，它是一个线程安全的存放诊断日志的容器。通常，会在处理请求前将请求的唯一标示放到MDC容器中，比如sessionId。这个唯一标示会随着日志一起输出。配置文件可以使用占位符进行变量替换。
 
 类似于上面介绍的方式，我们需要提供专用的Callable和Runnable。另外，为了能够同时支持MDC和普通线程，这两个类采用装饰器模式，进行功能追加。就单个类来说，对外的展现依然是委托模式。
 ![](media/15661756105776/15662086854501.jpg)
同样的思路，同样的模式。不一样的是，父线程的信息暂存，我们直接使用MDC的内部方法，并在任务的执行前后，进行相应操作。
 
# 四、解决Hystrix透传问题

同样的问题，在Netflix公司的熔断组件Hystrix中，依然存在。Hystrix线程池模式下，透传ThreadLocal需要进行改造，它本身是无法完成这个功能的。

但是Hystrix策略无法简单通过yml文件方式配置。我们参考Spring Cloud中对此策略的扩展方式，开发自己的策略。需要继承HystrixConcurrentStrategy。

构造代码还是较长的，可以查看github项目。但有一个地方需要说明。
![](media/15661756105776/15662093008407.jpg)
我们使用装饰器模式，对代码进行了层层嵌套，同时将多线程透传功能、MDC传递功能给追加了进来。这样，我们的这个类，就同时在以上三个环境中拥有了透传功能。

# End
同样的思路，可以用在其他组件上。比如我们在多篇调用链的文章里，提到的trace信息在多线程环境下的传递。

一般就是在当前线程暂存数据，然后在提交任务时进行包装。值得注意的是，这种方式侵入性还是比较大的，适合封装在通用的基础工具包中。你要是在业务中这么用，大概率会被骂死。

那可如何是好。

ThreadLocal会引发很多棘手的bug，造成代码污染。在使用之前，一定要确保你确实需要使用它。比如你在SimpleDateFormat类上用了线程局部变量，可以将它替换成DateTimeFormatter。

我们不善于解决问题，我们只善于解决容易出问题的类。

>作者简介：**小姐姐味道**  (xjjdog)，一个不允许程序员走弯路的公众号。聚焦基础架构和Linux。十年架构，日百亿流量，与你探讨高并发世界，给你不一样的味道。我的个人微信xjjdog0，欢迎添加好友，​进一步交流。​

近期热门文章​

[《必看！java后端，亮剑诛仙》](https://mp.weixin.qq.com/s/Cuv0SyjzasDKC0wIQxrgaw)  
后端技术索引，中肯火爆

[《Linux上，最常用的一批命令解析（10年精选）》](https://mp.weixin.qq.com/s/9RbTGQ4k4s92mrSf2xJ5TQ)
CSDN发布首日，1k赞。点赞率1/8。

[《这次要是讲不明白Spring Cloud核心组件，那我就白编这故事了》](https://mp.weixin.qq.com/s/hjYAddJEqgg3ZWTJnPTD9g)
用故事讲解核心组件，包你满意

[《Linux生产环境上，最常用的一套“Sed“技巧》](https://mp.weixin.qq.com/s/wP9_wvoTARRrlszsOmvMgQ)
最常用系列Sed篇，简单易懂。Vim篇更加易懂。
