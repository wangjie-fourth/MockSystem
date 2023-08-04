package org.github.fourth.mocksystem.testapplication.testcase;

public class AddBeforeAfterCodeCase {

    public static final String BEFORE_CODE = "startTime = System.currentTimeMillis();";
    public static final String AFTER_CODE = "endTime = System.currentTimeMillis();" +
            "opTime = (endTime-startTime)/1000;" +
            "System.out.println(\"[wangjie] Withdrawal operation completed in:\" + opTime + \" seconds!\");";

    public static final String ADD_LOG_PRINT = "[wangjie] Withdrawal operation completed in:";
    public static void testAddBeforeAfterCode(int amount) throws InterruptedException {
        System.out.printf("[Application] Successful Withdrawal of [{%s}] units!%n", amount);
    }


}
