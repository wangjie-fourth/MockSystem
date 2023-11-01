/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.injection.full;

import org.github.fourth.mocksystem.packages.mockit.internal.injection.InjectionPoint;
import org.github.fourth.mocksystem.packages.mockit.internal.injection.TestedClass;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.sql.CommonDataSource;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class TestDataSource {
    @Nullable
    private final String dsName;
    private Class<? extends CommonDataSource> dsClass;
    private CommonDataSource ds;

    TestDataSource(@Nonnull InjectionPoint injectionPoint) {
        dsName = injectionPoint.name;
    }

    @Nullable
    CommonDataSource createIfDataSourceDefinitionAvailable(@Nonnull TestedClass testedClass) {
        if (dsName == null) {
            return null;
        }

        Class<?> targetClass = testedClass.targetClass;

        do {
            createDataSource(targetClass);

            if (ds != null) {
                return ds;
            }

            targetClass = targetClass.getSuperclass();
        }
        while (targetClass != Object.class);

        throw new IllegalStateException(
                "Missing @DataSourceDefinition of name \"" + dsName + "\" on " + testedClass.nameOfTestedClass);
    }

    private void createDataSource(@Nonnull Class<?> targetClass) {
        for (Annotation annotation : targetClass.getDeclaredAnnotations()) {
            String annotationName = annotation.annotationType().getName();

            if ("javax.annotation.sql.DataSourceDefinitions".equals(annotationName)) {
                createDataSource((DataSourceDefinitions) annotation);
            } else if ("javax.annotation.sql.DataSourceDefinition".equals(annotationName)) {
                createDataSource((DataSourceDefinition) annotation);
            }

            if (ds != null) {
                return;
            }
        }
    }

    private void createDataSource(@Nonnull DataSourceDefinitions dsDefs) {
        for (DataSourceDefinition dsDef : dsDefs.value()) {
            createDataSource(dsDef);

            if (ds != null) {
                return;
            }
        }
    }

    private void createDataSource(@Nonnull DataSourceDefinition dsDef) {
        String configuredDataSourceName = InjectionPoint.getNameFromJNDILookup(dsDef.name());

        if (configuredDataSourceName.equals(dsName)) {
            instantiateConfiguredDataSourceClass(dsDef);
            setDataSourcePropertiesFromConfiguredValues(dsDef);
        }
    }

    private void instantiateConfiguredDataSourceClass(@Nonnull DataSourceDefinition dsDef) {
        String className = dsDef.className();

        try {
            //noinspection unchecked
            dsClass = (Class<? extends CommonDataSource>) Class.forName(className);
            ds = dsClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void setDataSourcePropertiesFromConfiguredValues(@Nonnull DataSourceDefinition dsDef) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(dsClass, Object.class);
            PropertyDescriptor[] properties = beanInfo.getPropertyDescriptors();

            setProperty(properties, "url", dsDef.url());
            setProperty(properties, "user", dsDef.user());
            setProperty(properties, "password", dsDef.password());
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void setProperty(@Nonnull PropertyDescriptor[] properties, @Nonnull String name, @Nonnull String value)
            throws InvocationTargetException, IllegalAccessException {
        for (PropertyDescriptor property : properties) {
            if (property.getName().equals(name)) {
                Method writeMethod = property.getWriteMethod();

                if (writeMethod != null) {
                    writeMethod.invoke(ds, value);
                }

                return;
            }
        }
    }
}
