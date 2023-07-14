package org.github.fourth.mocksystem.testapplication;

import org.github.fourth.mocksystem.testapplication.testcase.AddBeforeAfterCodeCase;
import org.github.fourth.mocksystem.testapplication.testcase.MockMethodResponse;

/**
 * @author jwang55
 */
public class TestAllCase {

    public static void main(String[] args) {
        try {
            System.out.println("case1:start==============================");
            AddBeforeAfterCodeCase.testAddBeforeAfterCode(5);

            System.out.println("case2:start==============================");
            String result = MockMethodResponse.testMockMethodResponse();
            System.out.println(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

}
