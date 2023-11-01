/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.injection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface BeanExporter {
    @Nullable
    Object getBean(@Nonnull String name);
}
