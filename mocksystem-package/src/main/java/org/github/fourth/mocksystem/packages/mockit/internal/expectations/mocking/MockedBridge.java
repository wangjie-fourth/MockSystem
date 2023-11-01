/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking;

import org.github.fourth.mocksystem.packages.mockit.internal.MockingBridge;
import org.github.fourth.mocksystem.packages.mockit.internal.expectations.ExecutionMode;
import org.github.fourth.mocksystem.packages.mockit.internal.state.TestRun;
import org.github.fourth.mocksystem.packages.mockit.internal.util.ObjectMethods;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;

import static org.github.fourth.mocksystem.packages.mockit.internal.expectations.RecordAndReplayExecution.RECORD_OR_REPLAY_LOCK;
import static org.github.fourth.mocksystem.packages.mockit.internal.expectations.RecordAndReplayExecution.recordOrReplay;
import static org.github.fourth.mocksystem.packages.mockit.internal.util.Utilities.calledFromSpecialThread;

public final class MockedBridge extends MockingBridge {
    @Nonnull
    public static final MockingBridge MB = new MockedBridge();

    private MockedBridge() {
        super("$MB");
    }

    @Nullable
    @Override
    public Object invoke(@Nullable Object mocked, Method method, @Nonnull Object[] args) throws Throwable {
        String mockedClassDesc = (String) args[1];

        if (calledFromSpecialThread() || notToBeMocked(mocked, mockedClassDesc)) {
            return Void.class;
        }

        String mockName = (String) args[2];
        String mockDesc = (String) args[3];
        String mockNameAndDesc = mockName + mockDesc;
        Integer executionMode = (Integer) args[5];
        Object[] mockArgs = extractMockArguments(6, args);

        boolean regularExecutionWithRecordReplayLock =
                executionMode == ExecutionMode.Regular.ordinal() && RECORD_OR_REPLAY_LOCK.isHeldByCurrentThread();
        Object rv;

        if (regularExecutionWithRecordReplayLock && mocked != null) {
            rv = ObjectMethods.evaluateOverride(mocked, mockNameAndDesc, args);

            if (rv != null) {
                return rv;
            }
        }

        if (
                TestRun.getExecutingTest().isProceedingIntoRealImplementation() ||
                        regularExecutionWithRecordReplayLock ||
                        TestRun.isInsideNoMockingZone()
        ) {
            return Void.class;
        }

        TestRun.enterNoMockingZone();

        try {
            int mockAccess = (Integer) args[0];
            String genericSignature = (String) args[4];
            rv = recordOrReplay(
                    mocked, mockAccess, mockedClassDesc, mockNameAndDesc, genericSignature, executionMode, mockArgs);
        } finally {
            TestRun.exitNoMockingZone();
        }

        return rv;
    }
}
