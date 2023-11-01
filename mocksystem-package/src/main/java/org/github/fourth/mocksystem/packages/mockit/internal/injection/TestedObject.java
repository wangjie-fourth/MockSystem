/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.injection;

import org.github.fourth.mocksystem.packages.mockit.Tested;
import org.github.fourth.mocksystem.packages.mockit.internal.injection.field.FieldInjection;
import org.github.fourth.mocksystem.packages.mockit.internal.injection.full.FullInjection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

import static org.github.fourth.mocksystem.packages.mockit.internal.util.AutoBoxing.isWrapperOfPrimitiveType;
import static org.github.fourth.mocksystem.packages.mockit.internal.util.DefaultValues.defaultValueForPrimitiveType;

abstract class TestedObject {
    @Nonnull
    final InjectionState injectionState;
    @Nonnull
    private final String testedName;
    @Nonnull
    final Tested metadata;
    @Nullable
    private final FullInjection fullInjection;
    @Nonnull
    private final TestedClass testedClass;
    @Nullable
    private final TestedObjectCreation testedObjectCreation;
    @Nullable
    private List<Field> targetFields;
    boolean createAutomatically;
    boolean requireDIAnnotation;

    @Nullable
    static Tested getTestedAnnotationIfPresent(@Nonnull Annotation annotation) {
        if (annotation instanceof Tested) {
            return (Tested) annotation;
        }

        return annotation.annotationType().getAnnotation(Tested.class);
    }

    TestedObject(
            @Nonnull InjectionState injectionState, @Nonnull Tested metadata,
            @Nonnull String testedName, @Nonnull Type testedType, @Nonnull Class<?> testedClass) {
        this.injectionState = injectionState;
        this.testedName = testedName;
        this.metadata = metadata;
        fullInjection = metadata.fullyInitialized() ? new FullInjection(injectionState, testedClass, testedName) : null;

        if (testedClass.isInterface() || testedClass.isEnum() || testedClass.isPrimitive() || testedClass.isArray()) {
            testedObjectCreation = null;
            this.testedClass = new TestedClass(testedType, testedClass);
        } else {
            testedObjectCreation = new TestedObjectCreation(injectionState, fullInjection, testedType, testedClass);
            this.testedClass = testedObjectCreation.testedClass;
            injectionState.lifecycleMethods.findLifecycleMethods(testedClass);
        }
    }

    boolean isAvailableDuringSetup() {
        return metadata.availableDuringSetup();
    }

    void instantiateWithInjectableValues(@Nonnull Object testClassInstance) {
        if (alreadyInstantiated(testClassInstance)) {
            return;
        }

        Object testedObject = getExistingTestedInstanceIfApplicable(testClassInstance);
        Class<?> testedObjectClass = testedClass.targetClass;

        if (isNonInstantiableType(testedObjectClass, testedObject)) {
            reusePreviouslyCreatedInstance(testClassInstance);
            return;
        }

        injectionState.setTestedTypeReflection(testedClass.reflection);

        if (testedObject == null && createAutomatically) {
            if (reusePreviouslyCreatedInstance(testClassInstance)) {
                return;
            }

            testedObject = createAndRegisterNewObject(testClassInstance);
        } else if (testedObject != null) {
            registerTestedObject(testedObject);
            testedObjectClass = testedObject.getClass();
        }

        if (testedObject != null && testedObjectClass.getClassLoader() != null) {
            performFieldInjection(testedObjectClass, testedObject);
            executeInitializationMethodsIfAny(testedObjectClass, testedObject);
        }
    }

    boolean alreadyInstantiated(@Nonnull Object testClassInstance) {
        return false;
    }

    @Nullable
    abstract Object getExistingTestedInstanceIfApplicable(@Nonnull Object testClassInstance);

    private static boolean isNonInstantiableType(@Nonnull Class<?> targetClass, @Nullable Object currentValue) {
        return
                targetClass.isPrimitive() && defaultValueForPrimitiveType(targetClass).equals(currentValue) ||
                        currentValue == null && (
                                targetClass.isArray() || targetClass.isEnum() || targetClass.isAnnotation() ||
                                        isWrapperOfPrimitiveType(targetClass)
                        );
    }

    private boolean reusePreviouslyCreatedInstance(@Nonnull Object testClassInstance) {
        Object previousInstance = injectionState.getTestedInstance(testedClass.declaredType, testedName);

        if (previousInstance != null) {
            setInstance(testClassInstance, previousInstance);
            return true;
        }

        return false;
    }

    void setInstance(@Nonnull Object testClassInstance, @Nullable Object testedInstance) {
    }

    @Nullable
    private Object createAndRegisterNewObject(@Nonnull Object testClassInstance) {
        Object testedInstance = null;

        if (testedObjectCreation != null) {
            testedInstance = testedObjectCreation.create();
            setInstance(testClassInstance, testedInstance);
            registerTestedObject(testedInstance);
        }

        return testedInstance;
    }

    private void registerTestedObject(@Nonnull Object testedObject) {
        InjectionPoint injectionPoint = new InjectionPoint(testedClass.declaredType, testedName);
        injectionState.saveTestedObject(injectionPoint, testedObject);
    }

    private void performFieldInjection(@Nonnull Class<?> targetClass, @Nonnull Object testedObject) {
        FieldInjection fieldInjection =
                new FieldInjection(injectionState, testedClass, fullInjection, requireDIAnnotation);

        if (targetFields == null) {
            targetFields = fieldInjection.findAllTargetInstanceFieldsInTestedClassHierarchy(targetClass);
            requireDIAnnotation = fieldInjection.isDIAnnotationRequired();
        }

        fieldInjection.injectIntoEligibleFields(targetFields, testedObject);
    }

    private void executeInitializationMethodsIfAny(@Nonnull Class<?> testedClass, @Nonnull Object testedObject) {
        if (createAutomatically) {
            injectionState.lifecycleMethods.executeInitializationMethodsIfAny(testedClass, testedObject);
        }
    }

    void clearIfAutomaticCreation(@Nonnull Object testClassInstance) {
        if (createAutomatically && !isAvailableDuringSetup()) {
            setInstance(testClassInstance, null);
        }
    }
}
