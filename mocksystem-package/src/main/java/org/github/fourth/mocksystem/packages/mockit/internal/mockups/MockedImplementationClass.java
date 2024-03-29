/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.mockups;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.github.fourth.mocksystem.packages.mockit.MockUp;
import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader;
import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassVisitor;
import org.github.fourth.mocksystem.packages.mockit.internal.classGeneration.ImplementationClass;
import org.github.fourth.mocksystem.packages.mockit.internal.util.Utilities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

import static java.lang.reflect.Modifier.isPublic;

@SuppressFBWarnings({"EI_EXPOSE_REP2"})
public final class MockedImplementationClass<T> {
    private static final ClassLoader THIS_CL = MockedImplementationClass.class.getClassLoader();

    @Nonnull
    private final MockUp<?> mockUpInstance;
    @Nullable
    private ImplementationClass<T> implementationClass;
    private Class<T> generatedClass;

    public MockedImplementationClass(@Nonnull MockUp<?> mockUpInstance) {
        this.mockUpInstance = mockUpInstance;
    }

    @Nonnull
    public Class<T> createImplementation(@Nonnull Class<T> interfaceToBeMocked, @Nullable Type typeToMock) {
        createImplementation(interfaceToBeMocked);
        byte[] generatedBytecode = implementationClass == null ? null : implementationClass.getGeneratedBytecode();

        MockClassSetup mockClassSetup = new MockClassSetup(generatedClass, typeToMock, mockUpInstance, generatedBytecode);
        mockClassSetup.redefineMethodsInGeneratedClass();

        return generatedClass;
    }

    @Nonnull
    Class<T> createImplementation(@Nonnull Class<T> interfaceToBeMocked) {
        if (isPublic(interfaceToBeMocked.getModifiers())) {
            generateImplementationForPublicInterface(interfaceToBeMocked);
        } else {
            //noinspection unchecked
            generatedClass = (Class<T>) Proxy.getProxyClass(interfaceToBeMocked.getClassLoader(), interfaceToBeMocked);
        }

        return generatedClass;
    }

    private void generateImplementationForPublicInterface(@Nonnull Class<T> interfaceToBeMocked) {
        implementationClass = new ImplementationClass<T>(interfaceToBeMocked) {
            @Nonnull
            @Override
            protected ClassVisitor createMethodBodyGenerator(@Nonnull ClassReader typeReader) {
                return new InterfaceImplementationGenerator(typeReader, generatedClassName);
            }
        };

        generatedClass = implementationClass.generateClass();
    }

    @Nonnull
    public Class<T> createImplementation(@Nonnull Type[] interfacesToBeMocked) {
        Class<?>[] interfacesToMock = new Class<?>[interfacesToBeMocked.length];

        for (int i = 0; i < interfacesToMock.length; i++) {
            interfacesToMock[i] = Utilities.getClassType(interfacesToBeMocked[i]);
        }

        //noinspection unchecked
        generatedClass = (Class<T>) Proxy.getProxyClass(THIS_CL, interfacesToMock);
        new MockClassSetup(generatedClass, null, mockUpInstance, null).redefineMethods();

        return generatedClass;
    }
}
