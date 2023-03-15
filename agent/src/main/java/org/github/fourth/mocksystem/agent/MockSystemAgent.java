package org.github.fourth.mocksystem.agent;

import java.lang.instrument.Instrumentation;

/**
 * @author jwang55
 */
public class MockSystemAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[Agent] In premain method");
        String className = "com.baeldung.instrumentation.application.MyAtm";
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("[Agent] In agentmain method");
        String className = "com.baeldung.instrumentation.application.MyAtm";
    }

}
