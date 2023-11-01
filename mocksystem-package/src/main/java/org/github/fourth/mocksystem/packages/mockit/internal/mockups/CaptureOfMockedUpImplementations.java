/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.mockups;

import org.github.fourth.mocksystem.packages.mockit.MockUp;
import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader;
import org.github.fourth.mocksystem.packages.mockit.internal.BaseClassModifier;
import org.github.fourth.mocksystem.packages.mockit.internal.capturing.CaptureOfImplementations;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;

import static org.github.fourth.mocksystem.packages.mockit.internal.util.Utilities.getClassType;

public final class CaptureOfMockedUpImplementations extends CaptureOfImplementations<Void> {
    private final MockClassSetup mockClassSetup;

    public CaptureOfMockedUpImplementations(@Nonnull MockUp<?> mockUp, @Nonnull Type baseType) {
        Class<?> baseClassType = getClassType(baseType);
        mockClassSetup = new MockClassSetup(baseClassType, baseType, mockUp, null);
    }

    @Nonnull
    @Override
    protected BaseClassModifier createModifier(
            @Nullable ClassLoader cl, @Nonnull ClassReader cr, @Nonnull Class<?> baseType, Void typeMetadata) {
        return mockClassSetup.createClassModifier(cr);
    }

    @Override
    protected void redefineClass(@Nonnull Class<?> realClass, @Nonnull byte[] modifiedClass) {
        mockClassSetup.applyClassModifications(realClass, modifiedClass);
    }

    @Nullable
    public <T> Class<T> apply() {
        @SuppressWarnings("unchecked") Class<T> baseType = (Class<T>) mockClassSetup.realClass;
        Class<T> baseClassType = baseType;
        Class<T> mockedClass = null;

        if (baseType.isInterface()) {
            mockedClass = new MockedImplementationClass<T>(mockClassSetup.mockUp).createImplementation(baseType);
            baseClassType = mockedClass;
        }

        if (baseClassType != Object.class) {
            redefineClass(baseClassType, baseType, null);
        }

        makeSureAllSubtypesAreModified(baseType, false, null);
        return mockedClass;
    }
}
