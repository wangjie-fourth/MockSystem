/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.mockups;

import org.github.fourth.mocksystem.packages.mockit.Mock;
import org.github.fourth.mocksystem.packages.mockit.MockUp;
import org.github.fourth.mocksystem.packages.mockit.external.asm.*;
import org.github.fourth.mocksystem.packages.mockit.internal.ClassFile;
import org.github.fourth.mocksystem.packages.mockit.internal.mockups.MockMethods.MockMethod;
import org.github.fourth.mocksystem.packages.mockit.internal.state.ParameterNames;
import org.github.fourth.mocksystem.packages.mockit.internal.util.ClassLoad;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader.SKIP_CODE;
import static org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader.SKIP_FRAMES;
import static org.github.fourth.mocksystem.packages.mockit.external.asm.Opcodes.*;

/**
 * Responsible for collecting the signatures of all methods defined in a given mock class which are explicitly annotated
 * as {@link Mock mocks}.
 */
final class MockMethodCollector extends ClassVisitor {
    private static final int INVALID_METHOD_ACCESSES = ACC_BRIDGE + ACC_SYNTHETIC + ACC_ABSTRACT + ACC_NATIVE;

    @Nonnull
    private final MockMethods mockMethods;

    private boolean collectingFromSuperClass;
    @Nullable
    private String enclosingClassDescriptor;

    MockMethodCollector(@Nonnull MockMethods mockMethods) {
        this.mockMethods = mockMethods;
    }

    void collectMockMethods(@Nonnull Class<?> mockClass) {
        ClassLoad.registerLoadedClass(mockClass);

        Class<?> classToCollectMocksFrom = mockClass;

        do {
            ClassReader mcReader = ClassFile.readFromFile(classToCollectMocksFrom);
            mcReader.accept(this, SKIP_CODE + SKIP_FRAMES);
            classToCollectMocksFrom = classToCollectMocksFrom.getSuperclass();
            collectingFromSuperClass = true;
        }
        while (classToCollectMocksFrom != Object.class && classToCollectMocksFrom != MockUp.class);
    }

    @Override
    public void visit(
            int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName,
            @Nullable String[] interfaces) {
        if (!collectingFromSuperClass) {
            mockMethods.setMockClassInternalName(name);

            int p = name.lastIndexOf('$');

            if (p > 0) {
                enclosingClassDescriptor = "(L" + name.substring(0, p) + ";)V";
            }
        }
    }

    /**
     * Adds the method specified to the set of mock methods, as long as it's annotated with {@code @Mock}.
     *
     * @param methodSignature generic signature for a Java 5 generic method, ignored since redefinition only needs to
     *                        consider the "erased" signature
     * @param exceptions      zero or more thrown exceptions in the method "throws" clause, also ignored
     */
    @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
    @Nullable
    @Override
    public MethodVisitor visitMethod(
            int access, @Nonnull String methodName, @Nonnull String methodDesc, String methodSignature, String[] exceptions) {
        if ((access & INVALID_METHOD_ACCESSES) != 0) {
            return null;
        }

        if ("<init>".equals(methodName)) {
            if (!collectingFromSuperClass && methodDesc.equals(enclosingClassDescriptor)) {
                enclosingClassDescriptor = null;
            }

            return null;
        }

        return new MockMethodVisitor(access, methodName, methodDesc);
    }

    private final class MockMethodVisitor extends MethodVisitor {
        private final int access;
        @Nonnull
        private final String methodName;
        @Nonnull
        private final String methodDesc;

        MockMethodVisitor(int access, @Nonnull String methodName, @Nonnull String methodDesc) {
            this.access = access;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
        }

        @Nullable
        @Override
        public AnnotationVisitor visitAnnotation(@Nullable String desc, boolean visible) {
            if ("Lmockit/Mock;".equals(desc)) {
                MockMethod mockMethod = mockMethods.addMethod(collectingFromSuperClass, access, methodName, methodDesc);

                if (mockMethod != null && mockMethod.requiresMockState()) {
                    MockState mockState = new MockState(mockMethod);
                    mockMethods.addMockState(mockState);
                }
            }

            return null;
        }

        @Override
        public void visitLocalVariable(
                @Nonnull String name, @Nonnull String desc, String signature, Label start, Label end, @Nonnegative int index) {
            String classDesc = mockMethods.getMockClassInternalName();
            ParameterNames.registerName(classDesc, access, methodName, methodDesc, desc, name, index);
        }
    }
}
