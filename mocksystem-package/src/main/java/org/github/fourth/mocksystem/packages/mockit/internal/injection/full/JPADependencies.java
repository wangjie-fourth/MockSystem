/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package org.github.fourth.mocksystem.packages.mockit.internal.injection.full;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.github.fourth.mocksystem.packages.mockit.internal.injection.InjectionPoint;
import org.github.fourth.mocksystem.packages.mockit.internal.injection.InjectionState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;

/**
 * Detects and resolves dependencies belonging to the {@code javax.persistence} API, namely {@code EntityManagerFactory}
 * and {@code EntityManager}.
 */
@SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE")
final class JPADependencies {
    static boolean isApplicable(@Nonnull Class<?> dependencyType) {
        return dependencyType == EntityManager.class || dependencyType == EntityManagerFactory.class;
    }

    @Nonnull
    private final InjectionState injectionState;
    @Nullable
    private String defaultPersistenceUnitName;

    JPADependencies(@Nonnull InjectionState injectionState) {
        this.injectionState = injectionState;
    }

    @Nullable
    String getDependencyIdIfAvailable(@Nonnull Annotation annotation) {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        String unitName = null;

        if (annotationType == PersistenceUnit.class) {
            unitName = ((PersistenceUnit) annotation).unitName();
        } else if (annotationType == PersistenceContext.class) {
            unitName = ((PersistenceContext) annotation).unitName();
        }

        return unitName == null ? null : getNameOfPersistentUnit(unitName);
    }

    @Nonnull
    private String getNameOfPersistentUnit(@Nullable String injectionPointName) {
        return injectionPointName != null && !injectionPointName.isEmpty() ?
                injectionPointName : discoverNameOfDefaultPersistenceUnit();
    }

    @Nonnull
    private String discoverNameOfDefaultPersistenceUnit() {
        if (defaultPersistenceUnitName != null) {
            return defaultPersistenceUnitName;
        }

        defaultPersistenceUnitName = "<unknown>";
        InputStream xmlFile = getClass().getResourceAsStream("/META-INF/persistence.xml");

        if (xmlFile != null) {
            try {
                SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                parser.parse(xmlFile, new DefaultHandler() {
                    @Override
                    public void startElement(String uri, String localName, String qName, Attributes attributes) {
                        if ("persistence-unit".equals(qName)) {
                            defaultPersistenceUnitName = attributes.getValue("name");
                        }
                    }
                });
                xmlFile.close();
            } catch (ParserConfigurationException ignore) {
            } catch (SAXException ignore) {
            } catch (IOException ignore) {
            }
        }

        return defaultPersistenceUnitName;
    }

    @Nonnull
    Object createAndRegisterDependency(@Nonnull Class<?> dependencyType, @Nonnull InjectionPoint dependencyKey) {
        if (dependencyType == EntityManagerFactory.class) {
            InjectionPoint injectionPoint = createFactoryInjectionPoint(dependencyKey);
            EntityManagerFactory emFactory = createAndRegisterEntityManagerFactory(injectionPoint);
            return emFactory;
        }

        return createAndRegisterEntityManager(dependencyKey);
    }

    @Nonnull
    private InjectionPoint createFactoryInjectionPoint(@Nonnull InjectionPoint injectionPoint) {
        String persistenceUnitName = getNameOfPersistentUnit(injectionPoint.name);
        return new InjectionPoint(EntityManagerFactory.class, persistenceUnitName);
    }

    @Nonnull
    private EntityManagerFactory createAndRegisterEntityManagerFactory(@Nonnull InjectionPoint injectionPoint) {
        String persistenceUnitName = injectionPoint.name;
        EntityManagerFactory emFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
        injectionState.saveGlobalDependency(injectionPoint, emFactory);
        return emFactory;
    }

    @Nonnull
    private EntityManager createAndRegisterEntityManager(@Nonnull InjectionPoint injectionPoint) {
        InjectionPoint emFactoryKey = createFactoryInjectionPoint(injectionPoint);
        EntityManagerFactory emFactory = injectionState.getGlobalDependency(emFactoryKey);

        if (emFactory == null) {
            emFactory = createAndRegisterEntityManagerFactory(emFactoryKey);
        }

        EntityManager entityManager = emFactory.createEntityManager();
        injectionState.saveInstantiatedDependency(injectionPoint, entityManager);
        return entityManager;
    }
}
