package utils;

import org.owasp.html.PolicyFactory;
import static org.owasp.html.Sanitizers.*;

/**
 * Class with static utilities to handle HTML content.
 */
public class HtmlUtil {

    private static PolicyFactory FULL_EDITOR_POLICY;
    private static PolicyFactory MINIMAL_EDITOR_POLICY;
    private static PolicyFactory PDF_POLICY;

    static {
        FULL_EDITOR_POLICY = BLOCKS.and(FORMATTING).and(IMAGES).and(TABLES).and(LINKS).and(STYLES);
        MINIMAL_EDITOR_POLICY = BLOCKS.and(FORMATTING).and(LINKS).and(STYLES);
        PDF_POLICY = BLOCKS.and(FORMATTING).and(LINKS);
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
