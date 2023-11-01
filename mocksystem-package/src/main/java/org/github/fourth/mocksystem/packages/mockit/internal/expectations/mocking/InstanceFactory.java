/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking;

import org.github.fourth.mocksystem.packages.mockit.internal.reflection.ConstructorReflection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class InstanceFactory {
    @Nonnull
    final Class<?> concreteClass;
    @Nullable
    Object lastInstance;

    InstanceFactory(@Nonnull Class<?> concreteClass) {
        this.concreteClass = concreteClass;
    }

    @Nonnull
    public abstract Object create();

    @Nullable
    public final Object getLastInstance() {
        return lastInstance;
    }

    public abstract void clearLastInstance();

    static final class InterfaceInstanceFactory extends InstanceFactory {
        @Nullable
        private Object emptyProxy;

        InterfaceInstanceFactory(@Nonnull Object emptyProxy) {
            super(emptyProxy.getClass());
            this.emptyProxy = emptyProxy;
        }

        @Nonnull
        @Override
        public Object create() {
            if (emptyProxy == null) {
                emptyProxy = ConstructorReflection.newUninitializedInstance(concreteClass);
            }

            lastInstance = emptyProxy;
            return emptyProxy;
        }

        @Override
        public void clearLastInstance() {
            emptyProxy = null;
            lastInstance = null;
        }
    }

    static final class ClassInstanceFactory extends InstanceFactory {
        ClassInstanceFactory(@Nonnull Class<?> concreteClass) {
            super(concreteClass);
        }

        @Override
        @Nonnull
        public Object create() {
            lastInstance = ConstructorReflection.newUninitializedInstance(concreteClass);
            return lastInstance;
        }

        @Override
        public void clearLastInstance() {
            lastInstance = null;
        }
    }
}
