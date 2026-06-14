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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
                "val-continue-after-fail"
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
}
