/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.state;

import org.github.fourth.mocksystem.packages.mockit.internal.ClassIdentification;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.github.fourth.mocksystem.packages.mockit.internal.expectations.RecordAndReplayExecution.RECORD_OR_REPLAY_LOCK;

public final class SavePoint {
    @Nonnull
    private final Set<ClassIdentification> previousTransformedClasses;
    @Nonnull
    private final Map<Class<?>, byte[]> previousRedefinedClasses;
    private final int previousCaptureTransformerCount;
    @Nonnull
    private final List<Class<?>> previousMockedClasses;
    @Nonnull
    private final MockClasses.SavePoint previousMockClasses;

    public SavePoint() {
        MockFixture mockFixture = TestRun.mockFixture();
        previousTransformedClasses = mockFixture.getTransformedClasses();
        previousRedefinedClasses = mockFixture.getRedefinedClasses();
        previousCaptureTransformerCount = mockFixture.getCaptureTransformerCount();
        previousMockedClasses = mockFixture.getMockedClasses();
        previousMockClasses = TestRun.getMockClasses().new SavePoint();
    }

    public synchronized void rollback() {
        RECORD_OR_REPLAY_LOCK.lock();

        try {
            MockFixture mockFixture = TestRun.mockFixture();
            mockFixture.removeCaptureTransformers(previousCaptureTransformerCount);
            mockFixture.restoreTransformedClasses(previousTransformedClasses);
            mockFixture.restoreRedefinedClasses(previousRedefinedClasses);
            mockFixture.removeMockedClasses(previousMockedClasses);
            previousMockClasses.rollback();
        } finally {
            RECORD_OR_REPLAY_LOCK.unlock();
        }
    }
}
