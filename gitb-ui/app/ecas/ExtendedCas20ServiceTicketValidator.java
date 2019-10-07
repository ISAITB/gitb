package ecas;

import config.Configurations;
import org.jasig.cas.client.util.XmlUtils;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom ticket validator to ensure that the features of the ECAS client are reproduced.
 * These are notably:
 * - The request of the user details.
 * - The ticket validation URL to allow also self-registered users.
 */
public class ExtendedCas20ServiceTicketValidator extends Cas20ServiceTicketValidator {

    /**
     * Constructs an instance of the CAS 2.0 Service Ticket Validator with the supplied
     * CAS server url prefix.
     *
     * @param casServerUrlPrefix the CAS Server URL prefix.
     */
    public ExtendedCas20ServiceTicketValidator(String casServerUrlPrefix) {
        super(casServerUrlPrefix);
        setCustomParameters(Collections.singletonMap(Configurations.AUTHENTICATION_SSO_CUSTOM_PARAMETERS__USER_DETAILS(), "true"));
    }

    @Override
    protected String getUrlSuffix() {
        return Configurations.AUTHENTICATION_SSO_TICKET_VALIDATION_URL_SUFFIX();
    }

    @Override
    protected Map<String, Object> extractCustomAttributes(final String xml) {
        Map<String, Object> attributes = super.extractCustomAttributes(xml);
        if (attributes == null) {
            attributes = new HashMap<>(3);
        }
        attributes.put("email", XmlUtils.getTextForElement(xml, Configurations.AUTHENTICATION_SSO_USER_ATTRIBUTES__EMAIL()));
        attributes.put("firstName", XmlUtils.getTextForElement(xml, Configurations.AUTHENTICATION_SSO_USER_ATTRIBUTES__FIRST_NAME()));
        attributes.put("lastName", XmlUtils.getTextForElement(xml, Configurations.AUTHENTICATION_SSO_USER_ATTRIBUTES__LAST_NAME()));
        return attributes;
    }

}
