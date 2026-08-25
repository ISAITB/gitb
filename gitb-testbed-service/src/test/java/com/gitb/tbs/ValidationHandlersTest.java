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

package com.gitb.tbs;

import com.gitb.core.AnyContent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import static com.gitb.tbs.TdlTestHelper.*;

class ValidationHandlersTest extends BaseIntegrationTest {

    @BeforeAll
    static void stubAll() {
        for (String id : new String[]{
                "val-expr-pass",
                "val-expr-fail",
                "val-expr-warning",
                "val-number-equal",
                "val-number-range",
                "val-number-fail",
                "val-string-equal",
                "val-string-contains",
                "val-string-fail",
                "val-regexp-match",
                "val-regexp-fail",
                "val-xpath-all-pass",
                "val-xpath-partial-fail",
                "val-xml-valid",
                "val-xml-invalid",
                "val-xml-schematron",
                "val-json-valid",
                "val-json-invalid",
                "val-xmlmatch-match",
                "val-xmlmatch-nomatch",
                "val-stop-on-error",
                "val-continue-after-fail",
                "val-pdf-core-valid",
                "val-pdf-core-damaged-lenient",
                "val-pdf-core-damaged-strict",
                "val-pdf-unparseable",
                "val-pdf-encrypted-no-password",
                "val-pdf-encrypted-with-password",
                "val-pdf-profile-pass",
                "val-pdf-profile-fail",
                "val-pdf-profiles-multiple",
                "val-pdf-profiles-list",
                "val-pdf-profiles-invalid",
                "val-pdf-profiles-case",
                "val-pdf-core-header-lenient",
                "val-pdf-core-header-strict",
                "val-pdf-core-xref-lenient",
                "val-pdf-core-xref-strict",
                "val-pdf-core-pages-lenient",
                "val-pdf-core-pages-strict",
                "val-pdf-core-content-lenient",
                "val-pdf-core-content-strict",
                "val-pdf-core-object-lenient",
                "val-pdf-core-object-strict"
        }) {
            stubTdl(id, "tdl/val/" + id + ".xml");
        }
    }

    @Test
    void exprPass() throws Exception {
        assertSuccess(run("val-expr-pass"));
    }

    @Test
    void exprFail() throws Exception {
        assertFailed(run("val-expr-fail"));
    }

    @Test
    void exprWarning() throws Exception {
        assertSuccess(run("val-expr-warning"));
    }

    @Test
    void numberEqual() throws Exception {
        assertSuccess(run("val-number-equal"));
    }

    @Test
    void numberRange() throws Exception {
        assertSuccess(run("val-number-range"));
    }

    @Test
    void numberFail() throws Exception {
        assertFailed(run("val-number-fail"));
    }

    @Test
    void stringEqual() throws Exception {
        assertSuccess(run("val-string-equal"));
    }

    @Test
    void stringContains() throws Exception {
        assertSuccess(run("val-string-contains"));
    }

    @Test
    void stringFail() throws Exception {
        assertFailed(run("val-string-fail"));
    }

    @Test
    void regexpMatch() throws Exception {
        assertSuccess(run("val-regexp-match"));
    }

    @Test
    void regexpFail() throws Exception {
        assertFailed(run("val-regexp-fail"));
    }

    @Test
    void xpathAllPass() throws Exception {
        assertSuccess(run("val-xpath-all-pass"));
    }

    @Test
    void xpathPartialFail() throws Exception {
        assertFailed(run("val-xpath-partial-fail"));
    }

    @Test
    void xmlValid() throws Exception {
        assertSuccess(run("val-xml-valid"));
    }

    @Test
    void xmlInvalid() throws Exception {
        assertFailed(run("val-xml-invalid"));
    }

    @Test
    void xmlSchematron() throws Exception {
        assertSuccess(run("val-xml-schematron"));
    }

    @Test
    void jsonValid() throws Exception {
        assertSuccess(run("val-json-valid"));
    }

    @Test
    void jsonInvalid() throws Exception {
        assertFailed(run("val-json-invalid"));
    }

    @Test
    void xmlMatchMatch() throws Exception {
        assertSuccess(run("val-xmlmatch-match"));
    }

    @Test
    void xmlMatchNoMatch() throws Exception {
        assertFailed(run("val-xmlmatch-nomatch"));
    }

    @Test
    void stopOnError() throws Exception {
        assertFailed(run("val-stop-on-error"));
    }

    @Test
    void continueAfterFail() throws Exception {
        assertFailed(run("val-continue-after-fail"));
    }

    // PdfValidator tests

    @Test
    void pdfCoreValid() throws Exception {
        assertSuccess(run("val-pdf-core-valid", null, pdfInput("plain.pdf")));
    }

    @Test
    void pdfCoreDamagedLenient() throws Exception {
        assertSuccess(run("val-pdf-core-damaged-lenient", null, pdfInput("damaged.pdf")));
    }

    @Test
    void pdfCoreDamagedStrict() throws Exception {
        assertFailed(run("val-pdf-core-damaged-strict", null, pdfInput("damaged.pdf")));
    }

    @Test
    void pdfUnparseable() throws Exception {
        assertFailed(run("val-pdf-unparseable"));
    }

    @Test
    void pdfEncryptedNoPassword() throws Exception {
        assertFailed(run("val-pdf-encrypted-no-password", null, pdfInput("encrypted.pdf")));
    }

    @Test
    void pdfEncryptedWithPassword() throws Exception {
        assertSuccess(run("val-pdf-encrypted-with-password", null, pdfInput("encrypted.pdf")));
    }

    @Test
    void pdfProfilePass() throws Exception {
        assertSuccess(run("val-pdf-profile-pass", null, pdfInput("pdfa-1b.pdf")));
    }

    @Test
    void pdfProfileFail() throws Exception {
        assertFailed(run("val-pdf-profile-fail", null, pdfInput("plain.pdf")));
    }

    @Test
    void pdfProfilesMultiple() throws Exception {
        assertFailed(run("val-pdf-profiles-multiple", null, pdfInput("pdfa-1b.pdf")));
    }

    @Test
    void pdfProfilesList() throws Exception {
        assertFailed(run("val-pdf-profiles-list", null, pdfInput("pdfa-1b.pdf")));
    }

    @Test
    void pdfProfilesInvalid() throws Exception {
        run("val-pdf-profiles-invalid", null, pdfInput("pdfa-1b.pdf"))
                .assertSuccess()
                .assertLogs(List.of("Ignoring unknown PDF conformance profile [NOT_A_PROFILE]"));
    }

    @Test
    void pdfProfilesCase() throws Exception {
        assertSuccess(run("val-pdf-profiles-case", null, pdfInput("pdfa-1b.pdf")));
    }

    // CORE-* rule reachability tests: each rule below has a fixture crafted to trigger exactly that
    // rule (verified independently against the real PdfValidator before being added here), asserted
    // via strictMode - only that rule's finding can turn a lenient (SUCCESS) result into FAILURE.

    @Test
    void pdfCoreHeaderLenient() throws Exception {
        assertSuccess(run("val-pdf-core-header-lenient", null, pdfInput("bad-header-version.pdf")));
    }

    @Test
    void pdfCoreHeaderStrict() throws Exception {
        assertFailed(run("val-pdf-core-header-strict", null, pdfInput("bad-header-version.pdf")));
    }

    @Test
    void pdfCoreXrefLenient() throws Exception {
        assertSuccess(run("val-pdf-core-xref-lenient", null, pdfInput("bad-xref-line.pdf")));
    }

    @Test
    void pdfCoreXrefStrict() throws Exception {
        assertFailed(run("val-pdf-core-xref-strict", null, pdfInput("bad-xref-line.pdf")));
    }

    @Test
    void pdfCorePagesLenient() throws Exception {
        assertSuccess(run("val-pdf-core-pages-lenient", null, pdfInput("zero-pages.pdf")));
    }

    @Test
    void pdfCorePagesStrict() throws Exception {
        assertFailed(run("val-pdf-core-pages-strict", null, pdfInput("zero-pages.pdf")));
    }

    @Test
    void pdfCoreContentLenient() throws Exception {
        assertSuccess(run("val-pdf-core-content-lenient", null, pdfInput("bad-content.pdf")));
    }

    @Test
    void pdfCoreContentStrict() throws Exception {
        assertFailed(run("val-pdf-core-content-strict", null, pdfInput("bad-content.pdf")));
    }

    @Test
    void pdfCoreObjectLenient() throws Exception {
        assertSuccess(run("val-pdf-core-object-lenient", null, pdfInput("warning-object.pdf")));
    }

    @Test
    void pdfCoreObjectStrict() throws Exception {
        assertFailed(run("val-pdf-core-object-strict", null, pdfInput("warning-object.pdf")));
    }

    private List<AnyContent> pdfInput(String fixtureFileName) {
        String resourcePath = "pdf/" + fixtureFileName;
        try (InputStream is = ValidationHandlersTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Classpath resource not found: " + resourcePath);
            }
            return List.of(binaryInput("content", is.readAllBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
