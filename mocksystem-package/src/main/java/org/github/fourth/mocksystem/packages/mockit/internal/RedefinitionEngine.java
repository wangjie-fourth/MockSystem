/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.github.fourth.mocksystem.packages.mockit.internal.startup.Startup;
import org.github.fourth.mocksystem.packages.mockit.internal.state.MockFixture;
import org.github.fourth.mocksystem.packages.mockit.internal.state.TestRun;
import org.github.fourth.mocksystem.packages.mockit.internal.util.GeneratedClasses;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.instrument.ClassDefinition;
import java.util.Map;
import java.util.Map.Entry;

@SuppressFBWarnings("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
public final class RedefinitionEngine {
    @Nonnull
    private Class<?> realClass;

    public RedefinitionEngine() {
    }

    public RedefinitionEngine(@Nonnull Class<?> realClass) {
        this.realClass = realClass;
    }

    public static void redefineClasses(@Nonnull ClassDefinition... definitions) {
        Startup.redefineMethods(definitions);

        MockFixture mockFixture = TestRun.mockFixture();

        for (ClassDefinition def : definitions) {
            mockFixture.addRedefinedClass(def.getDefinitionClass(), def.getDefinitionClassFile());
        }
    }

    public void redefineMethodsWhileRegisteringTheClass(@Nonnull byte[] modifiedClassfile) {
        redefineMethods(modifiedClassfile);
        addToMapOfRedefinedClasses(null, modifiedClassfile);
        TestRun.mockFixture().registerMockedClass(realClass);
    }

    private void addToMapOfRedefinedClasses(@Nullable String mockClassInternalName, @Nonnull byte[] modifiedClassfile) {
        TestRun.mockFixture().addRedefinedClass(mockClassInternalName, realClass, modifiedClassfile);
    }

    private void redefineMethods(@Nonnull byte[] modifiedClassfile) {
        Startup.redefineMethods(realClass, modifiedClassfile);
    }

    public void redefineMethods(@Nonnull Map<Class<?>, byte[]> modifiedClassfiles) {
        ClassDefinition[] classDefs = new ClassDefinition[modifiedClassfiles.size()];
        int i = 0;

        for (Entry<Class<?>, byte[]> classAndBytecode : modifiedClassfiles.entrySet()) {
            realClass = classAndBytecode.getKey();
            byte[] modifiedClassfile = classAndBytecode.getValue();

            classDefs[i++] = new ClassDefinition(realClass, modifiedClassfile);
            addToMapOfRedefinedClasses(null, modifiedClassfile);
        }

        Startup.redefineMethods(classDefs);
    }

    public void restoreDefinition(@Nonnull Class<?> aClass, @Nullable byte[] previousDefinition) {
        if (previousDefinition == null) {
            restoreOriginalDefinition(aClass);
        } else {
            restoreToDefinition(aClass, previousDefinition);
        }
    }

    public void restoreOriginalDefinition(@Nonnull Class<?> aClass) {
        if (!GeneratedClasses.isGeneratedImplementationClass(aClass)) {
            realClass = aClass;
            byte[] realClassFile = ClassFile.createReaderOrGetFromCache(aClass).b;
            redefineMethods(realClassFile);
        }
    }

    public void restoreToDefinition(@Nonnull Class<?> aClass, @Nonnull byte[] definitionToRestore) {
        realClass = aClass;
        redefineMethods(definitionToRestore);
    }
}
