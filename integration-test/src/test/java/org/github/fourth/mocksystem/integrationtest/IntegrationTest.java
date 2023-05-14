package org.github.fourth.mocksystem.integrationtest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@DisplayName("模拟测试")
public class IntegrationTest {

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

    @Test
    @DisplayName("模拟测试")
    public void mockCase() {
        System.out.println("test");
        if (!isLocalMachine()) {
            return;
        }

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", "/Library/Java/JavaVirtualMachines/jdk1.8.0_251.jdk/Contents/Home/bin/java -javaagent:../agent/target/agent-1.0.jar -jar ../test-application/target/test-application-1.0-jar-with-dependencies.jar");
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
                System.out.println("Success!");
                System.out.println(output);
                Assertions.assertTrue(output.toString().contains("[wangjie] Withdrawal operation completed in:"));
                System.exit(0);
            } else {
                Assertions.fail("command line exec fail!");
            }

        } catch (Throwable e) {
            e.printStackTrace();
            Assertions.fail("error occur");
        }
    }
}
