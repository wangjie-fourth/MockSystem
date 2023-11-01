/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.invocation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.github.fourth.mocksystem.packages.mockit.Invocation;
import org.github.fourth.mocksystem.packages.mockit.internal.expectations.RecordAndReplayExecution;
import org.github.fourth.mocksystem.packages.mockit.internal.reflection.MethodReflection;
import org.github.fourth.mocksystem.packages.mockit.internal.reflection.ParameterReflection;
import org.github.fourth.mocksystem.packages.mockit.internal.state.TestRun;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.concurrent.locks.ReentrantLock;

@SuppressFBWarnings({"UL_UNRELEASED_LOCK", "UL_UNRELEASED_LOCK_EXCEPTION_PATH"})
abstract class DynamicInvocationResult extends InvocationResult {
    private static final Object[] NO_ARGS = {};

    @Nonnull
    private final Object targetObject;
    @Nonnull
    final Method methodToInvoke;
    private final boolean hasInvocationParameter;
    private final int numberOfRegularParameters;

    DynamicInvocationResult(@Nonnull Object targetObject, @Nonnull Method methodToInvoke) {
        this.targetObject = targetObject;
        this.methodToInvoke = methodToInvoke;

        Class<?>[] parameters = methodToInvoke.getParameterTypes();
        int n = parameters.length;

        hasInvocationParameter = n > 0 && parameters[0] == Invocation.class;
        numberOfRegularParameters = hasInvocationParameter ? n - 1 : n;
    }

    @Nullable
    public final Object invokeMethodOnTargetObject(
            @Nullable Object mockOrRealObject, @Nonnull ExpectedInvocation invocation,
            @Nonnull InvocationConstraints constraints, @Nonnull Object[] args) {
        Object[] delegateArgs = numberOfRegularParameters == 0 ? NO_ARGS : args;
        Object result;

        if (hasInvocationParameter) {
            result = invokeMethodWithContext(mockOrRealObject, invocation, constraints, args, delegateArgs);
        } else {
            result = executeMethodToInvoke(delegateArgs);
        }

        return result;
    }

    @Nullable
    private Object invokeMethodWithContext(
            @Nullable Object mockOrRealObject, @Nonnull ExpectedInvocation expectedInvocation,
            @Nonnull InvocationConstraints constraints, @Nonnull Object[] invokedArgs, @Nonnull Object[] delegateArgs) {
        Invocation invocation = new DelegateInvocation(mockOrRealObject, invokedArgs, expectedInvocation, constraints);
        Object[] delegateArgsWithInvocation = ParameterReflection.argumentsWithExtraFirstValue(delegateArgs, invocation);

        Object result = executeMethodToInvoke(delegateArgsWithInvocation);

        return
                expectedInvocation.isConstructor() &&
                        TestRun.getExecutingTest().isProceedingIntoRealImplementation() ? Void.class : result;
    }

    @Nullable
    protected final Object executeMethodToInvoke(@Nonnull Object[] args) {
        ReentrantLock reentrantLock = RecordAndReplayExecution.RECORD_OR_REPLAY_LOCK;

        if (!reentrantLock.isHeldByCurrentThread()) {
            return MethodReflection.invoke(targetObject, methodToInvoke, args);
        }

        reentrantLock.unlock();

        try {
            return MethodReflection.invoke(targetObject, methodToInvoke, args);
        } finally {
            //noinspection LockAcquiredButNotSafelyReleased
            reentrantLock.lock();
        }
    }
}
