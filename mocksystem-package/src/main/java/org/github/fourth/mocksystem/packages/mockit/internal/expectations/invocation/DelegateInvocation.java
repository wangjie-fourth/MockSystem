/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.invocation;

import org.github.fourth.mocksystem.packages.mockit.internal.BaseInvocation;
import org.github.fourth.mocksystem.packages.mockit.internal.state.ExecutingTest;
import org.github.fourth.mocksystem.packages.mockit.internal.state.TestRun;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;

final class DelegateInvocation extends BaseInvocation {
    @Nonnull
    private final InvocationArguments invocationArguments;

    DelegateInvocation(
            @Nullable Object invokedInstance, @Nonnull Object[] invokedArguments,
            @Nonnull ExpectedInvocation expectedInvocation, @Nonnull InvocationConstraints constraints) {
        super(invokedInstance, invokedArguments, constraints.invocationCount);
        invocationArguments = expectedInvocation.arguments;
    }

    @Nonnull
    @Override
    protected Member findRealMember() {
        return invocationArguments.getRealMethodOrConstructor();
    }

    @Override
    public void prepareToProceed() {
        ExecutingTest executingTest = TestRun.getExecutingTest();

        if (getInvokedMember() instanceof Constructor) {
            executingTest.markAsProceedingIntoRealImplementation();
        } else {
            executingTest.markAsProceedingIntoRealImplementation(this);
        }
    }

    @Override
    public void cleanUpAfterProceed() {
        TestRun.getExecutingTest().clearProceedingState();
    }
}
