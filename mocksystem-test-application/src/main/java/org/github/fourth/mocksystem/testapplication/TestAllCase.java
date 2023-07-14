package org.github.fourth.mocksystem.testapplication;

import org.github.fourth.mocksystem.testapplication.testcase.AddBeforeAfterCodeCase;

/**
 * @author jwang55
 */
public class TestAllCase {

    public static void main(String[] args) {
        try {
            System.out.println("start==============================");
            AddBeforeAfterCodeCase.testAddBeforeAfterCode(5);
            System.out.println("end==============================");
            System.out.println("\n");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

}
