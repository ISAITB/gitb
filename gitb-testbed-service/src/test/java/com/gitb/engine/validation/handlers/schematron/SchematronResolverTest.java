/*
 * Copyright (C) 2026 European Union
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

package com.gitb.engine.validation.handlers.schematron;

import com.gitb.exceptions.GITBEngineInternalError;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SchematronResolverTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.org/rules.sch",
            "https://example.org/rules.sch",
            "ftp://example.org/rules.sch",
            "file:///etc/rules.sch",
            "file:/etc/rules.sch",
            "jar:file:/tmp/a.zip!/rules.sch",
            "C:\\temp\\rules.sch"
    })
    void testReferencesOutsideTheTestSuiteAreRejected(String href) {
        var resolver = new SchematronResolver("suite", "testCase", "schematron/main.sch");
        assertNull(resolver.getRejectionMessage());
        var error = assertThrows(GITBEngineInternalError.class, () -> resolver.getResolvedSchematronResource(href));
        assertTrue(error.getMessage().contains(href), error.getMessage());
        assertEquals(error.getMessage(), resolver.getRejectionMessage());
        // The same applies to XSLT-based Schematron.
        assertThrows(GITBEngineInternalError.class, () -> new SchematronResolver("suite", "testCase", "schematron/main.xslt").resolve(href, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"rules.sch", "sub/dir/rules.sch", "../rules.sch", "dir/a:b.sch"})
    void testRelativeReferencesAreNotRejectedAsRemote(String href) {
        var resolver = new SchematronResolver("suite", "testCase", null);
        // Relative references can only be resolved if the Schematron is itself part of the test suite.
        var error = assertThrows(GITBEngineInternalError.class, () -> resolver.getResolvedSchematronResource(href));
        assertTrue(error.getMessage().contains("not part of the test suite"), error.getMessage());
        assertNull(resolver.getRejectionMessage());
    }

}
