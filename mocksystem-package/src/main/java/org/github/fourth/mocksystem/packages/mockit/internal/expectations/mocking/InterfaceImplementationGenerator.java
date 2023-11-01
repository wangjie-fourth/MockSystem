/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking;

import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader;
import org.github.fourth.mocksystem.packages.mockit.internal.classGeneration.BaseImplementationGenerator;
import org.github.fourth.mocksystem.packages.mockit.internal.classGeneration.MockedTypeInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;

import static org.github.fourth.mocksystem.packages.mockit.external.asm.Opcodes.ACC_PUBLIC;
import static org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking.MockedTypeModifier.generateDirectCallToHandler;

final class InterfaceImplementationGenerator extends BaseImplementationGenerator {
    @Nonnull
    private final MockedTypeInfo mockedTypeInfo;
    private String interfaceName;

    InterfaceImplementationGenerator(
            @Nonnull ClassReader classReader, @Nonnull Type mockedType, @Nonnull String implementationClassName) {
        super(classReader, implementationClassName);
        mockedTypeInfo = new MockedTypeInfo(mockedType);
    }

    @Override
    public void visit(
            int version, int access, @Nonnull String name, @Nullable String signature, @Nullable String superName,
            @Nullable String[] interfaces) {
        interfaceName = name;
        String classSignature = signature == null ? null : signature + mockedTypeInfo.implementationSignature;
        super.visit(version, access, name, classSignature, superName, interfaces);
    }

    @Override
    protected void generateMethodBody(
            int access, @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nullable String[] exceptions) {
        mw = cw.visitMethod(ACC_PUBLIC, name, desc, signature, exceptions);

        String className = null;

        if (signature != null) {
            String subInterfaceOverride = getSubInterfaceOverride(mockedTypeInfo.genericTypeMap, name, signature);

            if (subInterfaceOverride != null) {
                className = interfaceName;
                desc = subInterfaceOverride.substring(name.length());
                signature = null;
            }
        }

        if (className == null) {
            className = isOverrideOfMethodFromSuperInterface(name, desc) ? interfaceName : methodOwner;
        }

        generateDirectCallToHandler(mw, className, access, name, desc, signature);
        generateReturnWithObjectAtTopOfTheStack(desc);
        mw.visitMaxs(1, 0);
    }
}
