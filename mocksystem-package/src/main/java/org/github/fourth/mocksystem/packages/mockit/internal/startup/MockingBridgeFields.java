/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.startup;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader;
import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassVisitor;
import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassWriter;
import org.github.fourth.mocksystem.packages.mockit.internal.MockingBridge;
import org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking.MockedBridge;
import org.github.fourth.mocksystem.packages.mockit.internal.mockups.MockMethodBridge;
import org.github.fourth.mocksystem.packages.mockit.internal.mockups.MockupBridge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.print.PrintException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static java.lang.reflect.Modifier.isPublic;
import static org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader.SKIP_FRAMES;
import static org.github.fourth.mocksystem.packages.mockit.external.asm.Opcodes.*;

@SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
final class MockingBridgeFields {
    private MockingBridgeFields() {
    }

    @Nonnull
    static String createSyntheticFieldsInJREClassToHoldMockingBridges(@Nonnull Instrumentation instrumentation) {
        FieldAdditionTransformer fieldAdditionTransformer = new FieldAdditionTransformer(instrumentation);
        instrumentation.addTransformer(fieldAdditionTransformer);

        // Loads some JRE classes expected to not be loaded yet.
        NegativeArraySizeException.class.getName();

        if (fieldAdditionTransformer.hostClassName == null) {
            PrintException.class.getName();
        }

        return fieldAdditionTransformer.hostClassName;
    }

    private static final class FieldAdditionTransformer implements ClassFileTransformer {
        private static final int FIELD_ACCESS = ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC;
        @Nonnull
        private final Instrumentation instrumentation;
        String hostClassName;

        FieldAdditionTransformer(@Nonnull Instrumentation instrumentation) {
            this.instrumentation = instrumentation;
        }

        @Nullable
        @Override
        public byte[] transform(
                @Nullable ClassLoader loader, @Nonnull String className, @Nullable Class<?> classBeingRedefined,
                @Nullable ProtectionDomain protectionDomain, @Nonnull byte[] classfileBuffer) {
            if (loader == null && hostClassName == null) { // adds the fields to the first public JRE class to be loaded
                ClassReader cr = new ClassReader(classfileBuffer);

                if (isPublic(cr.getAccess())) {
                    instrumentation.removeTransformer(this);
                    hostClassName = className;
                    return getModifiedJREClassWithAddedFields(cr);
                }
            }

            return null;
        }

        @Nonnull
        private byte[] getModifiedJREClassWithAddedFields(@Nonnull ClassReader classReader) {
            final ClassWriter cw = new ClassWriter(classReader);

            ClassVisitor cv = new ClassVisitor(cw) {
                @Override
                public void visitEnd() {
                    addField(MockedBridge.MB);
                    addField(MockupBridge.MB);
                    addField(MockMethodBridge.MB);
                }

                private void addField(@Nonnull MockingBridge mb) {
                    cw.visitField(FIELD_ACCESS, mb.id, "Ljava/lang/reflect/InvocationHandler;", null, null);
                }
            };

            classReader.accept(cv, SKIP_FRAMES);
            return cw.toByteArray();
        }
    }
}
