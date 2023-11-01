/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal;

import org.github.fourth.mocksystem.packages.mockit.internal.expectations.mocking.MockedBridge;
import org.github.fourth.mocksystem.packages.mockit.internal.mockups.MockMethodBridge;
import org.github.fourth.mocksystem.packages.mockit.internal.mockups.MockupBridge;
import org.github.fourth.mocksystem.packages.mockit.internal.startup.InstrumentationHolder;
import org.github.fourth.mocksystem.packages.mockit.internal.util.ClassLoad;
import org.github.fourth.mocksystem.packages.mockit.internal.util.StackTrace;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationHandler;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public abstract class MockingBridge implements InvocationHandler {
    private static final Object[] EMPTY_ARGS = {};
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static boolean fieldsSet;
    public final String id;

    /**
     * The instance is stored in a place directly accessible through the Java SE API, so that it can
     * be recovered from any class loader.
     */
    protected MockingBridge(@Nonnull String id) {
        this.id = id;
    }

    protected static boolean notToBeMocked(@Nullable Object mocked, @Nonnull String mockedClassDesc) {
        return
                (mocked == null && "java/lang/System".equals(mockedClassDesc) ||
                        mocked != null && instanceOfClassThatParticipatesInClassLoading(mocked.getClass())
                ) && wasCalledDuringClassLoading();
    }

    public static boolean instanceOfClassThatParticipatesInClassLoading(@Nonnull Class<?> mockedClass) {
        return
                mockedClass == System.class || mockedClass == File.class || mockedClass == URL.class ||
                        mockedClass == FileInputStream.class || mockedClass == Manifest.class ||
                        JarFile.class.isAssignableFrom(mockedClass) || JarEntry.class.isAssignableFrom(mockedClass) ||
                        Vector.class.isAssignableFrom(mockedClass) || Hashtable.class.isAssignableFrom(mockedClass);
    }

    private static boolean wasCalledDuringClassLoading() {
        if (LOCK.isHeldByCurrentThread()) {
            return true;
        }

        LOCK.lock();

        try {
            StackTrace st = new StackTrace(new Throwable());
            int n = st.getDepth();

            for (int i = 3; i < n; i++) {
                StackTraceElement ste = st.getElement(i);

                if (
                        "ClassLoader.java".equals(ste.getFileName()) &&
                                "loadClass getResource loadLibrary".contains(ste.getMethodName())
                ) {
                    return true;
                }
            }

            return false;
        } finally {
            LOCK.unlock();
        }
    }

    @Nonnull
    protected static Object[] extractMockArguments(int startingIndex, @Nonnull Object[] args) {
        if (args.length > startingIndex) {
            Object[] mockArgs = new Object[args.length - startingIndex];
            System.arraycopy(args, startingIndex, mockArgs, 0, mockArgs.length);
            return mockArgs;
        }

        return EMPTY_ARGS;
    }

    public static void setMockingBridgeFields() {
        Class<?> hostClass = ClassLoad.loadByInternalName(InstrumentationHolder.hostJREClassName);
        setMockingBridgeField(hostClass, MockedBridge.MB);
        setMockingBridgeField(hostClass, MockupBridge.MB);
        setMockingBridgeField(hostClass, MockMethodBridge.MB);
    }

    private static void setMockingBridgeField(@Nonnull Class<?> hostClass, @Nonnull MockingBridge mockingBridge) {
        try {
            hostClass.getDeclaredField(mockingBridge.id).set(null, mockingBridge);
        } catch (NoSuchFieldException ignore) {
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    public static String getHostClassName() {
        if (!fieldsSet) {
            setMockingBridgeFields();
            fieldsSet = true;
        }

        return InstrumentationHolder.hostJREClassName;
    }
}
