package utils;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import static org.owasp.html.Sanitizers.*;

/**
 * Class with static utilities to handle HTML content.
 */
public class HtmlUtil {

    private final static PolicyFactory FULL_EDITOR_POLICY;
    private final static PolicyFactory MINIMAL_EDITOR_POLICY;
    private final static PolicyFactory PDF_POLICY;

    public static final PolicyFactory LINKS_WITH_TARGET = new HtmlPolicyBuilder()
            .allowStandardUrlProtocols().allowElements("a")
            .allowAttributes("href", "target").onElements("a").requireRelNofollowOnLinks()
            .toFactory();

    static {
        FULL_EDITOR_POLICY = BLOCKS.and(FORMATTING).and(IMAGES).and(TABLES).and(LINKS_WITH_TARGET).and(STYLES);
        MINIMAL_EDITOR_POLICY = BLOCKS.and(FORMATTING).and(LINKS_WITH_TARGET).and(STYLES);
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
