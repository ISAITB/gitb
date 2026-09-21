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

package com.gitb.engine.validation.handlers.pdf;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The catalogue of rule identifiers used to report core (PDFBox-based) parsing findings.
 * <p>
 * {@link #UNPARSEABLE} and {@link #ENCRYPTED} are always reported as errors - the document could not
 * be opened at all, so no further parsing or profile validation is possible. All other rules describe
 * recoverable issues, whose severity depends on the validator's {@code strictMode} input: {@link Finding.Severity#ERROR}
 * when {@code strictMode} is enabled, {@link Finding.Severity#WARNING} otherwise.
 * <p>
 * Explicit checks against the PDFBox object model directly determine {@link #HEADER} and {@link #PAGES}.
 * Any other recoverable problem is only visible as a message logged by PDFBox itself (captured via
 * {@link PdfBoxLogCapture}) and is classified onto one of the remaining rules using {@link #classify(String)},
 * falling back to {@link #OTHER}.
 * <p>
 * This set only includes rules confirmed reachable against PDFBox 3.0.8 - each has a passing test in
 * {@code ValidationHandlersTest} demonstrating a real trigger. {@code CATALOG}, {@code FONT},
 * {@code TRAILER} and {@code EOF} rules were considered and dropped: {@code PDDocument.getDocumentCatalog()}
 * can never return null (confirmed from its source - it falls back to a synthetic empty catalog), no
 * PDFBox log message reachable from this validator's parse phase (loading plus content-stream
 * tokenization, no rendering or text extraction) ever mentions a font, and the sole log message each of
 * {@code TRAILER} and {@code EOF} could have mapped to could not be reproduced despite deliberate attempts
 * (from source inspection, {@code TRAILER}'s message is structurally unreachable - its only caller cannot
 * be positioned as the message requires).
 */
public enum CoreRule {

    UNPARSEABLE("CORE-UNPARSEABLE", "The content could not be parsed as a PDF document."),
    ENCRYPTED("CORE-ENCRYPTED", "The document is encrypted and no password was provided."),
    HEADER("CORE-HEADER", "The PDF header is missing, malformed, or declares an unexpected version."),
    XREF("CORE-XREF", "The cross-reference table or stream is damaged; the document structure was reconstructed."),
    PAGES("CORE-PAGES", "The page tree is missing, empty, or contains invalid entries."),
    OBJECT("CORE-OBJECT", "An indirect object could not be resolved at its declared offset."),
    CONTENT("CORE-CONTENT", "A page content stream could not be fully parsed."),
    OTHER("CORE-OTHER", "Other recoverable parsing issue reported by the parser.");

    private final String id;
    private final String defaultMessage;

    CoreRule(String id, String defaultMessage) {
        this.id = id;
        this.defaultMessage = defaultMessage;
    }

    public String id() {
        return id;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    /**
     * Ordered classification of a PDFBox log message onto one of the recoverable core rules. The first
     * matching entry wins, falling back to {@link #OTHER} if nothing matches.
     */
    private record Classifier(Pattern pattern, CoreRule rule) {}

    private static final List<Classifier> CLASSIFIERS = List.of(
            new Classifier(Pattern.compile("xref|cross-?reference|cross reference", Pattern.CASE_INSENSITIVE), XREF),
            new Classifier(Pattern.compile("header|%pdf-|version", Pattern.CASE_INSENSITIVE), HEADER),
            new Classifier(Pattern.compile("\\bpage(s)?\\b", Pattern.CASE_INSENSITIVE), PAGES),
            new Classifier(Pattern.compile("content stream|operator|token", Pattern.CASE_INSENSITIVE), CONTENT),
            new Classifier(Pattern.compile("object|offset", Pattern.CASE_INSENSITIVE), OBJECT)
    );

    public static CoreRule classify(String logMessage) {
        if (logMessage != null) {
            for (var classifier: CLASSIFIERS) {
                if (classifier.pattern().matcher(logMessage).find()) {
                    return classifier.rule();
                }
            }
        }
        return OTHER;
    }

}
