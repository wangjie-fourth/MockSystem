/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.state;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.github.fourth.mocksystem.packages.mockit.internal.util.Utilities.getClassType;

public final class CascadingTypes {
    @Nonnull
    private final Map<Type, MockedTypeCascade> mockedTypesToCascades;

    CascadingTypes() {
        mockedTypesToCascades = new ConcurrentHashMap<Type, MockedTypeCascade>(4);
    }

    public void add(boolean fromMockField, @Nonnull Type mockedType) {
        Class<?> mockedClass = getClassType(mockedType);
        String mockedTypeDesc = org.github.fourth.mocksystem.packages.mockit.external.asm.Type.getInternalName(mockedClass);
        add(mockedTypeDesc, fromMockField, mockedType);
    }

    @Nonnull
    MockedTypeCascade add(@Nonnull String mockedTypeDesc, boolean fromMockField, @Nonnull Type mockedType) {
        MockedTypeCascade cascade = mockedTypesToCascades.get(mockedType);

        if (cascade == null) {
            cascade = new MockedTypeCascade(fromMockField, mockedType, mockedTypeDesc);
            mockedTypesToCascades.put(mockedType, cascade);
        }

        return cascade;
    }

    @Nonnull
    MockedTypeCascade getCascade(@Nonnull Type mockedType) {
        return mockedTypesToCascades.get(mockedType);
    }

    @Nullable
    public MockedTypeCascade getCascade(@Nonnull String mockedTypeDesc, @Nullable Object mockInstance) {
        if (mockedTypesToCascades.isEmpty()) {
            return null;
        }

        if (mockInstance != null) {
            MockedTypeCascade cascade = findCascadeForInstance(mockInstance);

            if (cascade != null) {
                return cascade;
            }
        }

        for (MockedTypeCascade cascade : mockedTypesToCascades.values()) {
            if (cascade.mockedTypeDesc.equals(mockedTypeDesc)) {
                return cascade;
            }
        }

        return null;
    }

    @Nullable
    private MockedTypeCascade findCascadeForInstance(@Nonnull Object mockInstance) {
        for (MockedTypeCascade cascade : mockedTypesToCascades.values()) {
            if (cascade.hasInstance(mockInstance)) {
                return cascade;
            }
        }

        return null;
    }

    void clearNonSharedCascadingTypes() {
        if (!mockedTypesToCascades.isEmpty()) {
            Iterator<MockedTypeCascade> itr = mockedTypesToCascades.values().iterator();

            while (itr.hasNext()) {
                MockedTypeCascade cascade = itr.next();

                if (cascade.fromMockField) {
                    cascade.discardCascadedMocks();
                } else {
                    itr.remove();
                }
            }
        }
    }

    public void clear() {
        mockedTypesToCascades.clear();
    }

    public void addInstance(@Nonnull Type mockedType, @Nonnull Object cascadingInstance) {
        MockedTypeCascade cascade = mockedTypesToCascades.get(mockedType);

        if (cascade != null) {
            cascade.addInstance(cascadingInstance);
        }
    }
}
