/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.injection.field;

import org.github.fourth.mocksystem.packages.mockit.internal.injection.InjectionPointProvider;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

final class FieldToInject extends InjectionPointProvider {
    @Nonnull
    private final Field targetField;

    FieldToInject(@Nonnull Field targetField) {
        super(targetField.getGenericType(), targetField.getName());
        this.targetField = targetField;
    }

    @Nonnull
    @Override
    public Class<?> getClassOfDeclaredType() {
        return targetField.getType();
    }

    @Nonnull
    @Override
    public Annotation[] getAnnotations() {
        return targetField.getDeclaredAnnotations();
    }

    @Override
    public String toString() {
        return "field " + super.toString();
    }
}
