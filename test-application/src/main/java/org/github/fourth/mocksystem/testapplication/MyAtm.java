package org.github.fourth.mocksystem.testapplication;

/**
 * @author jwang55
 */
public class MyAtm {

    private static final int account = 10;

    public static void withdrawMoney(int amount) throws InterruptedException {
        System.out.println(String.format("[Application] Successful Withdrawal of [{%s}] units!", amount));
    }

    public static void main(String[] args) {
        try {
            System.out.println("111111");
            withdrawMoney(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
