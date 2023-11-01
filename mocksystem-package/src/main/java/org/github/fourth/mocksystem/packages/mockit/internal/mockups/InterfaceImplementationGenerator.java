/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.mockups;

import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader;
import org.github.fourth.mocksystem.packages.mockit.internal.classGeneration.BaseImplementationGenerator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.github.fourth.mocksystem.packages.mockit.external.asm.Opcodes.ACC_PUBLIC;

public final class InterfaceImplementationGenerator extends BaseImplementationGenerator {
    public InterfaceImplementationGenerator(@Nonnull ClassReader classReader, @Nonnull String implementationClassName) {
        super(classReader, implementationClassName);
    }

    @Override
    protected void generateMethodBody(
            int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions) {
        mw = cw.visitMethod(ACC_PUBLIC, name, desc, signature, exceptions);
        generateEmptyImplementation(desc);
    }
}
