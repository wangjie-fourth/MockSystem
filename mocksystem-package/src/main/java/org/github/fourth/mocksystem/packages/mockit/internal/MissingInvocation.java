/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal;

import javax.annotation.Nonnull;

/**
 * Thrown to indicate that one or more expected invocations still had not occurred by the end of the test.
 */
public final class MissingInvocation extends Error {
    public MissingInvocation(@Nonnull String detailMessage) {
        super(detailMessage);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
