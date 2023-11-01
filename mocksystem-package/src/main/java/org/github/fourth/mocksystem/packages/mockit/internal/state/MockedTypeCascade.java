/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.state;

import org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking.CascadingTypeRedefinition;
import org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking.InstanceFactory;
import org.github.fourth.mocksystem.packages.mockit.internal.reflection.GenericTypeReflection;
import org.github.fourth.mocksystem.packages.mockit.internal.reflection.RealMethodOrConstructor;
import org.github.fourth.mocksystem.packages.mockit.internal.util.DefaultValues;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.synchronizedList;
import static org.github.fourth.mocksystem.packages.mockit.external.asm.Type.getInternalName;
import static org.github.fourth.mocksystem.packages.mockit.internal.util.Utilities.containsReference;
import static org.github.fourth.mocksystem.packages.mockit.internal.util.Utilities.getClassType;

public final class MockedTypeCascade {
    @Nonnull
    private static final CascadingTypes CASCADING_TYPES = TestRun.getExecutingTest().getCascadingTypes();

    final boolean fromMockField;
    @Nonnull
    private final Type mockedType;
    @Nonnull
    final String mockedTypeDesc;
    @Nullable
    Class<?> mockedClass;
    @Nullable
    private GenericTypeReflection genericReflection;
    @Nonnull
    private final Map<String, Type> cascadedTypesAndMocks;
    @Nonnull
    private final List<Object> cascadingInstances;

    MockedTypeCascade(boolean fromMockField, @Nonnull Type mockedType, @Nonnull String mockedTypeDesc) {
        this.fromMockField = fromMockField;
        this.mockedType = mockedType;
        this.mockedTypeDesc = mockedTypeDesc;
        cascadedTypesAndMocks = new ConcurrentHashMap<String, Type>(4);
        cascadingInstances = synchronizedList(new ArrayList<Object>());
    }

    @Nullable
    public static Object getMock(
            @Nonnull String mockedTypeDesc, @Nonnull String mockedMethodNameAndDesc, @Nullable Object mockInstance,
            @Nonnull String returnTypeDesc, @Nonnull Class<?> returnType) {
        MockedTypeCascade cascade = CASCADING_TYPES.getCascade(mockedTypeDesc, mockInstance);

        if (cascade == null) {
            return null;
        }

        String cascadedReturnTypeDesc = getReturnTypeIfCascadingSupportedForIt(returnTypeDesc);

        if (cascadedReturnTypeDesc == null) {
            return null;
        }

        return cascade.getCascadedInstance(mockedMethodNameAndDesc, cascadedReturnTypeDesc, returnType);
    }

    @Nullable
    public static Object getMock(
            @Nonnull String mockedTypeDesc, @Nonnull String mockedMethodNameAndDesc, @Nullable Object mockInstance,
            @Nonnull String returnTypeDesc, @Nullable String genericSignature) {
        char typeCode = returnTypeDesc.charAt(0);

        if (typeCode != 'L') {
            return null;
        }

        MockedTypeCascade cascade = CASCADING_TYPES.getCascade(mockedTypeDesc, mockInstance);

        if (cascade == null) {
            return null;
        }

        String resolvedReturnTypeDesc = null;

        if (genericSignature != null) {
            resolvedReturnTypeDesc = cascade.getGenericReturnType(genericSignature);
        }

        if (resolvedReturnTypeDesc == null) {
            resolvedReturnTypeDesc = getReturnTypeIfCascadingSupportedForIt(returnTypeDesc);

            if (resolvedReturnTypeDesc == null) {
                return null;
            }
        } else if (resolvedReturnTypeDesc.charAt(0) == '[') {
            return DefaultValues.computeForArrayType(resolvedReturnTypeDesc);
        }

        return cascade.getCascadedInstance(mockedMethodNameAndDesc, resolvedReturnTypeDesc, mockInstance);
    }

    @Nullable
    private String getGenericReturnType(@Nonnull String genericSignature) {
        String returnTypeDesc = getGenericReflection().resolveReturnType(genericSignature);

        if (returnTypeDesc.charAt(0) == '[') {
            return returnTypeDesc;
        }

        String returnTypeName = returnTypeDesc.substring(1, returnTypeDesc.length() - 1);
        return isTypeSupportedForCascading(returnTypeName) ? returnTypeName : null;
    }

    @Nonnull
    private synchronized GenericTypeReflection getGenericReflection() {
        GenericTypeReflection reflection = genericReflection;

        if (reflection == null) {
            Class<?> ownerClass = getClassWithCalledMethod();
            reflection = new GenericTypeReflection(ownerClass, mockedType);
            genericReflection = reflection;
        }

        return reflection;
    }

    @Nullable
    private static String getReturnTypeIfCascadingSupportedForIt(@Nonnull Class<?> returnType) {
        String typeName = getInternalName(returnType);
        return isTypeSupportedForCascading(typeName) ? typeName : null;
    }

    @SuppressWarnings("OverlyComplexMethod")
    private static boolean isTypeSupportedForCascading(@Nonnull String typeName) {
        //noinspection SimplifiableIfStatement
        if (typeName.contains("/Process") || typeName.endsWith("/Runnable")) {
            return true;
        }

        return
                (!typeName.startsWith("java/lang/") || typeName.contains("management")) &&
                        !typeName.startsWith("java/math/") &&
                        (!typeName.startsWith("java/util/") ||
                                typeName.endsWith("/Date") || typeName.endsWith("/Callable") || typeName.endsWith("Future") ||
                                typeName.contains("logging")) &&
                        !"java/time/Duration".equals(typeName);
    }

    @Nullable
    private static String getReturnTypeIfCascadingSupportedForIt(@Nonnull String typeDesc) {
        String typeName = typeDesc.substring(1, typeDesc.length() - 1);
        return isTypeSupportedForCascading(typeName) ? typeName : null;
    }

    @Nullable
    private Object getCascadedInstance(
            @Nonnull String methodNameAndDesc, @Nonnull String returnTypeInternalName, @Nonnull Class<?> returnClass) {
        if (!cascadedTypesAndMocks.containsKey(returnTypeInternalName)) {
            cascadedTypesAndMocks.put(returnTypeInternalName, returnClass);
            CASCADING_TYPES.add(returnTypeInternalName, false, returnClass);
        }

        return createNewCascadedInstanceOrUseNonCascadedOneIfAvailable(methodNameAndDesc, returnClass);
    }

    @Nullable
    private Object getCascadedInstance(
            @Nonnull String methodNameAndDesc, @Nonnull String returnTypeInternalName, @Nullable Object mockInstance) {
        MockedTypeCascade nextLevel = this;
        Type returnType = cascadedTypesAndMocks.get(returnTypeInternalName);
        Class<?> returnClass;

        if (returnType == null) {
            Class<?> cascadingClass = getClassWithCalledMethod();

            Type genericReturnType;
            try {
                genericReturnType = getGenericReturnType(cascadingClass, methodNameAndDesc);
            } catch (NoSuchMethodException ignore) {
                return null;
            }

            Class<?> resolvedReturnType = getClassType(genericReturnType);

            if (resolvedReturnType.isAssignableFrom(cascadingClass)) {
                if (mockInstance != null) {
                    return mockInstance;
                }

                returnType = mockedType;
                returnClass = cascadingClass;
            } else {
                Object defaultReturnValue = DefaultValues.computeForType(resolvedReturnType);

                if (defaultReturnValue != null) {
                    return defaultReturnValue;
                }

                cascadedTypesAndMocks.put(returnTypeInternalName, genericReturnType);
                nextLevel = CASCADING_TYPES.add(returnTypeInternalName, false, genericReturnType);
                returnType = genericReturnType;
                returnClass = resolvedReturnType;
            }
        } else {
            nextLevel = CASCADING_TYPES.getCascade(returnType);
            returnClass = getClassType(returnType);
        }

        if (getReturnTypeIfCascadingSupportedForIt(returnClass) == null) {
            return null;
        }

        return nextLevel.createNewCascadedInstanceOrUseNonCascadedOneIfAvailable(methodNameAndDesc, returnType);
    }

    @Nonnull
    private Class<?> getClassWithCalledMethod() {
        if (mockedClass != null) {
            return mockedClass;
        }

        if (mockedType instanceof Class<?>) {
            return (Class<?>) mockedType;
        }

        return (Class<?>) ((ParameterizedType) mockedType).getRawType();
    }

    @Nonnull
    private Type getGenericReturnType(@Nonnull Class<?> cascadingClass, @Nonnull String methodNameAndDesc)
            throws NoSuchMethodException {
        Method cascadingMethod = new RealMethodOrConstructor(cascadingClass, methodNameAndDesc).getMember();
        Type genericReturnType = cascadingMethod.getGenericReturnType();

        if (genericReturnType instanceof TypeVariable<?>) {
            genericReturnType = getGenericReflection().resolveTypeVariable((TypeVariable<?>) genericReturnType);
        }

        return genericReturnType;
    }

    @Nullable
    private Object createNewCascadedInstanceOrUseNonCascadedOneIfAvailable(
            @Nonnull String methodNameAndDesc, @Nonnull Type mockedReturnType) {
        InstanceFactory instanceFactory = TestRun.mockFixture().findInstanceFactory(mockedReturnType);

        if (instanceFactory == null) {
            String methodName = methodNameAndDesc.substring(0, methodNameAndDesc.indexOf('('));
            CascadingTypeRedefinition typeRedefinition = new CascadingTypeRedefinition(methodName, mockedReturnType);
            instanceFactory = typeRedefinition.redefineType();

            if (instanceFactory == null) {
                return null;
            }
        } else {
            Object lastInstance = instanceFactory.getLastInstance();

            if (lastInstance != null) {
                return lastInstance;
            }
        }

        Object cascadedInstance = instanceFactory.create();
        instanceFactory.clearLastInstance();
        addInstance(cascadedInstance);
        TestRun.getExecutingTest().addInjectableMock(cascadedInstance);
        return cascadedInstance;
    }

    void discardCascadedMocks() {
        cascadedTypesAndMocks.clear();
        cascadingInstances.clear();
    }

    void addInstance(@Nonnull Object cascadingInstance) {
        cascadingInstances.add(cascadingInstance);
    }

    boolean hasInstance(@Nonnull Object cascadingInstance) {
        return containsReference(cascadingInstances, cascadingInstance);
    }
}
