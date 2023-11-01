/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations;

import org.github.fourth.mocksystem.packages.mockit.internal.expectations.argumentMatching.ArgumentMatcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

final class VerifiedExpectation {
    @Nonnull
    final Expectation expectation;
    @Nonnull
    final Object[] arguments;
    @Nullable
    final List<ArgumentMatcher<?>> argMatchers;
    final int replayIndex;

    VerifiedExpectation(
            @Nonnull Expectation expectation, @Nonnull Object[] arguments, @Nullable List<ArgumentMatcher<?>> argMatchers,
            int replayIndex) {
        this.expectation = expectation;
        this.arguments = arguments;
        this.argMatchers = argMatchers;
        this.replayIndex = replayIndex;
    }

    @Nullable
    Object captureNewInstance() {
        return expectation.invocation.instance;
    }
}
