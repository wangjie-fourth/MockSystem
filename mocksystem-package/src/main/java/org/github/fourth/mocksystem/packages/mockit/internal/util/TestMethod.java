/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public final class TestMethod {
    @Nonnull
    public final String testClassDesc;
    @Nonnull
    public final String testMethodDesc;
    @Nonnull
    private final Type[] parameterTypes;
    @Nonnull
    private final Class<?>[] parameterClasses;
    @Nonnull
    private final Annotation[][] parameterAnnotations;
    @Nonnull
    private final Object[] parameterValues;

    public TestMethod(@Nonnull Method testMethod, @Nonnull Object[] parameterValues) {
        testClassDesc = org.github.fourth.mocksystem.packages.mockit.external.asm.Type.getInternalName(testMethod.getDeclaringClass());
        testMethodDesc = testMethod.getName() + org.github.fourth.mocksystem.packages.mockit.external.asm.Type.getMethodDescriptor(testMethod);
        parameterTypes = testMethod.getGenericParameterTypes();
        parameterClasses = testMethod.getParameterTypes();
        parameterAnnotations = testMethod.getParameterAnnotations();
        this.parameterValues = parameterValues;
    }

    @Nonnegative
    public int getParameterCount() {
        return parameterTypes.length;
    }

    @Nonnull
    public Type getParameterType(@Nonnegative int index) {
        return parameterTypes[index];
    }

    @Nonnull
    public Class<?> getParameterClass(@Nonnegative int index) {
        return parameterClasses[index];
    }

    @Nonnull
    public Annotation[] getParameterAnnotations(@Nonnegative int index) {
        return parameterAnnotations[index];
    }

    @Nullable
    public Object getParameterValue(@Nonnegative int index) {
        return parameterValues[index];
    }

    public void setParameterValue(@Nonnegative int index, @Nullable Object value) {
        parameterValues[index] = value;
    }
}
