/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.argumentMatching;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.regex.Pattern;

public final class PatternMatcher implements ArgumentMatcher<PatternMatcher> {
    @Nonnull
    private final Pattern pattern;

    public PatternMatcher(@Nonnull String regex) {
        pattern = Pattern.compile(regex);
    }

    @Override
    public boolean same(@Nonnull PatternMatcher other) {
        return pattern == other.pattern;
    }

    @Override
    public boolean matches(@Nullable Object argValue) {
        return argValue instanceof CharSequence && pattern.matcher((CharSequence) argValue).matches();
    }

    @Override
    public void writeMismatchPhrase(@Nonnull ArgumentMismatch argumentMismatch) {
        argumentMismatch.append("a string matching \"").append(pattern.toString()).append('"');
    }
}
