/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.mockups;

import org.github.fourth.mocksystem.packages.mockit.MockUp;
import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader;
import org.github.fourth.mocksystem.packages.mockit.internal.BaseClassModifier;
import org.github.fourth.mocksystem.packages.mockit.internal.ClassFile;
import org.github.fourth.mocksystem.packages.mockit.internal.startup.Startup;
import org.github.fourth.mocksystem.packages.mockit.internal.state.CachedClassfiles;
import org.github.fourth.mocksystem.packages.mockit.internal.state.MockClasses;
import org.github.fourth.mocksystem.packages.mockit.internal.state.TestRun;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import static org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader.SKIP_FRAMES;

public final class MockClassSetup {
    @Nonnull
    final Class<?> realClass;
    @Nullable
    private ClassReader rcReader;
    @Nonnull
    private final MockMethods mockMethods;
    @Nonnull
    final MockUp<?> mockUp;
    private final boolean forStartupMock;

    public MockClassSetup(
            @Nonnull Class<?> realClass, @Nonnull Class<?> classToMock, @Nullable Type mockedType, @Nonnull MockUp<?> mockUp) {
        this(realClass, classToMock, mockedType, mockUp, null);
    }

    public MockClassSetup(
            @Nonnull Class<?> realClass, @Nullable Type mockedType, @Nonnull MockUp<?> mockUp, @Nullable byte[] realClassCode) {
        this(realClass, realClass, mockedType, mockUp, realClassCode);
    }

    private MockClassSetup(
            @Nonnull Class<?> realClass, @Nonnull Class<?> classToMock, @Nullable Type mockedType, @Nonnull MockUp<?> mockUp,
            @Nullable byte[] realClassCode) {
        this.realClass = classToMock;
        mockMethods = new MockMethods(realClass, mockedType);
        this.mockUp = mockUp;
        forStartupMock = Startup.initializing;
        rcReader = realClassCode == null ? null : new ClassReader(realClassCode);

        Class<?> mockUpClass = mockUp.getClass();
        new MockMethodCollector(mockMethods).collectMockMethods(mockUpClass);

        mockMethods.registerMockStates(mockUp, forStartupMock);

        MockClasses mockClasses = TestRun.getMockClasses();

        if (forStartupMock) {
            mockClasses.addMock(mockMethods.getMockClassInternalName(), mockUp);
        } else {
            mockClasses.addMock(mockUp);
        }
    }

    public void redefineMethodsInGeneratedClass() {
        byte[] modifiedClassFile = modifyRealClass(realClass);

        if (modifiedClassFile != null) {
            applyClassModifications(realClass, modifiedClassFile);
        }
    }

    @Nonnull
    public Set<Class<?>> redefineMethods() {
        return redefineMethodsInClassHierarchy();
    }

    @Nonnull
    private Set<Class<?>> redefineMethodsInClassHierarchy() {
        Set<Class<?>> redefinedClasses = new HashSet<Class<?>>();
        @Nullable Class<?> classToModify = realClass;

        while (classToModify != null && mockMethods.hasUnusedMocks()) {
            byte[] modifiedClassFile = modifyRealClass(classToModify);

            if (modifiedClassFile != null) {
                applyClassModifications(classToModify, modifiedClassFile);
                redefinedClasses.add(classToModify);
            }

            Class<?> superClass = classToModify.getSuperclass();
            classToModify = superClass == Object.class || superClass == Proxy.class ? null : superClass;
            rcReader = null;
        }

        return redefinedClasses;
    }

    @Nullable
    private byte[] modifyRealClass(@Nonnull Class<?> classToModify) {
        if (rcReader == null) {
            rcReader = createClassReaderForRealClass(classToModify);
        }

        MockupsModifier modifier = new MockupsModifier(rcReader, classToModify, mockUp, mockMethods);
        rcReader.accept(modifier, SKIP_FRAMES);

        return modifier.wasModified() ? modifier.toByteArray() : null;
    }

    @Nonnull
    BaseClassModifier createClassModifier(@Nonnull ClassReader cr) {
        return new MockupsModifier(cr, realClass, mockUp, mockMethods);
    }

    @Nonnull
    private static ClassReader createClassReaderForRealClass(@Nonnull Class<?> classToModify) {
        return ClassFile.createReaderFromLastRedefinitionIfAny(classToModify);
    }

    void applyClassModifications(@Nonnull Class<?> classToModify, @Nonnull byte[] modifiedClassFile) {
        Startup.redefineMethods(classToModify, modifiedClassFile);

        if (forStartupMock) {
            CachedClassfiles.addClassfile(classToModify, modifiedClassFile);
        } else {
            String mockClassDesc = mockMethods.getMockClassInternalName();
            TestRun.mockFixture().addRedefinedClass(mockClassDesc, classToModify, modifiedClassFile);
        }
    }
}
