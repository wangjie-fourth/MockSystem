/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.capturing;

import org.github.fourth.mocksystem.packages.mockit.internal.util.ClassLoad;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.ProtectionDomain;

import static org.github.fourth.mocksystem.packages.mockit.internal.util.GeneratedClasses.isGeneratedClass;

final class CapturedType {
    private static final ProtectionDomain JMOCKIT_DOMAIN = CapturedType.class.getProtectionDomain();

    @Nonnull
    final Class<?> baseType;

    CapturedType(@Nonnull Class<?> baseType) {
        this.baseType = baseType;
    }

    @SuppressWarnings("UnnecessaryFullyQualifiedName")
    boolean isToBeCaptured(@Nonnull Class<?> aClass) {
        if (baseType == Object.class) {
            if (aClass.isArray()
                    || org.github.fourth.mocksystem.packages.mockit.MockUp.class.isAssignableFrom(aClass)
                    || org.github.fourth.mocksystem.packages.mockit.Delegate.class.isAssignableFrom(aClass)
                    || org.github.fourth.mocksystem.packages.mockit.Expectations.class.isAssignableFrom(aClass)
                    || org.github.fourth.mocksystem.packages.mockit.Verifications.class.isAssignableFrom(aClass)) {
                return false;
            }
        } else if (aClass == baseType || !baseType.isAssignableFrom(aClass)) {
            return false;
        }

        return !aClass.isInterface()
                && !isNotToBeCaptured(aClass.getClassLoader(), aClass.getProtectionDomain(), aClass.getName());
    }

    static boolean isNotToBeCaptured(
            @Nullable ClassLoader loader, @Nullable ProtectionDomain protectionDomain, @Nonnull String classNameOrDesc) {
        //noinspection SimplifiableIfStatement
        if (
                loader == null && classNameOrDesc.startsWith("java") ||
                        protectionDomain == JMOCKIT_DOMAIN || isGeneratedClass(classNameOrDesc)
        ) {
            return true;
        }

        return
                classNameOrDesc.endsWith("Test") ||
                        classNameOrDesc.startsWith("junit") ||
                        classNameOrDesc.startsWith("sun") && !hasSubPackage(classNameOrDesc, 4, "management") ||
                        classNameOrDesc.startsWith("org") && (
                                hasSubPackage(classNameOrDesc, 4, "junit") || hasSubPackage(classNameOrDesc, 4, "testng") ||
                                        hasSubPackage(classNameOrDesc, 4, "hamcrest")
                        ) ||
                        classNameOrDesc.startsWith("com") && (
                                hasSubPackage(classNameOrDesc, 4, "sun") && !hasSubPackage(classNameOrDesc, 8, "proxy") ||
                                        hasSubPackage(classNameOrDesc, 4, "intellij")
                        ) ||
                        ClassLoad.isGeneratedSubclass(classNameOrDesc);
    }

    private static boolean hasSubPackage(@Nonnull String nameOrDesc, int offset, @Nonnull String subPackage) {
        return nameOrDesc.regionMatches(offset, subPackage, 0, subPackage.length());
    }
}
