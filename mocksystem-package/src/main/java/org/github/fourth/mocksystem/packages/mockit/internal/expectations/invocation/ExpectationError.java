/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.invocation;

import org.github.fourth.mocksystem.packages.mockit.internal.util.StackTrace;

import javax.annotation.Nonnull;

final class ExpectationError extends AssertionError {
    private String message;

    @Override
    @Nonnull
    public String toString() {
        return message;
    }

    void prepareForDisplay(@Nonnull String title) {
        message = title;
        StackTrace.filterStackTrace(this);
    }

    void defineCause(@Nonnull String title, @Nonnull Throwable error) {
        prepareForDisplay(title);
        error.initCause(this);
    }
}
