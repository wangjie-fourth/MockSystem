/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.transformation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.github.fourth.mocksystem.packages.mockit.external.asm.*;
import org.github.fourth.mocksystem.packages.mockit.internal.expectations.argumentCapturing.ArgumentCapturing;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.github.fourth.mocksystem.packages.mockit.external.asm.Opcodes.*;
import static org.github.fourth.mocksystem.packages.mockit.internal.util.TypeConversion.isBoxing;
import static org.github.fourth.mocksystem.packages.mockit.internal.util.TypeConversion.isUnboxing;

@SuppressFBWarnings("EI_EXPOSE_REP")
public final class InvocationBlockModifier extends MethodVisitor {
    private static final String CLASS_DESC = "mockit/internal/expectations/ActiveInvocations";
    private static final Type[] NO_PARAMETERS = new Type[0];
    private static final String ANY_FIELDS =
            "any anyString anyInt anyBoolean anyLong anyDouble anyFloat anyChar anyShort anyByte";
    private static final String WITH_METHODS =
            "withArgThat(Lorg/hamcrest/Matcher;)Ljava/lang/Object; " +
                    "with(Lmockit/Delegate;)Ljava/lang/Object; " +
                    "withAny(Ljava/lang/Object;)Ljava/lang/Object; " +
                    "withCapture()Ljava/lang/Object; withCapture(Ljava/util/List;)Ljava/lang/Object; " +
                    "withCapture(Ljava/lang/Object;)Ljava/util/List; " +
                    "withEqual(Ljava/lang/Object;)Ljava/lang/Object; withEqual(DD)D withEqual(FD)F " +
                    "withInstanceLike(Ljava/lang/Object;)Ljava/lang/Object; " +
                    "withInstanceOf(Ljava/lang/Class;)Ljava/lang/Object; " +
                    "withNotEqual(Ljava/lang/Object;)Ljava/lang/Object; " +
                    "withNull()Ljava/lang/Object; withNotNull()Ljava/lang/Object; " +
                    "withSameInstance(Ljava/lang/Object;)Ljava/lang/Object; " +
                    "withSubstring(Ljava/lang/CharSequence;)Ljava/lang/CharSequence; " +
                    "withPrefix(Ljava/lang/CharSequence;)Ljava/lang/CharSequence; " +
                    "withSuffix(Ljava/lang/CharSequence;)Ljava/lang/CharSequence; " +
                    "withMatch(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;";

    @Nonnull
    private final MethodWriter mw;

    // Input data:
    @Nonnull
    private final String blockOwner;
    private final boolean callEndInvocations;

    // Takes care of withCapture() matchers, if any:
    private boolean justAfterWithCaptureInvocation;
    @Nonnull
    private final ArgumentCapturing argumentCapturing;

    // Stores the index of the local variable holding a list passed in a withCapture(List) call, if any:
    @Nonnegative
    private int lastLoadedVarIndex;

    // Helper fields that allow argument matchers to be moved to the correct positions of their corresponding parameters:
    @Nonnull
    private final int[] matcherStacks;
    @Nonnegative
    private int matcherCount;
    @Nonnegative
    private int stackSize;
    @Nonnull
    private Type[] parameterTypes;

    InvocationBlockModifier(@Nonnull MethodWriter mw, @Nonnull String blockOwner, boolean callEndInvocations) {
        super(mw);
        this.mw = mw;
        this.blockOwner = blockOwner;
        this.callEndInvocations = callEndInvocations;
        matcherStacks = new int[40];
        argumentCapturing = new ArgumentCapturing(this);
        parameterTypes = NO_PARAMETERS;
    }

    public void generateCallToActiveInvocationsMethod(@Nonnull String name, @Nonnull String desc) {
        visitMethodInstruction(INVOKESTATIC, CLASS_DESC, name, desc, false);
    }

    @Override
    @SuppressWarnings("EmptyBlock")
    public void visitFieldInsn(
            @Nonnegative int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
        boolean getField = opcode == GETFIELD;

        if ((getField || opcode == PUTFIELD) && blockOwner.equals(owner)) {
            if (name.indexOf('$') > 0) {
                // Nothing to do.
            } else if (getField && name.startsWith("any") && ANY_FIELDS.contains(name)) {
                generateCodeToAddArgumentMatcherForAnyField(owner, name, desc);
                return;
            } else if (!getField && generateCodeThatReplacesAssignmentToSpecialField(name)) {
                visitInsn(POP);
                return;
            }
        }

        stackSize += stackSizeVariationForFieldAccess(opcode, desc);
        mw.visitFieldInsn(opcode, owner, name, desc);
    }

    private boolean generateCodeThatReplacesAssignmentToSpecialField(@Nonnull String fieldName) {
        if ("result".equals(fieldName)) {
            generateCallToActiveInvocationsMethod("addResult", "(Ljava/lang/Object;)V");
            return true;
        }

        if ("times".equals(fieldName) || "minTimes".equals(fieldName) || "maxTimes".equals(fieldName)) {
            generateCallToActiveInvocationsMethod(fieldName, "(I)V");
            return true;
        }

        return false;
    }

    private void generateCodeToAddArgumentMatcherForAnyField(
            @Nonnull String fieldOwner, @Nonnull String name, @Nonnull String desc) {
        mw.visitFieldInsn(GETFIELD, fieldOwner, name, desc);
        generateCallToActiveInvocationsMethod(name, "()V");
        matcherStacks[matcherCount++] = stackSize;
    }

    private static int stackSizeVariationForFieldAccess(@Nonnegative int opcode, @Nonnull String fieldType) {
        char c = fieldType.charAt(0);
        boolean twoByteType = c == 'D' || c == 'J';

        switch (opcode) {
            case GETSTATIC:
                return twoByteType ? 2 : 1;
            case PUTSTATIC:
                return twoByteType ? -2 : -1;
            case GETFIELD:
                return twoByteType ? 1 : 0;
            default:
                return twoByteType ? -3 : -2;
        }
    }

    @Override
    public void visitMethodInsn(
            @Nonnegative int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf) {
        if (opcode == INVOKESTATIC && (isBoxing(owner, name, desc) || isAccessMethod(owner, name))) {
            // It's an invocation to a primitive boxing method or to a synthetic method for private access, just ignore it.
            visitMethodInstruction(INVOKESTATIC, owner, name, desc, itf);
        } else if (isCallToArgumentMatcher(opcode, owner, name, desc)) {
            visitMethodInstruction(INVOKEVIRTUAL, owner, name, desc, itf);

            boolean withCaptureMethod = "withCapture".equals(name);

            if (argumentCapturing.registerMatcher(withCaptureMethod, desc, lastLoadedVarIndex)) {
                justAfterWithCaptureInvocation = withCaptureMethod;
                matcherStacks[matcherCount++] = stackSize;
            }
        } else if (isUnboxing(opcode, owner, desc)) {
            if (justAfterWithCaptureInvocation) {
                generateCodeToReplaceNullWithZeroOnTopOfStack(desc);
                justAfterWithCaptureInvocation = false;
            } else {
                visitMethodInstruction(opcode, owner, name, desc, itf);
            }
        } else {
            handleMockedOrNonMockedInvocation(opcode, owner, name, desc, itf);
        }
    }

    private boolean isAccessMethod(@Nonnull String methodOwner, @Nonnull String name) {
        return !methodOwner.equals(blockOwner) && name.startsWith("access$");
    }

    private void visitMethodInstruction(
            @Nonnegative int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf) {
        int argSize = Type.getArgumentsAndReturnSizes(desc);
        int sizeVariation = (argSize & 0x03) - (argSize >> 2);

        if (opcode == INVOKESTATIC) {
            sizeVariation++;
        }

        stackSize += sizeVariation;
        mw.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    private boolean isCallToArgumentMatcher(
            @Nonnegative int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
        return
                opcode == INVOKEVIRTUAL && owner.equals(blockOwner) &&
                        name.startsWith("with") && WITH_METHODS.contains(name + desc);
    }

    private void generateCodeToReplaceNullWithZeroOnTopOfStack(@Nonnull String unboxingMethodDesc) {
        char primitiveTypeCode = unboxingMethodDesc.charAt(2);
        visitInsn(POP);

        int zeroOpcode;
        switch (primitiveTypeCode) {
            case 'J':
                zeroOpcode = LCONST_0;
                break;
            case 'F':
                zeroOpcode = FCONST_0;
                break;
            case 'D':
                zeroOpcode = DCONST_0;
                break;
            default:
                zeroOpcode = ICONST_0;
        }

        visitInsn(zeroOpcode);
    }

    private void handleMockedOrNonMockedInvocation(
            @Nonnegative int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf) {
        if (matcherCount == 0) {
            visitMethodInstruction(opcode, owner, name, desc, itf);
        } else {
            boolean mockedInvocationUsingTheMatchers = handleInvocationParameters(desc);
            visitMethodInstruction(opcode, owner, name, desc, itf);
            handleArgumentCapturingIfNeeded(mockedInvocationUsingTheMatchers);
        }
    }

    private boolean handleInvocationParameters(@Nonnull String desc) {
        parameterTypes = Type.getArgumentTypes(desc);
        int stackAfter = stackSize - sumOfParameterSizes();
        boolean mockedInvocationUsingTheMatchers = stackAfter < matcherStacks[0];

        if (mockedInvocationUsingTheMatchers) {
            generateCallsToMoveArgMatchers(stackAfter);
            argumentCapturing.generateCallsToSetArgumentTypesToCaptureIfAny();
            matcherCount = 0;
        }

        return mockedInvocationUsingTheMatchers;
    }

    @Nonnegative
    private int sumOfParameterSizes() {
        int sum = 0;

        for (Type argType : parameterTypes) {
            sum += argType.getSize();
        }

        return sum;
    }

    private void generateCallsToMoveArgMatchers(@Nonnegative int initialStack) {
        int stack = initialStack;
        int nextMatcher = 0;
        int matcherStack = matcherStacks[0];

        for (int i = 0; i < parameterTypes.length && nextMatcher < matcherCount; i++) {
            stack += parameterTypes[i].getSize();

            if (stack == matcherStack || stack == matcherStack + 1) {
                if (nextMatcher < i) {
                    generateCallToMoveArgMatcher(nextMatcher, i);
                    argumentCapturing.updateCaptureIfAny(nextMatcher, i);
                }

                matcherStack = matcherStacks[++nextMatcher];
            }
        }
    }

    private void generateCallToMoveArgMatcher(@Nonnegative int originalMatcherIndex, @Nonnegative int toIndex) {
        mw.visitIntInsn(SIPUSH, originalMatcherIndex);
        mw.visitIntInsn(SIPUSH, toIndex);
        generateCallToActiveInvocationsMethod("moveArgMatcher", "(II)V");
    }

    private void handleArgumentCapturingIfNeeded(boolean mockedInvocationUsingTheMatchers) {
        if (mockedInvocationUsingTheMatchers) {
            argumentCapturing.generateCallsToCaptureMatchedArgumentsIfPending();
        }

        justAfterWithCaptureInvocation = false;
    }

    @Override
    public void visitLabel(@Nonnull Label label) {
        mw.visitLabel(label);

        if (!label.isDebug()) {
            stackSize = 0;
        }
    }

    @Override
    public void visitTypeInsn(@Nonnegative int opcode, @Nonnull String type) {
        argumentCapturing.registerTypeToCaptureIfApplicable(opcode, type);

        if (opcode == NEW) {
            stackSize++;
        }

        mw.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitIntInsn(@Nonnegative int opcode, int operand) {
        if (opcode != NEWARRAY) {
            stackSize++;
        }

        mw.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(@Nonnegative int opcode, @Nonnegative int varIndex) {
        if (opcode == ALOAD) {
            lastLoadedVarIndex = varIndex;
        }

        argumentCapturing.registerAssignmentToCaptureVariableIfApplicable(opcode, varIndex);

        if (opcode != RET) {
            stackSize += Frame.SIZE[opcode];
        }

        mw.visitVarInsn(opcode, varIndex);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        stackSize++;

        if (cst instanceof Long || cst instanceof Double) {
            stackSize++;
        }

        mw.visitLdcInsn(cst);
    }

    @Override
    public void visitJumpInsn(@Nonnegative int opcode, Label label) {
        if (opcode != JSR) {
            stackSize += Frame.SIZE[opcode];
        }

        mw.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        stackSize--;
        mw.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        stackSize--;
        mw.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, @Nonnegative int dims) {
        stackSize += 1 - dims;
        mw.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public void visitInsn(@Nonnegative int opcode) {
        if (opcode == RETURN && callEndInvocations) {
            generateCallToActiveInvocationsMethod("endInvocations", "()V");
        } else {
            stackSize += Frame.SIZE[opcode];
        }

        mw.visitInsn(opcode);
    }

    @Override
    public void visitLocalVariable(
            @Nonnull String name, @Nonnull String desc, @Nullable String signature, @Nonnull Label start, @Nonnull Label end,
            @Nonnegative int index) {
        if (signature != null) {
            argumentCapturing.registerTypeToCaptureIntoListIfApplicable(index, signature);
        }

        // In classes instrumented with EMMA some local variable information can be lost, so we discard it entirely to
        // avoid a ClassFormatError.
        if (end.position > 0) {
            mw.visitLocalVariable(name, desc, signature, start, end, index);
        }
    }

    @Nonnull
    public MethodWriter getMethodWriter() {
        return mw;
    }

    @Nonnegative
    public int getMatcherCount() {
        return matcherCount;
    }

    @Nonnull
    public Type getParameterType(@Nonnegative int parameterIndex) {
        return parameterTypes[parameterIndex];
    }
}
