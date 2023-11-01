/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.invocation;

import org.github.fourth.mocksystem.packages.mockit.internal.reflection.RealMethodOrConstructor;
import org.github.fourth.mocksystem.packages.mockit.internal.state.TestRun;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;

final class RealImplementationResult extends DynamicInvocationResult {
    RealImplementationResult(@Nonnull Object instanceToInvoke, @Nonnull String methodToInvoke)
            throws NoSuchMethodException {
        super(
                instanceToInvoke,
                new RealMethodOrConstructor(instanceToInvoke.getClass(), methodToInvoke).<Method>getMember());
    }

    @Nullable
    @Override
    Object produceResult(@Nonnull Object[] args) {
        TestRun.getExecutingTest().markAsProceedingIntoRealImplementation();
        return executeMethodToInvoke(args);
    }
}
