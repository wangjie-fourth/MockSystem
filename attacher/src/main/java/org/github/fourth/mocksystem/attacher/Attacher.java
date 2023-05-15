package org.github.fourth.mocksystem.attacher;

import com.sun.tools.attach.VirtualMachine;

/**
 *
 */
public class Attacher {
    public static void main(String[] args) throws InterruptedException {
        if (args == null) {
            System.out.println("agent attach arguments is null.");
            return;
        }

        if (args.length < 2) {
            System.out.printf("agent attach arguments length is %d, need length: 2%n", args.length);
            return;
        }

        String pid = args[0];
        String agentPathAndOptions = args[1];

        System.out.printf("agent attach pid: %s%n, agentPath and Options: %s%n", pid, agentPathAndOptions);

        try {
            VirtualMachine virtualMachine = VirtualMachine.attach(pid);
            virtualMachine.loadAgent(agentPathAndOptions);
            virtualMachine.detach();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private static class LocalDemo {
        public static void main(String[] args) {
            String pid = "19384";
            String agentPathAndOptions = "D:\\project\\myself\\MockSystem\\agent\\target\\agent-1.0.jar";
            try {
                VirtualMachine virtualMachine = VirtualMachine.attach(pid);
                virtualMachine.loadAgent(agentPathAndOptions);
                virtualMachine.detach();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }
}
