package org.github.fourth.mocksystem.testapplication.testcase;

public class AddBeforeAfterCodeCase {

    public static final String beforeCode = "startTime = System.currentTimeMillis();";
    public static final String afterCode = "endTime = System.currentTimeMillis();" +
            "opTime = (endTime-startTime)/1000;" +
            "System.out.println(\"[wangjie] Withdrawal operation completed in:\" + opTime + \" seconds!\");";

    public static final String addLogPrint = "[wangjie] Withdrawal operation completed in:";
    public static void testAddBeforeAfterCode(int amount) throws InterruptedException {
        System.out.printf("[Application] Successful Withdrawal of [{%s}] units!%n", amount);
    }


}
