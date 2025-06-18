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

package utils;

import org.owasp.html.CssSchema;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import java.util.Set;

import static org.owasp.html.Sanitizers.*;

/**
 * Class with static utilities to handle HTML content.
 */
public class HtmlUtil {

    private final static PolicyFactory FULL_EDITOR_POLICY;
    private final static PolicyFactory MINIMAL_EDITOR_POLICY;
    private final static PolicyFactory PDF_POLICY;

    private final static PolicyFactory TABLES_EXTENDED = new HtmlPolicyBuilder()
            .allowElements("td").allowAttributes("colspan", "rowspan").onElements("td").toFactory();

    private final static PolicyFactory LINKS_WITH_TARGET = new HtmlPolicyBuilder()
            .allowElements((elementName, attrs) -> {
                int targetIndex = attrs.indexOf("target");
                if (targetIndex < 0) {
                    attrs.add("target");
                    attrs.add("_blank");
                } else {
                    attrs.set(targetIndex + 1, "_blank");
                }
                return elementName;
            }, "a")
            .allowStandardUrlProtocols()
            .allowAttributes("href", "target").onElements("a").requireRelsOnLinks("noopener", "noreferrer", "nofollow")
            .toFactory();

    private final static PolicyFactory STYLES_EXTENDED = new HtmlPolicyBuilder()
            .allowStyling(CssSchema.union(CssSchema.DEFAULT, CssSchema.withProperties(Set.of("display", "float")))).toFactory();

    static {
        FULL_EDITOR_POLICY = BLOCKS.and(FORMATTING).and(IMAGES).and(TABLES).and(TABLES_EXTENDED).and(LINKS_WITH_TARGET).and(STYLES_EXTENDED);
        MINIMAL_EDITOR_POLICY = BLOCKS.and(FORMATTING).and(LINKS_WITH_TARGET).and(STYLES);
        PDF_POLICY = FULL_EDITOR_POLICY;
    }

    /**
     * Sanitize HTML content coming from a complete editor (e.g. legal notices).
     *
     * @param unsanitizedInput The unsafe input.
     * @return The safe input.
     */
    public static String sanitizeEditorContent(String unsanitizedInput) {
        return FULL_EDITOR_POLICY.sanitize(unsanitizedInput);
    }

    /**
     * Sanitize HTML content coming from a minimal editor (e.g. feedback form).
     *
     * @param unsanitizedInput The unsafe input.
     * @return The safe input.
     */
    public static String sanitizeMinimalEditorContent(String unsanitizedInput) {
        return MINIMAL_EDITOR_POLICY.sanitize(unsanitizedInput);
    }

    /**
     * Sanitize HTML content coming from an editor to be included in PDF reports (e.g. conformance certificate).
     *
     * @param unsanitizedInput The unsafe input.
     * @return The safe input.
     */
    public static String sanitizePdfContent(String unsanitizedInput) {
        return PDF_POLICY.sanitize(unsanitizedInput);
    }

}
