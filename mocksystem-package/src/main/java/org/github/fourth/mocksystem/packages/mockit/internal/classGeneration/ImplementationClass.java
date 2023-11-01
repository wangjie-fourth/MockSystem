/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.classGeneration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader;
import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassVisitor;
import org.github.fourth.mocksystem.packages.mockit.internal.ClassFile;
import org.github.fourth.mocksystem.packages.mockit.internal.util.ClassLoad;
import org.github.fourth.mocksystem.packages.mockit.internal.util.GeneratedClasses;
import org.github.fourth.mocksystem.packages.mockit.internal.util.Utilities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;

import static org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader.SKIP_FRAMES;

/**
 * Allows the creation of new implementation classes for interfaces and abstract classes.
 */
@SuppressFBWarnings("EI_EXPOSE_REP")
public abstract class ImplementationClass<T> {
    @Nonnull
    protected final Class<?> sourceClass;
    @Nonnull
    protected String generatedClassName;
    @Nullable
    private byte[] generatedBytecode;

    protected ImplementationClass(@Nonnull Type mockedType) {
        this(Utilities.getClassType(mockedType));
    }

    protected ImplementationClass(@Nonnull Class<?> mockedClass) {
        this(mockedClass, GeneratedClasses.getNameForGeneratedClass(mockedClass, null));
    }

    protected ImplementationClass(@Nonnull Class<?> sourceClass, @Nonnull String desiredClassName) {
        this.sourceClass = sourceClass;
        generatedClassName = desiredClassName;
    }

    @Nonnull
    public final Class<T> generateClass() {
        ClassReader classReader = ClassFile.createReaderOrGetFromCache(sourceClass);

        ClassVisitor modifier = createMethodBodyGenerator(classReader);
        classReader.accept(modifier, SKIP_FRAMES);

        return defineNewClass(modifier);
    }

    @Nonnull
    protected abstract ClassVisitor createMethodBodyGenerator(@Nonnull ClassReader typeReader);

    @Nonnull
    private Class<T> defineNewClass(@Nonnull ClassVisitor modifier) {
        final ClassLoader parentLoader = ClassLoad.getClassLoaderWithAccess(sourceClass);
        final byte[] modifiedClassfile = modifier.toByteArray();

        try {
            @SuppressWarnings("unchecked")
            Class<T> generatedClass = (Class<T>) new ClassLoader(parentLoader) {
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    if (!name.equals(generatedClassName)) {
                        return parentLoader.loadClass(name);
                    }

                    return defineClass(name, modifiedClassfile, 0, modifiedClassfile.length);
                }
            }.findClass(generatedClassName);

            generatedBytecode = modifiedClassfile;
            return generatedClass;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to define class: " + generatedClassName, e);
        }
    }

    @Nullable
    public final byte[] getGeneratedBytecode() {
        return generatedBytecode;
    }
}
