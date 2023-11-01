/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.argumentMatching;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class InequalityMatcher extends EqualityMatcher {
    public InequalityMatcher(@Nullable Object notEqualArg) {
        super(notEqualArg);
    }

    @Override
    public boolean matches(@Nullable Object argValue) {
        return !super.matches(argValue);
    }

    @Override
    public void writeMismatchPhrase(@Nonnull ArgumentMismatch argumentMismatch) {
        argumentMismatch.append("not ").appendFormatted(object);
    }
}
