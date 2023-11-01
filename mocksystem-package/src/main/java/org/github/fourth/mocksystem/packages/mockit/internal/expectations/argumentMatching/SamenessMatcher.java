/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.argumentMatching;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SamenessMatcher implements ArgumentMatcher<SamenessMatcher> {
    @Nullable
    private final Object object;

    public SamenessMatcher(@Nullable Object object) {
        this.object = object;
    }

    @Override
    public boolean same(@Nonnull SamenessMatcher other) {
        return object == other.object;
    }

    @Override
    public boolean matches(@Nullable Object argValue) {
        return argValue == object;
    }

    @Override
    public void writeMismatchPhrase(@Nonnull ArgumentMismatch argumentMismatch) {
        argumentMismatch.append("same instance as ").appendFormatted(object);
    }
}
