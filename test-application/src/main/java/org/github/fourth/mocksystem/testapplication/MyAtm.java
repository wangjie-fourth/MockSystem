package org.github.fourth.mocksystem.testapplication;

/**
 * @author jwang55
 */
public class MyAtm {

    public static void withdrawMoney(int amount) throws InterruptedException {
        Thread.sleep(1000);
        System.out.printf("[Application] Successful Withdrawal of [{%s}] units!%n", amount);
    }

    public static void main(String[] args) {
        try {
            withdrawMoney(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

}
