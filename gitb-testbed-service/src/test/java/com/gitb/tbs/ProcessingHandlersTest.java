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

class ProcessingHandlersTest extends BaseIntegrationTest {

    @BeforeAll
    static void stubAll() {
        for (String id : new String[]{
                "proc-tokgen-uuid",
                "proc-tokgen-string",
                "proc-tokgen-number",
                "proc-tokgen-timestamp",
                "proc-template-basic",
                "proc-template-conditional",
                "proc-template-list",
                "proc-base64-encode",
                "proc-base64-decode",
                "proc-base64-roundtrip",
                "proc-xpath-extract",
                "proc-xpath-predicate",
                "proc-jsonpath-extract",
                "proc-jsonpath-filter",
                "proc-jsonptr-extract",
                "proc-regexp-match",
                "proc-regexp-collect",
                "proc-collection-append",
                "proc-collection-remove",
                "proc-collection-size",
                "proc-varutils-exists",
                "proc-delay-basic",
                "proc-xslt-transform",
                "proc-varutils-type"
        }) {
            stubTdl(id, "tdl/proc/" + id + ".xml");
        }
    }

    @Test
    void tokenGeneratorUuid() throws Exception {
        assertSuccess(run("proc-tokgen-uuid"));
    }

    @Test
    void tokenGeneratorString() throws Exception {
        assertSuccess(run("proc-tokgen-string"));
    }

    @Test
    void tokenGeneratorNumber() throws Exception {
        assertSuccess(run("proc-tokgen-number"));
    }

    @Test
    void tokenGeneratorTimestamp() throws Exception {
        assertSuccess(run("proc-tokgen-timestamp"));
    }

    @Test
    void templateBasic() throws Exception {
        assertSuccess(run("proc-template-basic"));
    }

    @Test
    void templateConditional() throws Exception {
        assertSuccess(run("proc-template-conditional"));
    }

    @Test
    void templateList() throws Exception {
        assertSuccess(run("proc-template-list"));
    }

    @Test
    void base64Encode() throws Exception {
        assertSuccess(run("proc-base64-encode"));
    }

    @Test
    void base64Decode() throws Exception {
        assertSuccess(run("proc-base64-decode"));
    }

    @Test
    void base64Roundtrip() throws Exception {
        assertSuccess(run("proc-base64-roundtrip"));
    }

    @Test
    void xpathExtract() throws Exception {
        assertSuccess(run("proc-xpath-extract"));
    }

    @Test
    void xpathPredicate() throws Exception {
        assertSuccess(run("proc-xpath-predicate"));
    }

    @Test
    void jsonPathExtract() throws Exception {
        assertSuccess(run("proc-jsonpath-extract"));
    }

    @Test
    void jsonPathFilter() throws Exception {
        assertSuccess(run("proc-jsonpath-filter"));
    }

    @Test
    void jsonPointerExtract() throws Exception {
        assertSuccess(run("proc-jsonptr-extract"));
    }

    @Test
    void regexpMatch() throws Exception {
        assertSuccess(run("proc-regexp-match"));
    }

    @Test
    void regexpCollect() throws Exception {
        assertSuccess(run("proc-regexp-collect"));
    }

    @Test
    void collectionAppend() throws Exception {
        assertSuccess(run("proc-collection-append"));
    }

    @Test
    void collectionRemove() throws Exception {
        assertSuccess(run("proc-collection-remove"));
    }

    @Test
    void collectionSize() throws Exception {
        assertSuccess(run("proc-collection-size"));
    }

    @Test
    void varUtilsExists() throws Exception {
        assertSuccess(run("proc-varutils-exists"));
    }

    @Test
    void delayBasic() throws Exception {
        assertSuccess(run("proc-delay-basic"));
    }

    @Test
    void xsltTransform() throws Exception {
        assertSuccess(run("proc-xslt-transform"));
    }

    @Test
    void varUtilsType() throws Exception {
        assertSuccess(run("proc-varutils-type"));
    }
}
