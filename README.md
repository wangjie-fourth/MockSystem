<a href="https://codecov.io/gh/wangjie-fourth/MockSystem/branch/main"><img src="https://img.shields.io/codecov/c/github/wangjie-fourth/MockSystem/main?logo=codecov&logoColor=white" alt="Coverage Status"></a>
<a href="https://github.com/wangjie-fourth/MockSystem/actions/workflows/build_and_test.yaml">
<img src="https://img.shields.io/github/actions/workflow/status/wangjie-fourth/MockSystem/build_and_test.yaml?branch=main&logo=github&logoColor=white&label=fast ci" alt="Github Workflow Build Status">
</a>
<a href="https://github.com/wangjie-fourth/MockSystem"><img src="https://img.shields.io/github/repo-size/wangjie-fourth/MockSystem" alt="GitHub repo size"></a>
<a href="https://github.com/wangjie-fourth/MockSystem/issues"><img src="https://img.shields.io/github/issues/wangjie-fourth/MockSystem" alt="GitHub issues"></a>
<a href="https://github.com/wangjie-fourth/MockSystem/graphs/contributors"><img src="https://img.shields.io/github/contributors/wangjie-fourth/MockSystem" alt="GitHub Contributors"></a>
<a href="https://github.com/wangjie-fourth/MockSystem/stargazers"><img src="https://img.shields.io/github/stars/wangjie-fourth/MockSystem" alt="GitHub Stars"></a>
<a href="https://github.com/wangjie-fourth/MockSystem/fork"><img src="https://img.shields.io/github/forks/wangjie-fourth/MockSystem" alt="GitHub Forks"></a>


# MockSystem
使用javaAgent，动态的Mock某些方法的方法。
```shell
java -javaagent:./mocksystem-agent/target/mocksystem-agent-1.0.jar -jar ./mocksystem-test-application/target/mocksystem-test-application-1.0-jar-with-dependencies.jar
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5055 -javaagent:./mocksystem-agent/target/mocksystem-agent-1.0.0.jar -jar ./mocksystem-test-application/target/mocksystem-test-application-1.0.0-jar-with-dependencies.jar
```

一些思路：
提供一个页面，来配置这些mock返回数据；
被mock的应用只需要加上javaagent启动后，就可以实现自动mock接口返回值了。

1、如何让别人配置后，本地的javaagent来修改这个类的字节码？
本地修改mock信息后，触发新版本的agent打包，将修改的信息包含进去，并生成新agent的jar包。
然后目标机器来重新加载这个运行的agent包。



未完事项：
1、动态attach需要给一个命令行执行的例子，现在缺少tool.jar
将tools.jar塞入项目解决
```shell
mvn install:install-file -Dfile=D:/software/java/jdk8u322-b06/lib/tools.jar
 -DgroupId=com.sun
 -DartifactId=tools
 -Dversion=1.8.0
 -Dpackaging=jar
 -DlocalRepositoryPath=D:/project/myself/MockSystem/attacher/my-repo
```

2、运行时加载javaagent的测试

3、test-appliction需要配置化的运行时间

4、看mockito是如何实现自动mock的
普通方法: mockito是通过生成子类做代理解决的
powerMock: 通过修改字节码的方式实现的.类是由一个新的ClassLoad加载的


5、可以参考jmockit的实现方式
修改本地字节码之后，启动一个agent来加载修改后的字节码文件