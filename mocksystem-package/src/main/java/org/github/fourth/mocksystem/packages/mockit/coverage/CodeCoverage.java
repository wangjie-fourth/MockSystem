/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.coverage;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.github.fourth.mocksystem.packages.mockit.coverage.data.CoverageData;
import org.github.fourth.mocksystem.packages.mockit.coverage.modification.ClassModification;
import org.github.fourth.mocksystem.packages.mockit.internal.startup.AgentLoader;
import org.github.fourth.mocksystem.packages.mockit.internal.startup.Startup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

@SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
public final class CodeCoverage implements ClassFileTransformer {
    private static CodeCoverage instance;
    private static boolean inATestRun = true;

    @Nonnull
    private final ClassModification classModification;
    @Nonnull
    private final OutputFileGenerator outputGenerator;
    private boolean outputPendingForShutdown;
    private boolean inactive;

    public static void main(@Nonnull String[] args) {
        if (args.length == 1) {
            String pid = args[0];

            try {
                //noinspection ResultOfMethodCallIgnored
                Integer.parseInt(pid);
                new AgentLoader(pid).loadAgent("coverage");
                return;
            } catch (NumberFormatException ignore) {
            }
        }

        OutputFileGenerator generator = createOutputFileGenerator(null);
        generator.generateAggregateReportFromInputFiles(args);
    }

    @Nonnull
    private static OutputFileGenerator createOutputFileGenerator(@Nullable ClassModification classModification) {
        OutputFileGenerator generator = new OutputFileGenerator(classModification);
        CoverageData.instance().setWithCallPoints(generator.isWithCallPoints());
        return generator;
    }

    public static boolean isTestRun() {
        return inATestRun;
    }

    public CodeCoverage() {
        classModification = new ClassModification();
        outputGenerator = createOutputFileGenerator(classModification);
        outputPendingForShutdown = true;
        instance = this;

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                TestRun.terminate();

                if (outputPendingForShutdown) {
                    if (outputGenerator.isOutputToBeGenerated()) {
                        outputGenerator.generate(CodeCoverage.this);
                    }

                    new CoverageCheck().verifyThresholds();
                }

                Startup.instrumentation().removeTransformer(CodeCoverage.this);
            }
        });
    }

    public static boolean active() {
        String coverageOutput = Configuration.getProperty("output");
        String coverageClasses = Configuration.getProperty("classes");
        String coverageMetrics = Configuration.getProperty("metrics");

        return
                (coverageOutput != null || coverageClasses != null || coverageMetrics != null) &&
                        !("none".equals(coverageOutput) || "none".equals(coverageClasses) || "none".equals(coverageMetrics));
    }

    @Nonnull
    public static CodeCoverage create(boolean standalone, boolean generateOutputOnShutdown) {
        inATestRun = !standalone;
        instance = new CodeCoverage();
        instance.outputPendingForShutdown = generateOutputOnShutdown;
        return instance;
    }

    public static void resetConfiguration() {
        Startup.instrumentation().removeTransformer(instance);
        CoverageData.instance().clear();

        CodeCoverage coverage = create(true, false);
        Startup.instrumentation().addTransformer(coverage);
        instance.outputPendingForShutdown = false;
    }

    public static void generateOutput(boolean resetState) {
        instance.outputGenerator.generate(null);
        instance.outputPendingForShutdown = false;

        if (resetState) {
            CoverageData.instance().reset();
        }
    }

    @Nullable
    @Override
    public byte[] transform(
            @Nullable ClassLoader loader, @Nonnull String internalClassName, @Nullable Class<?> classBeingRedefined,
            @Nullable ProtectionDomain protectionDomain, @Nonnull byte[] originalClassfile) {
        if (loader == null || classBeingRedefined != null || protectionDomain == null || inactive) {
            return null;
        }

        String className = internalClassName.replace('/', '.');

        byte[] modifiedClassfile = classModification.modifyClass(className, protectionDomain, originalClassfile);
        return modifiedClassfile;
    }

    void deactivate() {
        inactive = true;
    }
}
