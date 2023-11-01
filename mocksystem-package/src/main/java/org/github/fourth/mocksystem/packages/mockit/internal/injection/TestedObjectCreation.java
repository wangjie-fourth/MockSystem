/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.injection;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassReader;
import org.github.fourth.mocksystem.packages.mockit.external.asm.ClassVisitor;
import org.github.fourth.mocksystem.packages.mockit.internal.classGeneration.ImplementationClass;
import org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking.SubclassGenerationModifier;
import org.github.fourth.mocksystem.packages.mockit.internal.injection.constructor.ConstructorInjection;
import org.github.fourth.mocksystem.packages.mockit.internal.injection.constructor.ConstructorSearch;
import org.github.fourth.mocksystem.packages.mockit.internal.injection.full.FullInjection;
import org.github.fourth.mocksystem.packages.mockit.internal.state.TestRun;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

import static java.lang.reflect.Modifier.isAbstract;

@SuppressFBWarnings("EI_EXPOSE_REP2")
public final class TestedObjectCreation {
    @Nonnull
    private final InjectionState injectionState;
    @Nullable
    private final FullInjection fullInjection;
    @Nonnull
    private final Class<?> actualTestedClass;
    @Nonnull
    final TestedClass testedClass;

    TestedObjectCreation(
            @Nonnull InjectionState injectionState, @Nullable FullInjection fullInjection,
            @Nonnull Type declaredType, @Nonnull Class<?> declaredClass) {
        this.injectionState = injectionState;
        this.fullInjection = fullInjection;
        actualTestedClass = isAbstract(declaredClass.getModifiers()) ?
                generateSubclass(declaredType, declaredClass) : declaredClass;
        testedClass = new TestedClass(declaredType, declaredClass);
    }

    @Nonnull
    private Class<?> generateSubclass(@Nonnull final Type testedType, @Nonnull final Class<?> abstractClass) {
        Class<?> generatedSubclass = new ImplementationClass<Object>(abstractClass) {
            @Nonnull
            @Override
            protected ClassVisitor createMethodBodyGenerator(@Nonnull ClassReader typeReader) {
                return new SubclassGenerationModifier(abstractClass, testedType, typeReader, generatedClassName, true);
            }
        }.generateClass();

        TestRun.mockFixture().registerMockedClass(generatedSubclass);
        return generatedSubclass;
    }

    public TestedObjectCreation(
            @Nonnull InjectionState injectionState, @Nullable FullInjection fullInjection,
            @Nonnull Class<?> implementationClass) {
        this.injectionState = injectionState;
        this.fullInjection = fullInjection;
        actualTestedClass = implementationClass;
        testedClass = new TestedClass(implementationClass, implementationClass);
    }

    @Nonnull
    public Object create() {
        ConstructorSearch constructorSearch =
                new ConstructorSearch(injectionState, actualTestedClass, fullInjection != null);
        Constructor<?> constructor = constructorSearch.findConstructorToUse();

        if (constructor == null) {
            String description = constructorSearch.getDescription();
            throw new IllegalArgumentException(
                    "No constructor in tested class that can be satisfied by available injectables" + description);
        }

        ConstructorInjection constructorInjection =
                new ConstructorInjection(testedClass, injectionState, fullInjection, constructor);

        return constructorInjection.instantiate(constructorSearch.parameterProviders);
    }
}
