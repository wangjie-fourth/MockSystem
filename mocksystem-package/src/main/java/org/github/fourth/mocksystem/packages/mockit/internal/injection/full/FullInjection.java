/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.injection.full;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.github.fourth.mocksystem.packages.mockit.internal.injection.*;
import org.github.fourth.mocksystem.packages.mockit.internal.reflection.GenericTypeReflection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.Conversation;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.sql.CommonDataSource;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.logging.Logger;

import static java.lang.reflect.Modifier.isStatic;
import static org.github.fourth.mocksystem.packages.mockit.external.asm.Opcodes.*;
import static org.github.fourth.mocksystem.packages.mockit.internal.injection.InjectionPoint.*;
import static org.github.fourth.mocksystem.packages.mockit.internal.reflection.ConstructorReflection.newInstanceUsingDefaultConstructorIfAvailable;
import static org.github.fourth.mocksystem.packages.mockit.internal.util.Utilities.getClassType;

/**
 * Responsible for recursive injection of dependencies into a {@code @Tested(fullyInitialized = true)} object.
 */
@SuppressFBWarnings("EI_EXPOSE_REP2")
public final class FullInjection {
    private static final int INVALID_TYPES = ACC_ABSTRACT + ACC_ANNOTATION + ACC_ENUM;

    @Nonnull
    private final InjectionState injectionState;
    @Nonnull
    private final Class<?> testedClass;
    @Nonnull
    private final String testedName;
    @Nullable
    private final ServletDependencies servletDependencies;
    @Nullable
    private final JPADependencies jpaDependencies;
    @Nullable
    private Class<?> dependencyClass;
    @Nullable
    private InjectionPointProvider injectionProvider;

    public FullInjection(
            @Nonnull InjectionState injectionState, @Nonnull Class<?> testedClass, @Nonnull String testedName) {
        this.injectionState = injectionState;
        this.testedClass = testedClass;
        this.testedName = testedName;
        servletDependencies = SERVLET_CLASS == null ? null : new ServletDependencies(injectionState);
        jpaDependencies = PERSISTENCE_UNIT_CLASS == null ? null : new JPADependencies(injectionState);
    }

    @Nullable
    public Object reuseInstance(
            @Nonnull TestedClass testedClass, @Nonnull InjectionPointProvider injectionProvider,
            @Nullable String qualifiedName) {
        this.injectionProvider = injectionProvider;
        InjectionPoint injectionPoint = getInjectionPoint(testedClass.reflection, injectionProvider, qualifiedName);
        Object dependency = injectionState.getInstantiatedDependency(testedClass, injectionProvider, injectionPoint);
        return dependency;
    }

    @Nonnull
    private InjectionPoint getInjectionPoint(
            @Nonnull GenericTypeReflection reflection, @Nonnull InjectionPointProvider injectionProvider,
            @Nullable String qualifiedName) {
        Type dependencyType = injectionProvider.getDeclaredType();

        if (dependencyType instanceof TypeVariable<?>) {
            dependencyType = reflection.resolveTypeVariable((TypeVariable<?>) dependencyType);
            dependencyClass = getClassType(dependencyType);
        } else {
            dependencyClass = injectionProvider.getClassOfDeclaredType();
        }

        if (qualifiedName != null && !qualifiedName.isEmpty()) {
            return new InjectionPoint(dependencyClass, qualifiedName);
        }

        if (jpaDependencies != null && JPADependencies.isApplicable(dependencyClass)) {
            for (Annotation annotation : injectionProvider.getAnnotations()) {
                String id = jpaDependencies.getDependencyIdIfAvailable(annotation);

                if (id != null) {
                    return new InjectionPoint(dependencyClass, id);
                }
            }
        }

        return new InjectionPoint(dependencyType);
    }

    @Nullable
    public Object createOrReuseInstance(
            @Nonnull Injector injector, @Nonnull InjectionPointProvider injectionProvider, @Nullable String qualifiedName) {
        TestedClass testedClass = injector.testedClass;
        setInjectionProvider(injectionProvider);
        InjectionPoint injectionPoint = getInjectionPoint(testedClass.reflection, injectionProvider, qualifiedName);
        Object dependency = injectionState.getInstantiatedDependency(testedClass, injectionProvider, injectionPoint);

        if (dependency != null) {
            return dependency;
        }

        Class<?> typeToInject = dependencyClass;

        if (typeToInject == Logger.class) {
            return Logger.getLogger(testedClass.nameOfTestedClass);
        }

        if (typeToInject == null || !isInstantiableType(typeToInject)) {
            return null;
        }

        dependency = typeToInject.isInterface() ?
                createInstanceOfSupportedInterfaceIfApplicable(testedClass, typeToInject, injectionPoint, injectionProvider) :
                createAndRegisterNewInstance(typeToInject, injector, injectionPoint, injectionProvider);

        return dependency;
    }

    private void setInjectionProvider(@Nonnull InjectionPointProvider injectionProvider) {
        injectionProvider.parent = this.injectionProvider;
        this.injectionProvider = injectionProvider;
    }

    private static boolean isInstantiableType(@Nonnull Class<?> type) {
        if (type.isPrimitive() || type.isArray() || type.isAnnotation()) {
            return false;
        }

        if (!type.isInterface()) {
            int typeModifiers = type.getModifiers();

            if ((typeModifiers & INVALID_TYPES) != 0 || !isStatic(typeModifiers) && type.isMemberClass()) {
                return false;
            }

            if (type.getClassLoader() == null) {
                return false;
            }
        }

        return true;
    }

    @Nullable
    private Object createInstanceOfSupportedInterfaceIfApplicable(
            @Nonnull TestedClass testedClass, @Nonnull Class<?> typeToInject,
            @Nonnull InjectionPoint injectionPoint, @Nonnull InjectionPointProvider injectionProvider) {
        Object dependency = null;

        if (CommonDataSource.class.isAssignableFrom(typeToInject)) {
            dependency = createAndRegisterDataSource(testedClass, injectionPoint);
        } else if (INJECT_CLASS != null && typeToInject == Provider.class) {
            dependency = createProviderInstance(injectionProvider);
        } else if (CONVERSATION_CLASS != null && typeToInject == Conversation.class) {
            dependency = createAndRegisterConversationInstance();
        } else if (servletDependencies != null && ServletDependencies.isApplicable(typeToInject)) {
            dependency = servletDependencies.createAndRegisterDependency(typeToInject);
        } else if (jpaDependencies != null && JPADependencies.isApplicable(typeToInject)) {
            dependency = jpaDependencies.createAndRegisterDependency(typeToInject, injectionPoint);
        }

        return dependency;
    }

    @Nullable
    private Object createAndRegisterDataSource(@Nonnull TestedClass testedClass, @Nonnull InjectionPoint injectionPoint) {
        TestDataSource dsCreation = new TestDataSource(injectionPoint);
        CommonDataSource dataSource = dsCreation.createIfDataSourceDefinitionAvailable(testedClass);

        if (dataSource != null) {
            injectionState.saveInstantiatedDependency(injectionPoint, dataSource);
        }

        return dataSource;
    }

    @Nonnull
    private Object createProviderInstance(@Nonnull InjectionPointProvider injectionProvider) {
        ParameterizedType genericType = (ParameterizedType) injectionProvider.getDeclaredType();
        final Class<?> providedClass = (Class<?>) genericType.getActualTypeArguments()[0];

        if (providedClass.isAnnotationPresent(Singleton.class)) {
            return new Provider<Object>() {
                private Object dependency;

                @Override
                public synchronized Object get() {
                    if (dependency == null) {
                        dependency = createNewInstance(providedClass);
                    }

                    return dependency;
                }
            };
        }

        return new Provider<Object>() {
            @Override
            public Object get() {
                Object dependency = createNewInstance(providedClass);
                return dependency;
            }
        };
    }

    @Nullable
    private Object createNewInstance(@Nonnull Class<?> dependencyClass) {
        if (dependencyClass.isInterface()) {
            return null;
        }

        if (dependencyClass.getClassLoader() == null) {
            return newInstanceUsingDefaultConstructorIfAvailable(dependencyClass);
        }

        return new TestedObjectCreation(injectionState, this, dependencyClass).create();
    }

    @Nonnull
    private Object createAndRegisterConversationInstance() {
        Conversation conversation = new TestConversation();

        InjectionPoint injectionPoint = new InjectionPoint(Conversation.class);
        injectionState.saveInstantiatedDependency(injectionPoint, conversation);
        return conversation;
    }

    @Nullable
    private Object createAndRegisterNewInstance(
            @Nonnull Class<?> typeToInstantiate, @Nonnull Injector injector, @Nonnull InjectionPoint injectionPoint,
            @Nonnull InjectionPointProvider injectionProvider) {
        Object dependency = createNewInstance(typeToInstantiate);

        if (dependency != null) {
            if (injectionPoint.name == null) {
                injectionPoint = new InjectionPoint(injectionPoint.type, injectionProvider.getName());
            }

            registerNewInstance(injector, injectionPoint, dependency);
        }

        return dependency;
    }

    private void registerNewInstance(
            @Nonnull Injector injector, @Nonnull InjectionPoint injectionPoint, @Nonnull Object dependency) {
        injectionState.saveInstantiatedDependency(injectionPoint, dependency);

        Class<?> instantiatedClass = dependency.getClass();

        if (injector.testedClass.isClassFromSameModuleOrSystemAsTestedClass(instantiatedClass)) {
            injector.fillOutDependenciesRecursively(dependency);
            injectionState.lifecycleMethods.findLifecycleMethods(instantiatedClass);
            injectionState.lifecycleMethods.executeInitializationMethodsIfAny(instantiatedClass, dependency);
        }
    }

    @Override
    public String toString() {
        String description = "@Tested object \"" + testedClass.getSimpleName() + ' ' + testedName + '"';

        if (injectionProvider != null) {
            InjectionPointProvider parentInjectionProvider = injectionProvider.parent;

            if (parentInjectionProvider != null) {
                description = parentInjectionProvider + "\r\n  of " + description;
            }
        }

        return description;
    }
}
