package org.github.fourth.mocksystem.integrationtest;

import com.sun.tools.attach.VirtualMachine;
import org.omg.SendingContext.RunTime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.github.fourth.mocksystem.integrationtest.IntegrationTest.AGENT_JAR_LOCATION;
import static org.github.fourth.mocksystem.integrationtest.IntegrationTest.getProcessID;

public class ProcessUtils {

    private final Process process;
    private final String outLog;
    private final String errorLog;

    public ProcessUtils(ProcessBuilder processBuilder) throws IOException {
        this.process = processBuilder.start();
        String pid = String.valueOf(getProcessID(process));
        System.out.println(pid);
        try {
            VirtualMachine virtualMachine = VirtualMachine.attach(pid);
            virtualMachine.loadAgent(AGENT_JAR_LOCATION);
            virtualMachine.detach();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        String line;
        while ((line = outReader.readLine()) != null) {
            output.append(line).append("\n");
            System.out.println(line);
        }
        outLog = output.toString();
        while ((line = errorReader.readLine()) != null) {
            errorOutput.append(line).append("\n");
            System.err.println(line);
        }
        errorLog = errorOutput.toString();
    }

    public Process getProcess() {
        return process;
    }

    public int getExitCode() throws InterruptedException {
        return process.waitFor();
    }

    public String getOutLog() {
        return outLog;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public String getLog() throws InterruptedException {
        return this.getExitCode() == 0 ? this.getOutLog() : this.getErrorLog();
    }
}
