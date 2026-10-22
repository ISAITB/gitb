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

import com.gitb.engine.utils.rdf.RdfContentRestrictions;
import com.gitb.engine.utils.rdf.RdfSyntaxNotAllowedException;
import org.apache.jena.riot.Lang;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RdfContentRestrictionsTest {

    @Test
    void testNoRestrictions() {
        var restrictions = RdfContentRestrictions.of(null, null);
        assertFalse(restrictions.isRestricted());
        assertTrue(restrictions.isAccepted(Lang.TURTLE));
        assertTrue(restrictions.isAccepted(Lang.JSONLD));
        assertTrue(restrictions.isAccepted(null));
        assertDoesNotThrow(() -> restrictions.check(null));
    }

    @Test
    void testBlacklist() {
        var restrictions = RdfContentRestrictions.of(null, "application/ld+json");
        assertTrue(restrictions.isRestricted());
        assertFalse(restrictions.isAccepted(Lang.JSONLD));
        assertFalse(restrictions.isAccepted(Lang.JSONLD11));
        assertTrue(restrictions.isAccepted(Lang.TURTLE));
        assertTrue(restrictions.isAccepted(Lang.TRIG));
        assertFalse(restrictions.isAccepted(null));
        assertThrows(RdfSyntaxNotAllowedException.class, () -> restrictions.check(Lang.JSONLD));
        assertDoesNotThrow(() -> restrictions.check(Lang.TURTLE));
    }

    @Test
    void testBlacklistCoversEquivalentSyntaxes() {
        var restrictions = RdfContentRestrictions.of(null, "application/json");
        assertFalse(restrictions.isAccepted(Lang.JSONLD11));
        restrictions = RdfContentRestrictions.of(null, "text/xml");
        assertFalse(restrictions.isAccepted(Lang.RDFXML));
    }

    @Test
    void testWhitelist() {
        var restrictions = RdfContentRestrictions.of("text/turtle, application/rdf+xml", null);
        assertTrue(restrictions.isAccepted(Lang.TURTLE));
        assertTrue(restrictions.isAccepted(Lang.RDFXML));
        assertFalse(restrictions.isAccepted(Lang.JSONLD));
        assertFalse(restrictions.isAccepted(Lang.TRIG));
        assertFalse(restrictions.isAccepted(null));
        restrictions = RdfContentRestrictions.of("text/xml", null);
        assertTrue(restrictions.isAccepted(Lang.RDFXML));
    }

    @Test
    void testWhitelistTakesPrecedenceOverBlacklist() {
        var restrictions = RdfContentRestrictions.of("text/turtle", "text/turtle");
        assertTrue(restrictions.isAccepted(Lang.TURTLE));
        assertFalse(restrictions.isAccepted(Lang.RDFXML));
    }

    @Test
    void testBlankValuesAreIgnored() {
        assertSame(RdfContentRestrictions.NONE, RdfContentRestrictions.of("", " "));
        assertSame(RdfContentRestrictions.NONE, RdfContentRestrictions.of(" , ", null));
        var restrictions = RdfContentRestrictions.of("text/turtle,, ", null);
        assertTrue(restrictions.isAccepted(Lang.TURTLE));
        assertFalse(restrictions.isAccepted(Lang.RDFXML));
    }

    @Test
    void testUnknownSyntaxFailsStartup() {
        assertThrows(IllegalStateException.class, () -> RdfContentRestrictions.of(null, "application/ld+jsn"));
        assertThrows(IllegalStateException.class, () -> RdfContentRestrictions.of(null, "application/pdf"));
        assertThrows(IllegalStateException.class, () -> RdfContentRestrictions.of("text/turtle, application/pdf", null));
        assertThrows(IllegalStateException.class, () -> RdfContentRestrictions.of(null, "not a mime type"));
    }

    @Test
    void testToLanguage() {
        assertEquals(Lang.TURTLE, RdfContentRestrictions.toLanguage("text/turtle"));
        assertEquals(Lang.RDFXML, RdfContentRestrictions.toLanguage("application/xml"));
        assertEquals(Lang.RDFXML, RdfContentRestrictions.toLanguage("text/xml; charset=utf-8"));
        assertEquals(Lang.JSONLD, RdfContentRestrictions.toLanguage("application/json"));
        assertNull(RdfContentRestrictions.toLanguage("application/pdf"));
        assertNull(RdfContentRestrictions.toLanguage(""));
        assertNull(RdfContentRestrictions.toLanguage((String) null));
    }

}
