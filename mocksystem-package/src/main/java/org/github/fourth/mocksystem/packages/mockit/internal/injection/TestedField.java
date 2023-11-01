/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.injection;

import org.github.fourth.mocksystem.packages.mockit.Tested;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;

import static java.lang.reflect.Modifier.isFinal;
import static org.github.fourth.mocksystem.packages.mockit.internal.reflection.FieldReflection.getFieldValue;
import static org.github.fourth.mocksystem.packages.mockit.internal.reflection.FieldReflection.setFieldValue;

final class TestedField extends TestedObject {
    @Nonnull
    private final Field testedField;

    TestedField(@Nonnull InjectionState injectionState, @Nonnull Field field, @Nonnull Tested metadata) {
        super(injectionState, metadata, field.getName(), field.getGenericType(), field.getType());
        testedField = field;
    }

    @Override
    boolean alreadyInstantiated(@Nonnull Object testClassInstance) {
        return isAvailableDuringSetup() && getFieldValue(testedField, testClassInstance) != null;
    }

    @Nullable
    @Override
    Object getExistingTestedInstanceIfApplicable(@Nonnull Object testClassInstance) {
        Object testedObject = null;

        if (!createAutomatically) {
            testedObject = getFieldValue(testedField, testClassInstance);
            createAutomatically = testedObject == null && !isFinal(testedField.getModifiers());
        }

        return testedObject;
    }

    @Override
    void setInstance(@Nonnull Object testClassInstance, @Nullable Object testedInstance) {
        setFieldValue(testedField, testClassInstance, testedInstance);
    }
}
