/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.coverage.data;

import java.io.Serializable;

public interface PerFileCoverage extends Serializable {
    int getTotalItems();

    int getCoveredItems();

    int getCoveragePercentage();
}
