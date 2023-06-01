package org.github.fourth.mocksystem.integrationtest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessUtils {

    private final Process process;
    private final String outLog;
    private final String errorLog;

    public ProcessUtils(ProcessBuilder processBuilder) throws IOException {
        this.process = processBuilder.start();
        BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        String line;
        while ((line = outReader.readLine()) != null) {
            output.append(line).append("\n");
        }
        outLog = output.toString();
        System.out.println(outLog);
        while ((line = errorReader.readLine()) != null) {
            errorOutput.append(line).append("\n");
        }
        errorLog = errorOutput.toString();
        System.err.println(errorLog);
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
