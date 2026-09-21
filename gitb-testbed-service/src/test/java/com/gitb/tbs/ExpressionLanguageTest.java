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

class ExpressionLanguageTest extends BaseIntegrationTest {

    @BeforeAll
    static void stubAll() {
        for (String id : new String[]{
                "expr-string-concat",
                "expr-string-functions",
                "expr-numeric-arithmetic",
                "expr-numeric-comparison",
                "expr-boolean-logic",
                "expr-type-str-to-num",
                "expr-type-num-to-str",
                "expr-type-boolean",
                "expr-map-access",
                "expr-map-nested",
                "expr-list-access",
                "expr-list-count",
                "expr-list-string-join",
                "expr-xpath-conditional",
                "expr-xpath-on-xml",
                "expr-xpath-nodeset",
                "expr-pure-ref-preserves-type",
                "expr-embed-casts-string",
                "expr-empty-check",
                "expr-complex-compound"
        }) {
            stubTdl(id, "tdl/expr/" + id + ".xml");
        }
    }

    @Test
    void stringConcat() throws Exception {
        assertSuccess(run("expr-string-concat"));
    }

    @Test
    void stringFunctions() throws Exception {
        assertSuccess(run("expr-string-functions"));
    }

    @Test
    void numericArithmetic() throws Exception {
        assertSuccess(run("expr-numeric-arithmetic"));
    }

    @Test
    void numericComparison() throws Exception {
        assertSuccess(run("expr-numeric-comparison"));
    }

    @Test
    void booleanLogic() throws Exception {
        assertSuccess(run("expr-boolean-logic"));
    }

    @Test
    void typeStrToNum() throws Exception {
        assertSuccess(run("expr-type-str-to-num"));
    }

    @Test
    void typeNumToStr() throws Exception {
        assertSuccess(run("expr-type-num-to-str"));
    }

    @Test
    void typeBoolean() throws Exception {
        assertSuccess(run("expr-type-boolean"));
    }

    @Test
    void mapAccess() throws Exception {
        assertSuccess(run("expr-map-access"));
    }

    @Test
    void mapNested() throws Exception {
        assertSuccess(run("expr-map-nested"));
    }

    @Test
    void listAccess() throws Exception {
        assertSuccess(run("expr-list-access"));
    }

    @Test
    void listCount() throws Exception {
        assertSuccess(run("expr-list-count"));
    }

    @Test
    void listStringJoin() throws Exception {
        assertSuccess(run("expr-list-string-join"));
    }

    @Test
    void xpathConditional() throws Exception {
        assertSuccess(run("expr-xpath-conditional"));
    }

    @Test
    void xpathOnXml() throws Exception {
        assertSuccess(run("expr-xpath-on-xml"));
    }

    @Test
    void xpathNodeset() throws Exception {
        assertSuccess(run("expr-xpath-nodeset"));
    }

    @Test
    void pureRefPreservesType() throws Exception {
        assertSuccess(run("expr-pure-ref-preserves-type"));
    }

    @Test
    void embedCastsString() throws Exception {
        assertSuccess(run("expr-embed-casts-string"));
    }

    @Test
    void emptyCheck() throws Exception {
        assertSuccess(run("expr-empty-check"));
    }

    @Test
    void complexCompound() throws Exception {
        assertSuccess(run("expr-complex-compound"));
    }
}
