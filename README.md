
使用javaAgent，动态的Mock某些方法的方法。
```shell
java -javaagent:./agent/target/agent-1.0.jar -jar ./test-application/target/test-application-1.0-jar-with-dependencies.jar
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5055 -javaagent:./agent/target/agent-1.0.jar -jar ./test-application/target/test-application-1.0-jar-with-dependencies.jar
```