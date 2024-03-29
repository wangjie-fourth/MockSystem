/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.mockups;

import org.github.fourth.mocksystem.packages.mockit.internal.MockingBridge;
import org.github.fourth.mocksystem.packages.mockit.internal.state.TestRun;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;

public final class MockupBridge extends MockingBridge {
    @Nonnull
    public static final MockingBridge MB = new MockupBridge();

    private MockupBridge() {
        super("$MUB");
    }

    @Nullable
    @Override
    public Object invoke(@Nullable Object mocked, Method method, @Nonnull Object[] args) throws Throwable {
        if (TestRun.isInsideNoMockingZone()) {
            return false;
        }

        TestRun.enterNoMockingZone();

        try {
            String mockClassDesc = (String) args[0];

            if (notToBeMocked(mocked, mockClassDesc)) {
                return false;
            }

            Integer mockStateIndex = (Integer) args[1];
            return TestRun.updateMockState(mockClassDesc, mocked, mockStateIndex);
        } finally {
            TestRun.exitNoMockingZone();
        }
    }
}
