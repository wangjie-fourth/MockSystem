/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.injection.constructor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking.MockedType;
import org.github.fourth.mocksystem.packages.mockit.internal.injection.InjectionPointProvider;
import org.github.fourth.mocksystem.packages.mockit.internal.injection.InjectionState;
import org.github.fourth.mocksystem.packages.mockit.internal.injection.Injector;
import org.github.fourth.mocksystem.packages.mockit.internal.injection.TestedClass;
import org.github.fourth.mocksystem.packages.mockit.internal.injection.full.FullInjection;
import org.github.fourth.mocksystem.packages.mockit.internal.state.ParameterNames;
import org.github.fourth.mocksystem.packages.mockit.internal.state.TestRun;
import org.github.fourth.mocksystem.packages.mockit.internal.util.MethodFormatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.github.fourth.mocksystem.packages.mockit.internal.injection.InjectionPoint.*;
import static org.github.fourth.mocksystem.packages.mockit.internal.injection.InjectionPointProvider.NULL;
import static org.github.fourth.mocksystem.packages.mockit.internal.reflection.ConstructorReflection.invoke;
import static org.github.fourth.mocksystem.packages.mockit.internal.util.Utilities.NO_ARGS;
import static org.github.fourth.mocksystem.packages.mockit.internal.util.Utilities.getClassType;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public final class ConstructorInjection extends Injector {
    @Nonnull
    private final Constructor<?> constructor;

    public ConstructorInjection(
            @Nonnull TestedClass testedClass, @Nonnull InjectionState injectionState, @Nullable FullInjection fullInjection,
            @Nonnull Constructor<?> constructor) {
        super(testedClass, injectionState, fullInjection);
        this.constructor = constructor;
    }

    @Nonnull
    public Object instantiate(@Nonnull List<InjectionPointProvider> parameterProviders) {
        Type[] parameterTypes = constructor.getGenericParameterTypes();
        int n = parameterTypes.length;
        List<InjectionPointProvider> consumedInjectables = n == 0 ? null : injectionState.saveConsumedInjectables();
        Object[] arguments = n == 0 ? NO_ARGS : new Object[n];
        boolean varArgs = constructor.isVarArgs();

        if (varArgs) {
            n--;
        }

        for (int i = 0; i < n; i++) {
            @Nonnull InjectionPointProvider parameterProvider = parameterProviders.get(i);
            Object value;

            if (parameterProvider instanceof ConstructorParameter) {
                value = createOrReuseArgumentValue((ConstructorParameter) parameterProvider);
            } else {
                value = getArgumentValueToInject(parameterProvider, i);
            }

            if (value != null) {
                Type parameterType = parameterTypes[i];
                arguments[i] = wrapInProviderIfNeeded(parameterType, value);
            }
        }

        if (varArgs) {
            Type parameterType = parameterTypes[n];
            arguments[n] = obtainInjectedVarargsArray(parameterType);
        }

        if (consumedInjectables != null) {
            injectionState.restoreConsumedInjectables(consumedInjectables);
        }

        return invokeConstructor(arguments);
    }

    @Nonnull
    private Object createOrReuseArgumentValue(@Nonnull ConstructorParameter constructorParameter) {
        Object value = constructorParameter.getValue(null);

        if (value != null) {
            return value;
        }

        injectionState.setTypeOfInjectionPoint(constructorParameter.getDeclaredType());
        String qualifiedName = getQualifiedName(constructorParameter.getAnnotations());

        assert fullInjection != null;
        value = fullInjection.createOrReuseInstance(this, constructorParameter, qualifiedName);

        if (value == null) {
            String parameterName = constructorParameter.getName();
            String message =
                    "Missing @Tested or @Injectable" + missingValueDescription(parameterName) +
                            "\r\n  when initializing " + fullInjection;
            throw new IllegalStateException(message);
        }

        return value;
    }

    @Nullable
    private Object getArgumentValueToInject(@Nonnull InjectionPointProvider injectable, int parameterIndex) {
        Object argument = injectionState.getValueToInject(injectable);

        if (argument == null) {
            String classDesc = getClassDesc();
            String constructorDesc = getConstructorDesc();
            String parameterName = ParameterNames.getName(classDesc, constructorDesc, parameterIndex);

            if (parameterName == null) {
                parameterName = injectable.getName();
            }

            throw new IllegalArgumentException("No injectable value available" + missingValueDescription(parameterName));
        }

        return argument == NULL ? null : argument;
    }

    @Nonnull
    private String getClassDesc() {
        return org.github.fourth.mocksystem.packages.mockit.external.asm.Type.getInternalName(constructor.getDeclaringClass());
    }

    @Nonnull
    private String getConstructorDesc() {
        return "<init>" + org.github.fourth.mocksystem.packages.mockit.external.asm.Type.getConstructorDescriptor(constructor);
    }

    @Nonnull
    private Object obtainInjectedVarargsArray(@Nonnull Type parameterType) {
        Type varargsElementType = getTypeOfInjectionPointFromVarargsParameter(parameterType);
        injectionState.setTypeOfInjectionPoint(varargsElementType);

        List<Object> varargValues = new ArrayList<Object>();
        MockedType injectable;

        while ((injectable = injectionState.findNextInjectableForInjectionPoint()) != null) {
            Object value = injectionState.getValueToInject(injectable);

            if (value != null) {
                value = wrapInProviderIfNeeded(varargsElementType, value);
                varargValues.add(value);
            }
        }

        Object varargArray = newArrayFromList(varargsElementType, varargValues);
        return varargArray;
    }

    @Nonnull
    private static Object newArrayFromList(@Nonnull Type elementType, @Nonnull List<Object> values) {
        Class<?> componentType = getClassType(elementType);
        int elementCount = values.size();
        Object array = Array.newInstance(componentType, elementCount);

        for (int i = 0; i < elementCount; i++) {
            Array.set(array, i, values.get(i));
        }

        return array;
    }

    @Nonnull
    private String missingValueDescription(@Nonnull String name) {
        String classDesc = getClassDesc();
        String constructorDesc = getConstructorDesc();
        String constructorDescription = new MethodFormatter(classDesc, constructorDesc).toString();
        int p = constructorDescription.indexOf('#');
        String friendlyConstructorDesc = constructorDescription.substring(p + 1).replace("java.lang.", "");

        return " for parameter \"" + name + "\" in constructor " + friendlyConstructorDesc;
    }

    @Nonnull
    private Object invokeConstructor(@Nonnull Object[] arguments) {
        TestRun.exitNoMockingZone();

        try {
            return invoke(constructor, arguments);
        } finally {
            TestRun.enterNoMockingZone();
        }
    }
}
