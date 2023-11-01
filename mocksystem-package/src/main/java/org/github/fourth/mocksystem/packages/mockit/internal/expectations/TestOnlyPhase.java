/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations;

import org.github.fourth.mocksystem.packages.mockit.internal.expectations.argumentMatching.ArgumentMatcher;
import org.github.fourth.mocksystem.packages.mockit.internal.expectations.argumentMatching.CaptureMatcher;
import org.github.fourth.mocksystem.packages.mockit.internal.expectations.argumentMatching.ClassMatcher;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static org.github.fourth.mocksystem.packages.mockit.internal.util.Utilities.indexOfReference;

public abstract class TestOnlyPhase extends Phase {
    protected boolean matchInstance;
    @Nullable
    protected List<ArgumentMatcher<?>> argMatchers;
    @Nullable
    Expectation currentExpectation;

    TestOnlyPhase(@Nonnull RecordAndReplayExecution recordAndReplay) {
        super(recordAndReplay);
    }

    public final void addArgMatcher(@Nonnull ArgumentMatcher<?> matcher) {
        getArgumentMatchers().add(matcher);
    }

    @Nonnull
    private List<ArgumentMatcher<?>> getArgumentMatchers() {
        if (argMatchers == null) {
            argMatchers = new ArrayList<ArgumentMatcher<?>>();
        }

        return argMatchers;
    }

    public final void moveArgMatcher(@Nonnegative int originalMatcherIndex, @Nonnegative int toIndex) {
        List<ArgumentMatcher<?>> matchers = getArgumentMatchers();
        int i = getMatcherPositionIgnoringNulls(originalMatcherIndex, matchers);

        for (i--; i < toIndex; i++) {
            matchers.add(i, null);
        }
    }

    @Nonnegative
    private static int getMatcherPositionIgnoringNulls(
            @Nonnegative int originalMatcherIndex, @Nonnull List<ArgumentMatcher<?>> matchers) {
        int i = 0;

        for (int matchersFound = 0; matchersFound <= originalMatcherIndex; i++) {
            if (matchers.get(i) != null) {
                matchersFound++;
            }
        }

        return i;
    }

    public final void setExpectedSingleArgumentType(@Nonnegative int parameterIndex, @Nonnull Class<?> argumentType) {
        ArgumentMatcher<?> newMatcher = ClassMatcher.create(argumentType);
        getArgumentMatchers().set(parameterIndex, newMatcher);
    }

    public final void setExpectedMultiArgumentType(@Nonnegative int parameterIndex, @Nonnull Class<?> argumentType) {
        CaptureMatcher<?> matcher = (CaptureMatcher<?>) getArgumentMatchers().get(parameterIndex);
        matcher.setExpectedType(argumentType);
    }

    public void setMaxInvocationCount(int maxInvocations) {
        if (currentExpectation != null) {
            int currentMinimum = currentExpectation.constraints.minInvocations;
            int minInvocations = maxInvocations < 0 ? currentMinimum : Math.min(currentMinimum, maxInvocations);
            handleInvocationCountConstraint(minInvocations, maxInvocations);
        }
    }

    public abstract void handleInvocationCountConstraint(int minInvocations, int maxInvocations);

    protected static boolean isEnumElement(@Nonnull Object mock) {
        Class<?> mockedClass = mock.getClass();
        return mockedClass.isEnum() && indexOfReference(mockedClass.getEnumConstants(), mock) >= 0;
    }
}
