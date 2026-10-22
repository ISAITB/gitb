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

package com.gitb.engine.utils.rdf;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.gitb.engine.TestEngineConfiguration.ENV_RDF_CONTENT_BLACKLIST;
import static com.gitb.engine.TestEngineConfiguration.ENV_RDF_CONTENT_WHITELIST;

/**
 * The (operator-level) restrictions on the RDF syntaxes that are accepted when parsing RDF content (models, shapes and
 * documents loaded via {@code owl:imports}).
 * <p>
 * If a whitelist is defined only its syntaxes are accepted (and the blacklist is ignored). Otherwise all syntaxes are
 * accepted except those in the blacklist. If neither is defined everything is accepted.
 */
public final class RdfContentRestrictions {

    private static final Logger LOG = LoggerFactory.getLogger(RdfContentRestrictions.class);

    /** No restrictions. */
    public static final RdfContentRestrictions NONE = new RdfContentRestrictions(null, null);

    private static final Map<String, Lang> EQUIVALENT_CONTENT_TYPES = Map.of(
            "application/xml", RDFLanguages.RDFXML,
            "text/xml", RDFLanguages.RDFXML,
            "application/json", RDFLanguages.JSONLD
    );

    private final Set<Lang> accepted;
    private final Set<Lang> rejected;

    private RdfContentRestrictions(Set<Lang> accepted, Set<Lang> rejected) {
        this.accepted = accepted;
        this.rejected = rejected;
    }

    /**
     * Create the restrictions from the configured values.
     *
     * @param whitelist Comma-separated mime types that are the only ones accepted (may be null or blank).
     * @param blacklist Comma-separated mime types that are not accepted (may be null or blank).
     * @return The restrictions.
     * @throws IllegalStateException If a listed mime type is not a known RDF syntax.
     */
    public static RdfContentRestrictions of(String whitelist, String blacklist) {
        Set<Lang> accepted = toLanguages(ENV_RDF_CONTENT_WHITELIST, whitelist);
        Set<Lang> rejected = toLanguages(ENV_RDF_CONTENT_BLACKLIST, blacklist);
        if (accepted != null && rejected != null) {
            LOG.warn("Both {} and {} are set. The blacklist will be ignored.", ENV_RDF_CONTENT_WHITELIST, ENV_RDF_CONTENT_BLACKLIST);
            rejected = null;
        }
        if (accepted != null) {
            LOG.info("An RDF content type whitelist is in force allowing only: {}", accepted.stream().map(Lang::getName).sorted().toList());
        }
        if (rejected != null) {
            LOG.info("An RDF content type blacklist is in force blocking: {}", rejected.stream().map(Lang::getName).sorted().toList());
        }
        if (accepted == null && rejected == null) {
            return NONE;
        }
        return new RdfContentRestrictions(accepted, rejected);
    }

    private static Set<Lang> toLanguages(String propertyName, String values) {
        if (StringUtils.isBlank(values)) {
            return null;
        }
        Set<Lang> languages = new HashSet<>();
        for (String value: Arrays.stream(StringUtils.split(values, ',')).map(String::trim).filter(StringUtils::isNotEmpty).toList()) {
            Lang lang = null;
            try {
                lang = normalise(toLanguage(value));
            } catch (RuntimeException e) {
                // Invalid content type - handled below.
            }
            if (lang == null) {
                throw new IllegalStateException("Property [%s] contains the value [%s] which is not a supported RDF content type".formatted(propertyName, value));
            }
            languages.add(lang);
        }
        return languages.isEmpty() ? null : languages;
    }

    /**
     * Determine the RDF language for a content type (considering also equivalent content types).
     *
     * @param contentType The content type (may include parameters).
     * @return The language or null if the content type is blank or not a known RDF syntax.
     */
    public static Lang toLanguage(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            return null;
        }
        return toLanguage(ContentType.create(contentType));
    }

    /**
     * Determine the RDF language for a content type (considering also equivalent content types).
     *
     * @param contentType The content type.
     * @return The language or null if the content type is not a known RDF syntax.
     */
    public static Lang toLanguage(ContentType contentType) {
        if (contentType == null) {
            return null;
        }
        Lang lang = RDFLanguages.contentTypeToLang(contentType);
        if (lang == null) {
            lang = EQUIVALENT_CONTENT_TYPES.get(contentType.getContentTypeStr());
        }
        return lang;
    }

    /**
     * Ensure that equivalent languages (JSON-LD 1.0 and 1.1) are treated as one.
     *
     * @param lang The language.
     * @return The language to consider.
     */
    public static Lang normalise(Lang lang) {
        return Lang.JSONLD.equals(lang) ? Lang.JSONLD11 : lang;
    }

    /**
     * @return Whether any restriction is in force.
     */
    public boolean isRestricted() {
        return accepted != null || rejected != null;
    }

    /**
     * Check whether the provided syntax is accepted. If restrictions are in force an unknown (null) syntax is not accepted.
     *
     * @param lang The language to check.
     * @return The check result.
     */
    public boolean isAccepted(Lang lang) {
        if (!isRestricted()) {
            return true;
        }
        if (lang == null) {
            return false;
        }
        lang = normalise(lang);
        if (accepted != null) {
            return accepted.contains(lang);
        }
        return !rejected.contains(lang);
    }

    /**
     * Ensure the provided syntax is accepted.
     *
     * @param lang The language to check.
     * @throws RdfSyntaxNotAllowedException If the syntax is not accepted.
     */
    public void check(Lang lang) {
        if (!isAccepted(lang)) {
            throw new RdfSyntaxNotAllowedException(lang == null ? null : lang.getContentType().getContentTypeStr());
        }
    }

}
