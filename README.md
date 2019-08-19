# 多线程透传

今天发了篇文章，是关于threadlocal的。这里聊一些平常面试中隐秘的东西。这种问题一般关注的人比较少，但却是最常问的问题。工作中也是经常出bug的地方。
https://mp.weixin.qq.com/s/8hc1dNlJy_zfDiyb8G3fUw

通过无数的面试总结，我发现很多人对threadlocal的结构有错误的认识。

错误：
threadlocal是一个内容持有类，它里面有一个map，map的key是线程，value是set的值。不同线程的数据，都在这一个map里。这样取值的时候，只需要threadlocal.get(currentThread)就可以。

正确：
threadlocal只是一个辅助类，它是作为map的key存在的。而这个map，是在thread类里的。这个map是thread类的成员变量。

这种问题，通常一翻源码就能看到。但是还是产生了这么多的误解，这和网络上一些错误的文章传播有关。

与此类似的，SimpleDateFormat是非线程安全的，这个在早几年的面试中，碰到的概率可谓其高。


threadlocal是线程同步的一种方式。其他方式还有：
1、wait、notify、notifyAll
2、volitile
3、synchronized
4、Lock、Contidition
5、LockSupoort park、unpark
6、cas，atomic类
7、无锁队列

