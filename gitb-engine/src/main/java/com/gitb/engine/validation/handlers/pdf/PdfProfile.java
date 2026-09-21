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

import org.apache.commons.lang3.StringUtils;
import org.verapdf.pdfa.flavours.PDFAFlavour;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The PDF conformance profiles supported by the {@link PdfValidator}, and their mapping to the
 * corresponding veraPDF flavour.
 */
public enum PdfProfile {

    PDFA_1A(PDFAFlavour.PDFA_1_A),
    PDFA_1B(PDFAFlavour.PDFA_1_B),
    PDFA_2A(PDFAFlavour.PDFA_2_A),
    PDFA_2B(PDFAFlavour.PDFA_2_B),
    PDFA_2U(PDFAFlavour.PDFA_2_U),
    PDFA_3A(PDFAFlavour.PDFA_3_A),
    PDFA_3B(PDFAFlavour.PDFA_3_B),
    PDFA_3U(PDFAFlavour.PDFA_3_U),
    PDFA_4(PDFAFlavour.PDFA_4),
    PDFA_4E(PDFAFlavour.PDFA_4_E),
    PDFA_4F(PDFAFlavour.PDFA_4_F),
    PDFUA_1(PDFAFlavour.PDFUA_1);

    private final PDFAFlavour flavour;

    PdfProfile(PDFAFlavour flavour) {
        this.flavour = flavour;
    }

    public PDFAFlavour flavour() {
        return flavour;
    }

    private static final Map<String, PdfProfile> BY_NAME = new LinkedHashMap<>();
    static {
        for (PdfProfile profile: values()) {
            BY_NAME.put(profile.name(), profile);
        }
    }

    /**
     * Parse the raw values provided for the "profiles" input into the set of distinct profiles to check.
     * <p>
     * Each raw value may itself be a comma-separated list of identifiers. Matching is case-insensitive.
     * Values that do not correspond to a known profile are reported (trimmed, as originally provided)
     * to the supplied consumer and otherwise ignored.
     *
     * @param rawValues The raw (untrimmed, possibly comma-separated) profile values as provided in the input.
     * @param onInvalid Callback invoked (in encounter order) for each value that could not be matched to a known profile.
     * @return The distinct profiles to check, in the order they were first requested.
     */
    public static List<PdfProfile> parse(List<String> rawValues, Consumer<String> onInvalid) {
        var result = new LinkedHashSet<PdfProfile>();
        for (var rawValue: rawValues) {
            if (rawValue == null) {
                continue;
            }
            for (var part: rawValue.split(",")) {
                var trimmedPart = part.trim();
                if (StringUtils.isBlank(trimmedPart)) {
                    continue;
                }
                var profile = BY_NAME.get(trimmedPart.toUpperCase());
                if (profile == null) {
                    onInvalid.accept(trimmedPart);
                } else {
                    result.add(profile);
                }
            }
        }
        return new ArrayList<>(result);
    }

}
