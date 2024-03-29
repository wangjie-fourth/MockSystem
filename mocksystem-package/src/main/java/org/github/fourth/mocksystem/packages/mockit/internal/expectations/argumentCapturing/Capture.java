/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.argumentCapturing;

import org.github.fourth.mocksystem.packages.mockit.external.asm.MethodWriter;
import org.github.fourth.mocksystem.packages.mockit.external.asm.Type;
import org.github.fourth.mocksystem.packages.mockit.internal.expectations.transformation.InvocationBlockModifier;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.github.fourth.mocksystem.packages.mockit.external.asm.Opcodes.ALOAD;
import static org.github.fourth.mocksystem.packages.mockit.external.asm.Opcodes.SIPUSH;
import static org.github.fourth.mocksystem.packages.mockit.internal.util.TypeConversion.generateCastOrUnboxing;
import static org.github.fourth.mocksystem.packages.mockit.internal.util.TypeConversion.isPrimitiveWrapper;

final class Capture {
    @Nonnull
    private final InvocationBlockModifier invocationBlockModifier;
    @Nonnull
    private final MethodWriter mw;
    @Nonnegative
    private final int opcode;
    @Nonnegative
    private final int varIndex;
    @Nullable
    private String typeToCapture;
    @Nonnegative
    private int parameterIndex;
    @Nonnegative
    private boolean parameterIndexFixed;

    Capture(
            @Nonnull InvocationBlockModifier invocationBlockModifier, @Nonnegative int opcode, @Nonnegative int varIndex,
            @Nullable String typeToCapture, @Nonnegative int parameterIndex) {
        this.invocationBlockModifier = invocationBlockModifier;
        mw = invocationBlockModifier.getMethodWriter();
        this.opcode = opcode;
        this.varIndex = varIndex;
        this.typeToCapture = typeToCapture;
        this.parameterIndex = parameterIndex;
    }

    Capture(
            @Nonnull InvocationBlockModifier invocationBlockModifier, @Nonnegative int varIndex,
            @Nonnegative int parameterIndex) {
        this.invocationBlockModifier = invocationBlockModifier;
        mw = invocationBlockModifier.getMethodWriter();
        opcode = ALOAD;
        this.varIndex = varIndex;
        this.parameterIndex = parameterIndex;
    }

    /**
     * Generates bytecode that will be responsible for performing the following steps:
     * 1. Get the argument value (an Object) for the last matched invocation.
     * 2. Cast to a reference type or unbox to a primitive type, as needed.
     * 3. Store the converted value in its local variable.
     */
    void generateCodeToStoreCapturedValue() {
        if (opcode != ALOAD) {
            mw.visitIntInsn(SIPUSH, parameterIndex);
            invocationBlockModifier.generateCallToActiveInvocationsMethod("matchedArgument", "(I)Ljava/lang/Object;");

            Type argType = getArgumentType();
            generateCastOrUnboxing(mw, argType, opcode);

            mw.visitVarInsn(opcode, varIndex);
        }
    }

    @Nonnull
    private Type getArgumentType() {
        if (typeToCapture == null) {
            return invocationBlockModifier.getParameterType(parameterIndex);
        } else if (typeToCapture.charAt(0) == '[') {
            return Type.getType(typeToCapture);
        } else {
            return Type.getType('L' + typeToCapture + ';');
        }
    }

    boolean fixParameterIndex(@Nonnegative int originalIndex, @Nonnegative int newIndex) {
        if (!parameterIndexFixed && parameterIndex == originalIndex) {
            parameterIndex = newIndex;
            parameterIndexFixed = true;
            return true;
        }

        return false;
    }

    void generateCallToSetArgumentTypeIfNeeded() {
        if (opcode == ALOAD) {
            mw.visitIntInsn(SIPUSH, parameterIndex);
            mw.visitLdcInsn(varIndex);
            invocationBlockModifier.generateCallToActiveInvocationsMethod("setExpectedArgumentType", "(II)V");
        } else if (typeToCapture != null && !isTypeToCaptureSameAsParameterType(typeToCapture)) {
            mw.visitIntInsn(SIPUSH, parameterIndex);
            mw.visitLdcInsn(typeToCapture);
            invocationBlockModifier.generateCallToActiveInvocationsMethod(
                    "setExpectedArgumentType", "(ILjava/lang/String;)V");
        }
    }

    private boolean isTypeToCaptureSameAsParameterType(@Nonnull String typeDesc) {
        Type parameterType = invocationBlockModifier.getParameterType(parameterIndex);
        int sort = parameterType.getSort();

        if (sort == Type.OBJECT || sort == Type.ARRAY) {
            return typeDesc.equals(parameterType.getInternalName());
        }

        return isPrimitiveWrapper(typeDesc);
    }
}
