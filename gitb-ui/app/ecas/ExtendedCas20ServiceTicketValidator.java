package ecas;

import org.jasig.cas.client.util.XmlUtils;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ExtendedCas20ServiceTicketValidator extends Cas20ServiceTicketValidator {

    /**
     * Constructs an instance of the CAS 2.0 Service Ticket Validator with the supplied
     * CAS server url prefix.
     *
     * @param casServerUrlPrefix the CAS Server URL prefix.
     */
    public ExtendedCas20ServiceTicketValidator(String casServerUrlPrefix) {
        super(casServerUrlPrefix);
        setCustomParameters(Collections.singletonMap("userDetails", "true"));
    }

    @Override
    protected Map<String, Object> extractCustomAttributes(final String xml) {
        Map<String, Object> attributes = super.extractCustomAttributes(xml);
        if (attributes == null) {
            attributes = new HashMap<>(3);
        }
        // TODO parse EU login attributes here.
        attributes.put("email", XmlUtils.getTextForElement(xml, "email"));
        attributes.put("firstName", XmlUtils.getTextForElement(xml, "firstName"));
        attributes.put("lastName", XmlUtils.getTextForElement(xml, "lastName"));
        return attributes;
    }

}
