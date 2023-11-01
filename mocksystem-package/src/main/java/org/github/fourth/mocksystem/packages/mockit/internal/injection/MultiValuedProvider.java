/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.injection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.github.fourth.mocksystem.packages.mockit.internal.util.Utilities.getClassType;

final class MultiValuedProvider extends InjectionPointProvider {
    @Nonnull
    private final List<InjectionPointProvider> individualProviders;

    MultiValuedProvider(@Nonnull Type elementType) {
        super(elementType, "");
        individualProviders = new ArrayList<InjectionPointProvider>();
    }

    void addInjectable(@Nonnull InjectionPointProvider provider) {
        individualProviders.add(provider);
    }

    @Nonnull
    @Override
    public Class<?> getClassOfDeclaredType() {
        return getClassType(declaredType);
    }

    @Nullable
    @Override
    public Object getValue(@Nullable Object owner) {
        List<Object> values = new ArrayList<Object>(individualProviders.size());

        for (InjectionPointProvider provider : individualProviders) {
            Object value = provider.getValue(owner);
            values.add(value);
        }

        return values;
    }
}
