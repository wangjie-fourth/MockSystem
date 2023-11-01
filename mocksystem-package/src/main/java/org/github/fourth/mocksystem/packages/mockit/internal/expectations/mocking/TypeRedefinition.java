/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class TypeRedefinition extends BaseTypeRedefinition {
    TypeRedefinition(@Nonnull MockedType typeMetadata) {
        super(typeMetadata);
    }

    @Nullable
    final InstanceFactory redefineType() {
        //noinspection ConstantConditions
        return redefineType(typeMetadata.getDeclaredType());
    }
}
