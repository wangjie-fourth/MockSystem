/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking;

import org.github.fourth.mocksystem.packages.mockit.internal.reflection.FieldReflection;
import org.github.fourth.mocksystem.packages.mockit.internal.state.TestRun;
import org.github.fourth.mocksystem.packages.mockit.internal.util.StackTrace;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.lang.reflect.Modifier.isFinal;
import static org.github.fourth.mocksystem.packages.mockit.external.asm.Opcodes.ACC_STATIC;
import static org.github.fourth.mocksystem.packages.mockit.external.asm.Opcodes.ACC_SYNTHETIC;

@SuppressWarnings("UnnecessaryFullyQualifiedName")
public final class FieldTypeRedefinitions extends TypeRedefinitions {
    private static final int FIELD_ACCESS_MASK = ACC_SYNTHETIC + ACC_STATIC;

    @Nonnull
    private final Map<MockedType, InstanceFactory> mockInstanceFactories;
    @Nonnull
    private final List<MockedType> mockFieldsNotSet;

    public FieldTypeRedefinitions(@Nonnull Class<?> testClass) {
        mockInstanceFactories = new HashMap<MockedType, InstanceFactory>();
        mockFieldsNotSet = new ArrayList<MockedType>();

        TestRun.enterNoMockingZone();

        try {
            redefineFieldTypes(testClass);
        } finally {
            TestRun.exitNoMockingZone();
        }
    }

    private void redefineFieldTypes(@Nonnull Class<?> classWithMockFields) {
        Class<?> superClass = classWithMockFields.getSuperclass();

        if (superClass != null
                && superClass != Object.class
                && superClass != org.github.fourth.mocksystem.packages.mockit.Expectations.class
                && superClass != org.github.fourth.mocksystem.packages.mockit.StrictExpectations.class) {
            redefineFieldTypes(superClass);
        }

        Field[] fields = classWithMockFields.getDeclaredFields();

        for (Field candidateField : fields) {
            int fieldModifiers = candidateField.getModifiers();

            if ((fieldModifiers & FIELD_ACCESS_MASK) == 0) {
                redefineFieldType(candidateField, fieldModifiers);
            }
        }
    }

    private void redefineFieldType(@Nonnull Field field, int modifiers) {
        MockedType mockedType = new MockedType(field);

        if (mockedType.isMockableType()) {
            boolean partialMocking = field.isAnnotationPresent(org.github.fourth.mocksystem.packages.mockit.Tested.class);
            boolean needsValueToSet = !isFinal(modifiers) && !partialMocking;

            redefineFieldType(mockedType, partialMocking, needsValueToSet);

            if (!partialMocking) {
                registerCaptureOfNewInstances(mockedType);
            }
        }
    }

    private void redefineFieldType(@Nonnull MockedType mockedType, boolean partialMocking, boolean needsValueToSet) {
        FieldTypeRedefinition typeRedefinition = new FieldTypeRedefinition(mockedType);
        boolean redefined;

        if (needsValueToSet) {
            InstanceFactory factory = typeRedefinition.redefineType();
            redefined = factory != null;

            if (redefined) {
                mockInstanceFactories.put(mockedType, factory);
            }
        } else {
            if (partialMocking) {
                redefined = typeRedefinition.redefineTypeForTestedField();
            } else {
                redefined = typeRedefinition.redefineTypeForFinalField();
            }

            if (redefined) {
                mockFieldsNotSet.add(mockedType);
            }
        }

        if (redefined) {
            addTargetClass(mockedType);
        }
    }

    private void registerCaptureOfNewInstances(@Nonnull MockedType mockedType) {
        if (mockedType.getMaxInstancesToCapture() > 0) {
            if (captureOfNewInstances == null) {
                captureOfNewInstances = new CaptureOfNewInstancesForFields();
            }

            captureOfNewInstances.registerCaptureOfNewInstances(mockedType, null);
        }
    }

    public void assignNewInstancesToMockFields(@Nonnull Object target) {
        TestRun.getExecutingTest().clearInjectableAndNonStrictMocks();
        createAndAssignNewInstances(target);
        obtainAndRegisterInstancesOfFieldsNotSet(target);
    }

    private void createAndAssignNewInstances(@Nonnull Object target) {
        for (Entry<MockedType, InstanceFactory> metadataAndFactory : mockInstanceFactories.entrySet()) {
            MockedType mockedType = metadataAndFactory.getKey();
            InstanceFactory instanceFactory = metadataAndFactory.getValue();

            Object mock = assignNewInstanceToMockField(target, mockedType, instanceFactory);
            registerMock(mockedType, mock);
        }
    }

    @Nonnull
    private Object assignNewInstanceToMockField(
            @Nonnull Object target, @Nonnull MockedType mockedType, @Nonnull InstanceFactory instanceFactory) {
        Field mockField = mockedType.field;
        assert mockField != null;
        Object mock = FieldReflection.getFieldValue(mockField, target);

        if (mock == null) {
            try {
                mock = instanceFactory.create();
            } catch (NoClassDefFoundError e) {
                StackTrace.filterStackTrace(e);
                e.printStackTrace();
                throw e;
            } catch (ExceptionInInitializerError e) {
                StackTrace.filterStackTrace(e);
                e.printStackTrace();
                throw e;
            }

            FieldReflection.setFieldValue(mockField, target, mock);

            if (mockedType.getMaxInstancesToCapture() > 0) {
                assert captureOfNewInstances != null;
                CaptureOfNewInstancesForFields capture = (CaptureOfNewInstancesForFields) captureOfNewInstances;
                capture.resetCaptureCount(mockField);
            }
        }

        return mock;
    }

    private void obtainAndRegisterInstancesOfFieldsNotSet(@Nonnull Object target) {
        for (MockedType metadata : mockFieldsNotSet) {
            assert metadata.field != null;
            Object mock = FieldReflection.getFieldValue(metadata.field, target);

            if (mock != null) {
                registerMock(metadata, mock);
            }
        }
    }

    /**
     * Returns true iff the mock instance concrete class is not mocked in some test, ie it's a class
     * which only appears in the code under test.
     */
    public boolean captureNewInstanceForApplicableMockField(@Nonnull Object mock) {
        if (captureOfNewInstances == null) {
            return false;
        }

        Object fieldOwner = TestRun.getCurrentTestInstance();
        return captureOfNewInstances.captureNewInstance(fieldOwner, mock);
    }

    @Override
    public void cleanUp() {
        TestRun.getExecutingTest().getCascadingTypes().clear();
        super.cleanUp();
    }
}
