/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.capturing;

import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader;
import org.github.fourth.mocksystem.packages.mockit.internal.BaseClassModifier;
import org.github.fourth.mocksystem.packages.mockit.internal.ClassFile;
import org.github.fourth.mocksystem.packages.mockit.internal.startup.Startup;
import org.github.fourth.mocksystem.packages.mockit.internal.state.TestRun;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader.SKIP_FRAMES;

public abstract class CaptureOfImplementations<M> {
    protected CaptureOfImplementations() {
    }

    public final void makeSureAllSubtypesAreModified(
            @Nonnull Class<?> baseType, boolean registerCapturedClasses, @Nullable M typeMetadata) {
        CapturedType captureMetadata = new CapturedType(baseType);
        redefineClassesAlreadyLoaded(captureMetadata, baseType, typeMetadata);
        createCaptureTransformer(captureMetadata, registerCapturedClasses, typeMetadata);
    }

    private void redefineClassesAlreadyLoaded(
            @Nonnull CapturedType captureMetadata, @Nonnull Class<?> baseType, @Nullable M typeMetadata) {
        Class<?>[] classesLoaded = Startup.instrumentation().getAllLoadedClasses();

        for (Class<?> aClass : classesLoaded) {
            if (captureMetadata.isToBeCaptured(aClass)) {
                redefineClass(aClass, baseType, typeMetadata);
            }
        }
    }

    public void redefineClass(@Nonnull Class<?> realClass, @Nonnull Class<?> baseType, @Nullable M typeMetadata) {
        if (!TestRun.mockFixture().containsRedefinedClass(realClass)) {
            ClassReader classReader;

            try {
                classReader = ClassFile.createReaderOrGetFromCache(realClass);
            } catch (ClassFile.NotFoundException ignore) {
                return;
            }

            TestRun.ensureThatClassIsInitialized(realClass);

            BaseClassModifier modifier = createModifier(realClass.getClassLoader(), classReader, baseType, typeMetadata);
            classReader.accept(modifier, SKIP_FRAMES);

            if (modifier.wasModified()) {
                byte[] modifiedClass = modifier.toByteArray();
                redefineClass(realClass, modifiedClass);
            }
        }
    }

    @Nonnull
    protected abstract BaseClassModifier createModifier(
            @Nullable ClassLoader cl, @Nonnull ClassReader cr, @Nonnull Class<?> baseType, @Nullable M typeMetadata);

    protected abstract void redefineClass(@Nonnull Class<?> realClass, @Nonnull byte[] modifiedClass);

    private void createCaptureTransformer(
            @Nonnull CapturedType captureMetadata, boolean registerCapturedClasses, @Nullable M typeMetadata) {
        CaptureTransformer<M> transformer =
                new CaptureTransformer<M>(captureMetadata, this, registerCapturedClasses, typeMetadata);

        Startup.instrumentation().addTransformer(transformer, true);
        TestRun.mockFixture().addCaptureTransformer(transformer);
    }
}
