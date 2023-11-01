/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.injection;

import org.github.fourth.mocksystem.packages.mockit.internal.injection.full.FullInjection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Injector {
    @Nonnull
    public final TestedClass testedClass;
    @Nonnull
    protected final InjectionState injectionState;
    @Nullable
    protected final FullInjection fullInjection;

    protected Injector(
            @Nonnull TestedClass testedClass, @Nonnull InjectionState state, @Nullable FullInjection fullInjection) {
        this.testedClass = testedClass;
        injectionState = state;
        this.fullInjection = fullInjection;
    }

    public void fillOutDependenciesRecursively(@Nonnull Object dependency) {
    }
}
