/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.transformation;

import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader;
import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassVisitor;
import org.github.fourth.mocksystem.packages.mockit.internal.ClassFile;
import org.github.fourth.mocksystem.packages.mockit.internal.startup.Startup;
import org.github.fourth.mocksystem.packages.mockit.internal.util.ClassNaming;
import org.github.fourth.mocksystem.packages.mockit.internal.util.VisitInterruptedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import static java.lang.reflect.Modifier.isFinal;
import static org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader.SKIP_FRAMES;

public final class ExpectationsTransformer implements ClassFileTransformer {
    @Nonnull
    private final List<String> baseSubclasses;

    public ExpectationsTransformer(@Nonnull Instrumentation instrumentation) {
        baseSubclasses = new ArrayList<String>();
        baseSubclasses.add("mockit/Expectations");
        baseSubclasses.add("mockit/StrictExpectations");
        baseSubclasses.add("mockit/Verifications");
        baseSubclasses.add("mockit/FullVerifications");
        baseSubclasses.add("mockit/VerificationsInOrder");
        baseSubclasses.add("mockit/FullVerificationsInOrder");

        Class<?>[] alreadyLoaded = instrumentation.getAllLoadedClasses();
        findAndModifyOtherBaseSubclasses(alreadyLoaded);
        modifyFinalSubclasses(alreadyLoaded);
    }

    private void findAndModifyOtherBaseSubclasses(@Nonnull Class<?>[] alreadyLoaded) {
        for (Class<?> aClass : alreadyLoaded) {
            if (
                    aClass.getClassLoader() != null && !isFinalClass(aClass) &&
                            isExpectationsOrVerificationsSubclassFromUserCode(aClass)
            ) {
                modifyInvocationsSubclass(aClass, false);
            }
        }
    }

    private static boolean isFinalClass(@Nonnull Class<?> aClass) {
        return isFinal(aClass.getModifiers()) || ClassNaming.isAnonymousClass(aClass);
    }

    private static boolean isExpectationsOrVerificationsSubclassFromUserCode(@Nonnull Class<?> aClass) {
        if (isExpectationsOrVerificationsAPIClass(aClass)) {
            return false;
        }

        Class<?> superclass = aClass.getSuperclass();

        while (superclass != null && superclass != Object.class && superclass.getClassLoader() != null) {
            if (isExpectationsOrVerificationsAPIClass(superclass)) {
                return true;
            }

            superclass = superclass.getSuperclass();
        }

        return false;
    }

    private static boolean isExpectationsOrVerificationsAPIClass(@Nonnull Class<?> aClass) {
        return
                ("mockit.Expectations mockit.StrictExpectations mockit.Verifications mockit.FullVerifications " +
                        "mockit.VerificationsInOrder mockit.FullVerificationsInOrder").contains(aClass.getName());
    }

    private void modifyFinalSubclasses(@Nonnull Class<?>[] alreadyLoaded) {
        for (Class<?> aClass : alreadyLoaded) {
            if (
                    aClass.getClassLoader() != null && isFinalClass(aClass) &&
                            isExpectationsOrVerificationsSubclassFromUserCode(aClass)
            ) {
                modifyInvocationsSubclass(aClass, true);
            }
        }
    }

    private void modifyInvocationsSubclass(@Nonnull Class<?> aClass, boolean isFinalClass) {
        ClassReader cr = ClassFile.createClassFileReader(aClass);
        byte[] modifiedClassfile = modifyInvocationsSubclass(cr, aClass.getClassLoader(), isFinalClass);

        if (modifiedClassfile != null) {
            Startup.redefineMethods(aClass, modifiedClassfile);
        }
    }

    @Nullable
    private byte[] modifyInvocationsSubclass(@Nonnull ClassReader cr, ClassLoader loader, boolean finalClass) {
        ClassVisitor modifier = new EndOfBlockModifier(cr, loader, baseSubclasses, finalClass);

        try {
            cr.accept(modifier, SKIP_FRAMES);
            return modifier.toByteArray();
        } catch (VisitInterruptedException ignore) {
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }

    @Nullable
    @Override
    public byte[] transform(
            @Nullable ClassLoader loader, @Nonnull String className, @Nullable Class<?> classBeingRedefined,
            @Nullable ProtectionDomain protectionDomain, @Nonnull byte[] classfileBuffer) {
        if (classBeingRedefined == null && protectionDomain != null) {
            ClassReader cr = new ClassReader(classfileBuffer);
            String superClassName = cr.getSuperName();

            if (
                    !baseSubclasses.contains(superClassName) &&
                            !superClassName.endsWith("Expectations") && !superClassName.endsWith("Verifications")
            ) {
                return null;
            }

            boolean finalClass = ClassNaming.isAnonymousClass(className);
            return modifyInvocationsSubclass(cr, loader, finalClass);
        }

        return null;
    }
}
