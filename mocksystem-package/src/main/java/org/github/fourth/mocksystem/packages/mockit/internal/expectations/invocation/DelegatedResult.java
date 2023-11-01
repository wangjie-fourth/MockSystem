/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.invocation;

import org.github.fourth.mocksystem.packages.mockit.Delegate;
import org.github.fourth.mocksystem.packages.mockit.internal.reflection.MethodReflection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class DelegatedResult extends DynamicInvocationResult {
    DelegatedResult(@Nonnull Delegate<?> delegate) {
        super(delegate, MethodReflection.findNonPrivateHandlerMethod(delegate));
    }

    @Nullable
    @Override
    Object produceResult(
            @Nullable Object invokedObject, @Nonnull ExpectedInvocation invocation,
            @Nonnull InvocationConstraints constraints, @Nonnull Object[] args) {
        return invokeMethodOnTargetObject(invokedObject, invocation, constraints, args);
    }
}
