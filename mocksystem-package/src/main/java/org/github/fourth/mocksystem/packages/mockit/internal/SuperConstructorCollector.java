/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader;
import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassVisitor;
import org.github.fourth.mocksystem.packages.mockit.external.asm.MethodVisitor;
import org.github.fourth.mocksystem.packages.mockit.internal.util.VisitInterruptedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader.SKIP_DEBUG;
import static org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader.SKIP_FRAMES;

@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
@SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
final class SuperConstructorCollector extends ClassVisitor {
    @Nonnull
    static final SuperConstructorCollector INSTANCE = new SuperConstructorCollector();

    @Nonnull
    private final Map<String, String> cache = new HashMap<String, String>();
    @Nullable
    private String constructorDesc;
    private boolean samePackage;

    private SuperConstructorCollector() {
    }

    @Nonnull
    synchronized String findConstructor(@Nonnull String classDesc, @Nonnull String superClassDesc) {
        constructorDesc = cache.get(superClassDesc);

        if (constructorDesc != null) {
            return constructorDesc;
        }

        findIfBothClassesAreInSamePackage(classDesc, superClassDesc);

        ClassReader cr = ClassFile.readFromFile(superClassDesc);
        try {
            cr.accept(this, SKIP_DEBUG + SKIP_FRAMES);
        } catch (VisitInterruptedException ignore) {
        }

        cache.put(superClassDesc, constructorDesc);

        return constructorDesc;
    }

    private void findIfBothClassesAreInSamePackage(@Nonnull String classDesc, @Nonnull String superClassDesc) {
        int p1 = classDesc.lastIndexOf('/');
        int p2 = superClassDesc.lastIndexOf('/');
        samePackage = p1 == p2 && (p1 < 0 || classDesc.substring(0, p1).equals(superClassDesc.substring(0, p2)));
    }

    @Override
    @Nullable
    public MethodVisitor visitMethod(
            int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions) {
        if (isAccessible(access) && "<init>".equals(name)) {
            constructorDesc = desc;
            throw VisitInterruptedException.INSTANCE;
        }

        return null;
    }

    private boolean isAccessible(int access) {
        return access != Modifier.PRIVATE && (access != 0 || samePackage);
    }
}
