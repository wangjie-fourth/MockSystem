/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.coverage.testRedundancy;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public final class TestCoverage {
    @Nonnull
    public static final TestCoverage INSTANCE = new TestCoverage();

    @Nonnull
    private final Map<Method, Integer> testsToItemsCovered = new LinkedHashMap<Method, Integer>();
    @Nullable
    private Method currentTestMethod;

    private TestCoverage() {
    }

    public void setCurrentTestMethod(@Nullable Method testMethod) {
        if (testMethod != null) {
            testsToItemsCovered.put(testMethod, 0);
        }

        currentTestMethod = testMethod;
    }

    public void recordNewItemCoveredByTestIfApplicable(int previousExecutionCount) {
        if (previousExecutionCount == 0 && currentTestMethod != null) {
            Integer itemsCoveredByTest = testsToItemsCovered.get(currentTestMethod);
            testsToItemsCovered.put(currentTestMethod, itemsCoveredByTest == null ? 1 : itemsCoveredByTest + 1);
        }
    }

    @Nonnull
    public List<Method> getRedundantTests() {
        List<Method> redundantTests = new ArrayList<Method>();

        for (Entry<Method, Integer> testAndItemsCovered : testsToItemsCovered.entrySet()) {
            Method testMethod = testAndItemsCovered.getKey();
            Integer itemsCovered = testAndItemsCovered.getValue();

            if (itemsCovered == 0) {
                redundantTests.add(testMethod);
            }
        }

        return redundantTests;
    }
}
