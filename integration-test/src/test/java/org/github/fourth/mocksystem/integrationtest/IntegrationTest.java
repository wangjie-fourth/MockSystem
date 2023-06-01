package org.github.fourth.mocksystem.integrationtest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;

@DisplayName("模拟测试")
public class IntegrationTest {
    public static boolean isWindows() {
        return System.getProperties().getProperty("os.name").toUpperCase().contains("WINDOWS");
    }

    private boolean isLocalMachine() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", " pwd ");
        try {

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitVal = process.waitFor();
            if (exitVal == 0) {
                System.out.println(output);
                return output.toString().contains("wangjie_fourth");
            } else {
                Assertions.fail("command line exec fail!");
                return false;
            }

        } catch (Throwable e) {
            e.printStackTrace();
            Assertions.fail("error occur");
            return false;
        }
    }

    private String runCommandAndGetOOut() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (isWindows()) {
            processBuilder.command("cmd", "/c", " java -javaagent:../agent/target/mocksystem-agent-1.0.jar -jar ../test-application/target/mocksystem-test-application-1.0-jar-with-dependencies.jar");
        } else if (isLocalMachine()) {
            processBuilder.command("bash", "-c", "/Library/Java/JavaVirtualMachines/jdk1.8.0_251.jdk/Contents/Home/bin/java -javaagent:../agent/target/mocksystem-agent-1.0.jar -jar ../test-application/target/mocksystem-test-application-1.0-jar-with-dependencies.jar");
        } else {
            processBuilder.command("bash", "-c"," java -javaagent:../agent/target/mocksystem-agent-1.0.jar -jar ../test-application/target/mocksystem-test-application-1.0-jar-with-dependencies.jar");
        }

        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        String errorLine;
        while ((errorLine = error.readLine()) != null) {
            errorOutput.append(errorLine).append("\n");
        }

        int exitCode = process.waitFor();
        System.out.println("Process terminated with " + exitCode);
        if (exitCode == 0) {
            System.out.println(output);
        } else {
            System.err.println(errorOutput);
        }

        return output.toString();

    }

    @Test
    @DisplayName("模拟测试")
    public void mockCase() throws IOException, InterruptedException {
        String outPut = runCommandAndGetOOut();
        Assertions.assertTrue(outPut.contains("[wangjie] Withdrawal operation completed in:"));
    }
}
