/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.argumentMatching;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public final class LenientEqualityMatcher extends EqualityMatcher {
    @Nonnull
    private final Map<Object, Object> instanceMap;

    public LenientEqualityMatcher(@Nullable Object equalArg, @Nonnull Map<Object, Object> instanceMap) {
        super(equalArg);
        this.instanceMap = instanceMap;
    }

    @Override
    public boolean matches(@Nullable Object argValue) {
        if (argValue == null) {
            return object == null;
        } else if (object == null) {
            return false;
        } else if (argValue == object || instanceMap.get(argValue) == object) {
            return true;
        }

        return areEqualWhenNonNull(argValue, object);
    }
}
