/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal;

import javax.annotation.Nonnull;

/**
 * Thrown to indicate that one or more unexpected invocations occurred during the test.
 */
public final class UnexpectedInvocation extends Error {
    public UnexpectedInvocation(@Nonnull String detailMessage) {
        super(detailMessage);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
