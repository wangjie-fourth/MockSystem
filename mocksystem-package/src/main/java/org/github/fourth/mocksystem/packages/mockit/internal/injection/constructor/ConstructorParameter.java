/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.injection.constructor;

import org.github.fourth.mocksystem.packages.mockit.internal.injection.InjectionPointProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static org.github.fourth.mocksystem.packages.mockit.internal.util.Utilities.getClassType;

final class ConstructorParameter extends InjectionPointProvider {
    @Nonnull
    private final Class<?> classOfDeclaredType;
    @Nonnull
    private final Annotation[] annotations;
    @Nullable
    private final Object value;

    ConstructorParameter(
            @Nonnull Type declaredType, @Nonnull Annotation[] annotations, @Nonnull String name, @Nullable Object value) {
        super(declaredType, name);
        classOfDeclaredType = getClassType(declaredType);
        this.annotations = annotations;
        this.value = value;
    }

    @Nonnull
    @Override
    public Class<?> getClassOfDeclaredType() {
        return classOfDeclaredType;
    }

    @Nonnull
    @Override
    public Annotation[] getAnnotations() {
        return annotations;
    }

    @Nullable
    @Override
    public Object getValue(@Nullable Object owner) {
        return value;
    }

    @Override
    public String toString() {
        return "parameter " + super.toString();
    }
}
