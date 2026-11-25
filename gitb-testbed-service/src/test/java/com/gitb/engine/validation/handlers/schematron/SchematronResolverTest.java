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

import com.gitb.engine.ModuleManager;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.repository.ITestCaseRepository;
import net.sf.saxon.TransformerFactoryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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
    @CsvSource(value = {
            "schematron/main.xslt|file:///schematron/main.xslt",
            "main.xslt|file:///main.xslt",
            "dir with space/main.xslt|file:///dir%20with%20space/main.xslt",
            "NULL|file:///schematron.xslt",
            "' '|file:///schematron.xslt"
    }, delimiter = '|', nullValues = "NULL")
    void testSystemIdIsAbsoluteAndIndependentOfTheWorkingDirectory(String path, String expected) {
        var systemId = new SchematronResolver("suite", "testCase", path).getSystemId();
        assertEquals(expected, systemId);
        // The system ID is already absolute so it cannot be made absolute against the working directory.
        assertTrue(URI.create(systemId).isAbsolute());
    }

    @Test
    void testRelativeReferencesOfRulesOutsideTheTestSuiteFailClearlyForXslt() {
        var resolver = new SchematronResolver("suite", "testCase", null);
        var error = assertThrows(GITBEngineInternalError.class, () -> resolver.resolve("valuesets.xml", resolver.getSystemId()));
        assertTrue(error.getMessage().contains("not part of the test suite"), error.getMessage());
        assertNull(resolver.getRejectionMessage());
    }

    /*
     * Regression tests for ISAITB/gitb#282: since ph-schematron 10.3.0 the XSLT is compiled from a resource, whose
     * system ID becomes the base of the references it makes (document(), xsl:include). A relative system ID is made
     * absolute by Saxon against the working directory of the process (e.g. /itbsrv in the gitb-srv image), which
     * made every reference resolve to a path that does not exist in the test suite.
     */

    private static final String VALUE_SETS = "<values><v>from-test-suite</v></values>";
    private static final String DOCUMENT_XSLT = """
            <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
                <xsl:template match="/"><out><xsl:value-of select="document('valuesets.xml')/values/v"/></out></xsl:template>
            </xsl:stylesheet>
            """;

    /**
     * Run the provided action with a test suite repository that serves the given artifacts (by path within the test
     * suite) and records the paths that were requested from it.
     */
    private static <T> T withRepository(Map<String, String> artifacts, List<String> requestedPaths, Supplier<T> action) {
        var repository = mock(ITestCaseRepository.class);
        when(repository.getTestArtifact(any(), any(), anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(2);
            requestedPaths.add(path);
            String content = artifacts.get(path);
            return content == null ? null : new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        });
        try (MockedStatic<ModuleManager> moduleManager = mockStatic(ModuleManager.class)) {
            var instance = mock(ModuleManager.class);
            when(instance.getTestCaseRepository()).thenReturn(repository);
            moduleManager.when(ModuleManager::getInstance).thenReturn(instance);
            return action.get();
        }
    }

    @ParameterizedTest
    @CsvSource(value = {
            // Rules path | base URI | reference | expected artifact path
            "schematron/rules.xslt|file:///schematron/rules.xslt|valuesets.xml|schematron/valuesets.xml",
            "rules.xslt|file:///rules.xslt|valuesets.xml|valuesets.xml",
            "a/b/rules.xslt|file:///a/b/rules.xslt|c/data.xml|a/b/c/data.xml",
            "dir with space/rules.xslt|file:///dir%20with%20space/rules.xslt|valuesets.xml|dir with space/valuesets.xml",
            // A module resolved relative to the rules, that makes its own references.
            "schematron/rules.xslt|file:///schematron/sub/module.xslt|data.xml|schematron/sub/data.xml",
            // Without a base, or with one that is not ours, the folder of the rules is used.
            "schematron/rules.xslt|NULL|valuesets.xml|schematron/valuesets.xml",
            "schematron/rules.xslt| |valuesets.xml|schematron/valuesets.xml",
            "schematron/rules.xslt|file:/itbsrv/schematron/rules.xslt|valuesets.xml|schematron/valuesets.xml",
            "schematron/rules.xslt|/itbsrv/schematron/rules.xslt|valuesets.xml|schematron/valuesets.xml"
    }, delimiter = '|', nullValues = "NULL")
    void testReferencesAreResolvedRelativeToTheBaseWithinTheTestSuite(String rulesPath, String baseUri, String href, String expectedArtifactPath) {
        var requested = new ArrayList<String>();
        var resolver = new SchematronResolver("suite", "testCase", rulesPath);
        var source = withRepository(Map.of(expectedArtifactPath, VALUE_SETS), requested, () -> resolver.resolve(href, baseUri));
        assertNotNull(source);
        assertEquals(List.of(expectedArtifactPath), requested);
    }

    @Test
    void testMissingArtifactIsAnErrorAndNeverLeftToTheProcessor() {
        var requested = new ArrayList<String>();
        var resolver = new SchematronResolver("suite", "testCase", "schematron/rules.xslt");
        // Returning null would let Saxon resolve the reference by itself (e.g. from the local file system).
        var error = assertThrows(GITBEngineInternalError.class, () -> withRepository(Map.of(), requested, () -> resolver.resolve("missing.xml", resolver.getSystemId())));
        assertTrue(error.getMessage().contains("schematron/missing.xml"), error.getMessage());
        assertEquals(List.of("schematron/missing.xml"), requested);
    }

    @Test
    void testDocumentReferenceOfCompiledXsltIsResolvedWithinTheTestSuite() throws Exception {
        var requested = new ArrayList<String>();
        var resolver = new SchematronResolver("suite", "testCase", "schematron/rules.xslt");
        var output = withRepository(Map.of("schematron/valuesets.xml", VALUE_SETS), requested, () -> transform(resolver, resolver.getSystemId()));
        assertTrue(output.contains("from-test-suite"), output);
        assertEquals(List.of("schematron/valuesets.xml"), requested);
    }

    @Test
    void testRelativeSystemIdWouldResolveAgainstTheWorkingDirectory() throws Exception {
        // Documents why the system ID must be absolute: with the relative path of the rules, the processor derives the base
        // from the working directory and the reference no longer points to the test suite's artifact.
        var requested = new ArrayList<String>();
        var resolver = new SchematronResolver("suite", "testCase", "schematron/rules.xslt");
        assertThrows(Exception.class, () -> withRepository(Map.of("schematron/valuesets.xml", VALUE_SETS), requested, () -> transform(resolver, "schematron/rules.xslt")));
        assertFalse(requested.contains("schematron/valuesets.xml"), requested.toString());
    }

    private static String transform(SchematronResolver resolver, String systemId) {
        try {
            TransformerFactory factory = new TransformerFactoryImpl();
            factory.setURIResolver(resolver);
            var templates = factory.newTemplates(new StreamSource(new StringReader(DOCUMENT_XSLT), systemId));
            var output = new StringWriter();
            templates.newTransformer().transform(new StreamSource(new StringReader("<in/>")), new StreamResult(output));
            return output.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
