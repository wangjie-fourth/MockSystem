/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.injection;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.github.fourth.mocksystem.packages.mockit.Injectable;
import org.github.fourth.mocksystem.packages.mockit.Tested;
import org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking.MockedType;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.github.fourth.mocksystem.packages.mockit.external.asm.Opcodes.ACC_STATIC;
import static org.github.fourth.mocksystem.packages.mockit.external.asm.Opcodes.ACC_SYNTHETIC;

@SuppressFBWarnings("EI_EXPOSE_REP")
public final class TestedClassInstantiations {
    private static final int FIELD_ACCESS_MASK = ACC_SYNTHETIC + ACC_STATIC;

    @Nonnull
    private final List<TestedField> testedFields;
    @Nonnull
    private final List<MockedType> injectableFields;
    @Nonnull
    final InjectionState injectionState;

    public TestedClassInstantiations() {
        testedFields = new LinkedList<TestedField>();
        injectableFields = new ArrayList<MockedType>();
        injectionState = new InjectionState();
    }

    public boolean findTestedAndInjectableFields(@Nonnull Class<?> testClass) {
        findAllTestedAndInjectableFieldsInTestClassHierarchy(testClass);
        return !testedFields.isEmpty();
    }

    private void findAllTestedAndInjectableFieldsInTestClassHierarchy(@Nonnull Class<?> testClass) {
        Class<?> superclass = testClass.getSuperclass();

        if (superclass.getClassLoader() != null) {
            findAllTestedAndInjectableFieldsInTestClassHierarchy(superclass);
        }

        Field[] fieldsFromTestClass = testClass.getDeclaredFields();

        for (Field candidateField : fieldsFromTestClass) {
            int fieldModifiers = candidateField.getModifiers();

            if ((fieldModifiers & FIELD_ACCESS_MASK) == 0) {
                addAsTestedOrInjectableFieldIfApplicable(candidateField);
            }
        }
    }

    private void addAsTestedOrInjectableFieldIfApplicable(@Nonnull Field fieldFromTestClass) {
        for (Annotation fieldAnnotation : fieldFromTestClass.getDeclaredAnnotations()) {
            Tested testedMetadata = TestedObject.getTestedAnnotationIfPresent(fieldAnnotation);

            if (testedMetadata != null) {
                TestedField testedField = new TestedField(injectionState, fieldFromTestClass, testedMetadata);
                testedFields.add(testedField);
                break;
            } else if (fieldAnnotation instanceof Injectable) {
                MockedType mockedType = new MockedType(fieldFromTestClass);
                injectableFields.add(mockedType);
                break;
            }
        }
    }

    private void instantiateTestedObject(@Nonnull Object testClassInstance, @Nonnull TestedObject testedObject) {
        try {
            testedObject.instantiateWithInjectableValues(testClassInstance);
        } finally {
            injectionState.resetConsumedInjectables();
        }
    }

    public void assignNewInstancesToTestedFields(@Nonnull Object testClassInstance, boolean beforeSetup) {
        injectionState.buildListsOfInjectables(testClassInstance, injectableFields);

        for (TestedObject testedField : testedFields) {
            if (!beforeSetup || testedField.isAvailableDuringSetup()) {
                instantiateTestedObject(testClassInstance, testedField);
            }
        }
    }

    public void clearTestedObjects() {
        injectionState.lifecycleMethods.executeTerminationMethodsIfAny();
        injectionState.clearTestedObjectsAndInstantiatedDependencies();

        Object testClassInstance = injectionState.getCurrentTestClassInstance();

        for (TestedObject testedField : testedFields) {
            testedField.clearIfAutomaticCreation(testClassInstance);
        }
    }

    @Nonnull
    public BeanExporter getBeanExporter() {
        return injectionState;
    }
}
