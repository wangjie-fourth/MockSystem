/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.util;

import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader;
import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassVisitor;
import org.github.fourth.mocksystem.packages.mockit.external.asm.Label;
import org.github.fourth.mocksystem.packages.mockit.external.asm.MethodVisitor;
import org.github.fourth.mocksystem.packages.mockit.internal.ClassFile;
import org.github.fourth.mocksystem.packages.mockit.internal.state.ParameterNames;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader.SKIP_FRAMES;
import static org.github.fourth.mocksystem.packages.mockit.external.asm.Opcodes.ACC_SYNTHETIC;

public final class ParameterNameExtractor extends ClassVisitor {
    @Nonnull
    private String classDesc;
    @Nonnegative
    private int memberAccess;
    @Nonnull
    private String memberName;
    @Nonnull
    private String memberDesc;

    public ParameterNameExtractor() {
        classDesc = memberName = memberDesc = "";
    }

    @Nonnull
    public String extractNames(@Nonnull Class<?> classOfInterest) {
        String className = classOfInterest.getName();
        classDesc = className.replace('.', '/');

        if (!ParameterNames.hasNamesForClass(classDesc)) {
            // Reads class from file, since JRE 1.6 (but not 1.7) discards parameter names on retransformation.
            ClassReader cr = ClassFile.readFromFile(classDesc);
            cr.accept(this, SKIP_FRAMES);
        }

        return classDesc;
    }

    @Nullable
    @Override
    public MethodVisitor visitMethod(
            int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions) {
        if ((access & ACC_SYNTHETIC) == 0) {
            memberAccess = access;
            memberName = name;
            memberDesc = desc;
            return new MethodOrConstructorVisitor();
        }

        return null;
    }

    private final class MethodOrConstructorVisitor extends MethodVisitor {
        @Override
        public void visitLocalVariable(
                @Nonnull String name, @Nonnull String desc, String signature, Label start, Label end, @Nonnegative int index) {
            ParameterNames.registerName(classDesc, memberAccess, memberName, memberDesc, desc, name, index);
        }
    }
}
