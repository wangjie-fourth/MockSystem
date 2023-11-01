/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.startup;

import org.github.fourth.mocksystem.packages.mockit.MockUp;
import org.github.fourth.mocksystem.packages.mockit.coverage.CodeCoverage;
import org.github.fourth.mocksystem.packages.mockit.internal.reflection.ConstructorReflection;
import org.github.fourth.mocksystem.packages.mockit.internal.util.ClassLoad;
import org.github.fourth.mocksystem.packages.mockit.internal.util.DefaultValues;
import org.github.fourth.mocksystem.packages.mockit.internal.util.StackTrace;
import org.github.fourth.mocksystem.packages.mockit.internal.util.Utilities;

import javax.annotation.Nonnull;
import java.lang.instrument.Instrumentation;
import java.util.Currency;
import java.util.Locale;
import java.util.TimeZone;

final class JMockitInitialization {
    @Nonnull
    private final StartupConfiguration config;

    JMockitInitialization() {
        config = new StartupConfiguration();
    }

    void initialize(@Nonnull Instrumentation inst) {
        preventEventualClassLoadingConflicts();
        applyInternalStartupMocksAsNeeded();

        if (CodeCoverage.active()) {
            inst.addTransformer(new CodeCoverage());
        }

        applyUserSpecifiedStartupMocksIfAny();
    }

    private static void preventEventualClassLoadingConflicts() {
        // Ensure the proper loading of data files by the JRE, whose names depend on calls to the System class,
        // which may get @Mocked.
        TimeZone.getDefault();
        Currency.getInstance(Locale.getDefault());

        DefaultValues.computeForReturnType("()J");
        Utilities.calledFromSpecialThread();
    }

    private void applyInternalStartupMocksAsNeeded() {
//      if (MockFrameworkMethod.hasDependenciesInClasspath()) {
//         new RunNotifierDecorator();
//         new MockFrameworkMethod();
//      }
    }

    private void applyUserSpecifiedStartupMocksIfAny() {
        for (String mockClassName : config.mockClasses) {
            applyStartupMock(mockClassName);
        }
    }

    private static void applyStartupMock(@Nonnull String mockClassName) {
        String argument = null;
        int p = mockClassName.indexOf('=');

        if (p > 0) {
            argument = mockClassName.substring(p + 1);
            mockClassName = mockClassName.substring(0, p);
        }

        try {
            Class<?> mockClass = ClassLoad.loadClassAtStartup(mockClassName);

            if (MockUp.class.isAssignableFrom(mockClass)) {
                if (argument == null) {
                    ConstructorReflection.newInstanceUsingDefaultConstructor(mockClass);
                } else {
                    ConstructorReflection.newInstance(mockClass, argument);
                }
            }
        } catch (UnsupportedOperationException ignored) {
        } catch (Throwable unexpectedFailure) {
            StackTrace.filterStackTrace(unexpectedFailure);
            unexpectedFailure.printStackTrace();
        }
    }
}
