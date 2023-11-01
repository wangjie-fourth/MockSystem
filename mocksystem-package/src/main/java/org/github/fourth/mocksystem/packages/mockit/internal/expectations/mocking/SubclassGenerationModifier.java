/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking;

import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader;
import org.github.fourth.mocksystem.packages.mockit.internal.classGeneration.BaseSubclassGenerator;
import org.github.fourth.mocksystem.packages.mockit.internal.util.ObjectMethods;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;

import static org.github.fourth.mocksystem.packages.mockit.external.asm.Opcodes.ACC_PUBLIC;
import static org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking.MockedTypeModifier.generateDirectCallToHandler;

public final class SubclassGenerationModifier extends BaseSubclassGenerator {
    public SubclassGenerationModifier(
            @Nonnull Class<?> baseClass, @Nonnull Type mockedType,
            @Nonnull ClassReader classReader, @Nonnull String subclassName, boolean copyConstructors) {
        super(baseClass, classReader, mockedType, subclassName, copyConstructors);
    }

    @Override
    @SuppressWarnings("AssignmentToMethodParameter")
    protected void generateMethodImplementation(
            @Nonnull String className, int access, @Nonnull String name, @Nonnull String desc,
            @Nullable String signature, @Nullable String[] exceptions) {
        if (signature != null && mockedTypeInfo != null) {
            signature = mockedTypeInfo.genericTypeMap.resolveReturnType(className, signature);
        }

        mw = cw.visitMethod(ACC_PUBLIC, name, desc, signature, exceptions);

        if (ObjectMethods.isMethodFromObject(name, desc)) {
            generateEmptyImplementation(desc);
        } else {
            generateDirectCallToHandler(mw, className, access, name, desc, signature);
            generateReturnWithObjectAtTopOfTheStack(desc);
            mw.visitMaxs(1, 0);
        }
    }
}
