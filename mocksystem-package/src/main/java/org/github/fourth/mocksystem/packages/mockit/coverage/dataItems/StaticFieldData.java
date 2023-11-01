/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.coverage.dataItems;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.github.fourth.mocksystem.packages.mockit.internal.state.TestRun;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
public final class StaticFieldData extends FieldData {
    private static final long serialVersionUID = -6596622341651601060L;

    @Nonnull
    private final transient Map<Integer, Boolean> testIdsToAssignments = new HashMap<Integer, Boolean>();

    void registerAssignment() {
        int testId = TestRun.getTestId();
        testIdsToAssignments.put(testId, Boolean.TRUE);
        writeCount++;
    }

    void registerRead() {
        int testId = TestRun.getTestId();
        testIdsToAssignments.put(testId, null);
        readCount++;
    }

    @Override
    void markAsCoveredIfNoUnreadValuesAreLeft() {
        for (Boolean withUnreadValue : testIdsToAssignments.values()) {
            if (withUnreadValue == null) {
                covered = true;
                break;
            }
        }
    }
}
