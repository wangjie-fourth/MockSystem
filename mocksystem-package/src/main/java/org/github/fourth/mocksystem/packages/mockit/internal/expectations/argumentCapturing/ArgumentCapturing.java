/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.argumentCapturing;

import org.github.fourth.mocksystem.packages.mockit.external.asm.Type;
import org.github.fourth.mocksystem.packages.mockit.internal.expectations.transformation.InvocationBlockModifier;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.github.fourth.mocksystem.packages.mockit.external.asm.Opcodes.*;

public final class ArgumentCapturing {
    private static final Map<Integer, String> VAR_INDEX_TO_TYPE_DESC = new HashMap<Integer, String>();

    @Nonnull
    private final InvocationBlockModifier modifier;
    @Nullable
    private List<Capture> captures;
    private boolean parameterForCapture;
    @Nullable
    private String capturedTypeDesc;

    public ArgumentCapturing(@Nonnull InvocationBlockModifier modifier) {
        this.modifier = modifier;
    }

    public boolean registerMatcher(
            boolean withCaptureMethod, @Nonnull String methodDesc, @Nonnegative int lastLoadedVarIndex) {
        if (withCaptureMethod && "(Ljava/lang/Object;)Ljava/util/List;".equals(methodDesc)) {
            return false;
        }

        if (withCaptureMethod) {
            if (methodDesc.contains("List")) {
                if (lastLoadedVarIndex > 0) {
                    Capture capture = new Capture(modifier, lastLoadedVarIndex, modifier.getMatcherCount());
                    addCapture(capture);
                }

                parameterForCapture = false;
            } else {
                parameterForCapture = true;
            }
        } else {
            parameterForCapture = false;
        }

        return true;
    }

    public void registerTypeToCaptureIfApplicable(@Nonnegative int opcode, @Nonnull String typeDesc) {
        if (opcode == CHECKCAST && parameterForCapture) {
            capturedTypeDesc = typeDesc;
        }
    }

    public void registerTypeToCaptureIntoListIfApplicable(@Nonnegative int varIndex, @Nonnull String signature) {
        if (signature.startsWith("Ljava/util/List<")) {
            String typeDesc = signature.substring(16, signature.length() - 2);
            int p = typeDesc.indexOf('<');

            if (p > 0) {
                typeDesc = typeDesc.substring(0, p) + ';';
            }

            Type type = Type.getType(typeDesc);
            VAR_INDEX_TO_TYPE_DESC.put(varIndex, type.getInternalName());
        }
    }

    public void registerAssignmentToCaptureVariableIfApplicable(@Nonnegative int opcode, @Nonnegative int varIndex) {
        if (opcode >= ISTORE && opcode <= ASTORE && parameterForCapture) {
            Capture capture = new Capture(modifier, opcode, varIndex, capturedTypeDesc, modifier.getMatcherCount() - 1);
            addCapture(capture);
            parameterForCapture = false;
            capturedTypeDesc = null;
        }
    }

    private void addCapture(@Nonnull Capture capture) {
        if (captures == null) {
            captures = new ArrayList<Capture>();
        }

        captures.add(capture);
    }

    public void updateCaptureIfAny(@Nonnegative int originalIndex, @Nonnegative int newIndex) {
        if (captures != null) {
            for (int i = captures.size() - 1; i >= 0; i--) {
                Capture capture = captures.get(i);

                if (capture.fixParameterIndex(originalIndex, newIndex)) {
                    break;
                }
            }
        }
    }

    public void generateCallsToSetArgumentTypesToCaptureIfAny() {
        if (captures != null) {
            for (Capture capture : captures) {
                capture.generateCallToSetArgumentTypeIfNeeded();
            }
        }
    }

    public void generateCallsToCaptureMatchedArgumentsIfPending() {
        if (captures != null) {
            for (Capture capture : captures) {
                capture.generateCodeToStoreCapturedValue();
            }

            captures = null;
        }
    }

    @Nullable
    public static String extractArgumentType(@Nonnegative int varIndex) {
        return VAR_INDEX_TO_TYPE_DESC.remove(varIndex);
    }
}
