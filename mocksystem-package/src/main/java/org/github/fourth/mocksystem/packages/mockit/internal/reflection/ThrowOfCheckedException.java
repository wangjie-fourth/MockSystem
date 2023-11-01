/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.reflection;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nonnull;

@SuppressFBWarnings({"EI_EXPOSE_STATIC_REP2", "NM_CLASS_NOT_EXCEPTION"})
public final class ThrowOfCheckedException {
    private static Exception exceptionToThrow;

    ThrowOfCheckedException() throws Exception {
        throw exceptionToThrow;
    }

    public static synchronized void doThrow(@Nonnull Exception checkedException) {
        exceptionToThrow = checkedException;
        ConstructorReflection.newInstanceUsingDefaultConstructor(ThrowOfCheckedException.class);
    }
}
