/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.coverage.reporting;

import org.github.fourth.mocksystem.packages.mockit.coverage.data.CoverageData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class BasicCoverageReport extends CoverageReport {
    public BasicCoverageReport(
            @Nonnull String outputDir, boolean outputDirCreated, @Nullable String[] sourceDirs,
            @Nonnull CoverageData coverageData) {
        super(outputDir, outputDirCreated, sourceDirs, coverageData, false);
    }
}
