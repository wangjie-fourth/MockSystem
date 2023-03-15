package org.github.fourth.mocksystem.testapplication;

/**
 * @author jwang55
 */
public class MyAtm {

    private static final int account = 10;

    public static void withdrawMoney(int amount) throws InterruptedException {
        Thread.sleep(2000l); //processing going on here
        System.out.println(String.format("[Application] Successful Withdrawal of [{%s}] units!", amount));
    }

}
