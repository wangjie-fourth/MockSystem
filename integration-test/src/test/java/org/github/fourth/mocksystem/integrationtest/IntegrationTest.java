package org.github.fourth.mocksystem.integrationtest;

import com.sun.tools.attach.VirtualMachine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Field;

@DisplayName("模拟测试")
public class IntegrationTest {

    private static final String AGENT_JAR_LOCATION = "../agent/target/mocksystem-agent-1.0.0.jar";

    private static final String TEST_APPLICATION_LOCATION = "../test-application/target/mocksystem-test-application-1.0.0-jar-with-dependencies.jar";

    private static final String LOCAL_JAVA_COMMAND_LOCATION = "/Library/Java/JavaVirtualMachines/jdk1.8.0_251.jdk/Contents/Home/bin/java";

    @Test
    @DisplayName("启动时加载测试")
    public void startLoadingTest() throws IOException, InterruptedException {
        String command;
        if (isWindows()) {
            command = String.format(" java -javaagent:%s  -jar  %s", AGENT_JAR_LOCATION, TEST_APPLICATION_LOCATION);
        } else if (isLocalMachine() && isMac()) {
            command = String.format("%s -javaagent:%s  -jar %s", LOCAL_JAVA_COMMAND_LOCATION, AGENT_JAR_LOCATION, TEST_APPLICATION_LOCATION);
        } else {
            command = String.format(" java -javaagent:%s  -jar  %s", AGENT_JAR_LOCATION, TEST_APPLICATION_LOCATION);
        }
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(computeShebang(), executeTheStringAsCommand(), command);

        Assertions.assertTrue(new ProcessUtils(processBuilder).getLog().contains("[wangjie] Withdrawal operation completed in:"));
    }

//    @Test
//    @DisplayName("运行时加载测试")
    public void runtimeLoadingTest() throws IOException, InterruptedException {
        String command;
        if (isLocalMachine() && isMac()) {
            command = String.format("%s -jar %s", LOCAL_JAVA_COMMAND_LOCATION, TEST_APPLICATION_LOCATION);
        } else {
            command = String.format(" java -jar  %s", TEST_APPLICATION_LOCATION);
        }
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(computeShebang(), executeTheStringAsCommand(), command);

        ProcessUtils processUtils = new ProcessUtils(processBuilder);

        String pid = String.valueOf(getPid(processUtils.getProcess()));
        System.out.println(pid);
        try {
            VirtualMachine virtualMachine = VirtualMachine.attach(pid);
            virtualMachine.loadAgent(AGENT_JAR_LOCATION);
            virtualMachine.detach();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        Thread.sleep(10000);
    }

    public static int getPid(Process process) {
        try {
            Class<?> cProcessImpl = process.getClass();
            Field fPid = cProcessImpl.getDeclaredField("pid");
            if (!fPid.isAccessible()) {
                fPid.setAccessible(true);
            }
            return fPid.getInt(process);
        } catch (Exception e) {
            return -1;
        }
    }

    private static String executeTheStringAsCommand() {
        return isWindows() ? "/c" : "-c";
    }

    private static String computeShebang() {
        return isWindows() ? "cmd" : "bash";
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
}
