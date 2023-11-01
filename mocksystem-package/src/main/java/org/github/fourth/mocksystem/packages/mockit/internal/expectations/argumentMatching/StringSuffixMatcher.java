/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.argumentMatching;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class StringSuffixMatcher extends SubstringMatcher {
    public StringSuffixMatcher(@Nonnull CharSequence substring) {
        super(substring);
    }

    @Override
    public boolean matches(@Nullable Object argValue) {
        return argValue instanceof CharSequence && argValue.toString().endsWith(substring);
    }

    @Override
    public void writeMismatchPhrase(@Nonnull ArgumentMismatch argumentMismatch) {
        argumentMismatch.append("a string ending with ").appendFormatted(substring);
    }
}
