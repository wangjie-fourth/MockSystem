/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations;

import org.github.fourth.mocksystem.packages.mockit.internal.state.TestRun;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.lang.reflect.Modifier.isNative;
import static java.lang.reflect.Modifier.isStatic;

public enum ExecutionMode {
    Regular {
        @Override
        boolean isNativeMethodToBeIgnored(int access) {
            return false;
        }

        @Override
        boolean isToExecuteRealImplementation(@Nullable Object instance) {
            return instance != null && !TestRun.mockFixture().isInstanceOfMockedClass(instance);
        }
    },

    Partial {
        @Override
        boolean isToExecuteRealImplementation(@Nullable Object instance) {
            return instance != null && !TestRun.mockFixture().isInstanceOfMockedClass(instance);
        }

        @Override
        boolean isWithRealImplementation(@Nullable Object instance) {
            return instance == null || !TestRun.getExecutingTest().isInjectableMock(instance);
        }

        @Override
        boolean isToExecuteRealObjectOverride(@Nonnull Object instance) {
            return true;
        }
    },

    PerInstance {
        @Override
        boolean isStaticMethodToBeIgnored(int access) {
            return isStatic(access);
        }

        @Override
        boolean isToExecuteRealImplementation(@Nullable Object instance) {
            return instance == null || !TestRun.getExecutingTest().isMockedInstance(instance);
        }

        @Override
        boolean isToExecuteRealObjectOverride(@Nonnull Object instance) {
            return !TestRun.getExecutingTest().isMockedInstance(instance);
        }
    };

    public final boolean isMethodToBeIgnored(int access) {
        return isStaticMethodToBeIgnored(access) || isNativeMethodToBeIgnored(access);
    }

    boolean isStaticMethodToBeIgnored(int access) {
        return false;
    }

    boolean isNativeMethodToBeIgnored(int access) {
        return isNative(access);
    }

    boolean isToExecuteRealImplementation(@Nullable Object instance) {
        return false;
    }

    boolean isWithRealImplementation(@Nullable Object instance) {
        return false;
    }

    boolean isToExecuteRealObjectOverride(@Nonnull Object instance) {
        return false;
    }
}
