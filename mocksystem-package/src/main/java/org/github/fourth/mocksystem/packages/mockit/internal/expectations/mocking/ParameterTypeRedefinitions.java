/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.github.fourth.mocksystem.packages.mockit.internal.util.TestMethod;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public final class ParameterTypeRedefinitions extends TypeRedefinitions {
    @Nonnull
    private final TestMethod testMethod;
    @Nonnull
    private final MockedType[] mockParameters;
    @Nonnull
    private final List<MockedType> injectableParameters;

    public ParameterTypeRedefinitions(@Nonnull TestMethod testMethod) {
        this.testMethod = testMethod;
        int n = testMethod.getParameterCount();
        mockParameters = new MockedType[n];
        injectableParameters = new ArrayList<MockedType>(n);

        for (int i = 0; i < n; i++) {
            getMockedTypeFromMockParameterDeclaration(i);
        }

        InstanceFactory[] instanceFactories = redefineMockedTypes();
        instantiateMockedTypes(instanceFactories);
    }

    private void getMockedTypeFromMockParameterDeclaration(@Nonnegative int parameterIndex) {
        Type parameterType = testMethod.getParameterType(parameterIndex);
        Annotation[] annotationsOnParameter = testMethod.getParameterAnnotations(parameterIndex);
        MockedType mockedType = new MockedType(testMethod, parameterIndex, parameterType, annotationsOnParameter);

        if (mockedType.isMockableType()) {
            mockParameters[parameterIndex] = mockedType;
        }

        if (mockedType.injectable) {
            injectableParameters.add(mockedType);
            testMethod.setParameterValue(parameterIndex, mockedType.providedValue);
        }
    }

    @Nonnull
    private InstanceFactory[] redefineMockedTypes() {
        int n = mockParameters.length;
        InstanceFactory[] instanceFactories = new InstanceFactory[n];

        for (int i = 0; i < n; i++) {
            MockedType mockedType = mockParameters[i];

            if (mockedType != null) {
                instanceFactories[i] = redefineMockedType(mockedType);
            }
        }

        return instanceFactories;
    }

    @Nullable
    private InstanceFactory redefineMockedType(@Nonnull MockedType mockedType) {
        TypeRedefinition typeRedefinition = new TypeRedefinition(mockedType);
        InstanceFactory instanceFactory = typeRedefinition.redefineType();

        if (instanceFactory != null) {
            addTargetClass(mockedType);
        }

        return instanceFactory;
    }

    private void registerCaptureOfNewInstances(@Nonnull MockedType mockedType, @Nonnull Object originalInstance) {
        if (captureOfNewInstances == null) {
            captureOfNewInstances = new CaptureOfNewInstances();
        }

        captureOfNewInstances.registerCaptureOfNewInstances(mockedType, originalInstance);
        captureOfNewInstances.makeSureAllSubtypesAreModified(mockedType);
    }

    private void instantiateMockedTypes(@Nonnull InstanceFactory[] instanceFactories) {
        for (int paramIndex = 0; paramIndex < instanceFactories.length; paramIndex++) {
            InstanceFactory instanceFactory = instanceFactories[paramIndex];

            if (instanceFactory != null) {
                MockedType mockedType = mockParameters[paramIndex];
                @Nonnull Object mockedInstance = instantiateMockedType(mockedType, instanceFactory);
                testMethod.setParameterValue(paramIndex, mockedInstance);
                mockedType.providedValue = mockedInstance;
            }
        }
    }

    @Nonnull
    private Object instantiateMockedType(@Nonnull MockedType mockedType, @Nonnull InstanceFactory instanceFactory) {
        Object mock = instanceFactory.create();
        registerMock(mockedType, mock);

        if (mockedType.withInstancesToCapture()) {
            registerCaptureOfNewInstances(mockedType, mock);
        }

        return mock;
    }

    @Nonnull
    public List<MockedType> getInjectableParameters() {
        return injectableParameters;
    }
}
