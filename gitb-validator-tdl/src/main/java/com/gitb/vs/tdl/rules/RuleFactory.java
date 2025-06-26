/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.vs.tdl.rules;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.*;

public class RuleFactory {

    private static final Logger LOG = LoggerFactory.getLogger(RuleFactory.class);
    private static final RuleFactory INSTANCE = new RuleFactory();

    private final List<Class<? extends AbstractCheck>> checkClasses = new ArrayList<>();
    private final List<Class<? extends TestCaseObserver>> testCaseObserverClasses = new ArrayList<>();

    public static RuleFactory getInstance() {
        return INSTANCE;
    }

    private RuleFactory() {
        Reflections reflections = new Reflections(RuleFactory.class.getPackage().getName());
        Set<Class<? extends AbstractCheck>> subTypes = reflections.getSubTypesOf(AbstractCheck.class);
        for (Class<? extends AbstractCheck> clazz: subTypes) {
            if (!Modifier.isAbstract(clazz.getModifiers())) {
                checkClasses.add(clazz);
            }
        }
        Set<Class<? extends TestCaseObserver>> observerClasses = reflections.getSubTypesOf(TestCaseObserver.class);
        for (Class<? extends TestCaseObserver> clazz: observerClasses) {
            if (!Modifier.isAbstract(clazz.getModifiers())) {
                testCaseObserverClasses.add(clazz);
            }
        }
        LOG.info("Loaded {} check classes and {} test case observers", checkClasses.size(), testCaseObserverClasses.size());
    }

    public List<AbstractCheck> getChecks() {
        List<AbstractCheck> checks = new ArrayList<>(checkClasses.size());
        boolean needsSorting = false;
        for (Class<? extends AbstractCheck> checkClass: checkClasses) {
            try {
                AbstractCheck check = checkClass.getDeclaredConstructor().newInstance();
                if (check.getOrder() != AbstractCheck.DEFAULT_ORDER) {
                    needsSorting = true;
                }
                checks.add(check);
            } catch (Exception e) {
                throw new IllegalStateException("Error while creating check instances", e);
            }
        }
        if (needsSorting) {
            checks.sort(Comparator.comparingInt(AbstractCheck::getOrder));
        }
        return Collections.unmodifiableList(checks);
    }

    public List<TestCaseObserver> getTestCaseObservers() {
        List<TestCaseObserver> observers = new ArrayList<>(testCaseObserverClasses.size());
        for (Class<? extends TestCaseObserver> observerClass: testCaseObserverClasses) {
            try {
                observers.add(observerClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new IllegalStateException("Error while creating test case observer instances", e);
            }
        }
        return Collections.unmodifiableList(observers);
    }

}
