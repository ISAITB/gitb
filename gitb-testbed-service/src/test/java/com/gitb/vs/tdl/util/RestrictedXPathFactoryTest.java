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

package com.gitb.vs.tdl.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.xpath.XPathExpressionException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RestrictedXPathFactoryTest {

    @TempDir
    Path tempDir;

    private String evaluate(String expression) throws Exception {
        return Utils.newRestrictedXPathFactory().newXPath().compile(expression)
                .evaluate(Utils.getSecureDocumentBuilderFactory().newDocumentBuilder().newDocument());
    }

    @Test
    void ordinaryExpressionsAreEvaluated() throws Exception {
        assertEquals("3", evaluate("string(1 + 2)"));
        assertEquals("ab", evaluate("concat('a', 'b')"));
    }

    @Test
    void resourcesCannotBeLookedUpAndThisIsRecognised() throws Exception {
        Path file = Files.writeString(tempDir.resolve("secret.txt"), "TOP-SECRET");
        for (String expression : new String[] {
                "unparsed-text('" + file.toUri() + "')",
                "string(doc('" + file.toUri() + "'))",
                "unparsed-text('http://127.0.0.1:1/x')",
                "string(count(collection('" + tempDir.toUri() + "?select=*.txt')))"
        }) {
            var e = assertThrows(XPathExpressionException.class, () -> evaluate(expression), expression);
            assertTrue(Utils.isRestrictedResourceFailure(e), expression + ": " + e);
            assertFalse(String.valueOf(e.getMessage()).contains("TOP-SECRET"));
        }
    }

    @Test
    void invalidExpressionsAreNotMistakenForRestrictedLookups() {
        var e = assertThrows(XPathExpressionException.class, () -> evaluate("1 +"));
        assertFalse(Utils.isRestrictedResourceFailure(e));
    }

    @Test
    void environmentIsHidden() throws Exception {
        assertEquals("", evaluate("string(environment-variable('PATH'))"));
    }

}
