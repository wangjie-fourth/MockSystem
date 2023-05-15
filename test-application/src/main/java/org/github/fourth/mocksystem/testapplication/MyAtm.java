package org.github.fourth.mocksystem.testapplication;

/**
 * @author jwang55
 */
public class MyAtm {

    public static void withdrawMoney(int amount) throws InterruptedException {
        Thread.sleep(5000);
        System.out.println("start==============================");
        System.out.printf("[Application] Successful Withdrawal of [{%s}] units!%n", amount);
    }

    public static void main(String[] args) {
        try {
            while (true) {
                withdrawMoney(5);
                System.out.println("end==============================");
                System.out.println("\n");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

}
