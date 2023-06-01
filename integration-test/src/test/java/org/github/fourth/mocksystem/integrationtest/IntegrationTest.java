package org.github.fourth.mocksystem.integrationtest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;

@DisplayName("模拟测试")
public class IntegrationTest {

    private static final String AGENT_JAR_LOCATION = "../agent/target/mocksystem-agent-1.0.0.jar";

    private static final String TEST_APPLICATION_LOCATION = "../test-application/target/mocksystem-test-application-1.0.0-jar-with-dependencies.jar";

    private static final String LOCAL_JAVA_COMMAND_LOCATION = "/Library/Java/JavaVirtualMachines/jdk1.8.0_251.jdk/Contents/Home/bin/java";

    @Test
    @DisplayName("静态启动测试")
    public void mockCase() throws IOException, InterruptedException {
        Assertions.assertTrue(runCommandAndGetOutPut().contains("[wangjie] Withdrawal operation completed in:"));
    }

    private static boolean isWindows() {
        return System.getProperties().getProperty("os.name").toUpperCase().contains("WINDOWS");
    }

    private static boolean isMac() {
        return System.getProperties().getProperty("os.name").toUpperCase().contains("MAC");
    }

    private static boolean isLocalMachine() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", " pwd ");
        try {
            return new ProcessUtils(processBuilder).getLog().contains("wangjie_fourth");
        } catch (Throwable e) {
            e.printStackTrace();
            Assertions.fail("error occur");
            return false;
        }
    }

    private static String runCommandAndGetOutPut() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (isWindows()) {
            processBuilder.command("cmd", "/c", String.format(" java -javaagent:%s  -jar  %s", AGENT_JAR_LOCATION, TEST_APPLICATION_LOCATION));
        } else if (isLocalMachine() && isMac()) {
            processBuilder.command("bash", "-c", String.format("%s -javaagent:%s  -jar %s", LOCAL_JAVA_COMMAND_LOCATION, AGENT_JAR_LOCATION, TEST_APPLICATION_LOCATION));
        } else {
            processBuilder.command("bash", "-c", String.format(" java -javaagent:%s  -jar  %s", AGENT_JAR_LOCATION, TEST_APPLICATION_LOCATION));
        }

        return new ProcessUtils(processBuilder).getLog();
    }
}
