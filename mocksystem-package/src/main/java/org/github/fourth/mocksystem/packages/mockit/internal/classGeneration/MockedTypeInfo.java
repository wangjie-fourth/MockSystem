/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.classGeneration;

import org.github.fourth.mocksystem.packages.mockit.internal.reflection.GenericTypeReflection;
import org.github.fourth.mocksystem.packages.mockit.internal.util.Utilities;

import javax.annotation.Nonnull;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class MockedTypeInfo {
    @Nonnull
    public final GenericTypeReflection genericTypeMap;
    @Nonnull
    public final String implementationSignature;

    public MockedTypeInfo(@Nonnull Type mockedType) {
        Class<?> mockedClass = Utilities.getClassType(mockedType);
        genericTypeMap = new GenericTypeReflection(mockedClass, mockedType);

        String signature = getGenericClassSignature(mockedType);
        String classDesc = mockedClass.getName().replace('.', '/');
        implementationSignature = 'L' + classDesc + signature;
    }

    @Nonnull
    private static String getGenericClassSignature(@Nonnull Type mockedType) {
        StringBuilder signature = new StringBuilder(100);

        if (mockedType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) mockedType;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();

            if (typeArguments.length > 0) {
                signature.append('<');

                for (Type typeArg : typeArguments) {
                    if (typeArg instanceof Class<?>) {
                        Class<?> classArg = (Class<?>) typeArg;
                        signature.append('L').append(classArg.getName().replace('.', '/')).append(';');
                    } else {
                        signature.append('*');
                    }
                }

                signature.append('>');
            }
        }

        signature.append(';');
        return signature.toString();
    }
}
